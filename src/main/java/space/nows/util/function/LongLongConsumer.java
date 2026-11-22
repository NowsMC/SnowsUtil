package space.nows.util.function;

/** Accepts a {@code long} key and a {@code long} value. */
@FunctionalInterface
public interface LongLongConsumer {
    void accept(long key, long value);
}
