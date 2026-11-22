package space.nows.util.function;

/** Accepts an {@code int} key and an object value. */
@FunctionalInterface
public interface IntObjConsumer<V> {
    void accept(int key, V value);
}
