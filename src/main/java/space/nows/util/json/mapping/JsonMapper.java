package space.nows.util.json.mapping;

import space.nows.util.check.Preconditions;
import space.nows.util.error.JsonException;
import space.nows.util.json.Json;
import space.nows.util.json.JsonArray;
import space.nows.util.json.JsonBoolean;
import space.nows.util.json.JsonNull;
import space.nows.util.json.JsonNumber;
import space.nows.util.json.JsonObject;
import space.nows.util.json.JsonString;
import space.nows.util.json.JsonValue;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Converts between Java objects and {@link JsonValue} trees by reflection. Supports records, classes with a
 * no-argument constructor (including inherited fields), enums, arrays, collections, maps and {@link Optional}.
 */
public final class JsonMapper {
    private final JsonDecoder decoder;

    /** Rejects JSON properties that have no matching Java property. */
    public JsonMapper() {
        this(true);
    }

    private JsonMapper(boolean failOnUnknown) {
        decoder = new JsonDecoder(failOnUnknown);
    }

    /** Ignores JSON properties that have no matching Java property. */
    public static JsonMapper lenient() {
        return new JsonMapper(false);
    }

    public JsonValue toJsonValue(Object value) {
        return toValue(value, 0);
    }

    public String toJson(Object value) {
        return Json.write(toJsonValue(value));
    }

    public String toJsonPretty(Object value) {
        return Json.writePretty(toJsonValue(value));
    }

    public <T> T fromJson(String input, Class<T> type) {
        return fromJson(Json.parse(input), type);
    }

    public <T> T fromJson(Reader input, Class<T> type) {
        return fromJson(Json.parse(input), type);
    }

    public <T> T fromJson(InputStream input, Class<T> type) {
        return fromJson(Json.parse(input), type);
    }

    public <T> T fromJson(JsonValue value, Class<T> type) {
        return fromJson(value, (Type) type);
    }

    /** Binds to a generic type such as {@code List<Person>} that cannot be expressed as a {@link Class} literal. */
    @SuppressWarnings("unchecked")
    public <T> T fromJson(JsonValue value, Type type) {
        Preconditions.notNull(value, "value");
        Preconditions.notNull(type, "type");
        try {
            return (T) decoder.decode(value, type);
        } catch (ReflectiveOperationException e) {
            throw new JsonException("data binding failed: " + e, e);
        }
    }

    private JsonValue toValue(Object value, int depth) {
        if (depth > Json.MAX_DEPTH) throw new JsonException("maximum nesting depth " + Json.MAX_DEPTH + " exceeded");
        if (value == null) return JsonNull.INSTANCE;
        if (value instanceof JsonValue json) return json;
        if (value instanceof String string) return new JsonString(string);
        if (value instanceof Character character) return new JsonString(character.toString());
        if (value instanceof Boolean bool) return JsonBoolean.of(bool);
        if (value instanceof BigDecimal decimal) return JsonNumber.of(decimal);
        if (value instanceof BigInteger integer) return JsonNumber.of(integer);
        if (value instanceof Number number) return JsonNumber.of(decimalOf(number));
        if (value instanceof Enum<?> constant) return new JsonString(enumName(constant));
        if (value instanceof Optional<?> optional) {
            return optional.isPresent() ? toValue(optional.get(), depth + 1) : JsonNull.INSTANCE;
        }
        if (value instanceof Map<?, ?> map) {
            JsonObject object = new JsonObject(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                object.put(String.valueOf(entry.getKey()), toValue(entry.getValue(), depth + 1));
            }
            return object;
        }
        if (value instanceof Iterable<?> iterable) {
            JsonArray array = new JsonArray();
            for (Object element : iterable) array.add(toValue(element, depth + 1));
            return array;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            JsonArray array = new JsonArray(length);
            for (int i = 0; i < length; i++) array.add(toValue(Array.get(value, i), depth + 1));
            return array;
        }
        return bean(value, depth);
    }

    private JsonObject bean(Object value, int depth) {
        Class<?> type = value.getClass();
        JsonInclude include = type.getAnnotation(JsonInclude.class);
        boolean includeNulls = include == null || include.value() == JsonInclude.Include.ALWAYS;
        JsonObject object = new JsonObject();
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                if (component.isAnnotationPresent(JsonIgnore.class)) continue;
                String name = propertyName(component.getName(), component.getAnnotation(JsonProperty.class));
                putProperty(object, name, invoke(component.getAccessor(), value, name), includeNulls, depth);
            }
        } else {
            for (Field field : instanceFields(type)) {
                if (field.isAnnotationPresent(JsonIgnore.class)) continue;
                String name = propertyName(field.getName(), field.getAnnotation(JsonProperty.class));
                putProperty(object, name, read(field, value, name), includeNulls, depth);
            }
        }
        return object;
    }

    private void putProperty(JsonObject object, String name, Object fieldValue, boolean includeNulls, int depth) {
        if (fieldValue != null || includeNulls) object.put(name, toValue(fieldValue, depth + 1));
    }

    private static Object invoke(Method accessor, Object target, String name) {
        if (!accessor.trySetAccessible()) throw inaccessible(name);
        try {
            return accessor.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new JsonException("cannot read property \"" + name + "\"", e);
        }
    }

    private static Object read(Field field, Object target, String name) {
        if (!field.trySetAccessible()) throw inaccessible(name);
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new JsonException("cannot read property \"" + name + "\"", e);
        }
    }

    static JsonException inaccessible(String name) {
        return new JsonException("cannot access property \"" + name + "\"");
    }

    /** Instance fields of {@code type} and its superclasses, excluding static, transient and synthetic fields. */
    static List<Field> instanceFields(Class<?> type) {
        List<Class<?>> hierarchy = new ArrayList<>();
        for (Class<?> current = type; current != null && current != Object.class; current = current.getSuperclass()) {
            hierarchy.add(0, current);
        }
        List<Field> fields = new ArrayList<>();
        for (Class<?> current : hierarchy) {
            for (Field field : current.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                if (!Modifier.isStatic(modifiers) && !Modifier.isTransient(modifiers) && !field.isSynthetic()) {
                    fields.add(field);
                }
            }
        }
        return fields;
    }

    static String propertyName(String fallback, JsonProperty property) {
        return property == null || property.value().isEmpty() ? fallback : property.value();
    }

    static String enumName(Enum<?> constant) {
        try {
            JsonProperty property = constant.getDeclaringClass().getField(constant.name()).getAnnotation(JsonProperty.class);
            return property == null || property.value().isEmpty() ? constant.name() : property.value();
        } catch (NoSuchFieldException e) {
            return constant.name();
        }
    }

    private static BigDecimal decimalOf(Number number) {
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            throw new JsonException("unsupported numeric value: " + number, e);
        }
    }
}
