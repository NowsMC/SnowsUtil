package space.nows.util.bits;

/** Unsigned widening conversions: byte/short to int, int to long. */
public final class Unsigned {
    private Unsigned() {}

    public static int toInt(byte b) { return b & 0xFF; }

    public static int toInt(short s) { return s & 0xFFFF; }

    public static long toLong(int i) { return i & 0xFFFFFFFFL; }
}
