package space.nows.util.bits;

import space.nows.util.error.Checks;

/** Single-bit and bit-field operations on an {@code int}. Bit indices must be in {@code [0, 32)} (no silent shift wrap-around). */
public final class BitOps {
    private BitOps() {}

    public static boolean isSet(int value, int bit) { return (value & (1 << Checks.index(bit, 32))) != 0; }

    public static int setBit(int value, int bit) { return value | (1 << Checks.index(bit, 32)); }

    public static int clearBit(int value, int bit) { return value & ~(1 << Checks.index(bit, 32)); }

    public static int toggleBit(int value, int bit) { return value ^ (1 << Checks.index(bit, 32)); }

    public static int count(int value) { return Integer.bitCount(value); }

    /** Isolates the lowest set bit as a mask ({@code 0} if none). */
    public static int lowestSetBit(int value) { return value & -value; }

    /** Isolates the highest set bit as a mask ({@code 0} if none). */
    public static int highestSetBit(int value) { return Integer.highestOneBit(value); }

    /** A mask with the low {@code width} bits set; {@code width} may be 0..32. */
    public static int mask(int width) {
        if (width < 0 || width > 32) throw new IllegalArgumentException("width must be in [0, 32] but was " + width);
        return width == 32 ? -1 : (1 << width) - 1;
    }

    /** Reads the unsigned field of {@code width} bits starting at bit {@code offset}. */
    public static int extract(int value, int offset, int width) {
        checkField(offset, width);
        return width == 0 ? 0 : (value >>> offset) & mask(width);
    }

    /** Returns {@code value} with the field of {@code width} bits at {@code offset} replaced by the low bits of {@code field}. */
    public static int deposit(int value, int offset, int width, int field) {
        checkField(offset, width);
        if (width == 0) return value;
        int m = mask(width) << offset;
        return (value & ~m) | ((field << offset) & m);
    }

    private static void checkField(int offset, int width) {
        if (offset < 0 || width < 0 || offset + (long) width > 32) {
            throw new IllegalArgumentException("field [" + offset + ", +" + width + ") does not fit in 32 bits");
        }
    }
}
