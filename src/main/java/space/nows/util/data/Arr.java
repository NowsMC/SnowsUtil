package space.nows.util.data;

import space.nows.util.check.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Mutable JSON array. */
public final class Arr implements Value, Iterable<Value> {
    private final ArrayList<Value> values;

    public Arr() {
        values = new ArrayList<>();
    }

    public Arr(int initialCapacity) {
        values = new ArrayList<>(Preconditions.nonNegative(initialCapacity, "initialCapacity"));
    }

    @Override public Kind type() { return Kind.ARRAY; }

    @Override public Arr asArray() { return this; }

    public Arr add(Value value) {
        values.add(Preconditions.notNull(value, "value"));
        return this;
    }

    public Arr add(String value) { return add(new Str(value)); }

    public Arr add(long value) { return add(Num.of(value)); }

    public Arr add(double value) { return add(Num.of(value)); }

    public Arr add(boolean value) { return add(Bool.of(value)); }

    public Arr addNull() { return add(Null.INSTANCE); }

    public Value get(int index) { return values.get(index); }

    public int size() { return values.size(); }

    public boolean isEmpty() { return values.isEmpty(); }

    /** Read-only view of the elements. */
    public List<Value> asList() { return Collections.unmodifiableList(values); }

    @Override public Iterator<Value> iterator() { return asList().iterator(); }

    @Override public boolean equals(Object other) {
        return other instanceof Arr array && values.equals(array.values);
    }

    @Override public int hashCode() { return values.hashCode(); }

    @Override public String toString() { return Debug.of(this); }
}
