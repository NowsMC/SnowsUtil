package space.nows.util.data.mapping;

import org.junit.jupiter.api.Test;
import space.nows.util.error.DataException;
import space.nows.util.io.ByteBuf;
import space.nows.util.io.Streams;
import space.nows.util.json.Json;

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class MapperEdgeTest {
    static class Base {
        String name = "base";
    }

    static class Derived extends Base {
        String name = "derived";
    }

    record WithMap(Map<Integer, String> labels) {}

    @Test
    void mapWithNonStringKeysRoundTrips() {
        Mapper mapper = new Mapper();
        WithMap original = new WithMap(Map.of(1, "one", 2, "two"));
        String json = Json.write(mapper.toData(original));
        WithMap back = mapper.fromData(Json.parse(json), WithMap.class);
        assertEquals(original, back, json);
    }

    @Test
    void shadowedFieldsAreRejectedInsteadOfLost() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.toData(new Derived()));
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("{\"name\":\"x\"}"), Derived.class));
    }

    enum Level {
        @Property("low") LOW,
        HIGH
    }

    record ByLevel(Map<Level, Integer> counts) {}

    @Test
    void enumMapKeysRoundTripWithTheirPropertyNames() {
        Mapper mapper = new Mapper();
        ByLevel original = new ByLevel(Map.of(Level.LOW, 1, Level.HIGH, 2));
        String json = Json.write(mapper.toData(original));
        assertTrue(json.contains("\"low\":1"), json);
        assertEquals(original, mapper.fromData(Json.parse(json), ByLevel.class));
    }

    @Test
    void floatOverflowIsReportedNotSilenced() {
        Mapper mapper = new Mapper();
        assertThrows(DataException.class, () -> mapper.fromData(Json.parse("1e40"), Float.class));
    }

    @Test
    void byteBufMatchesByteBufferForAllWidths() {
        Random r = new Random(41);
        for (int trial = 0; trial < 3000; trial++) {
            int s = r.nextInt();
            long l = r.nextLong();
            short sh = (short) r.nextInt();

            ByteBuf be = new ByteBuf(1).writeShort(sh).writeInt(s).writeLong(l);
            ByteBuffer beRef = ByteBuffer.allocate(14).order(ByteOrder.BIG_ENDIAN).putShort(sh).putInt(s).putLong(l).flip();
            assertArrayEquals(bytes(beRef), be.readBytes(be.readableBytes()), "big endian trial " + trial);

            ByteBuf le = new ByteBuf(1).writeShortLE(sh).writeIntLE(s).writeLongLE(l);
            ByteBuffer leRef = ByteBuffer.allocate(14).order(ByteOrder.LITTLE_ENDIAN).putShort(sh).putInt(s).putLong(l).flip();
            assertArrayEquals(bytes(leRef), le.readBytes(le.readableBytes()), "little endian trial " + trial);
        }
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] out = new byte[buffer.remaining()];
        buffer.get(out);
        return out;
    }

    @Test
    void zigZagAndVarIntRoundTripExtremes() {
        int[] ints = {0, 1, -1, Integer.MAX_VALUE, Integer.MIN_VALUE, 1 << 28, -(1 << 28)};
        for (int v : ints) {
            ByteBuf b = new ByteBuf().writeZigZagInt(v);
            assertEquals(v, b.readZigZagInt(), "zigzag " + v);
        }
        long[] longs = {0, -1, Long.MAX_VALUE, Long.MIN_VALUE, 1L << 56};
        for (long v : longs) {
            ByteBuf b = new ByteBuf().writeZigZagLong(v);
            assertEquals(v, b.readZigZagLong(), "zigzag " + v);
        }
    }

    @Test
    void streamsRespectExactSizeLimits() throws Exception {
        assertArrayEquals(new byte[]{1, 2, 3}, Streams.readAll(new ByteArrayInputStream(new byte[]{1, 2, 3}), 3));
        assertThrows(Exception.class, () -> Streams.readAll(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}), 3));
        assertArrayEquals(new byte[0], Streams.readAll(new ByteArrayInputStream(new byte[0]), 0));
        assertThrows(Exception.class, () -> Streams.readAll(new ByteArrayInputStream(new byte[]{1}), 0));
    }

    @Test
    void bigIntegerEncodingRoundTrips() {
        Mapper mapper = new Mapper();
        BigInteger big = new BigInteger("-123456789012345678901234567890");
        assertEquals(big, mapper.fromData(mapper.toData(big), BigInteger.class));
    }
}
