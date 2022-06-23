package cf.huzpsb.cm;

import cf.huzpsb.machinelearning.CloudAPI;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class main extends JavaPlugin implements Listener {
    private final Map<UUID, player> map = new ConcurrentHashMap<>();
    private String server = "unknown";
    private int port = -1;
    private boolean debug = false;
    private String cmd = "ban %p";

    @Override
    public void onEnable() {
        saveDefaultConfig();
        debug = getConfig().getBoolean("debug");
        server = getConfig().getString("server");
        port = getConfig().getInt("port");
        cmd = getConfig().getString("cmd");
        Bukkit.getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void playerJoinEvent(PlayerJoinEvent e) {
        player pl = new player();
        map.put(e.getPlayer().getUniqueId(), pl);
    }

    @EventHandler
    public void playerQuitEvent(PlayerQuitEvent e) {
        map.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onHit(EntityDamageByEntityEvent e) {
        if (!(e.getDamager() instanceof Player)) {
            return;
        }
        Player evp = (Player) e.getDamager();
        if (!map.containsKey(evp.getUniqueId())) {
            return;
        }
        player p = map.get(evp.getUniqueId());
        if (p.pt == p.angle.length) {
            return;
        }
        Vector eye = evp.getEyeLocation().getDirection();
        double fx = eye.getX();
        double fz = eye.getZ();
        double rx = e.getEntity().getLocation().getX() - evp.getLocation().getX();
        double rz = e.getEntity().getLocation().getZ() - evp.getLocation().getZ();
        if ((fx * fx + fz * fz) < 0.0001) {
            if (debug) {
                evp.sendMessage("[Exception] Eye div 0");
            }
            p.pt = 0;
            map.put(evp.getUniqueId(), p);
            return;
        }
        if ((rx * rx + rz * rz) < 0.0001) {
            if (debug) {
                evp.sendMessage("[Exception] Entity div 0");
            }
            p.pt = 0;
            map.put(evp.getUniqueId(), p);
            return;
        }
        double value = math.SignedHitbox(fx, fz, rx, rz);
        p.angle[p.pt] = value;
        p.pt = p.pt + 1;
        map.put(evp.getUniqueId(), p);

        if (p.pt == p.angle.length) {
            process(evp);
            p = map.get(evp.getUniqueId());
            p.vl -= 2;
            if (p.vl < 0) {
                p.vl = 0;
            }
            map.put(evp.getUniqueId(), p);
            if (debug) {
                evp.sendMessage("VL -> " + p.vl);
            }
            if (p.vl >= 15) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("%p", evp.getDisplayName()));
            }
        }
    }

    private void process(Player u) {
        player p = map.get(u.getUniqueId());
        p.pt = 0;
        double[] fft_power = new double[p.angle.length];
        double[] fft_powered = new double[p.angle.length];
        for (int x = 0; x < p.angle.length; x++) {
            fft_power[x] = math.FFT_power(p.angle, x);
        }
        double sum = 0;
        for (int x = 0; x < p.angle.length; x++) {
            sum = sum + fft_power[x];
        }
        sum = 100 / sum;
        for (int x = 0; x < p.angle.length; x++) {
            fft_powered[x] = fft_power[x] * sum;
        }
        if (debug) {
            if (p.label != null) {
                try {
                    File folder = new File("AI-AC");
                    if (!folder.exists()) {
                        folder.mkdir();
                    }
                    BufferedWriter out = new BufferedWriter(new FileWriter(new File(folder, u.getName() + System.currentTimeMillis() + ".csv")));
                    for (int i = 0; i < p.angle.length; i++) {
                        out.write(fft_powered[i] + ",");
                    }
                    out.write(p.label);
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                u.sendMessage("Use /type <label> to tag yourself!");
            }
        }
        int result = CloudAPI.cloud_calc(server, port, fft_powered);
        if (result == 1) {
            p.vl += 5;
        } else if (result == 2) {
            p.vl += 7;
        }
        map.put(u.getUniqueId(), p);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (debug) {
            if (!(sender instanceof Player)) {
                sender.sendMessage("You must be a player!");
                return true;
            }
            if (!map.containsKey(((Player) sender).getUniqueId())) {
                return true;
            }
            if (args.length != 1) {
                return false;
            }
            player p = map.get(((Player) sender).getUniqueId());
            p.label = args[0];
            sender.sendMessage("Label set!");
        }
        return true;
    }
}
