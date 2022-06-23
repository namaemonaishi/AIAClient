package cf.huzpsb.cm;

public class math {
    public static double FFT_cos(double[] raw, int j) {
        int n = raw.length / 2;
        double ans = 0;
        for (int i = 0; i < 2 * n; i++) {
            ans = ans + raw[i] * Math.cos(Math.PI * j * i / n);
        }
        return ans / n;
    }

    public static double FFT_sin(double[] raw, int j) {
        int n = raw.length / 2;
        double ans = 0;
        for (int i = 0; i < 2 * n; i++) {
            ans = ans + raw[i] * Math.sin(Math.PI * j * i / n);
        }
        return ans / n;
    }

    public static double FFT_power(double[] raw, int j) {
        double cos = FFT_cos(raw, j);
        double sin = FFT_sin(raw, j);
        return Math.sqrt((sin * sin + cos * cos));
    }

    private static double max(double a, double b) {
        if (a > b) {
            return a;
        }
        return b;
    }

    public static double abs(double a) {
        if (a > 0) {
            return a;
        }
        return -a;
    }

    private static boolean sm(double a, double b) {
        if (a > 0) {
            return b > 0;
        }
        return b < 0;
    }

    public static double UnsignedHitbox(double m, double n, double a, double b) {
        double absm = abs(m);
        double absn = abs(n);
        double absa = abs(a);
        double absb = abs(b);
        double absab = max(a, b);
        double k;
        if (absm > 1000 * absn) {
            if (sm(a, m)) {
                return absb;
            }
            return absab;
        }
        if (absn > 1000 * absm) {
            if (sm(b, n)) {
                return absa;
            }
            return absab;
        }
        if (m == n) {
            k = 1;
        } else {
            k = n / m;
        }
        if (k > 0) {
            if (sm(a + b, m)) {
                return abs(a - ((a + b) / (k + 1)));
            }
        } else {
            if (sm(a - b, m)) {
                return abs(b - ((k * (b - a)) / (k - 1)));
            }
        }
        return absab;
    }

    public static double SignedHitbox(double m, double n, double a, double b) {
        int flag;
        if (((m * b) + (n * a)) < 0) {
            flag = -1;
        } else {
            flag = 1;
        }
        return flag * UnsignedHitbox(m, n, a, b);
    }
}
