package space.nows.util.io;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class RemainingIoTest {
    @Test
    void copyTransfersAllBytesForSeveralBufferSizes() throws IOException {
        Random r = new Random(131);
        byte[] data = new byte[10_000];
        r.nextBytes(data);
        for (int bufferSize : new int[]{1, 7, 8192, 20_000}) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            assertEquals(data.length, Streams.copy(new ByteArrayInputStream(data), out, bufferSize));
            assertArrayEquals(data, out.toByteArray());
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        assertEquals(0, Streams.copy(new ByteArrayInputStream(new byte[0]), out));
        assertThrows(IllegalArgumentException.class, () -> Streams.copy(new ByteArrayInputStream(data), out, 0));
    }

    @Test
    void readExactlyFailsOnShortStreams() throws IOException {
        assertArrayEquals(new byte[]{1, 2}, Streams.readExactly(new ByteArrayInputStream(new byte[]{1, 2, 3}), 2));
        assertArrayEquals(new byte[0], Streams.readExactly(new ByteArrayInputStream(new byte[0]), 0));
        assertThrows(EOFException.class, () -> Streams.readExactly(new ByteArrayInputStream(new byte[]{1}), 2));
    }

    @Test
    void closeQuietlySwallowsFailuresAndAcceptsNull() {
        Streams.closeQuietly(null);
        Streams.closeQuietly(() -> { throw new IOException("ignored"); });
    }

    @Test
    void byteBufReadFromAppendsExactlyTheRequestedBytes() throws IOException {
        byte[] data = {1, 2, 3, 4, 5};
        ByteBuf buf = new ByteBuf(1);
        buf.readFrom(new ByteArrayInputStream(data), 3);
        assertEquals(3, buf.readableBytes());
        assertThrows(EOFException.class, () -> buf.readFrom(new ByteArrayInputStream(new byte[1]), 4));
        assertEquals(3, buf.readableBytes(), "nothing is appended after a short read");
        assertArrayEquals(new byte[]{1, 2, 3}, buf.readBytes(3));
    }

    @Test
    void byteBufReadIntoAndDirectBufferBridges() {
        ByteBuf buf = new ByteBuf().writeBytes(new byte[]{9, 8, 7, 6});
        ByteBuffer dst = ByteBuffer.allocateDirect(4);
        buf.readInto(dst);
        dst.flip();
        assertEquals(9, dst.get());
        assertEquals(0, buf.readableBytes());
        ByteBuffer direct = new ByteBuf().writeBytes(new byte[]{5, 6}).toDirectBuffer();
        assertTrue(direct.isDirect());
        assertEquals(2, direct.remaining());
        assertThrows(IndexOutOfBoundsException.class, () -> new ByteBuf().readInto(ByteBuffer.allocate(1)));
    }

    @Test
    void byteBufWriteFromAndCopyOfDoNotMoveSourcePosition() {
        ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3});
        ByteBuf copy = ByteBuf.copyOf(src);
        assertEquals(0, src.position(), "copyOf leaves the source position alone");
        assertArrayEquals(new byte[]{1, 2, 3}, copy.readBytes(3));
        ByteBuf sink = new ByteBuf();
        sink.writeFrom(src);
        assertEquals(3, src.position(), "writeFrom consumes the source");
        assertArrayEquals(new byte[]{1, 2, 3}, sink.readBytes(3));
    }

    @Test
    void byteBufWriteFromNonZeroPositionSourceAppendsRemainingOnly() {
        ByteBuffer src = ByteBuffer.wrap(new byte[]{1, 2, 3, 4});
        src.position(2);
        ByteBuf sink = new ByteBuf();
        sink.writeFrom(src);
        assertArrayEquals(new byte[]{3, 4}, sink.readBytes(2));
    }

    @Test
    void readFromRealStreamLargerThanInitialBuffer() throws IOException {
        byte[] payload = new byte[100_000];
        Arrays.fill(payload, (byte) 0x5A);
        InputStream in = new ByteArrayInputStream(payload);
        ByteBuf buf = new ByteBuf(4);
        buf.readFrom(in, payload.length);
        assertArrayEquals(payload, buf.readBytes(payload.length));
    }
}
