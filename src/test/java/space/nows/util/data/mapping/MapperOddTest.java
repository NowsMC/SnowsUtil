package space.nows.util.data.mapping;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import space.nows.util.collections.IntArrayDeque;
import space.nows.util.collections.IntHashSet;
import space.nows.util.collections.IntIntMap;
import space.nows.util.collections.IntList;
import space.nows.util.collections.IntMinHeap;
import space.nows.util.concurrent.MpmcQueue;
import space.nows.util.concurrent.ObjectPool;
import space.nows.util.concurrent.SpscQueue;
import space.nows.util.data.mapping.Mapper;
import space.nows.util.data.Num;
import space.nows.util.data.Value;
import space.nows.util.error.CapacityExceededException;
import space.nows.util.error.DataException;
import space.nows.util.io.ByteBuf;
import space.nows.util.io.FileIO;
import space.nows.util.io.Gzip;
import space.nows.util.json.Json;
import space.nows.util.text.Hex;
import static org.junit.jupiter.api.Assertions.*;

class MapperOddTest {
    record Pair(int a, int b) {}

    record WithIgnored(String name, @space.nows.util.data.mapping.Ignore String secret) {}

    record Holder(int[] values, List<Integer> boxed, Map<String, Integer> counts) {}

    static class NoDefaultConstructor {
        final int x;

        NoDefaultConstructor(int x) {
            this.x = x;
        }
    }

    static abstract class Abstract {
        int x;
    }

    static class Node {
        Node next;
        int value;
    }

    static class Empty {}

    enum NoConstants {}

    static class Chars {
        char c;
    }


    @Test
    void ignoredRecordComponentIsOmittedAndDefaulted() {
        Mapper mapper = new Mapper();
        WithIgnored original = new WithIgnored("n", "s");
        assertEquals("{\"name\":\"n\"}", Json.write(mapper.toData(original)));
        assertEquals(new WithIgnored("n", null), mapper.fromData(Json.parse("{\"name\":\"n\",\"secret\":\"x\"}"), WithIgnored.class));
    }

    @Test
    void missingNoArgConstructorIsADataException() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"x\":1}"), NoDefaultConstructor.class));
    }

    @Test
    void abstractAndInterfaceTargetsAreDataExceptions() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{}"), Abstract.class));
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{}"), Runnable.class));
    }

    @Test
    void emptyBeanRoundTrips() {
        Mapper mapper = new Mapper();
        assertEquals("{}", Json.write(mapper.toData(new Empty())));
        assertInstanceOf(Empty.class, mapper.fromData(Json.parse("{}"), Empty.class));
    }

    @Test
    void enumWithoutConstantsRejectsEveryName() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("\"x\""), NoConstants.class));
    }

    @Test
    void jsonNullIntoPrimitiveIsRejectedNotZeroed() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"values\":[null],\"boxed\":[null],\"counts\":{}}"), Holder.class),
                "null inside int[] silently becomes 0");
    }

    @Test
    void jsonNullForPrimitiveFieldIsRejectedNotDefaulted() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"a\":null,\"b\":1}"), Pair.class),
                "null for int silently becomes 0");
    }

    @Test
    void nullMapValuesAndNullListElementsRoundTrip() {
        Mapper mapper = new Mapper();
        Holder original = new Holder(new int[]{1, 2}, java.util.Arrays.asList(1, null), java.util.Collections.singletonMap("k", null));
        Holder back = mapper.fromData(Json.parse(Json.write(mapper.toData(original))), Holder.class);
        assertEquals(java.util.Arrays.asList(1, null), back.boxed());
        assertNull(back.counts().get("k"));
    }

    @Test
    void charFromEmptyOrNullStringBehaviour() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"c\":\"\"}"), Chars.class));
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"c\":null}"), Chars.class),
                "null for char silently becomes NUL");
    }

    @Test
    void optionalRecordComponentAbsentAndNull() {
        record Opt(Optional<Integer> v) {}
        Mapper mapper = new Mapper();
        assertEquals(Optional.empty(), mapper.fromData(Json.parse("{}"), Opt.class).v());
        assertEquals(Optional.empty(), mapper.fromData(Json.parse("{\"v\":null}"), Opt.class).v());
        assertEquals(Optional.of(3), mapper.fromData(Json.parse("{\"v\":3}"), Opt.class).v());
    }

    @Test
    void cyclicGraphFailsCleanly() {
        Mapper mapper = new Mapper();
        Node a = new Node();
        Node b = new Node();
        a.next = b;
        b.next = a;
        assertThrows(DataException.class, () -> mapper.toData(a));
    }

    @Test
    void extraNestedPropertyIsRejectedInStrictMode() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"next\":{\"value\":1,\"oops\":2}}"), Node.class));
        assertEquals(1, Mapper.lenient().fromData(Json.parse("{\"next\":{\"value\":1,\"oops\":2}}"), Node.class).next.value);
    }

    @Test
    void largeLongsAndNegativeZeroRoundTrip() {
        Mapper mapper = new Mapper();
        long big = (1L << 53) + 1;
        assertEquals(big, mapper.fromData(mapper.toData(big), Long.class));
        assertEquals(-0.0, mapper.fromData(mapper.toData(-0.0), Double.class));
        assertEquals(-0.0f, mapper.fromData(mapper.toData(-0.0f), Float.class));
        assertEquals("-0", Json.write(mapper.toData(-0.0)));
        assertEquals(Long.MIN_VALUE, mapper.fromData(mapper.toData(Long.MIN_VALUE), Long.class));
    }

}
