package space.nows.util.text;

import space.nows.util.check.Preconditions;
import space.nows.util.error.InvalidUtf8Exception;

/**
 * Allocation-light UTF-8 encoding and strict decoding. Encoding writes unpaired surrogates as {@code '?'}, the same
 * replacement {@link String#getBytes(java.nio.charset.Charset)} uses. Decoding rejects anything that is not well-formed
 * (overlong forms, surrogate code points, values above U+10FFFF, truncated or stray continuation bytes).
 */
public final class Utf8 {
    private Utf8() {}

    public static int encodedLength(CharSequence s) {
        Preconditions.notNull(s, "s");
        int n = 0;
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            if (c < 0x80) {
                n += 1;
            } else if (c < 0x800) {
                n += 2;
            } else if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(s.charAt(i + 1))) {
                n += 4;
                i++;
            } else if (Character.isSurrogate(c)) {
                n += 1;
            } else {
                n += 3;
            }
        }
        return n;
    }

    /** Writes the encoding of {@code s} at {@code dst[off]} and returns the number of bytes written. */
    public static int encode(CharSequence s, byte[] dst, int off) {
        Preconditions.notNull(s, "s");
        Preconditions.notNull(dst, "dst");
        int p = off;
        for (int i = 0, len = s.length(); i < len; i++) {
            char c = s.charAt(i);
            if (c < 0x80) {
                dst[p++] = (byte) c;
            } else if (c < 0x800) {
                dst[p++] = (byte) (0xC0 | (c >> 6));
                dst[p++] = (byte) (0x80 | (c & 0x3F));
            } else if (Character.isHighSurrogate(c) && i + 1 < len && Character.isLowSurrogate(s.charAt(i + 1))) {
                int cp = Character.toCodePoint(c, s.charAt(++i));
                dst[p++] = (byte) (0xF0 | (cp >> 18));
                dst[p++] = (byte) (0x80 | ((cp >> 12) & 0x3F));
                dst[p++] = (byte) (0x80 | ((cp >> 6) & 0x3F));
                dst[p++] = (byte) (0x80 | (cp & 0x3F));
            } else if (Character.isSurrogate(c)) {
                dst[p++] = '?';
            } else {
                dst[p++] = (byte) (0xE0 | (c >> 12));
                dst[p++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                dst[p++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        return p - off;
    }

    /**
     * Decodes {@code len} bytes starting at {@code off}.
     *
     * @throws InvalidUtf8Exception if the bytes are not well-formed UTF-8
     */
    public static String decode(byte[] src, int off, int len) {
        Preconditions.notNull(src, "src");
        Preconditions.range(off, len, src.length);
        char[] out = new char[len];
        int n = 0;
        int i = off;
        int end = off + len;
        while (i < end) {
            int b = src[i] & 0xFF;
            if (b < 0x80) {
                out[n++] = (char) b;
                i++;
                continue;
            }
            int extra;
            int cp;
            int min;
            if (b >= 0xC2 && b <= 0xDF) {
                extra = 1;
                cp = b & 0x1F;
                min = 0x80;
            } else if (b >= 0xE0 && b <= 0xEF) {
                extra = 2;
                cp = b & 0x0F;
                min = 0x800;
            } else if (b >= 0xF0 && b <= 0xF4) {
                extra = 3;
                cp = b & 0x07;
                min = 0x10000;
            } else {
                throw new InvalidUtf8Exception("invalid lead byte", i);
            }
            if (end - i <= extra) throw new InvalidUtf8Exception("truncated sequence", i);
            for (int k = 1; k <= extra; k++) {
                int c = src[i + k] & 0xFF;
                if ((c & 0xC0) != 0x80) throw new InvalidUtf8Exception("invalid continuation byte", i + k);
                cp = (cp << 6) | (c & 0x3F);
            }
            if (cp < min || cp > Character.MAX_CODE_POINT || (cp >= 0xD800 && cp <= 0xDFFF)) {
                throw new InvalidUtf8Exception("invalid code point", i);
            }
            if (cp >= 0x10000) {
                out[n++] = Character.highSurrogate(cp);
                out[n++] = Character.lowSurrogate(cp);
            } else {
                out[n++] = (char) cp;
            }
            i += extra + 1;
        }
        return new String(out, 0, n);
    }
}
