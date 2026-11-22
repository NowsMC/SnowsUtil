package space.nows.util.function;

/** Accepts a {@code long} key and an object value. */
@FunctionalInterface
public interface LongObjConsumer<V> {
    void accept(long key, V value);
}
