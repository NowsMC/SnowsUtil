package space.nows.util.concurrent;

import space.nows.util.check.Preconditions;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/** Creates threads named {@code prefix-1}, {@code prefix-2}, ... with a fixed daemon flag and priority. */
public final class NamedThreadFactory implements ThreadFactory {
    private final String prefix;
    private final boolean daemon;
    private final int priority;
    private final AtomicInteger counter = new AtomicInteger(1);

    public NamedThreadFactory(String prefix) { this(prefix, true, Thread.NORM_PRIORITY); }

    public NamedThreadFactory(String prefix, boolean daemon, int priority) {
        this.prefix = Preconditions.notEmpty(prefix, "prefix");
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY) {
            throw new IllegalArgumentException("priority must be in [" + Thread.MIN_PRIORITY + ", " + Thread.MAX_PRIORITY + "] but was " + priority);
        }
        this.daemon = daemon;
        this.priority = priority;
    }

    @Override public Thread newThread(Runnable r) {
        Thread t = new Thread(Preconditions.notNull(r, "r"), prefix + "-" + counter.getAndIncrement());
        t.setDaemon(daemon);
        t.setPriority(priority);
        return t;
    }
}
