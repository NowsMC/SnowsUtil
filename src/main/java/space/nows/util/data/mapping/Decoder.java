package space.nows.util.data.mapping;

import space.nows.util.error.DataException;
import space.nows.util.data.Arr;
import space.nows.util.data.Bool;
import space.nows.util.data.Null;
import space.nows.util.data.Num;
import space.nows.util.data.Obj;
import space.nows.util.data.Str;
import space.nows.util.data.Value;

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

final class Decoder {
    private final boolean failOnUnknown;

    Decoder(boolean failOnUnknown) {
        this.failOnUnknown = failOnUnknown;
    }

    Object decode(Value value, Type type) throws ReflectiveOperationException {
        Class<?> target = rawType(type);
        Type[] arguments = type instanceof ParameterizedType parameterized
                ? parameterized.getActualTypeArguments() : new Type[0];
        if (target == Object.class) return toJavaValue(value);
        if (Value.class.isAssignableFrom(target)) {
            if (target.isInstance(value)) return value;
            throw mismatch(target, value);
        }
        if (target == Optional.class) {
            Type element = arguments.length > 0 ? arguments[0] : Object.class;
            return value instanceof Null ? Optional.empty() : Optional.ofNullable(decode(value, element));
        }
        if (value instanceof Null) {
            if (target.isPrimitive()) throw new DataException("cannot bind null to " + target.getName());
            return null;
        }
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

    private Object array(Arr array, Type type, Class<?> target) throws ReflectiveOperationException {
        Type component = type instanceof GenericArrayType generic ? generic.getGenericComponentType() : target.getComponentType();
        Object result = Array.newInstance(rawType(component), array.size());
        for (int i = 0; i < array.size(); i++) Array.set(result, i, decode(array.get(i), component));
        return result;
    }

    private Collection<Object> collection(Arr array, Class<?> target, Type element) throws ReflectiveOperationException {
        Collection<Object> result = Set.class.isAssignableFrom(target)
                ? new LinkedHashSet<Object>() : new ArrayList<Object>(array.size());
        if (!target.isInstance(result)) throw new DataException("unsupported collection type: " + target.getTypeName());
        for (Value item : array) result.add(decode(item, element));
        return result;
    }

    private Map<Object, Object> map(Obj object, Class<?> target, Type keyType, Type valueType)
            throws ReflectiveOperationException {
        if (!target.isAssignableFrom(LinkedHashMap.class)) {
            throw new DataException("unsupported map type: " + target.getTypeName());
        }
        Map<Object, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Value> entry : object) {
            result.put(decode(keyValue(entry.getKey(), keyType), keyType), decode(entry.getValue(), valueType));
        }
        return result;
    }

    private static Value keyValue(String key, Type keyType) throws ReflectiveOperationException {
        Class<?> raw = rawType(keyType);
        if (isNumeric(raw)) {
            try {
                return Num.of(new BigDecimal(key));
            } catch (NumberFormatException e) {
                throw new DataException("map key \"" + key + "\" is not a number", e);
            }
        }
        if (raw == boolean.class || raw == Boolean.class) {
            if (key.equals("true") || key.equals("false")) return Bool.of(Boolean.parseBoolean(key));
            throw new DataException("map key \"" + key + "\" is not a boolean");
        }
        return new Str(key);
    }

    private static boolean isNumeric(Class<?> c) {
        return c == int.class || c == Integer.class || c == long.class || c == Long.class
                || c == short.class || c == Short.class || c == byte.class || c == Byte.class
                || c == double.class || c == Double.class || c == float.class || c == Float.class
                || c == BigInteger.class || c == BigDecimal.class || c == Number.class;
    }

    private Object record(Obj object, Class<?> target) throws ReflectiveOperationException {
        RecordComponent[] components = target.getRecordComponents();
        Class<?>[] parameterTypes = new Class<?>[components.length];
        Object[] arguments = new Object[components.length];
        Set<String> known = new HashSet<>();
        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            String name = Mapper.propertyName(component.getName(), component.getAnnotation(Property.class));
            parameterTypes[i] = component.getType();
            if (!known.add(name)) throw new DataException("duplicate property \"" + name + "\" in " + target.getSimpleName());
            Value property = component.isAnnotationPresent(Ignore.class) ? null : object.get(name);
            arguments[i] = property == null ? defaultValue(component.getType()) : decode(property, component.getGenericType());
        }
        checkUnknown(object, known);
        Constructor<?> constructor = target.getDeclaredConstructor(parameterTypes);
        if (!constructor.trySetAccessible()) throw new DataException("cannot access record constructor: " + target.getName());
        return constructor.newInstance(arguments);
    }

    private Object instance(Obj object, Class<?> target) throws ReflectiveOperationException {
        Constructor<?> constructor = target.getDeclaredConstructor();
        if (!constructor.trySetAccessible()) throw new DataException("cannot access constructor: " + target.getName());
        Object instance = constructor.newInstance();
        Set<String> known = new HashSet<>();
        for (Field field : Mapper.instanceFields(target)) {
            String name = Mapper.propertyName(field.getName(), field.getAnnotation(Property.class));
            if (!known.add(name)) throw new DataException("duplicate property \"" + name + "\" in " + target.getSimpleName());
            Value property = field.isAnnotationPresent(Ignore.class) ? null : object.get(name);
            if (property == null) continue;
            if (!field.trySetAccessible()) throw Mapper.inaccessible(name);
            field.set(instance, decode(property, field.getGenericType()));
        }
        checkUnknown(object, known);
        return instance;
    }

    private void checkUnknown(Obj object, Set<String> known) {
        if (!failOnUnknown) return;
        for (String name : object.keys()) {
            if (!known.contains(name)) throw new DataException("unknown property: \"" + name + "\"");
        }
    }

    private static Object enumConstant(String name, Class<?> target) {
        Object[] constants = target.getEnumConstants();
        for (Object constant : constants) {
            if (Mapper.enumName((Enum<?>) constant).equals(name)) return constant;
        }
        for (Object constant : constants) {
            if (((Enum<?>) constant).name().equals(name)) return constant;
        }
        throw new DataException("unknown " + target.getSimpleName() + " constant: \"" + name + "\"");
    }

    private static char singleChar(Value value) {
        String text = value.asString();
        if (text.length() != 1) throw new DataException("expected a single character but got " + text.length() + " characters");
        return text.charAt(0);
    }

    static Object toJavaValue(Value value) {
        if (value instanceof Null) return null;
        if (value instanceof Bool bool) return bool.value();
        if (value instanceof Str string) return string.value();
        if (value instanceof Num number) return javaNumber(number);
        if (value instanceof Arr array) {
            List<Object> result = new ArrayList<>(array.size());
            for (Value element : array) result.add(toJavaValue(element));
            return result;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Value> entry : value.asObject()) {
            result.put(entry.getKey(), toJavaValue(entry.getValue()));
        }
        return result;
    }

    private static Object javaNumber(Num number) {
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
        throw new DataException("unsupported target type: " + type.getTypeName());
    }

    private static Object defaultValue(Class<?> type) {
        if (type == Optional.class) return Optional.empty();
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

    private static DataException mismatch(Class<?> target, Value value) {
        return new DataException("cannot bind " + value.type().label() + " to " + target.getSimpleName());
    }
}
