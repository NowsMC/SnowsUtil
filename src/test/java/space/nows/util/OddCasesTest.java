package space.nows.util;

import org.junit.jupiter.api.Test;
import space.nows.util.collections.IntArrayDeque;
import space.nows.util.collections.IntHashSet;
import space.nows.util.collections.IntIntMap;
import space.nows.util.collections.IntList;
import space.nows.util.collections.IntMinHeap;
import space.nows.util.concurrent.MpmcQueue;
import space.nows.util.concurrent.ObjectPool;
import space.nows.util.concurrent.SpscQueue;
import space.nows.util.data.Num;
import space.nows.util.data.Value;
import space.nows.util.data.mapping.Mapper;
import space.nows.util.error.CapacityExceededException;
import space.nows.util.error.DataException;
import space.nows.util.io.ByteBuf;
import space.nows.util.io.FileIO;
import space.nows.util.io.Gzip;
import space.nows.util.json.Json;
import space.nows.util.text.Hex;

import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class OddCasesTest {
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
        assertEquals(0.0, mapper.fromData(mapper.toData(-0.0), Double.class), "BigDecimal has no negative zero");
        assertEquals(Long.MIN_VALUE, mapper.fromData(mapper.toData(Long.MIN_VALUE), Long.class));
    }

    @Test
    void emptyAndWhitespaceDocumentsAreRejected() {
        assertThrows(DataException.class, () -> Json.parse("  "));
        assertThrows(DataException.class, () -> Json.parse(new byte[0]));
    }

    @Test
    void numbersEqualByValueNotText() {
        assertEquals(Json.parse("0"), Json.parse("-0"));
        assertEquals(Json.parse("1.0"), Json.parse("1"));
        assertEquals(Json.parse("1").hashCode(), Json.parse("1.00").hashCode());
    }

    @Test
    void byteBufPositionInvariants() {
        ByteBuf buf = new ByteBuf(4).writeInt(7);
        assertThrows(IndexOutOfBoundsException.class, () -> buf.readerIndex(5));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.writerIndex(-1));
        assertEquals(7, buf.readInt());
        buf.compact();
        assertEquals(0, buf.readableBytes());
        assertThrows(IllegalArgumentException.class, () -> buf.ensureWritable(-1));
    }

    @Test
    void byteBufWrappedArrayGrowsWithoutCorruptingCaller() {
        byte[] wrapped = {1, 2, 3};
        ByteBuf buf = new ByteBuf(wrapped);
        buf.writeByte(4).writeByte(5);
        assertEquals(5, buf.readableBytes());
        assertArrayEquals(new byte[]{1, 2, 3, 4, 5}, buf.readBytes(5));
    }

    @Test
    void byteBufRefusesAbsurdReservations() {
        ByteBuf buf = new ByteBuf().writeByte(1);
        assertThrows(CapacityExceededException.class, () -> buf.ensureWritable(Integer.MAX_VALUE));
    }

    @Test
    void emptyStringsInByteBuf() {
        ByteBuf buf = new ByteBuf().writeString("");
        assertEquals("", buf.readString(0));
        assertEquals(0, new ByteBuf().writeUtf8(""));
    }

    @Test
    void readStringLeavesBufferUntouchedWhenTooLong() {
        ByteBuf buf = new ByteBuf().writeString("abcdef");
        int before = buf.readerIndex();
        assertThrows(IllegalArgumentException.class, () -> buf.readString(3));
        assertEquals(before, buf.readerIndex());
        assertEquals("abcdef", buf.readString(10));
    }

    @Test
    void fileRangeAndWholeFileEdges(@org.junit.jupiter.api.io.TempDir Path dir) throws IOException {
        Path file = dir.resolve("data.bin");
        Files.write(file, new byte[]{1, 2, 3});
        assertArrayEquals(new byte[0], FileIO.readRange(file, 3, 0));
        assertArrayEquals(new byte[0], FileIO.readRange(file, 4, 0), "zero-length read past EOF");
        assertThrows(EOFException.class, () -> FileIO.readRange(file, 3, 1));
        assertArrayEquals(new byte[]{1, 2, 3}, FileIO.readFile(file, 3));
        assertThrows(Exception.class, () -> FileIO.readFile(file, 2));
    }

    @Test
    void emptyFileIsMappableAndAtomicWriteReplaces(@org.junit.jupiter.api.io.TempDir Path dir) throws IOException {
        Path file = dir.resolve("empty.bin");
        Files.write(file, new byte[0]);
        assertEquals(0, FileIO.mapReadOnly(file).capacity());
        FileIO.writeFileAtomic(file, new byte[]{9});
        assertArrayEquals(new byte[]{9}, Files.readAllBytes(file));
        assertEquals(0, Files.list(dir).filter(p -> p.toString().endsWith(".tmp")).count(), "temporary file left behind");
    }

    @Test
    void gzipEdgeCases() throws IOException {
        byte[] empty = Gzip.gzip(new byte[0]);
        assertTrue(Gzip.isGzip(empty));
        assertArrayEquals(new byte[0], Gzip.gunzip(empty));
        assertFalse(Gzip.isGzip(new byte[0]));
        assertThrows(IOException.class, () -> Gzip.gunzip(new byte[]{1, 2, 3}));
        byte[] payload = new byte[1000];
        byte[] packed = Gzip.gzip(payload);
        assertArrayEquals(payload, Gzip.gunzip(packed, 1000), "exact limit must succeed");
        assertThrows(Exception.class, () -> Gzip.gunzip(packed, 999), "one byte over the limit must fail");
    }

    @Test
    void hexEdgeCases() {
        assertArrayEquals(new byte[0], Hex.fromHex(""));
        assertFalse(Hex.isHex(null));
        assertTrue(Hex.isHex(""));
    }

    @Test
    void intListAndHeapRejectEmptyAccess() {
        IntList list = new IntList();
        assertThrows(RuntimeException.class, list::first, "first() on empty IntList");
        assertThrows(RuntimeException.class, list::last, "last() on empty IntList");
        assertThrows(IndexOutOfBoundsException.class, () -> list.removeAt(-1));
        IntMinHeap heap = new IntMinHeap();
        assertThrows(NoSuchElementException.class, heap::poll);
        assertThrows(RuntimeException.class, heap::peek, "peek() on empty heap");
        IntArrayDeque deque = new IntArrayDeque(0);
        assertThrows(NoSuchElementException.class, deque::removeFirst);
        assertThrows(IndexOutOfBoundsException.class, () -> deque.get(0));
        deque.addFirst(5);
        assertEquals(5, deque.last());
    }

    @Test
    void setAndMapOddConfigurations() {
        assertThrows(IllegalArgumentException.class, () -> new IntHashSet(4, 1.5f));
        assertThrows(IllegalArgumentException.class, () -> new IntHashSet(-1));
        IntIntMap map = new IntIntMap(0, 0.5f, -7);
        assertEquals(-7, map.get(123));
        map.put(0, 0);
        assertEquals(0, map.get(0));
        assertTrue(map.containsKey(0));
        map.remove(0);
        assertFalse(map.containsKey(0));
        assertEquals(0, map.size());
    }

    @Test
    void queuesAndPoolAtBoundaries() {
        SpscQueue<String> one = new SpscQueue<>(1);
        assertEquals(1, one.capacity());
        assertTrue(one.offer("a"));
        assertFalse(one.offer("b"));
        assertEquals("a", one.poll());
        assertThrows(IllegalArgumentException.class, () -> new SpscQueue<>(0));
        assertThrows(IllegalArgumentException.class, () -> new SpscQueue<>((1 << 30) + 1));
        assertThrows(NullPointerException.class, () -> one.offer(null));
        MpmcQueue<String> mpmc = new MpmcQueue<>(1);
        int accepted = 0;
        while (mpmc.offer("x")) accepted++;
        assertEquals(mpmc.capacity(), accepted, "queue must hold exactly its reported capacity");
        ObjectPool<StringBuilder> pool = new ObjectPool<>(0, StringBuilder::new);
        assertFalse(pool.release(new StringBuilder()), "zero-sized pool retains nothing");
        assertEquals(0, pool.idleCount());
        assertEquals(0, pool.maxIdle());
    }

    @Test
    void valueEqualityAcrossKinds() {
        Value num = Num.of(3);
        assertNotEquals(num, new space.nows.util.data.Str("3"));
        assertNotEquals(Json.parse("[1]"), Json.parse("{\"0\":1}"));
    }
}
