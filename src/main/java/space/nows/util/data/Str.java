package space.nows.util.data;

import space.nows.util.check.Preconditions;

public final class Str implements Value {
    private final String value;

    public Str(String value) {
        this.value = Preconditions.notNull(value, "value");
    }

    @Override public Kind type() { return Kind.STRING; }

    @Override public String asString() { return value; }

    public String value() { return value; }

    @Override public boolean equals(Object other) {
        return other instanceof Str string && value.equals(string.value);
    }

    @Override public int hashCode() { return value.hashCode(); }

    @Override public String toString() { return Debug.of(this); }
}
