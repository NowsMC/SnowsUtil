package space.nows.util.text;

import java.util.ArrayList;
import java.util.List;

public final class StringUtils {
    private StringUtils() {}

    private static final char[] HEX = "0123456789abcdef".toCharArray();

    public static boolean isEmpty(CharSequence s) { return s == null || s.length() == 0; }

    public static boolean isBlank(CharSequence s) {
        if (s == null) return true;
        for (int i = 0, n = s.length(); i < n; i++) if (!Character.isWhitespace(s.charAt(i))) return false;
        return true;
    }

    public static List<String> split(String s, char sep) {
        List<String> out = new ArrayList<>();
        int start = 0;
        for (int i = 0, n = s.length(); i < n; i++) {
            if (s.charAt(i) == sep) {
                out.add(s.substring(start, i));
                start = i + 1;
            }
        }
        out.add(s.substring(start));
        return out;
    }

    public static int count(String s, char c) {
        int n = 0;
        for (int i = 0, len = s.length(); i < len; i++) if (s.charAt(i) == c) n++;
        return n;
    }

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

    public static String padLeft(String s, int width, char pad) {
        int missing = width - s.length();
        if (missing <= 0) return s;
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < missing; i++) sb.append(pad);
        return sb.append(s).toString();
    }

    public static String padRight(String s, int width, char pad) {
        int missing = width - s.length();
        if (missing <= 0) return s;
        StringBuilder sb = new StringBuilder(width).append(s);
        for (int i = 0; i < missing; i++) sb.append(pad);
        return sb.toString();
    }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        char c = s.charAt(0);
        char up = Character.toUpperCase(c);
        return c == up ? s : up + s.substring(1);
    }

    public static String toHex(byte[] data) {
        char[] out = new char[data.length * 2];
        for (int i = 0; i < data.length; i++) {
            int v = data[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    public static byte[] fromHex(String hex) {
        int n = hex.length();
        if ((n & 1) != 0) throw new IllegalArgumentException("odd length: " + n);
        byte[] out = new byte[n / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = Character.digit(hex.charAt(i * 2), 16);
            int lo = Character.digit(hex.charAt(i * 2 + 1), 16);
            if (hi < 0 || lo < 0) throw new IllegalArgumentException("invalid hex at " + (i * 2));
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }
}
