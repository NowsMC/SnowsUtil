package space.nows.util.io;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import space.nows.util.error.CapacityExceededException;
import space.nows.util.error.InsufficientDataException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class IoExtrasTest {
    @Test void varIntRoundTripAndSizes() {
        int[] values = {0, 1, 127, 128, 16383, 16384, Integer.MAX_VALUE, -1, Integer.MIN_VALUE, 123456789};
        ByteBuf buf = new ByteBuf();
        for (int v : values) buf.writeVarInt(v);
        for (int v : values) assertEquals(v, buf.readVarInt());
        assertEquals(0, buf.readableBytes());
        assertEquals(1, new ByteBuf().writeVarInt(127).readableBytes());
        assertEquals(2, new ByteBuf().writeVarInt(128).readableBytes());
        assertEquals(5, new ByteBuf().writeVarInt(-1).readableBytes());
    }

    @Test void varLongRoundTrip() {
        Random r = new Random(1);
        long[] values = new long[2000];
        for (int i = 0; i < values.length; i++) values[i] = r.nextLong() >> r.nextInt(64);
        ByteBuf buf = new ByteBuf(1);
        for (long v : values) buf.writeVarLong(v);
        for (long v : values) assertEquals(v, buf.readVarLong());
        assertEquals(10, new ByteBuf().writeVarLong(-1L).readableBytes());
    }

    @Test void zigZagKeepsSmallNegativesShort() {
        ByteBuf buf = new ByteBuf();
        buf.writeZigZagInt(-1).writeZigZagInt(Integer.MIN_VALUE).writeZigZagInt(Integer.MAX_VALUE).writeZigZagLong(Long.MIN_VALUE).writeZigZagLong(-64);
        assertEquals(1, new ByteBuf().writeZigZagInt(-1).readableBytes());
        assertEquals(-1, buf.readZigZagInt());
        assertEquals(Integer.MIN_VALUE, buf.readZigZagInt());
        assertEquals(Integer.MAX_VALUE, buf.readZigZagInt());
        assertEquals(Long.MIN_VALUE, buf.readZigZagLong());
        assertEquals(-64L, buf.readZigZagLong());
    }

    @Test void truncatedAndOversizedVarIntsConsumeNothing() {
        ByteBuf truncated = new ByteBuf(new byte[] {(byte) 0x80, (byte) 0x80});
        assertThrows(InsufficientDataException.class, truncated::readVarInt);
        assertEquals(0, truncated.readerIndex());
        ByteBuf tooLong = new ByteBuf(new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, 0x01});
        assertThrows(IllegalArgumentException.class, tooLong::readVarInt);
        assertEquals(0, tooLong.readerIndex());
        ByteBuf overflow = new ByteBuf(new byte[] {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, 0x7F});
        assertThrows(IllegalArgumentException.class, overflow::readVarInt);
        byte[] longOverflow = new byte[10];
        java.util.Arrays.fill(longOverflow, (byte) 0xFF);
        longOverflow[9] = 0x03;
        assertThrows(IllegalArgumentException.class, new ByteBuf(longOverflow)::readVarLong);
    }

    @Test void stringsWithLengthPrefix() {
        ByteBuf buf = new ByteBuf();
        buf.writeString("hello").writeString("").writeString("สวัสดี");
        assertEquals("hello", buf.readString(100));
        assertEquals("", buf.readString(100));
        assertEquals("สวัสดี", buf.readString(100));
        ByteBuf limited = new ByteBuf().writeString("abcdef");
        assertThrows(IllegalArgumentException.class, () -> limited.readString(3));
        assertEquals(0, limited.readerIndex());
        ByteBuf shortBuf = new ByteBuf(new byte[] {5, 'a', 'b'});
        assertThrows(InsufficientDataException.class, () -> shortBuf.readString(10));
        assertEquals(0, shortBuf.readerIndex());
    }

    @Test void byteBufferBridges() {
        ByteBuffer src = ByteBuffer.allocateDirect(6);
        src.put(new byte[] {1, 2, 3, 4, 5, 6}).flip();
        src.get();
        ByteBuf buf = new ByteBuf();
        buf.writeFrom(src);
        assertFalse(src.hasRemaining());
        assertArrayEquals(new byte[] {2, 3, 4, 5, 6}, buf.toByteArray());
        ByteBuffer dst = ByteBuffer.allocate(3);
        buf.readInto(dst);
        assertArrayEquals(new byte[] {2, 3, 4}, dst.array());
        assertThrows(InsufficientDataException.class, () -> buf.readInto(ByteBuffer.allocate(3)));
        ByteBuffer direct = buf.toDirectBuffer();
        assertTrue(direct.isDirect());
        assertEquals(ByteOrder.nativeOrder(), direct.order());
        assertEquals(2, direct.remaining());
        assertEquals(2, buf.readableBytes());
        ByteBuffer original = ByteBuffer.wrap(new byte[] {9, 8, 7});
        ByteBuf copy = ByteBuf.copyOf(original);
        assertEquals(3, original.remaining());
        assertArrayEquals(new byte[] {9, 8, 7}, copy.toByteArray());
    }

    @Test void fileHelpers(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("data.bin");
        byte[] data = new byte[5000];
        new Random(2).nextBytes(data);
        FileIO.writeFileAtomic(file, data);
        assertArrayEquals(data, Files.readAllBytes(file));
        assertArrayEquals(data, FileIO.readFile(file, 5000));
        assertThrows(CapacityExceededException.class, () -> FileIO.readFile(file, 4999));
        assertArrayEquals(java.util.Arrays.copyOfRange(data, 100, 612), FileIO.readRange(file, 100, 512));
        assertThrows(EOFException.class, () -> FileIO.readRange(file, 4990, 20));
        assertEquals(0, FileIO.readRange(file, 0, 0).length);
        FileIO.writeFileAtomic(file, new byte[] {1, 2, 3});
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(file));
        try (var files = Files.list(dir)) {
            assertEquals(1, files.count());
        }
        Files.write(file, data);
        MappedByteBuffer mapped = FileIO.mapReadOnly(file);
        assertEquals(5000, mapped.remaining());
        assertTrue(mapped.isReadOnly());
        assertEquals(data[4321], mapped.get(4321));
    }

    @Test void streamsCopyUsesTransferTo() throws IOException {
        byte[] data = new byte[100_000];
        new Random(3).nextBytes(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(data.length, Streams.copy(new ByteArrayInputStream(data), out));
        assertArrayEquals(data, out.toByteArray());
        assertThrows(NullPointerException.class, () -> Streams.copy(null, out));
    }
}
