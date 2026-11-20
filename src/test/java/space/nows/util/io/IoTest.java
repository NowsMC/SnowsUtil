package space.nows.util.io;

import org.junit.jupiter.api.Test;
import java.io.IOException;

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

    @Test void byteBufAndGzip() throws IOException {
        ByteBuf buf = new ByteBuf(4);
        buf.writeInt(42).writeLong(-1L).writeByte(7).writeShort(500);
        assertEquals(42, buf.readInt());
        assertEquals(-1L, buf.readLong());
        assertEquals(7, buf.readByte());
        assertEquals(500, buf.readShort());
        assertThrows(IndexOutOfBoundsException.class, buf::readByte);

        byte[] data = "hello hello hello hello".getBytes();
        assertArrayEquals(data, Gzip.gunzip(Gzip.gzip(data)));
    }
}
