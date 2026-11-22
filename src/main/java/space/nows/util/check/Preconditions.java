package space.nows.util.check;

import java.util.Objects;

/**
 * Argument and state checks. Index/range checks delegate to the JDK's intrinsified
 * {@link Objects} methods; messages are only built on the failure path.
 */
public final class Preconditions {
    private Preconditions() {}

    public static <T> T notNull(T value, String name) {
        if (value == null) throw new NullPointerException(name + " must not be null");
        return value;
    }

    /** {@code 0 <= index < length}. */
    public static int index(int index, int length) { return Objects.checkIndex(index, length); }

    /** {@code 0 <= position <= length} (insertion point). */
    public static int position(int position, int length) {
        if (position < 0 || position > length) {
            throw new IndexOutOfBoundsException("position " + position + " out of bounds for length " + length + " (inclusive)");
        }
        return position;
    }

    /** {@code [offset, offset + count)} lies inside {@code [0, length)}; overflow-safe. */
    public static void range(int offset, int count, int length) { Objects.checkFromIndexSize(offset, count, length); }

    public static int nonNegative(int value, String name) {
        if (value < 0) throw new IllegalArgumentException(name + " must be >= 0 but was " + value);
        return value;
    }

    public static int positive(int value, String name) {
        if (value <= 0) throw new IllegalArgumentException(name + " must be > 0 but was " + value);
        return value;
    }

    public static float finite(float value, String name) {
        if (!Float.isFinite(value)) throw new IllegalArgumentException(name + " must be finite but was " + value);
        return value;
    }

    public static void argument(boolean condition, String message) {
        if (!condition) throw new IllegalArgumentException(message);
    }

    public static void state(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    public static <T extends CharSequence> T notEmpty(T value, String name) {
        notNull(value, name);
        if (value.length() == 0) throw new IllegalArgumentException(name + " must not be empty");
        return value;
    }

    public static <T extends CharSequence> T notBlank(T value, String name) {
        notNull(value, name);
        for (int i = 0, n = value.length(); i < n; i++) if (!Character.isWhitespace(value.charAt(i))) return value;
        throw new IllegalArgumentException(name + " must not be blank");
    }
}
