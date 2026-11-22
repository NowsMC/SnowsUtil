package space.nows.util.text;

import space.nows.util.error.CapacityExceededException;
import space.nows.util.check.Preconditions;
import space.nows.util.error.InvalidHexException;

import java.nio.charset.StandardCharsets;

/** Hexadecimal encoding and strict (ASCII-only) decoding of byte arrays. */
public final class Hex {
    private Hex() {}

    private static final byte[] LOWER = "0123456789abcdef".getBytes(StandardCharsets.ISO_8859_1);
    private static final byte[] UPPER = "0123456789ABCDEF".getBytes(StandardCharsets.ISO_8859_1);

    public static String toHex(byte[] data) { return encode(data, 0, Preconditions.notNull(data, "data").length, LOWER); }

    public static String toHex(byte[] data, int off, int len) { return encode(data, off, len, LOWER); }

    public static String toUpperHex(byte[] data) { return encode(data, 0, Preconditions.notNull(data, "data").length, UPPER); }

    private static String encode(byte[] data, int off, int len, byte[] digits) {
        Preconditions.notNull(data, "data");
        Preconditions.range(off, len, data.length);
        if (len > Integer.MAX_VALUE / 2) throw new CapacityExceededException("hex string", 2L * len, Integer.MAX_VALUE);
        byte[] out = new byte[len * 2];
        for (int i = 0; i < len; i++) {
            int v = data[off + i] & 0xFF;
            out[i * 2] = digits[v >>> 4];
            out[i * 2 + 1] = digits[v & 0x0F];
        }
        return new String(out, StandardCharsets.ISO_8859_1);
    }

    /**
     * Decodes upper- or lower-case hex.
     *
     * @throws InvalidHexException if the length is odd or a character is not {@code [0-9a-fA-F]}
     */
    public static byte[] fromHex(String hex) {
        Preconditions.notNull(hex, "hex");
        int n = hex.length();
        if ((n & 1) != 0) throw new InvalidHexException("odd length " + n, n);
        byte[] out = new byte[n / 2];
        for (int i = 0; i < out.length; i++) {
            int hi = digit(hex, i * 2);
            int lo = digit(hex, i * 2 + 1);
            out[i] = (byte) ((hi << 4) | lo);
        }
        return out;
    }

    public static boolean isHex(CharSequence s) {
        if (s == null || (s.length() & 1) != 0) return false;
        for (int i = 0, n = s.length(); i < n; i++) if (digitOf(s.charAt(i)) < 0) return false;
        return true;
    }

    private static int digit(String s, int index) {
        int d = digitOf(s.charAt(index));
        if (d < 0) throw new InvalidHexException("invalid hex character '" + s.charAt(index) + "'", index);
        return d;
    }

    private static int digitOf(char c) {
        if (c >= '0' && c <= '9') return c - '0';
        if (c >= 'a' && c <= 'f') return c - 'a' + 10;
        if (c >= 'A' && c <= 'F') return c - 'A' + 10;
        return -1;
    }
}
