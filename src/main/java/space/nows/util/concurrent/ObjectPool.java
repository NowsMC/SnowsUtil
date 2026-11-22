package space.nows.util.concurrent;

import space.nows.util.check.Preconditions;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Thread-safe pool that keeps up to {@code maxIdle} released objects for reuse. {@link #acquire()} never blocks:
 * when nothing is idle it creates a new object. Releasing an object more than once, or using it after release,
 * is the caller's bug and is not detected.
 */
public final class ObjectPool<T> {
    private final MpmcQueue<T> idle;
    private final Supplier<? extends T> factory;
    private final Consumer<? super T> reset;

    public ObjectPool(int maxIdle, Supplier<? extends T> factory) { this(maxIdle, factory, null); }

    /** {@code reset}, if not {@code null}, runs on every object as it is released. */
    public ObjectPool(int maxIdle, Supplier<? extends T> factory, Consumer<? super T> reset) {
        this.idle = new MpmcQueue<>(maxIdle);
        this.factory = Preconditions.notNull(factory, "factory");
        this.reset = reset;
    }

    public T acquire() {
        T t = idle.poll();
        return t != null ? t : factory.get();
    }

    /** Returns {@code true} if the object was kept, {@code false} if the pool was full and it was dropped. */
    public boolean release(T t) {
        Preconditions.notNull(t, "t");
        if (reset != null) reset.accept(t);
        return idle.offer(t);
    }

    public int idleCount() { return idle.size(); }

    public int maxIdle() { return idle.capacity(); }
}
