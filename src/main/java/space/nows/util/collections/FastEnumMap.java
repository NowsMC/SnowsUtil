package space.nows.util.collections;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.BiConsumer;

public class FastEnumMap<E extends Enum<E>, V> {
    private final E[] universe;
    private final Object[] values;
    private final boolean[] present;
    private int size;

    public FastEnumMap(Class<E> keyType) {
        this.universe = Objects.requireNonNull(keyType, "keyType").getEnumConstants();
        if (universe == null) throw new IllegalArgumentException(keyType + " is not an enum");
        this.values = new Object[universe.length];
        this.present = new boolean[universe.length];
    }

    public int size() { return size; }

    public boolean isEmpty() { return size == 0; }

    public boolean containsKey(E key) { return present[key.ordinal()]; }

    @SuppressWarnings("unchecked")
    public V get(E key) { return (V) values[key.ordinal()]; }

    @SuppressWarnings("unchecked")
    public V put(E key, V value) {
        int i = key.ordinal();
        V old = (V) values[i];
        values[i] = value;
        if (!present[i]) {
            present[i] = true;
            size++;
        }
        return old;
    }

    @SuppressWarnings("unchecked")
    public V remove(E key) {
        int i = key.ordinal();
        V old = (V) values[i];
        if (present[i]) {
            present[i] = false;
            values[i] = null;
            size--;
        }
        return old;
    }

    public void clear() {
        Arrays.fill(values, null);
        Arrays.fill(present, false);
        size = 0;
    }

    @SuppressWarnings("unchecked")
    public void forEach(BiConsumer<? super E, ? super V> action) {
        for (int i = 0; i < universe.length; i++) {
            if (present[i]) action.accept(universe[i], (V) values[i]);
        }
    }
}
