package cf.huzpsb.machinelearning;

import java.io.Serializable;

public class TestData implements Serializable {
    private static final long serialVersionUID = 114514233331L;
    public double[] awx = null;

    public TestData(double[] d) {
        awx = d;
    }
}
