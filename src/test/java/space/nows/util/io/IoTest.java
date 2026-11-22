package space.nows.util.io;

import org.junit.jupiter.api.Test;
import space.nows.util.error.CapacityExceededException;
import space.nows.util.error.InsufficientDataException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class IoTest {
    @Test void byteAccess() {
        byte[] b = new byte[16];
        ByteAccess.writeInt(b, 0, 0x01020304);
        ByteAccess.writeLongLE(b, 8, 0x1122334455667788L);
        assertEquals(1, b[0]);
        assertEquals(0x01020304, ByteAccess.readInt(b, 0));
        assertEquals(0x04030201, ByteAccess.readIntLE(b, 0));
        assertEquals(0x1122334455667788L, ByteAccess.readLongLE(b, 8));
    }

    @Test void byteAccessBoundsAreCheckedByTheJdk() {
        assertThrows(IndexOutOfBoundsException.class, () -> ByteAccess.readInt(new byte[3], 0));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteAccess.readInt(new byte[8], -1));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteAccess.writeLong(new byte[7], 0, 1L));
        assertThrows(IndexOutOfBoundsException.class, () -> ByteAccess.readShort(new byte[8], 7));
    }

    @Test void byteBufMatchesByteBufferUnderRandomWrites() {
        Random r = new Random(31);
        for (int round = 0; round < 50; round++) {
            ByteBuf buf = new ByteBuf(1);
            ByteArrayOutputStream expected = new ByteArrayOutputStream();
            ByteBuffer be = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN), le = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i < 100; i++) {
                be.clear(); le.clear();
                switch (r.nextInt(9)) {
                    case 0 -> { int v = r.nextInt(); buf.writeByte(v); expected.write(v); }
                    case 1 -> { int v = r.nextInt(); buf.writeShort(v); be.putShort((short) v); expected.write(be.array(), 0, 2); }
                    case 2 -> { int v = r.nextInt(); buf.writeInt(v); be.putInt(v); expected.write(be.array(), 0, 4); }
                    case 3 -> { long v = r.nextLong(); buf.writeLong(v); be.putLong(v); expected.write(be.array(), 0, 8); }
                    case 4 -> { int v = r.nextInt(); buf.writeShortLE(v); le.putShort((short) v); expected.write(le.array(), 0, 2); }
                    case 5 -> { int v = r.nextInt(); buf.writeIntLE(v); le.putInt(v); expected.write(le.array(), 0, 4); }
                    case 6 -> { long v = r.nextLong(); buf.writeLongLE(v); le.putLong(v); expected.write(le.array(), 0, 8); }
                    case 7 -> { float v = r.nextFloat(); buf.writeFloat(v); be.putFloat(v); expected.write(be.array(), 0, 4); }
                    default -> { double v = r.nextDouble(); buf.writeDouble(v); be.putDouble(v); expected.write(be.array(), 0, 8); }
                }
            }
            assertArrayEquals(expected.toByteArray(), buf.toByteArray());
            assertEquals(expected.size(), buf.readableBytes());
        }
    }

    @Test void byteBufReadsWhatWasWritten() {
        ByteBuf buf = new ByteBuf(4);
        buf.writeInt(42).writeLong(-1L).writeByte(200).writeShort(65000).writeBoolean(true)
           .writeFloat(1.5f).writeDouble(-2.25).writeIntLE(0x01020304).writeShortLE(0x0A0B).writeLongLE(7L);
        assertEquals(42, buf.readInt());
        assertEquals(-1L, buf.readLong());
        assertEquals(200, buf.readUnsignedByte());
        assertEquals(65000, buf.readUnsignedShort());
        assertTrue(buf.readBoolean());
        assertEquals(1.5f, buf.readFloat());
        assertEquals(-2.25, buf.readDouble());
        assertEquals(0x01020304, buf.readIntLE());
        assertEquals(0x0A0B, buf.readShortLE());
        assertEquals(7L, buf.readLongLE());
        assertEquals(0, buf.readableBytes());
        buf.writeInt(-1);
        assertEquals(0xFFFFFFFFL, buf.readUnsignedInt());
    }

    @Test void insufficientDataIsReportedAndConsumesNothing() {
        ByteBuf buf = new ByteBuf();
        buf.writeShort(7);
        InsufficientDataException e = assertThrows(InsufficientDataException.class, buf::readInt);
        assertEquals(4, e.needed());
        assertEquals(2, e.available());
        assertEquals(2, buf.readableBytes());
        assertEquals(7, buf.readShort());
        assertThrows(IndexOutOfBoundsException.class, buf::readByte);
        assertThrows(IndexOutOfBoundsException.class, buf::peekByte);
        assertThrows(IndexOutOfBoundsException.class, () -> buf.skip(1));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.readBytes(1));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.readUtf8(1));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.readBytes(new byte[1]));
    }

    @Test void byteBufArgumentGuards() {
        assertThrows(IllegalArgumentException.class, () -> new ByteBuf(-1));
        assertThrows(NullPointerException.class, () -> new ByteBuf((byte[]) null));
        ByteBuf buf = new ByteBuf();
        assertThrows(IllegalArgumentException.class, () -> buf.ensureWritable(-1));
        CapacityExceededException c = assertThrows(CapacityExceededException.class, () -> buf.writeInt(1).ensureWritable(Integer.MAX_VALUE));
        assertTrue(c.requested() > c.limit());
        int before = buf.writerIndex();
        assertThrows(IndexOutOfBoundsException.class, () -> buf.writeBytes(new byte[4], 2, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.writeBytes(new byte[4], -1, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.writeBytes(new byte[4], 0, -1));
        assertThrows(NullPointerException.class, () -> buf.writeBytes(null));
        assertEquals(before, buf.writerIndex());
        assertThrows(IllegalArgumentException.class, () -> buf.readBytes(-1));
        assertThrows(IllegalArgumentException.class, () -> buf.skip(-1));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.readBytes(new byte[2], 1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.readerIndex(buf.writerIndex() + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.writerIndex(buf.capacity() + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> buf.writerIndex(-1));
    }

    @Test void byteBufWrapsAndGrows() {
        byte[] src = {1, 2, 3};
        ByteBuf buf = new ByteBuf(src);
        assertEquals(3, buf.readableBytes());
        assertEquals(1, buf.readByte());
        buf.writeBytes(new byte[100]);
        assertArrayEquals(new byte[]{1, 2, 3}, src);
        assertEquals(102, buf.readableBytes());
        assertTrue(buf.capacity() >= 103);
    }

    @Test void byteBufCompactSkipPeekAndIndexes() {
        ByteBuf buf = new ByteBuf();
        buf.writeBytes(new byte[]{1, 2, 3, 4, 5});
        buf.skip(2);
        assertEquals(3, buf.peekByte());
        assertEquals(2, buf.readerIndex());
        buf.compact();
        assertEquals(0, buf.readerIndex());
        assertEquals(3, buf.writerIndex());
        assertArrayEquals(new byte[]{3, 4, 5}, buf.toByteArray());
        assertEquals(3, buf.readableBytes());
        buf.readerIndex(1);
        assertEquals(4, buf.readByte());
        buf.reset();
        assertEquals(0, buf.readableBytes());
        assertTrue(buf.toString().contains("readerIndex=0"));
    }

    @Test void byteBufUtf8() {
        ByteBuf buf = new ByteBuf();
        String thai = "สวัสดี 😀 ok";
        int n = buf.writeUtf8(thai);
        assertEquals(thai.getBytes(java.nio.charset.StandardCharsets.UTF_8).length, n);
        assertEquals(thai, buf.readUtf8(n));
        assertEquals(0, buf.writeUtf8(""));
        assertEquals("", buf.readUtf8(0));
        assertThrows(NullPointerException.class, () -> buf.writeUtf8(null));
        buf.writeByte(0xFF);
        assertEquals("\uFFFD", buf.readUtf8(1));
    }

    @Test void byteBufJdkBridges() throws IOException {
        ByteBuf buf = new ByteBuf();
        buf.writeBytes(new byte[]{9, 8, 7, 6});
        buf.skip(1);
        ByteBuffer view = buf.asByteBuffer();
        assertTrue(view.isReadOnly());
        assertEquals(3, view.remaining());
        assertEquals(8, view.get());
        assertThrows(java.nio.ReadOnlyBufferException.class, () -> view.put((byte) 1));
        assertEquals(3, buf.readableBytes());

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(3, buf.writeTo(out));
        assertArrayEquals(new byte[]{8, 7, 6}, out.toByteArray());
        assertEquals(0, buf.readableBytes());

        ByteBuf in = new ByteBuf();
        in.readFrom(new ByteArrayInputStream(new byte[]{1, 2, 3, 4}), 3);
        assertArrayEquals(new byte[]{1, 2, 3}, in.toByteArray());
        ByteBuf shortRead = new ByteBuf();
        assertThrows(EOFException.class, () -> shortRead.readFrom(new ByteArrayInputStream(new byte[2]), 5));
        assertEquals(0, shortRead.readableBytes());
        assertThrows(IllegalArgumentException.class, () -> shortRead.readFrom(new ByteArrayInputStream(new byte[2]), -1));
    }

    @Test void copyRejectsABufferThatWouldSpinForever() throws IOException {
        byte[] data = new byte[1000];
        new Random(1).nextBytes(data);
        assertThrows(IllegalArgumentException.class, () -> Streams.copy(new ByteArrayInputStream(data), new ByteArrayOutputStream(), 0));
        assertThrows(IllegalArgumentException.class, () -> Streams.copy(new ByteArrayInputStream(data), new ByteArrayOutputStream(), -4));
        for (int size : new int[]{1, 7, 1000, 4096}) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertEquals(1000, Streams.copy(new ByteArrayInputStream(data), out, size));
            assertArrayEquals(data, out.toByteArray());
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(1000, Streams.copy(new ByteArrayInputStream(data), out));
        assertThrows(NullPointerException.class, () -> Streams.copy(null, out));
    }

    @Test void boundedReadAll() throws IOException {
        byte[] ten = new byte[10];
        assertEquals(10, Streams.readAll(new ByteArrayInputStream(ten), 10).length);
        assertEquals(10, Streams.readAll(new ByteArrayInputStream(ten), 1000).length);
        CapacityExceededException e = assertThrows(CapacityExceededException.class, () -> Streams.readAll(new ByteArrayInputStream(ten), 9));
        assertEquals(9, e.limit());
        assertEquals(0, Streams.readAll(new ByteArrayInputStream(new byte[0]), 0).length);
        assertThrows(CapacityExceededException.class, () -> Streams.readAll(new ByteArrayInputStream(ten), 0));
        assertThrows(IllegalArgumentException.class, () -> Streams.readAll(new ByteArrayInputStream(ten), -1));
        assertEquals(10, Streams.readAll(new ByteArrayInputStream(ten)).length);
    }

    @Test void readExactly() throws IOException {
        byte[] data = {1, 2, 3, 4};
        assertArrayEquals(new byte[]{1, 2}, Streams.readExactly(new ByteArrayInputStream(data), 2));
        assertArrayEquals(data, Streams.readExactly(new ByteArrayInputStream(data), 4));
        assertEquals(0, Streams.readExactly(new ByteArrayInputStream(data), 0).length);
        assertThrows(EOFException.class, () -> Streams.readExactly(new ByteArrayInputStream(data), 5));
        assertThrows(IllegalArgumentException.class, () -> Streams.readExactly(new ByteArrayInputStream(data), -1));
    }

    @Test void filesAndCloseQuietly() throws IOException {
        Path p = Files.createTempFile("snowsutil", ".bin");
        try {
            FileIO.writeFile(p, new byte[]{1, 2, 3});
            assertArrayEquals(new byte[]{1, 2, 3}, FileIO.readFile(p));
        } finally {
            Files.deleteIfExists(p);
        }
        assertThrows(NullPointerException.class, () -> FileIO.readFile(null));
        assertThrows(NullPointerException.class, () -> FileIO.writeFile(Path.of("x"), null));
        Streams.closeQuietly(null);
        Streams.closeQuietly(() -> { throw new IllegalStateException("boom"); });
    }

    @Test void gzipRoundTrip() throws IOException {
        byte[] data = new byte[10_000];
        new Random(2).nextBytes(data);
        byte[] zipped = Gzip.gzip(data);
        assertTrue(Gzip.isGzip(zipped));
        assertArrayEquals(data, Gzip.gunzip(zipped));
        assertArrayEquals(data, Gzip.gunzip(zipped, data.length));
        assertArrayEquals(new byte[0], Gzip.gunzip(Gzip.gzip(new byte[0])));
    }

    @Test void gunzipWithALimitStopsDecompressionBombs() throws IOException {
        byte[] zeros = new byte[2_000_000];
        byte[] bomb = Gzip.gzip(zeros);
        assertTrue(bomb.length < 10_000);
        CapacityExceededException e = assertThrows(CapacityExceededException.class, () -> Gzip.gunzip(bomb, 1000));
        assertEquals(1000, e.limit());
        assertEquals(zeros.length, Gzip.gunzip(bomb, zeros.length).length);
        assertThrows(CapacityExceededException.class, () -> Gzip.gunzip(bomb, zeros.length - 1));
        assertThrows(IllegalArgumentException.class, () -> Gzip.gunzip(bomb, -1));
    }

    @Test void gzipMagicAndMalformedInput() {
        assertFalse(Gzip.isGzip(null));
        assertFalse(Gzip.isGzip(new byte[]{0x1F}));
        assertFalse(Gzip.isGzip(new byte[]{1, 2, 3}));
        assertTrue(Gzip.isGzip(new byte[]{0x1F, (byte) 0x8B}));
        assertThrows(IOException.class, () -> Gzip.gunzip(new byte[]{1, 2, 3}));
        assertThrows(IOException.class, () -> Gzip.gunzip(new byte[]{1, 2, 3}, 100));
        assertThrows(NullPointerException.class, () -> Gzip.gzip(null));
        assertThrows(NullPointerException.class, () -> Gzip.gunzip(null));
    }
}
