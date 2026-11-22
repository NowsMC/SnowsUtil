package space.nows.util.array;

import space.nows.util.check.Preconditions;

import java.util.Arrays;

/** Structural edits that return a new array: insert, remove, append and concatenate. Swap-remove works in place. */
public final class ArrayEdit {
    private ArrayEdit() {}

    public static byte[] remove(byte[] a, int index) {
        Preconditions.index(index, Preconditions.notNull(a, "a").length);
        byte[] out = new byte[a.length - 1];
        System.arraycopy(a, 0, out, 0, index);
        System.arraycopy(a, index + 1, out, index, a.length - index - 1);
        return out;
    }

    public static int[] remove(int[] a, int index) {
        Preconditions.index(index, Preconditions.notNull(a, "a").length);
        int[] out = new int[a.length - 1];
        System.arraycopy(a, 0, out, 0, index);
        System.arraycopy(a, index + 1, out, index, a.length - index - 1);
        return out;
    }

    public static long[] remove(long[] a, int index) {
        Preconditions.index(index, Preconditions.notNull(a, "a").length);
        long[] out = new long[a.length - 1];
        System.arraycopy(a, 0, out, 0, index);
        System.arraycopy(a, index + 1, out, index, a.length - index - 1);
        return out;
    }

    public static <T> T[] remove(T[] a, int index) {
        Preconditions.index(index, Preconditions.notNull(a, "a").length);
        T[] out = Arrays.copyOf(a, a.length - 1);
        System.arraycopy(a, index + 1, out, index, a.length - index - 1);
        return out;
    }

    /** Overwrites {@code index} with the last live element and returns the new logical size. Order is not kept. */
    public static int swapRemove(byte[] a, int index, int size) {
        Preconditions.position(size, Preconditions.notNull(a, "a").length);
        Preconditions.index(index, size);
        a[index] = a[size - 1];
        return size - 1;
    }

    public static int swapRemove(int[] a, int index, int size) {
        Preconditions.position(size, Preconditions.notNull(a, "a").length);
        Preconditions.index(index, size);
        a[index] = a[size - 1];
        return size - 1;
    }

    public static int swapRemove(long[] a, int index, int size) {
        Preconditions.position(size, Preconditions.notNull(a, "a").length);
        Preconditions.index(index, size);
        a[index] = a[size - 1];
        return size - 1;
    }

    /** Overwrites {@code index} with the last live element, nulls the vacated slot and returns the new logical size. */
    public static <T> int swapRemove(T[] a, int index, int size) {
        Preconditions.position(size, Preconditions.notNull(a, "a").length);
        Preconditions.index(index, size);
        a[index] = a[size - 1];
        a[size - 1] = null;
        return size - 1;
    }

    public static byte[] insert(byte[] a, int index, byte value) {
        Preconditions.position(index, Preconditions.notNull(a, "a").length);
        byte[] out = new byte[ArrayLimits.checkedLength(a.length + 1L)];
        System.arraycopy(a, 0, out, 0, index);
        out[index] = value;
        System.arraycopy(a, index, out, index + 1, a.length - index);
        return out;
    }

    public static int[] insert(int[] a, int index, int value) {
        Preconditions.position(index, Preconditions.notNull(a, "a").length);
        int[] out = new int[ArrayLimits.checkedLength(a.length + 1L)];
        System.arraycopy(a, 0, out, 0, index);
        out[index] = value;
        System.arraycopy(a, index, out, index + 1, a.length - index);
        return out;
    }

    public static long[] insert(long[] a, int index, long value) {
        Preconditions.position(index, Preconditions.notNull(a, "a").length);
        long[] out = new long[ArrayLimits.checkedLength(a.length + 1L)];
        System.arraycopy(a, 0, out, 0, index);
        out[index] = value;
        System.arraycopy(a, index, out, index + 1, a.length - index);
        return out;
    }

    public static <T> T[] insert(T[] a, int index, T value) {
        Preconditions.position(index, Preconditions.notNull(a, "a").length);
        T[] out = Arrays.copyOf(a, ArrayLimits.checkedLength(a.length + 1L));
        System.arraycopy(a, index, out, index + 1, a.length - index);
        out[index] = value;
        return out;
    }

    public static byte[] append(byte[] a, byte value) {
        byte[] out = Arrays.copyOf(Preconditions.notNull(a, "a"), ArrayLimits.checkedLength(a.length + 1L));
        out[a.length] = value;
        return out;
    }

    public static int[] append(int[] a, int value) {
        int[] out = Arrays.copyOf(Preconditions.notNull(a, "a"), ArrayLimits.checkedLength(a.length + 1L));
        out[a.length] = value;
        return out;
    }

    public static long[] append(long[] a, long value) {
        long[] out = Arrays.copyOf(Preconditions.notNull(a, "a"), ArrayLimits.checkedLength(a.length + 1L));
        out[a.length] = value;
        return out;
    }

    public static <T> T[] append(T[] a, T value) {
        T[] out = Arrays.copyOf(Preconditions.notNull(a, "a"), ArrayLimits.checkedLength(a.length + 1L));
        out[a.length] = value;
        return out;
    }

    public static byte[] concat(byte[] a, byte[] b) {
        Preconditions.notNull(a, "a");
        Preconditions.notNull(b, "b");
        byte[] out = Arrays.copyOf(a, ArrayLimits.checkedLength((long) a.length + b.length));
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static int[] concat(int[] a, int[] b) {
        Preconditions.notNull(a, "a");
        Preconditions.notNull(b, "b");
        int[] out = Arrays.copyOf(a, ArrayLimits.checkedLength((long) a.length + b.length));
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static long[] concat(long[] a, long[] b) {
        Preconditions.notNull(a, "a");
        Preconditions.notNull(b, "b");
        long[] out = Arrays.copyOf(a, ArrayLimits.checkedLength((long) a.length + b.length));
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static <T> T[] concat(T[] a, T[] b) {
        Preconditions.notNull(a, "a");
        Preconditions.notNull(b, "b");
        T[] out = Arrays.copyOf(a, ArrayLimits.checkedLength((long) a.length + b.length));
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
}
