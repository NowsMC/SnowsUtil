package space.nows.util.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import space.nows.util.collections.IntIntMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
public class MapBenchmark {
    @Param({"1000", "100000"})
    int n;

    int[] keys;

    @Setup
    public void setup() {
        Random r = new Random(1);
        keys = new int[n];
        for (int i = 0; i < n; i++) keys[i] = r.nextInt();
    }

    @Benchmark
    public void jdkHashMap_putGet(Blackhole bh) {
        Map<Integer, Integer> m = new HashMap<>();
        for (int k : keys) m.put(k, k);
        long sum = 0;
        for (int k : keys) sum += m.get(k);
        bh.consume(sum);
    }

    @Benchmark
    public void intIntMap_putGet(Blackhole bh) {
        IntIntMap m = new IntIntMap();
        for (int k : keys) m.put(k, k);
        long sum = 0;
        for (int k : keys) sum += m.get(k);
        bh.consume(sum);
    }
}
