package space.nows.util.json;

public final class JsonNull implements JsonValue {
    public static final JsonNull INSTANCE = new JsonNull();

    private JsonNull() {}

    @Override public JsonType type() { return JsonType.NULL; }

    @Override public boolean equals(Object other) { return other instanceof JsonNull; }

    @Override public int hashCode() { return 0; }

    @Override public String toString() { return "null"; }
}
