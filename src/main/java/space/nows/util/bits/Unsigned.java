package space.nows.util.bits;

/** Unsigned widening conversions; thin, named wrappers over the JDK's {@code toUnsigned*} methods. */
public final class Unsigned {
    private Unsigned() {}

    public static int toInt(byte b) { return Byte.toUnsignedInt(b); }

    public static int toInt(short s) { return Short.toUnsignedInt(s); }

    public static long toLong(byte b) { return Byte.toUnsignedLong(b); }

    public static long toLong(short s) { return Short.toUnsignedLong(s); }

    public static long toLong(int i) { return Integer.toUnsignedLong(i); }
}
