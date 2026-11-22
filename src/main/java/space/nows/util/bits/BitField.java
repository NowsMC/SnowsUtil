package space.nows.util.bits;

/** Multi-bit fields inside an {@code int}: build masks, read a field, and write a field without disturbing its neighbours. */
public final class BitField {
    private BitField() {}

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
