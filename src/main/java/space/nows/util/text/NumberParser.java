package space.nows.util.text;

/** Lenient number parsing that returns a fallback instead of throwing. */
public final class NumberParser {
    private NumberParser() {}

    public static int parseInt(String s, int fallback) {
        if (s == null) return fallback;
        int len = s.length(), i = 0;
        if (len == 0) return fallback;
        boolean neg = false;
        char c0 = s.charAt(0);
        if (c0 == '-' || c0 == '+') {
            neg = c0 == '-';
            if (++i == len) return fallback;
        }
        long result = 0;
        for (; i < len; i++) {
            int d = s.charAt(i) - '0';
            if (d < 0 || d > 9) return fallback;
            result = result * 10 + d;
            if (result > 2147483648L) return fallback;
        }
        if (neg) result = -result;
        return (result < Integer.MIN_VALUE || result > Integer.MAX_VALUE) ? fallback : (int) result;
    }

    public static long parseLong(String s, long fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
