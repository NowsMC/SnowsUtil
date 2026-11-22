package space.nows.util.concurrent;

import java.util.concurrent.locks.LockSupport;

/**
 * Escalating wait for polling loops: spin first, then yield, then park for a short, growing interval (up to about
 * 1 ms). Call {@link #pause()} each time an attempt fails and {@link #reset()} after one succeeds.
 * One instance per waiting thread; it is not thread-safe.
 */
public final class Backoff {
    private static final int SPIN_LIMIT = 64;
    private static final int YIELD_LIMIT = 96;
    private static final int MAX_PARK_SHIFT = 5;
    private static final long PARK_BASE_NANOS = 30_000L;

    private int attempts;

    public void pause() {
        int n = attempts;
        if (n < SPIN_LIMIT) {
            Thread.onSpinWait();
        } else if (n < YIELD_LIMIT) {
            Thread.yield();
        } else {
            LockSupport.parkNanos(PARK_BASE_NANOS << Math.min(n - YIELD_LIMIT, MAX_PARK_SHIFT));
        }
        if (n < Integer.MAX_VALUE) attempts = n + 1;
    }

    public void reset() { attempts = 0; }
}
