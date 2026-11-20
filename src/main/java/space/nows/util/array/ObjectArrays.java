package space.nows.util.array;

/** Helpers for object arrays: swap, search, swap-remove. */
public final class ObjectArrays {
    private ObjectArrays() {}

    public static <T> void swap(T[] a, int i, int j) { T t = a[i]; a[i] = a[j]; a[j] = t; }

    public static <T> int indexOf(T[] a, Object value) {
        if (value == null) {
            for (int i = 0; i < a.length; i++) if (a[i] == null) return i;
        } else {
            for (int i = 0; i < a.length; i++) if (value.equals(a[i])) return i;
        }
        return -1;
    }

    public static <T> int swapRemove(T[] a, int index, int size) {
        checkIndex(index, size);
        a[index] = a[size - 1];
        a[size - 1] = null;
        return size - 1;
    }

    private static void checkIndex(int index, int length) {
        if (index < 0 || index >= length) throw new IndexOutOfBoundsException("index " + index + ", length " + length);
    }
}
