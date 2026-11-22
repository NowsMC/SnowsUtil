package space.nows.util.text;

import space.nows.util.check.Preconditions;

import java.util.ArrayList;
import java.util.List;

/** Splitting on a single separator character and counting its occurrences. */
public final class StringSplit {
    private StringSplit() {}

    /** Splits on every {@code sep}; empty fields are kept, so {@code "a,,b"} gives three parts. */
    public static List<String> split(String s, char sep) {
        Preconditions.notNull(s, "s");
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
        Preconditions.notNull(s, "s");
        int n = 0;
        for (int i = 0, len = s.length(); i < len; i++) if (s.charAt(i) == c) n++;
        return n;
    }
}
