package space.nows.util.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import space.nows.util.array.ArraySearch;
import space.nows.util.array.ArraySort;
import space.nows.util.hash.Hashing;
import space.nows.util.math.FastTrig;
import space.nows.util.math.Rounding;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class ArrayBenchmark {
    @Param({"1000", "100000"})
    int n;

    int[] ints;
    long[] longs;
    byte[] bytes;
    float[] angles;
    int[] work;
    long[] longWork;

    @Setup
    public void setup() {
        Random r = new Random(1);
        ints = r.ints(n).toArray();
        longs = r.longs(n).toArray();
        bytes = new byte[n];
        Arrays.fill(bytes, (byte) 1);
        bytes[n - 1] = 2;
        angles = new float[n];
        for (int i = 0; i < n; i++) angles[i] = r.nextFloat() * 20f - 10f;
        work = new int[n];
        longWork = new long[n];
    }

    @Benchmark
    public int byteIndexOf_swar() { return ArraySearch.indexOf(bytes, (byte) 2); }

    @Benchmark
    public int byteIndexOf_loop() {
        byte[] a = bytes;
        for (int i = 0; i < a.length; i++) if (a[i] == 2) return i;
        return -1;
    }

    @Benchmark
    public int[] intSort_jdk() {
        System.arraycopy(ints, 0, work, 0, n);
        Arrays.sort(work);
        return work;
    }

    @Benchmark
    public int[] intSort_radix() {
        System.arraycopy(ints, 0, work, 0, n);
        ArraySort.radixSort(work);
        return work;
    }

    @Benchmark
    public long[] longSort_jdk() {
        System.arraycopy(longs, 0, longWork, 0, n);
        Arrays.sort(longWork);
        return longWork;
    }

    @Benchmark
    public long[] longSort_radix() {
        System.arraycopy(longs, 0, longWork, 0, n);
        ArraySort.radixSort(longWork);
        return longWork;
    }

    @Benchmark
    public void sin_math(Blackhole bh) {
        float s = 0;
        for (float a : angles) s += (float) Math.sin(a);
        bh.consume(s);
    }

    @Benchmark
    public void sin_table(Blackhole bh) {
        float s = 0;
        for (float a : angles) s += FastTrig.sin(a);
        bh.consume(s);
    }

    @Benchmark
    public void sin_smooth(Blackhole bh) {
        float s = 0;
        for (float a : angles) s += FastTrig.sinSmooth(a);
        bh.consume(s);
    }

    @Benchmark
    public void floor_rounding(Blackhole bh) {
        int s = 0;
        for (float a : angles) s += Rounding.floor(a * 100f);
        bh.consume(s);
    }

    @Benchmark
    public void floor_branchy(Blackhole bh) {
        int s = 0;
        for (float a : angles) {
            float v = a * 100f;
            int i = (int) v;
            s += (v < i && i != Integer.MIN_VALUE) ? i - 1 : i;
        }
        bh.consume(s);
    }

    @Benchmark
    public long xxHash64() { return Hashing.xxHash64(bytes); }

    @Benchmark
    public long crc32c() { return Hashing.crc32c(bytes); }
}
