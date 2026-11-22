package space.nows.util.collections;

import space.nows.util.error.CapacityExceededException;

final class Growth {
    private Growth() {}

    static final int MAX_LENGTH = Integer.MAX_VALUE - 8;

    /** About 1.5x the current length, but never less than {@code minCapacity}. */
    static int arrayLength(int current, int minCapacity) {
        if (minCapacity > MAX_LENGTH || minCapacity < 0) {
            throw new CapacityExceededException("array", minCapacity < 0 ? (long) Integer.MAX_VALUE + 1 : minCapacity, MAX_LENGTH);
        }
        long grown = Math.max((long) minCapacity, current + (current >> 1) + 1L);
        return (int) Math.min(grown, MAX_LENGTH);
    }

    /** Smallest power of two {@code >= minCapacity}, at least 4. */
    static int powerOfTwo(int minCapacity) {
        if (minCapacity > (1 << 30) || minCapacity < 0) {
            throw new CapacityExceededException("array", minCapacity < 0 ? (long) Integer.MAX_VALUE + 1 : minCapacity, 1 << 30);
        }
        int cap = 4;
        while (cap < minCapacity) cap <<= 1;
        return cap;
    }
}
