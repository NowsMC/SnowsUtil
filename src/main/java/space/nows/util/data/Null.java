package space.nows.util.data;

public final class Null implements Value {
    public static final Null INSTANCE = new Null();

    private Null() {}

    @Override public Kind type() { return Kind.NULL; }

    @Override public boolean equals(Object other) { return other instanceof Null; }

    @Override public int hashCode() { return 0; }

    @Override public String toString() { return "null"; }
}
