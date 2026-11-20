package space.nows.util.io;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

/** Big/little-endian primitive reads and writes on {@code byte[]} (VarHandle based, no allocation). */
public final class ByteAccess {
    private ByteAccess() {}

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
}
