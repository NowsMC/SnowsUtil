package space.nows.util.text;

import space.nows.util.check.Preconditions;

/** Pads a string with a character on the left or right up to a target width. */
public final class Padding {
    private Padding() {}

    public static String padLeft(String s, int width, char pad) {
        Preconditions.notNull(s, "s");
        int missing = width - s.length();
        if (missing <= 0) return s;
        StringBuilder sb = new StringBuilder(width);
        for (int i = 0; i < missing; i++) sb.append(pad);
        return sb.append(s).toString();
    }

    public static String padRight(String s, int width, char pad) {
        Preconditions.notNull(s, "s");
        int missing = width - s.length();
        if (missing <= 0) return s;
        StringBuilder sb = new StringBuilder(width).append(s);
        for (int i = 0; i < missing; i++) sb.append(pad);
        return sb.toString();
    }
}
