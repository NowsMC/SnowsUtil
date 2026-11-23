package space.nows.util.json;

public final class JsonBoolean implements JsonValue {
    public static final JsonBoolean TRUE = new JsonBoolean(true);
    public static final JsonBoolean FALSE = new JsonBoolean(false);

    private final boolean value;

    private JsonBoolean(boolean value) {
        this.value = value;
    }

    public static JsonBoolean of(boolean value) { return value ? TRUE : FALSE; }

    @Override public JsonType type() { return JsonType.BOOLEAN; }

    @Override public boolean asBoolean() { return value; }

    public boolean value() { return value; }

    @Override public boolean equals(Object other) {
        return other instanceof JsonBoolean bool && value == bool.value;
    }

    @Override public int hashCode() { return Boolean.hashCode(value); }

    @Override public String toString() { return Boolean.toString(value); }
}
