package space.nows.util.data;

public final class DataNull implements DataValue {
    public static final DataNull INSTANCE = new DataNull();

    private DataNull() {}

    @Override public DataType type() { return DataType.NULL; }

    @Override public boolean equals(Object other) { return other instanceof DataNull; }

    @Override public int hashCode() { return 0; }

    @Override public String toString() { return "null"; }
}
