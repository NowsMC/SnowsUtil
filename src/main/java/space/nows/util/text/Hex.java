package space.nows.util.text;

/** Hexadecimal encoding and decoding of byte arrays. */
public final class Hex {
    private Hex() {}

    private static final char[] HEX = "0123456789abcdef".toCharArray();

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
