package space.nows.util.data;

import space.nows.util.check.Preconditions;
import space.nows.util.error.DataException;

import java.math.BigDecimal;
import java.math.BigInteger;

/** A JSON number held as an exact {@link BigDecimal}. Narrowing accessors throw {@link DataException} on overflow or loss. */
public final class DataNumber implements DataValue {
    private final BigDecimal value;

    private DataNumber(BigDecimal value) {
        this.value = value;
    }

    public static DataNumber of(int value) { return new DataNumber(BigDecimal.valueOf(value)); }

    public static DataNumber of(long value) { return new DataNumber(BigDecimal.valueOf(value)); }

    public static DataNumber of(double value) {
        Preconditions.argument(Double.isFinite(value), "JSON numbers must be finite but was " + value);
        return new DataNumber(BigDecimal.valueOf(value));
    }

    public static DataNumber of(BigDecimal value) { return new DataNumber(Preconditions.notNull(value, "value")); }

    public static DataNumber of(BigInteger value) {
        return new DataNumber(new BigDecimal(Preconditions.notNull(value, "value")));
    }

    @Override public DataType type() { return DataType.NUMBER; }

    @Override public DataNumber asNumber() { return this; }

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
            throw new DataException("number " + value + " is not an integer", e);
        }
    }

    private DataException outOfRange(String target, ArithmeticException cause) {
        return new DataException("number " + value + " does not fit in " + target, cause);
    }

    @Override public boolean equals(Object other) {
        return other instanceof DataNumber number && value.compareTo(number.value) == 0;
    }

    @Override public int hashCode() { return value.stripTrailingZeros().hashCode(); }

    @Override public String toString() { return value.toString(); }
}
