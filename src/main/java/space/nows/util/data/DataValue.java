package space.nows.util.data;

import space.nows.util.error.DataException;

/**
 * A value in a format-neutral tree shared by all text formats. The typed accessors throw {@link DataException}
 * when the value has a different type.
 */
public sealed interface DataValue permits DataObject, DataArray, DataString, DataNumber, DataBoolean, DataNull {
    /** Maximum nesting depth of arrays and objects, enforced by parsers, writers and the mapper. */
    int MAX_DEPTH = 512;

    DataType type();

    default DataObject asObject() { throw mismatch(DataType.OBJECT); }

    default DataArray asArray() { throw mismatch(DataType.ARRAY); }

    default String asString() { throw mismatch(DataType.STRING); }

    default DataNumber asNumber() { throw mismatch(DataType.NUMBER); }

    default boolean asBoolean() { throw mismatch(DataType.BOOLEAN); }

    private DataException mismatch(DataType expected) {
        return new DataException("expected " + expected.label() + " but got " + type().label());
    }
}
