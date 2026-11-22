package space.nows.util.function;

/** Accepts a {@code long} key and an {@code int} value. */
@FunctionalInterface
public interface LongIntConsumer {
    void accept(long key, int value);
}
