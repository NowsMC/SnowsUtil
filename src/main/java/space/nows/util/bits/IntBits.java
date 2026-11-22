package space.nows.util.bits;

import space.nows.util.check.Preconditions;

/** Single-bit operations on an {@code int}. Bit indices must be in {@code [0, 32)} (no silent shift wrap-around). */
public final class IntBits {
    private IntBits() {}

    public static boolean isSet(int value, int bit) { return (value & (1 << Preconditions.index(bit, 32))) != 0; }

    public static int setBit(int value, int bit) { return value | (1 << Preconditions.index(bit, 32)); }

    public static int clearBit(int value, int bit) { return value & ~(1 << Preconditions.index(bit, 32)); }

    public static int toggleBit(int value, int bit) { return value ^ (1 << Preconditions.index(bit, 32)); }
}
