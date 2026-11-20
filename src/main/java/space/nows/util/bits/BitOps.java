package space.nows.util.bits;

/** Single-bit operations on an {@code int}. */
public final class BitOps {
    private BitOps() {}

    public static boolean isSet(int value, int bit) { return (value & (1 << bit)) != 0; }

    public static int setBit(int value, int bit) { return value | (1 << bit); }

    public static int clearBit(int value, int bit) { return value & ~(1 << bit); }

    public static int toggleBit(int value, int bit) { return value ^ (1 << bit); }

    public static int count(int value) { return Integer.bitCount(value); }

    public static int lowestSetBit(int value) { return value & -value; }
}
