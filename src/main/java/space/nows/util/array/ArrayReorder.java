package space.nows.util.array;

import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;

/** In-place reordering: swapping two elements, reversing a range and shuffling. Generated from {@code ArrayReorder.tpl}; edit the template, not this file. */
public final class ArrayReorder {
    private ArrayReorder() {}

    public static void swap(byte[] a, int i, int j) { byte t = a[i]; a[i] = a[j]; a[j] = t; }

    public static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }

    public static void swap(long[] a, int i, int j) { long t = a[i]; a[i] = a[j]; a[j] = t; }

    public static <T> void swap(T[] a, int i, int j) { T t = a[i]; a[i] = a[j]; a[j] = t; }

    public static void reverse(byte[] a) { reverse(a, 0, a.length); }

    public static void reverse(int[] a) { reverse(a, 0, a.length); }

    public static void reverse(long[] a) { reverse(a, 0, a.length); }

    public static <T> void reverse(T[] a) { reverse(a, 0, a.length); }

    /** Reverses {@code [from, to)}. */
    public static void reverse(byte[] a, int from, int to) {
        Objects.checkFromToIndex(from, to, a.length);
        for (int i = from, j = to - 1; i < j; i++, j--) { byte t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    /** Reverses {@code [from, to)}. */
    public static void reverse(int[] a, int from, int to) {
        Objects.checkFromToIndex(from, to, a.length);
        for (int i = from, j = to - 1; i < j; i++, j--) { int t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    /** Reverses {@code [from, to)}. */
    public static void reverse(long[] a, int from, int to) {
        Objects.checkFromToIndex(from, to, a.length);
        for (int i = from, j = to - 1; i < j; i++, j--) { long t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    /** Reverses {@code [from, to)}. */
    public static <T> void reverse(T[] a, int from, int to) {
        Objects.checkFromToIndex(from, to, a.length);
        for (int i = from, j = to - 1; i < j; i++, j--) { T t = a[i]; a[i] = a[j]; a[j] = t; }
    }

    public static void shuffle(byte[] a) { shuffle(a, ThreadLocalRandom.current()); }

    public static void shuffle(int[] a) { shuffle(a, ThreadLocalRandom.current()); }

    public static void shuffle(long[] a) { shuffle(a, ThreadLocalRandom.current()); }

    public static <T> void shuffle(T[] a) { shuffle(a, ThreadLocalRandom.current()); }

    /** Fisher-Yates: every permutation is equally likely when {@code rng} is uniform. */
    public static void shuffle(byte[] a, RandomGenerator rng) {
        Objects.requireNonNull(rng, "rng");
        for (int i = a.length - 1; i > 0; i--) swap(a, i, rng.nextInt(i + 1));
    }

    /** Fisher-Yates: every permutation is equally likely when {@code rng} is uniform. */
    public static void shuffle(int[] a, RandomGenerator rng) {
        Objects.requireNonNull(rng, "rng");
        for (int i = a.length - 1; i > 0; i--) swap(a, i, rng.nextInt(i + 1));
    }

    /** Fisher-Yates: every permutation is equally likely when {@code rng} is uniform. */
    public static void shuffle(long[] a, RandomGenerator rng) {
        Objects.requireNonNull(rng, "rng");
        for (int i = a.length - 1; i > 0; i--) swap(a, i, rng.nextInt(i + 1));
    }

    /** Fisher-Yates: every permutation is equally likely when {@code rng} is uniform. */
    public static <T> void shuffle(T[] a, RandomGenerator rng) {
        Objects.requireNonNull(rng, "rng");
        for (int i = a.length - 1; i > 0; i--) swap(a, i, rng.nextInt(i + 1));
    }
}
