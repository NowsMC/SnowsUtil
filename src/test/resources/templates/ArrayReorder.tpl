package space.nows.util.array;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/** In-place reordering: swapping two elements, reversing a range and shuffling. Generated from {@code ArrayReorder.tpl}; edit the template, not this file. */
public final class ArrayReorder {
    private ArrayReorder() {}

#for byte,int,long,T
    public static $GEN$void swap($ARR$ a, int i, int j) { $E$ t = a[i]; a[i] = a[j]; a[j] = t; }
#endfor

#for byte,int,long,T
    public static $GEN$void reverse($ARR$ a) { reverse(a, 0, a.length); }
#endfor

#for byte,int,long,T
    /** Reverses {@code [from, to)}. */
    public static $GEN$void reverse($ARR$ a, int from, int to) {
        Objects.checkFromToIndex(from, to, a.length);
        for (int i = from, j = to - 1; i < j; i++, j--) { $E$ t = a[i]; a[i] = a[j]; a[j] = t; }
    }
#endfor

#for byte,int,long,T
    public static $GEN$void shuffle($ARR$ a) { shuffle(a, ThreadLocalRandom.current()); }
#endfor

#for byte,int,long,T
    /** Fisher-Yates: every permutation is equally likely when {@code rng} is uniform. */
    public static $GEN$void shuffle($ARR$ a, RandomGenerator rng) {
        Objects.requireNonNull(rng, "rng");
        for (int i = a.length - 1; i > 0; i--) swap(a, i, rng.nextInt(i + 1));
    }
#endfor
}
