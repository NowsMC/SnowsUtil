package space.nows.util.text;

import space.nows.util.check.Preconditions;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Escaping for double-quoted text in JSON and TOML basic strings. Quotes and backslashes are escaped, short forms are
 * used where they exist, and other control characters and unpaired surrogates become four-digit
 * hexadecimal Unicode escapes. Valid surrogate pairs are written as-is, so the output is always valid UTF-16 text.
 */
public final class Escapes {
    private Escapes() {}

    /** Appends {@code s} wrapped in double quotes and escaped. */
    public static void appendQuoted(CharSequence s, Appendable out) throws IOException {
        Preconditions.notNull(s, "s");
        Preconditions.notNull(out, "out");
        out.append('"');
        for (int i = 0, n = s.length(); i < n; i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> out.append("\\\"");
                case '\\' -> out.append("\\\\");
                case '\b' -> out.append("\\b");
                case '\f' -> out.append("\\f");
                case '\n' -> out.append("\\n");
                case '\r' -> out.append("\\r");
                case '\t' -> out.append("\\t");
                default -> {
                    if (c < 0x20) {
                        appendUnicode(c, out);
                    } else if (!Character.isSurrogate(c)) {
                        out.append(c);
                    } else if (Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(s.charAt(i + 1))) {
                        out.append(c).append(s.charAt(++i));
                    } else {
                        appendUnicode(c, out);
                    }
                }
            }
        }
        out.append('"');
    }

    /** Returns {@code s} wrapped in double quotes and escaped. */
    public static String quote(CharSequence s) {
        StringBuilder out = new StringBuilder(s.length() + 2);
        try {
            appendQuoted(s, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toString();
    }

    private static void appendUnicode(char c, Appendable out) throws IOException {
        out.append("\\u")
                .append(Hex.lowerDigit(c >>> 12))
                .append(Hex.lowerDigit(c >>> 8))
                .append(Hex.lowerDigit(c >>> 4))
                .append(Hex.lowerDigit(c));
    }
}
