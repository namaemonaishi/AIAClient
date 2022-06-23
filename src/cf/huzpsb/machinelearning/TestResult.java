package cf.huzpsb.machinelearning;

import java.io.Serializable;

public class TestResult implements Serializable {
    private static final long serialVersionUID = 114514233332L;
    public int result;

    public TestResult(int i) {
        result = i;
    }
}
