package space.nows.util.io;

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

class IoOddTest {
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

}
