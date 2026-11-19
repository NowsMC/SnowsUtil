package space.nows.util.array;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class ByteUtils {
    private ByteUtils() {}

    private static final VarHandle SHORT_BE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle SHORT_LE = MethodHandles.byteArrayViewVarHandle(short[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle INT_BE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle INT_LE = MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG_BE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.BIG_ENDIAN);
    private static final VarHandle LONG_LE = MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    public static short readShort(byte[] b, int off) { return (short) SHORT_BE.get(b, off); }

    public static short readShortLE(byte[] b, int off) { return (short) SHORT_LE.get(b, off); }

    public static void writeShort(byte[] b, int off, short v) { SHORT_BE.set(b, off, v); }

    public static void writeShortLE(byte[] b, int off, short v) { SHORT_LE.set(b, off, v); }

    public static int readInt(byte[] b, int off) { return (int) INT_BE.get(b, off); }

    public static int readIntLE(byte[] b, int off) { return (int) INT_LE.get(b, off); }

    public static void writeInt(byte[] b, int off, int v) { INT_BE.set(b, off, v); }

    public static void writeIntLE(byte[] b, int off, int v) { INT_LE.set(b, off, v); }

    public static long readLong(byte[] b, int off) { return (long) LONG_BE.get(b, off); }

    public static long readLongLE(byte[] b, int off) { return (long) LONG_LE.get(b, off); }

    public static void writeLong(byte[] b, int off, long v) { LONG_BE.set(b, off, v); }

    public static void writeLongLE(byte[] b, int off, long v) { LONG_LE.set(b, off, v); }

    public static int compare(byte[] a, byte[] b) { return Arrays.compareUnsigned(a, b); }

    public static boolean equals(byte[] a, byte[] b) { return Arrays.equals(a, b); }

    public static int indexOf(byte[] data, byte value) {
        for (int i = 0; i < data.length; i++) if (data[i] == value) return i;
        return -1;
    }

    public static int indexOf(byte[] data, byte[] pattern) { return indexOf(data, pattern, 0); }

    public static int indexOf(byte[] data, byte[] pattern, int from) {
        int n = data.length, m = pattern.length;
        if (from < 0) from = 0;
        if (m == 0) return Math.min(from, n);
        byte first = pattern[0];
        for (int i = from, max = n - m; i <= max; i++) {
            if (data[i] != first) continue;
            int j = 1;
            while (j < m && data[i + j] == pattern[j]) j++;
            if (j == m) return i;
        }
        return -1;
    }

    public static int unsigned(byte b) { return b & 0xFF; }

    public static int unsigned(short s) { return s & 0xFFFF; }

    public static long unsigned(int i) { return i & 0xFFFFFFFFL; }
}
