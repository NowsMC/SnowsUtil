package space.nows.util.data.mapping;

import space.nows.util.check.Preconditions;
import space.nows.util.data.DataArray;
import space.nows.util.data.DataBoolean;
import space.nows.util.data.DataNull;
import space.nows.util.data.DataNumber;
import space.nows.util.data.DataObject;
import space.nows.util.data.DataString;
import space.nows.util.data.DataValue;
import space.nows.util.error.DataException;

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
 * Converts between Java objects and {@link DataValue} trees by reflection. Supports records, classes with a
 * no-argument constructor (including inherited fields), enums, arrays, collections, maps and {@link Optional}.
 * The mapper is independent of any text format; pair it with a format's parser and writer.
 */
public final class DataMapper {
    private final DataDecoder decoder;

    /** Rejects properties that have no matching Java property. */
    public DataMapper() {
        this(true);
    }

    private DataMapper(boolean failOnUnknown) {
        decoder = new DataDecoder(failOnUnknown);
    }

    /** Ignores properties that have no matching Java property. */
    public static DataMapper lenient() {
        return new DataMapper(false);
    }

    public DataValue toData(Object value) {
        return toValue(value, 0);
    }

    public <T> T fromData(DataValue value, Class<T> type) {
        return fromData(value, (Type) type);
    }

    /** Binds to a generic type such as {@code List<Person>} that cannot be expressed as a {@link Class} literal. */
    @SuppressWarnings("unchecked")
    public <T> T fromData(DataValue value, Type type) {
        Preconditions.notNull(value, "value");
        Preconditions.notNull(type, "type");
        try {
            return (T) decoder.decode(value, type);
        } catch (ReflectiveOperationException e) {
            throw new DataException("data binding failed: " + e, e);
        }
    }

    private DataValue toValue(Object value, int depth) {
        if (depth > DataValue.MAX_DEPTH) {
            throw new DataException("maximum nesting depth " + DataValue.MAX_DEPTH + " exceeded");
        }
        if (value == null) return DataNull.INSTANCE;
        if (value instanceof DataValue data) return data;
        if (value instanceof String string) return new DataString(string);
        if (value instanceof Character character) return new DataString(character.toString());
        if (value instanceof Boolean bool) return DataBoolean.of(bool);
        if (value instanceof BigDecimal decimal) return DataNumber.of(decimal);
        if (value instanceof BigInteger integer) return DataNumber.of(integer);
        if (value instanceof Number number) return DataNumber.of(decimalOf(number));
        if (value instanceof Enum<?> constant) return new DataString(enumName(constant));
        if (value instanceof Optional<?> optional) {
            return optional.isPresent() ? toValue(optional.get(), depth + 1) : DataNull.INSTANCE;
        }
        if (value instanceof Map<?, ?> map) {
            DataObject object = new DataObject(map.size());
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                object.put(String.valueOf(entry.getKey()), toValue(entry.getValue(), depth + 1));
            }
            return object;
        }
        if (value instanceof Iterable<?> iterable) {
            DataArray array = new DataArray();
            for (Object element : iterable) array.add(toValue(element, depth + 1));
            return array;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            DataArray array = new DataArray(length);
            for (int i = 0; i < length; i++) array.add(toValue(Array.get(value, i), depth + 1));
            return array;
        }
        return bean(value, depth);
    }

    private DataObject bean(Object value, int depth) {
        Class<?> type = value.getClass();
        DataInclude include = type.getAnnotation(DataInclude.class);
        boolean includeNulls = include == null || include.value() == DataInclude.Include.ALWAYS;
        DataObject object = new DataObject();
        if (type.isRecord()) {
            for (RecordComponent component : type.getRecordComponents()) {
                if (component.isAnnotationPresent(DataIgnore.class)) continue;
                String name = propertyName(component.getName(), component.getAnnotation(DataProperty.class));
                putProperty(object, name, invoke(component.getAccessor(), value, name), includeNulls, depth);
            }
        } else {
            for (Field field : instanceFields(type)) {
                if (field.isAnnotationPresent(DataIgnore.class)) continue;
                String name = propertyName(field.getName(), field.getAnnotation(DataProperty.class));
                putProperty(object, name, read(field, value, name), includeNulls, depth);
            }
        }
        return object;
    }

    private void putProperty(DataObject object, String name, Object fieldValue, boolean includeNulls, int depth) {
        if (fieldValue != null || includeNulls) object.put(name, toValue(fieldValue, depth + 1));
    }

    private static Object invoke(Method accessor, Object target, String name) {
        if (!accessor.trySetAccessible()) throw inaccessible(name);
        try {
            return accessor.invoke(target);
        } catch (ReflectiveOperationException e) {
            throw new DataException("cannot read property \"" + name + "\"", e);
        }
    }

    private static Object read(Field field, Object target, String name) {
        if (!field.trySetAccessible()) throw inaccessible(name);
        try {
            return field.get(target);
        } catch (IllegalAccessException e) {
            throw new DataException("cannot read property \"" + name + "\"", e);
        }
    }

    static DataException inaccessible(String name) {
        return new DataException("cannot access property \"" + name + "\"");
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

    static String propertyName(String fallback, DataProperty property) {
        return property == null || property.value().isEmpty() ? fallback : property.value();
    }

    static String enumName(Enum<?> constant) {
        try {
            DataProperty property = constant.getDeclaringClass().getField(constant.name()).getAnnotation(DataProperty.class);
            return property == null || property.value().isEmpty() ? constant.name() : property.value();
        } catch (NoSuchFieldException e) {
            return constant.name();
        }
    }

    private static BigDecimal decimalOf(Number number) {
        try {
            return new BigDecimal(number.toString());
        } catch (NumberFormatException e) {
            throw new DataException("unsupported numeric value: " + number, e);
        }
    }
}
