package space.nows.util.array;

import space.nows.util.error.CapacityExceededException;
import space.nows.util.error.Checks;

import java.util.Arrays;
import java.util.Objects;

/** Helpers for object arrays: copy, resize, insert/remove, swap, search, reverse, concat. Component types are preserved. */
public final class ObjectArrays {
    private ObjectArrays() {}

    private static final int MAX_LENGTH = Integer.MAX_VALUE - 8;

    public static <T> T[] resize(T[] a, int newLength) {
        return Arrays.copyOf(Checks.notNull(a, "a"), Checks.nonNegative(newLength, "newLength"));
    }

    public static <T> T[] ensureCapacity(T[] a, int minCapacity) {
        Checks.notNull(a, "a");
        Checks.nonNegative(minCapacity, "minCapacity");
        if (a.length >= minCapacity) return a;
        if (minCapacity > MAX_LENGTH) throw new CapacityExceededException("array", minCapacity, MAX_LENGTH);
        long grown = Math.max((long) minCapacity, a.length + (a.length >> 1) + 1L);
        return Arrays.copyOf(a, (int) Math.min(grown, MAX_LENGTH));
    }

    public static <T> T[] remove(T[] a, int index) {
        Checks.index(index, Checks.notNull(a, "a").length);
        T[] out = Arrays.copyOf(a, a.length - 1);
        System.arraycopy(a, index + 1, out, index, a.length - index - 1);
        return out;
    }

    public static <T> T[] insert(T[] a, int index, T value) {
        Checks.position(index, Checks.notNull(a, "a").length);
        T[] out = Arrays.copyOf(a, checkedLength(a.length + 1L));
        System.arraycopy(a, index, out, index + 1, a.length - index);
        out[index] = value;
        return out;
    }

    public static <T> T[] append(T[] a, T value) {
        T[] out = Arrays.copyOf(Checks.notNull(a, "a"), checkedLength(a.length + 1L));
        out[a.length] = value;
        return out;
    }

    public static <T> T[] concat(T[] a, T[] b) {
        Checks.notNull(a, "a");
        Checks.notNull(b, "b");
        T[] out = Arrays.copyOf(a, checkedLength((long) a.length + b.length));
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    public static <T> void swap(T[] a, int i, int j) { T t = a[i]; a[i] = a[j]; a[j] = t; }

    public static <T> int indexOf(T[] a, Object value) {
        if (value == null) {
            for (int i = 0; i < a.length; i++) if (a[i] == null) return i;
        } else {
            for (int i = 0; i < a.length; i++) if (value.equals(a[i])) return i;
        }
        return -1;
    }

    public static <T> int lastIndexOf(T[] a, Object value) {
        for (int i = a.length - 1; i >= 0; i--) if (Objects.equals(value, a[i])) return i;
        return -1;
    }

    public static <T> boolean contains(T[] a, Object value) { return indexOf(a, value) >= 0; }

    public static <T> void reverse(T[] a) {
        for (int i = 0, j = a.length - 1; i < j; i++, j--) { T t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    /** Overwrites {@code index} with the last live element, nulls the vacated slot and returns the new logical size. */
    public static <T> int swapRemove(T[] a, int index, int size) {
        Checks.position(size, Checks.notNull(a, "a").length);
        Checks.index(index, size);
        a[index] = a[size - 1];
        a[size - 1] = null;
        return size - 1;
    }

    private static int checkedLength(long n) {
        if (n > MAX_LENGTH) throw new CapacityExceededException("array", n, MAX_LENGTH);
        return (int) n;
    }
}
