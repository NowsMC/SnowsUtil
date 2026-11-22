package space.nows.util.text;

import space.nows.util.check.Preconditions;

/** Null-aware string checks, defaults, capitalization and truncation. */
public final class Strings {
    private Strings() {}

    public static boolean isEmpty(CharSequence s) { return s == null || s.length() == 0; }

    public static boolean isBlank(CharSequence s) {
        if (s == null) return true;
        for (int i = 0, n = s.length(); i < n; i++) if (!Character.isWhitespace(s.charAt(i))) return false;
        return true;
    }

    /** Returns {@code s}, or {@code fallback} if {@code s} is null or blank. */
    public static String defaultIfBlank(String s, String fallback) { return isBlank(s) ? fallback : s; }

    public static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        char c = s.charAt(0);
        char up = Character.toUpperCase(c);
        return c == up ? s : up + s.substring(1);
    }

    /** At most {@code maxLength} chars; never leaves half of a surrogate pair at the cut. */
    public static String truncate(String s, int maxLength) {
        Preconditions.notNull(s, "s");
        Preconditions.nonNegative(maxLength, "maxLength");
        if (s.length() <= maxLength) return s;
        int end = maxLength;
        if (end > 0 && Character.isHighSurrogate(s.charAt(end - 1))) end--;
        return s.substring(0, end);
    }
}
