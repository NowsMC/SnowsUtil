package space.nows.util.json.mapping;

import space.nows.util.error.JsonException;
import space.nows.util.json.JsonArray;
import space.nows.util.json.JsonBoolean;
import space.nows.util.json.JsonNull;
import space.nows.util.json.JsonNumber;
import space.nows.util.json.JsonObject;
import space.nows.util.json.JsonString;
import space.nows.util.json.JsonValue;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.RecordComponent;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

final class JsonDecoder {
    private final boolean failOnUnknown;

    JsonDecoder(boolean failOnUnknown) {
        this.failOnUnknown = failOnUnknown;
    }

    Object decode(JsonValue value, Type type) throws ReflectiveOperationException {
        Class<?> target = rawType(type);
        Type[] arguments = type instanceof ParameterizedType parameterized
                ? parameterized.getActualTypeArguments() : new Type[0];
        if (target == Object.class) return toJavaValue(value);
        if (JsonValue.class.isAssignableFrom(target)) {
            if (target.isInstance(value)) return value;
            throw mismatch(target, value);
        }
        if (target == Optional.class) {
            Type element = arguments.length > 0 ? arguments[0] : Object.class;
            return value instanceof JsonNull ? Optional.empty() : Optional.ofNullable(decode(value, element));
        }
        if (value instanceof JsonNull) return defaultValue(target);
        if (target == String.class || target == CharSequence.class) return value.asString();
        if (target == char.class || target == Character.class) return singleChar(value);
        if (target == boolean.class || target == Boolean.class) return value.asBoolean();
        if (target == int.class || target == Integer.class) return value.asNumber().asInt();
        if (target == long.class || target == Long.class) return value.asNumber().asLong();
        if (target == short.class || target == Short.class) return value.asNumber().asShort();
        if (target == byte.class || target == Byte.class) return value.asNumber().asByte();
        if (target == double.class || target == Double.class) return value.asNumber().asDouble();
        if (target == float.class || target == Float.class) return value.asNumber().asFloat();
        if (target == BigDecimal.class || target == Number.class) return value.asNumber().asDecimal();
        if (target == BigInteger.class) return value.asNumber().asBigInteger();
        if (target.isEnum()) return enumConstant(value.asString(), target);
        if (target.isArray()) return array(value.asArray(), type, target);
        if (Collection.class.isAssignableFrom(target) || target == Iterable.class) {
            return collection(value.asArray(), target, arguments.length > 0 ? arguments[0] : Object.class);
        }
        if (Map.class.isAssignableFrom(target)) {
            Type keyType = arguments.length > 0 ? arguments[0] : Object.class;
            Type valueType = arguments.length > 1 ? arguments[1] : Object.class;
            return map(value.asObject(), target, keyType, valueType);
        }
        return target.isRecord() ? record(value.asObject(), target) : instance(value.asObject(), target);
    }

    private Object array(JsonArray array, Type type, Class<?> target) throws ReflectiveOperationException {
        Type component = type instanceof GenericArrayType generic ? generic.getGenericComponentType() : target.getComponentType();
        Object result = Array.newInstance(rawType(component), array.size());
        for (int i = 0; i < array.size(); i++) Array.set(result, i, decode(array.get(i), component));
        return result;
    }

    private Collection<Object> collection(JsonArray array, Class<?> target, Type element) throws ReflectiveOperationException {
        Collection<Object> result = Set.class.isAssignableFrom(target)
                ? new LinkedHashSet<Object>() : new ArrayList<Object>(array.size());
        if (!target.isInstance(result)) throw new JsonException("unsupported collection type: " + target.getTypeName());
        for (JsonValue item : array) result.add(decode(item, element));
        return result;
    }

