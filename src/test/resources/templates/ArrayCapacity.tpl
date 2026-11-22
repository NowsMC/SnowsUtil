package space.nows.util.array;

import space.nows.util.check.Preconditions;

import java.util.Arrays;

/** Amortized capacity growth for arrays used as growable buffers. Generated from {@code ArrayCapacity.tpl}; edit the template, not this file. */
public final class ArrayCapacity {
    private ArrayCapacity() {}

#for byte,int,long,T
    /** Returns {@code a} if it is already large enough, otherwise a copy grown by ~1.5x (at least {@code minCapacity}). */
    public static $GEN$$ARR$ ensureCapacity($ARR$ a, int minCapacity) {
        Preconditions.notNull(a, "a");
        Preconditions.nonNegative(minCapacity, "minCapacity");
        if (a.length >= minCapacity) return a;
        return Arrays.copyOf(a, ArrayLimits.grownLength(a.length, minCapacity));
    }
#endfor
}
