package space.nows.util.data;

import org.junit.jupiter.api.Test;
import space.nows.util.error.DataException;
import space.nows.util.json.Json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValueTest {
    @Test
    void objectsKeepInsertionOrderAndExposeReadOnlyViews() {
        Obj object = new Obj().put("b", 2).put("a", 1);

        assertEquals(List.of("b", "a"), List.copyOf(object.keys()));
        assertThrows(UnsupportedOperationException.class, () -> object.asMap().clear());
        assertThrows(UnsupportedOperationException.class, () -> object.iterator().remove());
        assertThrows(UnsupportedOperationException.class, () -> object.keys().remove("a"));
    }

    @Test
    void typedAccessorsReportMissingAndMismatchedValues() {
        Obj object = new Obj().put("value", 3);

        assertEquals(3, object.getInt("value"));
        assertThrows(DataException.class, () -> object.getString("missing"));
        assertThrows(DataException.class, () -> object.get("value").asBoolean());
        assertThrows(DataException.class, () -> object.getString("value"));
        assertThrows(DataException.class, () -> object.getArray("value"));
        assertEquals(Kind.NUMBER, object.get("value").type());
    }

    @Test
    void numberAccessorsRejectOverflowAndFractions() {
        assertThrows(DataException.class, () -> Num.of(3_000_000_000L).asInt());
        assertThrows(DataException.class, () -> Num.of(300).asByte());
        assertThrows(DataException.class, () -> Num.of(1.5).asBigInteger());
        assertThrows(IllegalArgumentException.class, () -> Num.of(Double.NaN));
        assertEquals(new BigInteger("123456789012345678901234567890"),
                Num.of(new BigInteger("123456789012345678901234567890")).asBigInteger());
    }

    @Test
    void numbersCompareByValueNotScale() {
        assertEquals(Num.of(new BigDecimal("1.0")), Num.of(1));
        assertEquals(Num.of(new BigDecimal("1.0")).hashCode(), Num.of(1).hashCode());
    }

    @Test
    void arraysAndNullsCompareByValue() {
        Arr array = new Arr().add("x").addNull().add(false);

        assertEquals(array, Json.parse(Json.write(array)));
        assertEquals(Kind.ARRAY, array.type());
        assertSame(Null.INSTANCE, Json.parse("null"));
    }
}