    private Map<Object, Object> map(JsonObject object, Class<?> target, Type keyType, Type valueType)
            throws ReflectiveOperationException {
        if (!target.isAssignableFrom(LinkedHashMap.class)) {
            throw new JsonException("unsupported map type: " + target.getTypeName());
        }
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> entry : object) {
            result.put(decode(new JsonString(entry.getKey()), keyType), decode(entry.getValue(), valueType));
        }
        return result;
    }

    private Object record(JsonObject object, Class<?> target) throws ReflectiveOperationException {
        RecordComponent[] components = target.getRecordComponents();
        Class<?>[] parameterTypes = new Class<?>[components.length];
        Object[] arguments = new Object[components.length];
        Set<String> known = new HashSet<>();
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            String name = JsonMapper.propertyName(component.getName(), component.getAnnotation(JsonProperty.class));
            parameterTypes[i] = component.getType();
            known.add(name);
            JsonValue property = component.isAnnotationPresent(JsonIgnore.class) ? null : object.get(name);
            arguments[i] = property == null ? defaultValue(component.getType()) : decode(property, component.getGenericType());
        }
        checkUnknown(object, known);
        Constructor<?> constructor = target.getDeclaredConstructor(parameterTypes);
        if (!constructor.trySetAccessible()) throw new JsonException("cannot access record constructor: " + target.getName());
        return constructor.newInstance(arguments);
    }

    private Object instance(JsonObject object, Class<?> target) throws ReflectiveOperationException {
        Constructor<?> constructor = target.getDeclaredConstructor();
        if (!constructor.trySetAccessible()) throw new JsonException("cannot access constructor: " + target.getName());
        Object instance = constructor.newInstance();
        Set<String> known = new HashSet<>();
        for (Field field : JsonMapper.instanceFields(target)) {
            String name = JsonMapper.propertyName(field.getName(), field.getAnnotation(JsonProperty.class));
            known.add(name);
            JsonValue property = field.isAnnotationPresent(JsonIgnore.class) ? null : object.get(name);
            if (property == null) continue;
            if (!field.trySetAccessible()) throw JsonMapper.inaccessible(name);
            field.set(instance, decode(property, field.getGenericType()));
        }
        checkUnknown(object, known);
        return instance;
    }

    private void checkUnknown(JsonObject object, Set<String> known) {
        if (!failOnUnknown) return;
        for (String name : object.keys()) {
            if (!known.contains(name)) throw new JsonException("unknown property: \"" + name + "\"");
        }
    }

    private static Object enumConstant(String name, Class<?> target) {
        Object[] constants = target.getEnumConstants();
        for (Object constant : constants) {
            if (JsonMapper.enumName((Enum<?>) constant).equals(name)) return constant;
        }
        for (Object constant : constants) {
            if (((Enum<?>) constant).name().equals(name)) return constant;
        }
        throw new JsonException("unknown " + target.getSimpleName() + " constant: \"" + name + "\"");
    }

    private static char singleChar(JsonValue value) {
        String text = value.asString();
        if (text.length() != 1) throw new JsonException("expected a single character but got " + text.length() + " characters");
        return text.charAt(0);
    }

    static Object toJavaValue(JsonValue value) {
        if (value instanceof JsonNull) return null;
        if (value instanceof JsonBoolean bool) return bool.value();
        if (value instanceof JsonString string) return string.value();
        if (value instanceof JsonNumber number) return javaNumber(number);
        if (value instanceof JsonArray array) {
            List<Object> result = new ArrayList<>(array.size());
            for (JsonValue element : array) result.add(toJavaValue(element));
            return result;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, JsonValue> entry : value.asObject()) {
            result.put(entry.getKey(), toJavaValue(entry.getValue()));
        }
        return result;
    }

    private static Object javaNumber(JsonNumber number) {
        BigDecimal decimal = number.asDecimal();
        try {
            return decimal.intValueExact();
        } catch (ArithmeticException notInt) {
            try {
                return decimal.longValueExact();
            } catch (ArithmeticException notLong) {
                return decimal;
            }
        }
    }

    private static Class<?> rawType(Type type) {
        if (type instanceof Class<?> direct) return direct;
        if (type instanceof ParameterizedType parameterized && parameterized.getRawType() instanceof Class<?> raw) return raw;
        if (type instanceof GenericArrayType generic) {
            return Array.newInstance(rawType(generic.getGenericComponentType()), 0).getClass();
        }
        if (type instanceof WildcardType wildcard) return rawType(wildcard.getUpperBounds()[0]);
        if (type instanceof TypeVariable<?>) return Object.class;
        throw new JsonException("unsupported target type: " + type.getTypeName());
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == char.class) return '\0';
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0f;
        if (type == double.class) return 0d;
        return null;
    }

    private static JsonException mismatch(Class<?> target, JsonValue value) {
        return new JsonException("cannot bind " + value.type().label() + " to " + target.getSimpleName());
    }
}
