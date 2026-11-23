package space.nows.util.json;

import space.nows.util.check.Preconditions;

public final class JsonString implements JsonValue {
    private final String value;

    public JsonString(String value) {
        this.value = Preconditions.notNull(value, "value");
    }

    @Override public JsonType type() { return JsonType.STRING; }

    @Override public String asString() { return value; }

    public String value() { return value; }

    @Override public boolean equals(Object other) {
        return other instanceof JsonString string && value.equals(string.value);
    }

    @Override public int hashCode() { return value.hashCode(); }

    @Override public String toString() { return Json.write(this); }
}
