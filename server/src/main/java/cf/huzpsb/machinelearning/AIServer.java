package cf.huzpsb.machinelearning;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class AIServer {
    private static final Logger l = LoggerFactory.getLogger(AIServer.class);
    private static final Map<String, IPinfo> map = new ConcurrentHashMap<>();
    private static final File f = new File("users");
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static int port = 0;
    private static final Runnable selfRun = () -> {
        try {
            Thread.sleep(5000L);
        } catch (Exception e) {
            e.printStackTrace();
        }
        l.error("[Self-Test] Sending test data...");
        l.error("[Self-Test] " + (test() ? "Success!" : "Warning!"));
    };
    private static int threadLimit = 0;
    private static long lastLog = System.currentTimeMillis();
    private static MultiLayerNetwork reloadModel;
    private static int threads = 0;

    public static void main(String[] args) throws Exception {
        l.error("[INFO] Starting AI-AC Server...");
        File set = new File("server.properties");
        if (!set.exists()) {
            Properties pr = new Properties();
            pr.setProperty("port", "1451");
            pr.setProperty("threads", "3");
            pr.store(new FileWriter(set), "AI-AC Server Settings");
        }
        Properties pr = new Properties();
        pr.load(new FileReader(set));
        threadLimit = Integer.parseInt(pr.getProperty("threads"));
        port = Integer.parseInt(pr.getProperty("port"));
        long stamp = System.currentTimeMillis();
        init();
        loadData();
        Thread selfTest = new Thread(selfRun);
        selfTest.start();
        Thread server = new Thread(() -> {
            while (true) {
                try (ServerSocket serverSocket = new ServerSocket(port)) {
                    final Socket socket = serverSocket.accept();
                    String s = socket.getInetAddress().getHostAddress();
                    final boolean log;
                    if (System.currentTimeMillis() - lastLog > 200) {
                        log = true;
                        lastLog = System.currentTimeMillis();
                    } else {
                        log = false;
                    }
                    if (log) {
                        l.error("[INFO] Income connection:" + socket);
                    }
                    try {
                        if (!cid(s)) {
                            socket.close();
                            if (log) {
                                l.error("[INFO] License not validated for:" + s);
                            }
                        } else {
                            if (threads < threadLimit) {
                                socket.setKeepAlive(true);
                                Thread sc = new Thread(() -> {
                                    threads++;
                                    try {
                                        ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
                                        Object i = ois.readObject();
                                        if (i instanceof TestData) {
                                            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
                                            oos.writeObject(new TestResult(calc(((TestData) i).awx)));
                                        }
                                    } catch (Exception e) {
                                        if (log) {
                                            e.printStackTrace();
                                        }
                                    }
                                    threads--;
                                });
                                sc.start();
                            } else {
                                if (log) {
                                    l.error("[ERROR] Exceed the maximum number of threads!");
                                }
                                socket.close();
                            }
                        }
                    } catch (Exception e) {
                        if (log) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    l.error("[ERROR] Can not handle the request!");
                    break;
                }
            }
            System.exit(-1);
        });
        server.start();

        l.error("[INFO] Done!(" + (System.currentTimeMillis() - stamp) + " ms) Listening on:" + port);
        full:
        while (true) {
            try {
                Scanner s = new Scanner(System.in);
                String c = s.nextLine();
                String[] cmd = c.split(" ");
                switch (cmd[0]) {
                    case "stop":
                    case "exit":
                        break full;
                    case "set":
                        if (cmd.length == 5) {
                            File fl = new File(f, cmd[1]);
                            if (fl.exists()) {
                                fl.delete();
                            }
                            PrintStream p = new PrintStream(Files.newOutputStream(fl.toPath()));
                            int limit = Integer.parseInt(cmd[4]);
                            long time = System.currentTimeMillis() + 86400000L * Integer.parseInt(cmd[2]);
                            p.println(cmd[3]);
                            p.println(time);
                            p.println(limit);
                            IPinfo i = new IPinfo();
                            i.tle = time;
                            i.f = fl;
                            i.call = limit;
                            p.close();
                            map.put(cmd[3], i);
                            l.error("[INFO] Done!");
                        } else {
                            l.error("[INFO] Command usage:set username duration(day) ip-address call-limit");
                        }
                        break;
                    case "list":
                        l.error("[INFO] Begin to print the user table...");
                        l.error("name ipaddress call-limit time-limit");
                        for (String str : map.keySet()) {
                            IPinfo i = map.get(str);
                            l.error(i.f.getName() + " " + str + " " + i.call + " " + simpleDateFormat.format(new Date(i.tle)));
                        }
                        l.error("[INFO] End of the user table...");
                        break;
                    case "get":
                        if (cmd.length == 2) {
                            String find = null;
                            for (String string : map.keySet()) {
                                IPinfo i = map.get(string);
                                if (i.f.getName().equalsIgnoreCase(cmd[1])) {
                                    find = string;
                                    break;
                                }
                            }
                            if (find != null) {
                                IPinfo i = map.get(find);
                                l.error("[INFO] Begin to print the user table...");
                                l.error("name ipaddress call-limit time-limit");
                                l.error(i.f.getName() + " " + cmd[1] + " " + i.call + " " + simpleDateFormat.format(new Date(i.tle)));
                                l.error("[INFO] End of the user table...");
                            } else {
                                l.error("[WARNING] The user does not exists!");
                            }
                        } else {
                            l.error("[INFO] Command usage:get user");
                        }
                        break;
                    case "remove":
                    case "ban":
                        if (cmd.length == 2) {
                            String find = null;
                            for (String string : map.keySet()) {
                                IPinfo i = map.get(string);
                                if (i.f.getName().equalsIgnoreCase(cmd[1])) {
                                    find = string;
                                    break;
                                }
                            }
                            if (find != null) {
                                IPinfo i = map.get(find);
                                map.remove(find);
                                i.f.delete();
                                l.error("[INFO] Done!");
                            } else {
                                l.error("[WARNING] The user does not exists!");
                            }
                        } else {
                            l.error("[INFO] Command usage:remove user");
                        }
                        break;
                    case "help":
                        l.error("[INFO] Available commands:about,exit,get,help,list,remove,set,test,train");
                        break;
                    case "test":
                        Thread selfTest2 = new Thread(selfRun);
                        selfTest2.start();
                        break;
                    case "about":
                    case "ver":
                        l.error("[INFO] AI-AC Server By:huzpsb (c)2022 All rights reserved");
                        break;
                    case "train":
                        if (cmd.length != 3) {
                            l.error("[INFO] AI-AC Training Wizard");
                            l.error("[WARNING] You will receive NO support regarding this util!Use at your own risk.");
                            l.error("[INFO] 1) Please merge your csv data collected by ai-ac plugin into 1 csv file.Per line per data.");
                            l.error("[INFO] 2) Please wash out datapoint that contains extremely edge data.");
                            l.error("[INFO] 3) Please make sure that you have EXACTLY 1000 lines of data.");
                            l.error("[INFO] 4) Please make sure that the same type of data is placed together.");
                            l.error("[INFO] 5) Please edit the label. 0-Legit 1-Cheat 2-(Legacy)Edged-Situation");
                            l.error("[INFO] 6) Please rename the csv file as \"hs.csv\".");
                            l.error("[INFO] 7) Please type \"train epoch learning-rate\" to start training.");
                            l.error("[WARNING] Note that the model should only be custom-trained ONCE.");
                        } else {
                            l.error("[INFO] Training started.");
                            int epoch = Integer.parseInt(cmd[1]);
                            double lr = Double.parseDouble(cmd[2]);
                            if (epoch * lr > 1 || epoch <= 0 || lr <= 0) {
                                l.error("[INFO] Training rate too high or invalid.");
                                break;
                            }
                            RecordReader recordReader = new CSVRecordReader(0, ',');
                            recordReader.initialize(new FileSplit(new File("hs.csv")));
                            DataSetIterator iterator = new RecordReaderDataSetIterator(recordReader, 1000, 22, 3);
                            DataSet allData = iterator.next();
                            reloadModel.setLearningRate(lr);
                            reloadModel.fit(allData);
                            //预训练一次后再强化
                            allData.shuffle();
                            SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(0.8);
                            DataSet trainingData = testAndTrain.getTrain();
                            DataSet testData = testAndTrain.getTest();
                            //强化用数据
                            Evaluation eval = new Evaluation(3);
                            INDArray output = reloadModel.output(testData.getFeatures());
                            eval.eval(testData.getLabels(), output);
                            l.error("[INFO] Pre-status.");
                            System.out.println(eval.stats());

                            for (int i = 0; i < epoch; i++) {
                                reloadModel.fit(trainingData);
                                Thread.sleep(10000L);
                            }
                            reloadModel.save(new File("diy_mlp.mod"));
                            output = reloadModel.output(testData.getFeatures());
                            eval = new Evaluation(3);
                            eval.eval(testData.getLabels(), output);
                            l.error("[INFO] Post-status.");
                            System.out.println(eval.stats());
                            l.error("[INFO] Training completed. File saved as: \"diy_mlp.mod\". Shutting down.");
                            break full;
                        }
                        break;
                    default:
                        l.error("[ERROR] Unknown command.Type help for help.");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        save();
        System.exit(0);
    }

    public static void init() {
        try {
            reloadModel = ModelSerializer.restoreMultiLayerNetwork(new File("mlp.mod"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static boolean test() {
        try {
            double[] magic = {14.66810786, 2.351020803, 5.427088556, 2.319755834, 2.907537865, 4.391293943, 5.486874762, 7.75301449, 1.927732205, 4.543850619, 5.416641483, 0.282271024, 5.416641483, 4.543850619, 1.927732205, 7.75301449, 5.486874762, 4.391293943, 2.907537865, 2.319755834, 5.427088556, 2.351020803};
            return (CloudAPI.cloud_calc("127.0.0.1", magic) == 1);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static int calc(double[] input) {
        double[][] data = {input};
        INDArray test = Nd4j.create(data);
        int[] result = reloadModel.predict(test);
        return result[0];
    }

    private static void loadData() throws Exception {

        if (!f.exists()) {
            f.mkdir();
        }
        File[] users = f.listFiles();
        if (users == null) {
            return;
        }
        for (File usr : users) {
            Scanner s = new Scanner(usr);
            String adder = s.nextLine();
            long l = s.nextLong();
            int c = s.nextInt();
            IPinfo ipi = new IPinfo();
            ipi.tle = l;
            ipi.call = c;
            ipi.f = usr;
            map.put(adder, ipi);
            s.close();
        }
    }

    private static void save() throws Exception {
        for (String str : map.keySet()) {
            IPinfo i = map.get(str);
            i.f.delete();
            if (System.currentTimeMillis() <= i.tle) {
                PrintStream p = new PrintStream(Files.newOutputStream(i.f.toPath()));
                p.println(str);
                p.println(i.tle);
                p.println(i.call);
                p.close();
            }
        }
    }

    private static boolean cid(String s) {
        if (!map.containsKey(s)) {
            return false;
        }
        IPinfo i = map.get(s);
        i.call--;
        if (i.call <= 0) {
            map.remove(s);
            i.f.delete();
            return false;
        }
        if (System.currentTimeMillis() > i.tle) {
            map.remove(s);
            i.f.delete();
            return false;
        }
        return true;
    }
}
