package space.nows.util.bench;

import org.openjdk.jmh.annotations.*;
import space.nows.util.concurrent.MpmcQueue;
import space.nows.util.concurrent.SpscQueue;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

@State(Scope.Group)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 2, time = 1)
@Measurement(iterations = 3, time = 1)
@Fork(1)
public class QueueBenchmark {
    private static final Integer ITEM = 1;

    SpscQueue<Integer> spsc = new SpscQueue<>(1024);
    MpmcQueue<Integer> mpmc = new MpmcQueue<>(1024);
    ArrayBlockingQueue<Integer> abq = new ArrayBlockingQueue<>(1024);
    ConcurrentLinkedQueue<Integer> clq = new ConcurrentLinkedQueue<>();

    @Benchmark @Group("spsc") @GroupThreads(1)
    public boolean spscOffer() { return spsc.offer(ITEM); }

    @Benchmark @Group("spsc") @GroupThreads(1)
    public Integer spscPoll() { return spsc.poll(); }

    @Benchmark @Group("mpmc") @GroupThreads(1)
    public boolean mpmcOffer() { return mpmc.offer(ITEM); }

    @Benchmark @Group("mpmc") @GroupThreads(1)
    public Integer mpmcPoll() { return mpmc.poll(); }

    @Benchmark @Group("arrayBlocking") @GroupThreads(1)
    public boolean abqOffer() { return abq.offer(ITEM); }

    @Benchmark @Group("arrayBlocking") @GroupThreads(1)
    public Integer abqPoll() { return abq.poll(); }

    @Benchmark @Group("linked") @GroupThreads(1)
    public boolean clqOffer() { return clq.offer(ITEM); }

    @Benchmark @Group("linked") @GroupThreads(1)
    public Integer clqPoll() { return clq.poll(); }
}
