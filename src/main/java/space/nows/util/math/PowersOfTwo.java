package space.nows.util.math;

/** Power-of-two tests, rounding and logarithms for {@code int} values. */
public final class PowersOfTwo {
    private PowersOfTwo() {}

    public static boolean isPowerOfTwo(int v) { return v > 0 && (v & (v - 1)) == 0; }

    public static int nextPowerOfTwo(int v) {
        if (v <= 1) return 1;
        if (v > (1 << 30)) throw new IllegalArgumentException("too large: " + v);
        return 1 << (32 - Integer.numberOfLeadingZeros(v - 1));
    }

    public static int log2(int v) {
        if (v <= 0) throw new IllegalArgumentException("v <= 0");
        return 31 - Integer.numberOfLeadingZeros(v);
    }

    /** Smallest {@code n} with {@code 2^n >= v}. */
    public static int ceilLog2(int v) {
        if (v <= 0) throw new IllegalArgumentException("v <= 0");
        return v == 1 ? 0 : 32 - Integer.numberOfLeadingZeros(v - 1);
    }

    /**
     * Rounds {@code value} up to a multiple of {@code alignment} (a power of two).
     *
     * @throws ArithmeticException if the result does not fit in an {@code int}
     */
    public static int alignUp(int value, int alignment) {
        if (!isPowerOfTwo(alignment)) throw new IllegalArgumentException("alignment must be a power of two: " + alignment);
        return Math.addExact(value, alignment - 1) & -alignment;
    }
}
