package cf.huzpsb.machinelearning;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class CloudAPI {
    public CloudAPI() {
        throw new UnsupportedOperationException("This is an util!");
    }

    public static int cloud_calc(String server, double[] input) {
        try (Socket socket = new Socket(server, AIServer.port)) {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.writeObject(new TestData(input));
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
            Object i = ois.readObject();
            if (i instanceof TestResult) {
                return ((TestResult) i).result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
}
