package space.nows.util.data;

public final class DataBoolean implements DataValue {
    public static final DataBoolean TRUE = new DataBoolean(true);
    public static final DataBoolean FALSE = new DataBoolean(false);

    private final boolean value;

    private DataBoolean(boolean value) {
        this.value = value;
    }

    public static DataBoolean of(boolean value) { return value ? TRUE : FALSE; }

    @Override public DataType type() { return DataType.BOOLEAN; }

    @Override public boolean asBoolean() { return value; }

    public boolean value() { return value; }

    @Override public boolean equals(Object other) {
        return other instanceof DataBoolean bool && value == bool.value;
    }

    @Override public int hashCode() { return Boolean.hashCode(value); }

    @Override public String toString() { return Boolean.toString(value); }
}
