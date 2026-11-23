package space.nows.util.json;

import space.nows.util.error.JsonException;

/** A JSON value. The typed accessors throw {@link JsonException} when the value has a different type. */
public sealed interface JsonValue permits JsonObject, JsonArray, JsonString, JsonNumber, JsonBoolean, JsonNull {
    JsonType type();

    default JsonObject asObject() { throw mismatch(JsonType.OBJECT); }

    default JsonArray asArray() { throw mismatch(JsonType.ARRAY); }

    default String asString() { throw mismatch(JsonType.STRING); }

    default JsonNumber asNumber() { throw mismatch(JsonType.NUMBER); }

    default boolean asBoolean() { throw mismatch(JsonType.BOOLEAN); }

    private JsonException mismatch(JsonType expected) {
        return new JsonException("expected " + expected.label() + " but got " + type().label());
    }
}
