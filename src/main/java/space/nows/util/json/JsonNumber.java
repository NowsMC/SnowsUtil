package space.nows.util.json;

import space.nows.util.check.Preconditions;
import space.nows.util.error.JsonException;

import java.math.BigDecimal;
import java.math.BigInteger;

/** A JSON number held as an exact {@link BigDecimal}. Narrowing accessors throw {@link JsonException} on overflow or loss. */
public final class JsonNumber implements JsonValue {
    private final BigDecimal value;

    private JsonNumber(BigDecimal value) {
        this.value = value;
    }

    public static JsonNumber of(int value) { return new JsonNumber(BigDecimal.valueOf(value)); }

    public static JsonNumber of(long value) { return new JsonNumber(BigDecimal.valueOf(value)); }

    public static JsonNumber of(double value) {
        Preconditions.argument(Double.isFinite(value), "JSON numbers must be finite but was " + value);
        return new JsonNumber(BigDecimal.valueOf(value));
    }

    public static JsonNumber of(BigDecimal value) { return new JsonNumber(Preconditions.notNull(value, "value")); }

    public static JsonNumber of(BigInteger value) {
        return new JsonNumber(new BigDecimal(Preconditions.notNull(value, "value")));
    }

    @Override public JsonType type() { return JsonType.NUMBER; }

    @Override public JsonNumber asNumber() { return this; }

    public BigDecimal asDecimal() { return value; }

    public double asDouble() { return value.doubleValue(); }

    public float asFloat() { return value.floatValue(); }

    public int asInt() {
        try {
            return value.intValueExact();
        } catch (ArithmeticException e) {
            throw outOfRange("int", e);
        }
    }

    public long asLong() {
        try {
            return value.longValueExact();
        } catch (ArithmeticException e) {
            throw outOfRange("long", e);
        }
    }

    public short asShort() {
        try {
            return value.shortValueExact();
        } catch (ArithmeticException e) {
            throw outOfRange("short", e);
        }
    }

    public byte asByte() {
        try {
            return value.byteValueExact();
        } catch (ArithmeticException e) {
            throw outOfRange("byte", e);
        }
    }

    public BigInteger asBigInteger() {
        try {
            return value.toBigIntegerExact();
        } catch (ArithmeticException e) {
            throw new JsonException("number " + value + " is not an integer", e);
        }
    }

    private JsonException outOfRange(String target, ArithmeticException cause) {
        return new JsonException("number " + value + " does not fit in " + target, cause);
    }

    @Override public boolean equals(Object other) {
        return other instanceof JsonNumber number && value.compareTo(number.value) == 0;
    }

    @Override public int hashCode() { return value.stripTrailingZeros().hashCode(); }

    @Override public String toString() { return value.toString(); }
}
