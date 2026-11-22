package space.nows.util.array;

import space.nows.util.error.CapacityExceededException;

final class ArrayLimits {
    private ArrayLimits() {}

    static final int MAX_LENGTH = Integer.MAX_VALUE - 8;

    static int checkedLength(long n) {
        if (n > MAX_LENGTH) throw new CapacityExceededException("array", n, MAX_LENGTH);
        return (int) n;
    }

    /** About 1.5x the current length, but never less than {@code minCapacity}. */
    static int grownLength(int currentLength, int minCapacity) {
        if (minCapacity > MAX_LENGTH) throw new CapacityExceededException("array", minCapacity, MAX_LENGTH);
        long grown = Math.max((long) minCapacity, currentLength + (currentLength >> 1) + 1L);
        return (int) Math.min(grown, MAX_LENGTH);
    }
}
