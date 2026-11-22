package space.nows.util.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import space.nows.util.collections.IntArrayDeque;
import space.nows.util.collections.IntIntMap;
import space.nows.util.collections.IntList;
import space.nows.util.collections.IntMinHeap;
import space.nows.util.collections.LongHashSet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class CollectionsBenchmark {
    @Param({"1000", "100000"})
    int n;

    int[] keys;
    long[] longKeys;

    @Setup
    public void setup() {
        Random r = new Random(1);
        keys = new int[n];
        longKeys = new long[n];
        for (int i = 0; i < n; i++) {
            keys[i] = r.nextInt(n / 4 + 1);
            longKeys[i] = r.nextLong();
        }
    }

    @Benchmark
    public void count_jdkMerge(Blackhole bh) {
        Map<Integer, Integer> m = new HashMap<>();
        for (int k : keys) m.merge(k, 1, Integer::sum);
        bh.consume(m);
    }

    @Benchmark
    public void count_intIntMapAddTo(Blackhole bh) {
        IntIntMap m = new IntIntMap();
        for (int k : keys) m.addTo(k, 1);
        bh.consume(m);
    }

    @Benchmark
    public void count_intIntMapMerge(Blackhole bh) {
        IntIntMap m = new IntIntMap();
        for (int k : keys) m.merge(k, 1, Integer::sum);
        bh.consume(m);
    }

    @Benchmark
    public void set_jdkHashSet(Blackhole bh) {
        Set<Long> s = new HashSet<>();
        for (long k : longKeys) s.add(k);
        long hits = 0;
        for (long k : longKeys) if (s.contains(k)) hits++;
        bh.consume(hits);
    }

    @Benchmark
    public void set_longHashSet(Blackhole bh) {
        LongHashSet s = new LongHashSet();
        for (long k : longKeys) s.add(k);
        long hits = 0;
        for (long k : longKeys) if (s.contains(k)) hits++;
        bh.consume(hits);
    }

    @Benchmark
    public void list_arrayList(Blackhole bh) {
        List<Integer> l = new ArrayList<>();
        for (int k : keys) l.add(k);
        long sum = 0;
        for (int v : l) sum += v;
        bh.consume(sum);
    }

    @Benchmark
    public void list_intList(Blackhole bh) {
        IntList l = new IntList();
        for (int k : keys) l.add(k);
        long sum = 0;
        for (int i = 0; i < l.size(); i++) sum += l.get(i);
        bh.consume(sum);
    }

    @Benchmark
    public void deque_arrayDeque(Blackhole bh) {
        ArrayDeque<Integer> d = new ArrayDeque<>();
        long sum = 0;
        for (int k : keys) {
            d.addLast(k);
            if ((k & 1) == 0) sum += d.removeFirst();
        }
        bh.consume(sum);
    }

    @Benchmark
    public void deque_intArrayDeque(Blackhole bh) {
        IntArrayDeque d = new IntArrayDeque();
        long sum = 0;
        for (int k : keys) {
            d.addLast(k);
            if ((k & 1) == 0) sum += d.removeFirst();
        }
        bh.consume(sum);
    }

    @Benchmark
    public void heap_priorityQueue(Blackhole bh) {
        PriorityQueue<Integer> q = new PriorityQueue<>();
        for (int k : keys) q.add(k);
        long sum = 0;
        while (!q.isEmpty()) sum += q.poll();
        bh.consume(sum);
    }

    @Benchmark
    public void heap_intMinHeap(Blackhole bh) {
        IntMinHeap q = new IntMinHeap();
        for (int k : keys) q.add(k);
        long sum = 0;
        while (!q.isEmpty()) sum += q.poll();
        bh.consume(sum);
    }
}
