package space.nows.util.math;

/**
 * Table-based sin/cos (4096 entries, absolute error about 1e-3). The table is only built when this class is first used.
 * Inputs must be finite and below about 1.6 million radians in magnitude; anything else throws
 * {@link IllegalArgumentException} instead of silently returning garbage.
 */
public final class FastTrig {
    private FastTrig() {}

    private static final int TABLE_BITS = 12;
    private static final int TABLE_SIZE = 1 << TABLE_BITS;
    private static final int TABLE_MASK = TABLE_SIZE - 1;
    private static final float TO_INDEX = TABLE_SIZE / FastMath.TWO_PI;
    private static final float INDEX_LIMIT = (float) (1 << 30);
    private static final float[] SIN_TABLE = new float[TABLE_SIZE];

    static {
        for (int i = 0; i < TABLE_SIZE; i++) {
            SIN_TABLE[i] = (float) Math.sin(i * (2.0 * Math.PI / TABLE_SIZE));
        }
    }

    private static int index(float radians) {
        float t = radians * TO_INDEX;
        if (!(t > -INDEX_LIMIT && t < INDEX_LIMIT)) {   // false for NaN, +/-Infinity and huge values
            throw new IllegalArgumentException("radians out of supported range: " + radians);
        }
        return FastMath.fastFloor(t);
    }

    public static float sin(float radians) { return SIN_TABLE[index(radians) & TABLE_MASK]; }

    public static float cos(float radians) { return SIN_TABLE[(index(radians) + (TABLE_SIZE >> 2)) & TABLE_MASK]; }
}
