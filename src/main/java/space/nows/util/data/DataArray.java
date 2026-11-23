package space.nows.util.data;

import space.nows.util.check.Preconditions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/** Mutable JSON array. */
public final class DataArray implements DataValue, Iterable<DataValue> {
    private final ArrayList<DataValue> values;

    public DataArray() {
        values = new ArrayList<>();
    }

    public DataArray(int initialCapacity) {
        values = new ArrayList<>(Preconditions.nonNegative(initialCapacity, "initialCapacity"));
    }

    @Override public DataType type() { return DataType.ARRAY; }

    @Override public DataArray asArray() { return this; }

    public DataArray add(DataValue value) {
        values.add(Preconditions.notNull(value, "value"));
        return this;
    }

    public DataArray add(String value) { return add(new DataString(value)); }

    public DataArray add(long value) { return add(DataNumber.of(value)); }

    public DataArray add(double value) { return add(DataNumber.of(value)); }

    public DataArray add(boolean value) { return add(DataBoolean.of(value)); }

    public DataArray addNull() { return add(DataNull.INSTANCE); }

    public DataValue get(int index) { return values.get(index); }

    public int size() { return values.size(); }

    public boolean isEmpty() { return values.isEmpty(); }

    /** Read-only view of the elements. */
    public List<DataValue> asList() { return Collections.unmodifiableList(values); }

    @Override public Iterator<DataValue> iterator() { return asList().iterator(); }

    @Override public boolean equals(Object other) {
        return other instanceof DataArray array && values.equals(array.values);
    }

    @Override public int hashCode() { return values.hashCode(); }

    @Override public String toString() { return DataText.of(this); }
}
