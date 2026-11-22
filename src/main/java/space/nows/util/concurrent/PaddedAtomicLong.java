package space.nows.util.concurrent;

import java.util.concurrent.atomic.AtomicLong;

/** An {@link AtomicLong} padded so that two instances allocated next to each other do not share a cache line. */
@SuppressWarnings({"serial", "unused"})
final class PaddedAtomicLong extends AtomicLong {
    private long p1, p2, p3, p4, p5, p6, p7;
}
