package space.nows.util.function;

/** Accepts an {@code int} key and a {@code long} value. */
@FunctionalInterface
public interface IntLongConsumer {
    void accept(int key, long value);
}
