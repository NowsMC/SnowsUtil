package space.nows.util.data;

import org.junit.jupiter.api.Test;
import space.nows.util.error.DataException;
import space.nows.util.json.Json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DataValueTest {
    @Test
    void objectsKeepInsertionOrderAndExposeReadOnlyViews() {
        DataObject object = new DataObject().put("b", 2).put("a", 1);

        assertEquals(List.of("b", "a"), List.copyOf(object.keys()));
        assertThrows(UnsupportedOperationException.class, () -> object.asMap().clear());
        assertThrows(UnsupportedOperationException.class, () -> object.iterator().remove());
        assertThrows(UnsupportedOperationException.class, () -> object.keys().remove("a"));
    }

    @Test
    void typedAccessorsReportMissingAndMismatchedValues() {
        DataObject object = new DataObject().put("value", 3);

        assertEquals(3, object.getInt("value"));
        assertThrows(DataException.class, () -> object.getString("missing"));
        assertThrows(DataException.class, () -> object.get("value").asBoolean());
        assertThrows(DataException.class, () -> object.getString("value"));
        assertThrows(DataException.class, () -> object.getArray("value"));
        assertEquals(DataType.NUMBER, object.get("value").type());
    }

    @Test
    void numberAccessorsRejectOverflowAndFractions() {
        assertThrows(DataException.class, () -> DataNumber.of(3_000_000_000L).asInt());
        assertThrows(DataException.class, () -> DataNumber.of(300).asByte());
        assertThrows(DataException.class, () -> DataNumber.of(1.5).asBigInteger());
        assertThrows(IllegalArgumentException.class, () -> DataNumber.of(Double.NaN));
        assertEquals(new BigInteger("123456789012345678901234567890"),
                DataNumber.of(new BigInteger("123456789012345678901234567890")).asBigInteger());
    }

    @Test
    void numbersCompareByValueNotScale() {
        assertEquals(DataNumber.of(new BigDecimal("1.0")), DataNumber.of(1));
        assertEquals(DataNumber.of(new BigDecimal("1.0")).hashCode(), DataNumber.of(1).hashCode());
    }

    @Test
    void arraysAndNullsCompareByValue() {
        DataArray array = new DataArray().add("x").addNull().add(false);

        assertEquals(array, Json.parse(Json.write(array)));
        assertEquals(DataType.ARRAY, array.type());
        assertSame(DataNull.INSTANCE, Json.parse("null"));
    }
}
