package space.nows.util.function;

/** Accepts an {@code int} key and an {@code int} value. */
@FunctionalInterface
public interface IntIntConsumer {
    void accept(int key, int value);
}
