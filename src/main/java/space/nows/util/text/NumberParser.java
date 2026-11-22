package space.nows.util.text;

/**
 * Lenient number parsing that returns a fallback instead of throwing. Integer parsers accept an optional
 * sign followed by ASCII digits only, and never use exceptions for control flow.
 */
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
        if (s == null) return fallback;
        int len = s.length(), i = 0;
        if (len == 0) return fallback;
        boolean neg = false;
        char c0 = s.charAt(0);
        if (c0 == '-' || c0 == '+') {
            neg = c0 == '-';
            if (++i == len) return fallback;
        }
        long limit = neg ? Long.MIN_VALUE : -Long.MAX_VALUE;
        long multMin = limit / 10;
        long result = 0;
        for (; i < len; i++) {
            int d = s.charAt(i) - '0';
            if (d < 0 || d > 9) return fallback;
            if (result < multMin) return fallback;
            result *= 10;
            if (result < limit + d) return fallback;
            result -= d;
        }
        return neg ? result : -result;
    }

    /** {@code "true"} or {@code "false"} in any case; anything else returns {@code fallback}. */
    public static boolean parseBoolean(String s, boolean fallback) {
        if (s == null) return fallback;
        if (s.equalsIgnoreCase("true")) return true;
        if (s.equalsIgnoreCase("false")) return false;
        return fallback;
    }

    /** Accepts exactly what {@link Double#parseDouble(String)} accepts (including {@code NaN}, exponents and surrounding whitespace). */
    public static double parseDouble(String s, double fallback) {
        if (s == null || s.isEmpty()) return fallback;
        try {
            return Double.parseDouble(s);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }
}
