package space.nows.util.text;

import java.util.ArrayList;
import java.util.List;

/** Allocation-light string helpers: blank checks, split, count, padding, capitalize. */
public final class StringUtils {
    private StringUtils() {}

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
}
