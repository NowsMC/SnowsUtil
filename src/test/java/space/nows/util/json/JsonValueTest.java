package space.nows.util.json;

import org.junit.jupiter.api.Test;
import space.nows.util.error.JsonException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JsonValueTest {
    @Test
    void objectsKeepInsertionOrderAndExposeReadOnlyViews() {
        JsonObject object = new JsonObject().put("b", 2).put("a", 1);

        assertEquals(List.of("b", "a"), List.copyOf(object.keys()));
        assertThrows(UnsupportedOperationException.class, () -> object.asMap().clear());
        assertThrows(UnsupportedOperationException.class, () -> object.iterator().remove());
        assertThrows(UnsupportedOperationException.class, () -> object.keys().remove("a"));
    }

    @Test
    void typedAccessorsReportMissingAndMismatchedValues() {
        JsonObject object = new JsonObject().put("value", 3);

        assertEquals(3, object.getInt("value"));
        assertThrows(JsonException.class, () -> object.getString("missing"));
        assertThrows(JsonException.class, () -> object.get("value").asBoolean());
        assertThrows(JsonException.class, () -> object.getString("value"));
        assertThrows(JsonException.class, () -> object.getArray("value"));
        assertEquals(JsonType.NUMBER, object.get("value").type());
    }

    @Test
    void numberAccessorsRejectOverflowAndFractions() {
        assertThrows(JsonException.class, () -> JsonNumber.of(3_000_000_000L).asInt());
        assertThrows(JsonException.class, () -> JsonNumber.of(300).asByte());
        assertThrows(JsonException.class, () -> JsonNumber.of(1.5).asBigInteger());
        assertThrows(IllegalArgumentException.class, () -> JsonNumber.of(Double.NaN));
        assertEquals(new BigInteger("123456789012345678901234567890"),
                JsonNumber.of(new BigInteger("123456789012345678901234567890")).asBigInteger());
    }

    @Test
    void numbersCompareByValueNotScale() {
        assertEquals(JsonNumber.of(new BigDecimal("1.0")), JsonNumber.of(1));
        assertEquals(JsonNumber.of(new BigDecimal("1.0")).hashCode(), JsonNumber.of(1).hashCode());
    }

    @Test
    void arraysAndNullsCompareByValue() {
        JsonArray array = new JsonArray().add("x").addNull().add(false);

        assertEquals(array, Json.parse(Json.write(array)));
        assertEquals(JsonType.ARRAY, array.type());
        assertSame(JsonNull.INSTANCE, Json.parse("null"));
    }
}
