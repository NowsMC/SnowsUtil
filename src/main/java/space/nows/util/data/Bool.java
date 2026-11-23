package space.nows.util.data;

public final class Bool implements Value {
    public static final Bool TRUE = new Bool(true);
    public static final Bool FALSE = new Bool(false);

    private final boolean value;

    private Bool(boolean value) {
        this.value = value;
    }

    public static Bool of(boolean value) { return value ? TRUE : FALSE; }

    @Override public Kind type() { return Kind.BOOLEAN; }

    @Override public boolean asBoolean() { return value; }

    public boolean value() { return value; }

    @Override public boolean equals(Object other) {
        return other instanceof Bool bool && value == bool.value;
    }

    @Override public int hashCode() { return Boolean.hashCode(value); }

    @Override public String toString() { return Boolean.toString(value); }
}
