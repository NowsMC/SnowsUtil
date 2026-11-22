package space.nows.util.text;

import space.nows.util.error.Checks;

import java.util.ArrayList;
import java.util.List;

/** Allocation-light, null-aware string helpers. Methods that take a {@code String} reject {@code null} with a named {@link NullPointerException}. */
public final class StringUtils {
    private StringUtils() {}

    public static boolean isEmpty(CharSequence s) { return s == null || s.length() == 0; }

    public static boolean isBlank(CharSequence s) {
        if (s == null) return true;
        for (int i = 0, n = s.length(); i < n; i++) if (!Character.isWhitespace(s.charAt(i))) return false;
        return true;
    }

    /** Returns {@code s}, or {@code fallback} if {@code s} is null or blank. */
    public static String defaultIfBlank(String s, String fallback) { return isBlank(s) ? fallback : s; }

    public static List<String> split(String s, char sep) {
        Checks.notNull(s, "s");
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
        Checks.notNull(s, "s");
        int n = 0;
        for (int i = 0, len = s.length(); i < len; i++) if (s.charAt(i) == c) n++;
        return n;
    }

    public static String padLeft(String s, int width, char pad) {
        Checks.notNull(s, "s");
        int missing = width - s.length();
        if (missing <= 0) return s;
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < missing; i++) sb.append(pad);
        return sb.append(s).toString();
    }

    public static String padRight(String s, int width, char pad) {
        Checks.notNull(s, "s");
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

    /** At most {@code maxLength} chars; never leaves half of a surrogate pair at the cut. */
    public static String truncate(String s, int maxLength) {
        Checks.notNull(s, "s");
        Checks.nonNegative(maxLength, "maxLength");
        if (s.length() <= maxLength) return s;
        int end = maxLength;
        if (end > 0 && Character.isHighSurrogate(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }
}
