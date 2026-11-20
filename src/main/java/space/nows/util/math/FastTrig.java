package space.nows.util.math;

/** Table-based sin/cos (4096 entries). The table is only built when this class is first used. */
public final class FastTrig {
    private FastTrig() {}

    private static final int TABLE_BITS = 12;
    private static final int TABLE_SIZE = 1 << TABLE_BITS;
    private static final int TABLE_MASK = TABLE_SIZE - 1;
    private static final float TO_INDEX = TABLE_SIZE / FastMath.TWO_PI;
    private static final float[] SIN_TABLE = new float[TABLE_SIZE];

    static {
        for (int i = 0; i < TABLE_SIZE; i++) {
            SIN_TABLE[i] = (float) Math.sin(i * (2.0 * Math.PI / TABLE_SIZE));
        }
    }

    public static float sin(float radians) { return SIN_TABLE[FastMath.fastFloor(radians * TO_INDEX) & TABLE_MASK]; }

    public static float cos(float radians) { return SIN_TABLE[(FastMath.fastFloor(radians * TO_INDEX) + (TABLE_SIZE >> 2)) & TABLE_MASK]; }
}
