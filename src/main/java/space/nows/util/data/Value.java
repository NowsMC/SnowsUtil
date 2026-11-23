package space.nows.util.data;

import space.nows.util.error.DataException;

/**
 * A value in a format-neutral tree shared by all text formats. The typed accessors throw {@link DataException}
 * when the value has a different type.
 */
public sealed interface Value permits Obj, Arr, Str, Num, Bool, Null {
    /** Maximum nesting depth of arrays and objects, enforced by parsers, writers and the mapper. */
    int MAX_DEPTH = 512;

    Kind type();

    default Obj asObject() { throw mismatch(Kind.OBJECT); }

    default Arr asArray() { throw mismatch(Kind.ARRAY); }

    default String asString() { throw mismatch(Kind.STRING); }

    default Num asNumber() { throw mismatch(Kind.NUMBER); }

    default boolean asBoolean() { throw mismatch(Kind.BOOLEAN); }

    private DataException mismatch(Kind expected) {
        return new DataException("expected " + expected.label() + " but got " + type().label());
    }
}
