package space.nows.util.data;

import space.nows.util.check.Preconditions;

public final class DataString implements DataValue {
    private final String value;

    public DataString(String value) {
        this.value = Preconditions.notNull(value, "value");
    }

    @Override public DataType type() { return DataType.STRING; }

    @Override public String asString() { return value; }

    public String value() { return value; }

    @Override public boolean equals(Object other) {
        return other instanceof DataString string && value.equals(string.value);
    }

    @Override public int hashCode() { return value.hashCode(); }

    @Override public String toString() { return DataText.of(this); }
}
