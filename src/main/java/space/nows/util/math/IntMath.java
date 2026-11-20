package space.nows.util.math;

/** Integer power-of-two and log2 helpers. */
public final class IntMath {
    private IntMath() {}

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
}
