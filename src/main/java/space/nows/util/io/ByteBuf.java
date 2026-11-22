package space.nows.util.io;

import space.nows.util.error.CapacityExceededException;
import space.nows.util.check.Preconditions;
import space.nows.util.error.InsufficientDataException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Growable byte buffer with independent read and write positions. Multi-byte values are big-endian unless the
 * method name ends in {@code LE}. Reads that run past the written data throw {@link InsufficientDataException}
 * and consume nothing.
 */
public final class ByteBuf {
    private static final int MAX_LENGTH = Integer.MAX_VALUE - 8;

    private byte[] data;
    private int readPos;
    private int writePos;

    public ByteBuf() { this(64); }

    public ByteBuf(int initialCapacity) {
        data = new byte[Math.max(16, Preconditions.nonNegative(initialCapacity, "initialCapacity"))];
    }

    /** Wraps {@code wrap} without copying; all of it is readable. A write that needs more room reallocates. */
    public ByteBuf(byte[] wrap) {
        data = Preconditions.notNull(wrap, "wrap");
        writePos = wrap.length;
    }

    // ---- state

    public int readableBytes() { return writePos - readPos; }

    public int capacity() { return data.length; }

    public int readerIndex() { return readPos; }

    public int writerIndex() { return writePos; }

    /** Moves the read position; it must not pass the write position. */
    public ByteBuf readerIndex(int index) {
        Preconditions.position(index, writePos);
        readPos = index;
        return this;
    }

    /** Moves the write position; it must lie between the read position and the capacity. */
    public ByteBuf writerIndex(int index) {
        if (index < readPos || index > data.length) {
            throw new IndexOutOfBoundsException("writerIndex " + index + " not in [" + readPos + ", " + data.length + "]");
        }
        writePos = index;
        return this;
    }

    /** Guarantees room for {@code n} more bytes. */
    public ByteBuf ensureWritable(int n) {
        Preconditions.nonNegative(n, "n");
        long need = (long) writePos + n;
        if (need > data.length) {
            if (need > MAX_LENGTH) throw new CapacityExceededException("ByteBuf", need, MAX_LENGTH);
            long grown = Math.max(need, data.length + (data.length >> 1) + 1L);
            data = Arrays.copyOf(data, (int) Math.min(grown, MAX_LENGTH));
        }
        return this;
    }

    private void ensureReadable(int n) {
        int available = writePos - readPos;
        if (available < n) throw new InsufficientDataException(n, available);
    }

    /** Discards read bytes by moving the unread ones to the start. */
    public ByteBuf compact() {
        int n = writePos - readPos;
        System.arraycopy(data, readPos, data, 0, n);
        readPos = 0;
        writePos = n;
        return this;
    }

    /** Forgets all content (does not zero the memory). */
    public ByteBuf reset() { readPos = 0; writePos = 0; return this; }

    // ---- write

    public ByteBuf writeByte(int v) { ensureWritable(1); data[writePos++] = (byte) v; return this; }

    public ByteBuf writeBoolean(boolean v) { return writeByte(v ? 1 : 0); }

    public ByteBuf writeShort(int v) { ensureWritable(2); ByteAccess.writeShort(data, writePos, (short) v); writePos += 2; return this; }

    public ByteBuf writeShortLE(int v) { ensureWritable(2); ByteAccess.writeShortLE(data, writePos, (short) v); writePos += 2; return this; }

    public ByteBuf writeInt(int v) { ensureWritable(4); ByteAccess.writeInt(data, writePos, v); writePos += 4; return this; }

    public ByteBuf writeIntLE(int v) { ensureWritable(4); ByteAccess.writeIntLE(data, writePos, v); writePos += 4; return this; }

    public ByteBuf writeLong(long v) { ensureWritable(8); ByteAccess.writeLong(data, writePos, v); writePos += 8; return this; }

    public ByteBuf writeLongLE(long v) { ensureWritable(8); ByteAccess.writeLongLE(data, writePos, v); writePos += 8; return this; }

    public ByteBuf writeFloat(float v) { return writeInt(Float.floatToRawIntBits(v)); }

    public ByteBuf writeDouble(double v) { return writeLong(Double.doubleToRawLongBits(v)); }

    public ByteBuf writeBytes(byte[] src) { return writeBytes(src, 0, Preconditions.notNull(src, "src").length); }

    public ByteBuf writeBytes(byte[] src, int off, int len) {
        Preconditions.notNull(src, "src");
        Preconditions.range(off, len, src.length);
        ensureWritable(len);
        System.arraycopy(src, off, data, writePos, len);
        writePos += len;
        return this;
    }

    /** Encodes {@code s} as UTF-8 and appends it (no length prefix). Returns the number of bytes written. */
    public int writeUtf8(CharSequence s) {
        byte[] bytes = Preconditions.notNull(s, "s").toString().getBytes(StandardCharsets.UTF_8);
        writeBytes(bytes);
        return bytes.length;
    }

    /** Appends exactly {@code len} bytes from {@code in}. Throws {@link EOFException} if the stream ends early; nothing is appended then. */
    public ByteBuf readFrom(InputStream in, int len) throws IOException {
        Preconditions.notNull(in, "in");
        Preconditions.nonNegative(len, "len");
        ensureWritable(len);
        int n = in.readNBytes(data, writePos, len);
        if (n < len) throw new EOFException("expected " + len + " bytes but stream ended after " + n);
        writePos += len;
        return this;
    }

    // ---- read

    public byte readByte() { ensureReadable(1); return data[readPos++]; }

    public int readUnsignedByte() { return readByte() & 0xFF; }

    public boolean readBoolean() { return readByte() != 0; }

    public short readShort() { ensureReadable(2); short v = ByteAccess.readShort(data, readPos); readPos += 2; return v; }

    public short readShortLE() { ensureReadable(2); short v = ByteAccess.readShortLE(data, readPos); readPos += 2; return v; }

    public int readUnsignedShort() { return readShort() & 0xFFFF; }

    public int readInt() { ensureReadable(4); int v = ByteAccess.readInt(data, readPos); readPos += 4; return v; }

    public int readIntLE() { ensureReadable(4); int v = ByteAccess.readIntLE(data, readPos); readPos += 4; return v; }

    public long readUnsignedInt() { return readInt() & 0xFFFFFFFFL; }

    public long readLong() { ensureReadable(8); long v = ByteAccess.readLong(data, readPos); readPos += 8; return v; }

    public long readLongLE() { ensureReadable(8); long v = ByteAccess.readLongLE(data, readPos); readPos += 8; return v; }

    public float readFloat() { return Float.intBitsToFloat(readInt()); }

    public double readDouble() { return Double.longBitsToDouble(readLong()); }

    public void readBytes(byte[] dst) { readBytes(dst, 0, Preconditions.notNull(dst, "dst").length); }

    public void readBytes(byte[] dst, int off, int len) {
        Preconditions.notNull(dst, "dst");
        Preconditions.range(off, len, dst.length);
        ensureReadable(len);
        System.arraycopy(data, readPos, dst, off, len);
        readPos += len;
    }

    public byte[] readBytes(int len) {
        Preconditions.nonNegative(len, "len");
        ensureReadable(len);
        byte[] out = Arrays.copyOfRange(data, readPos, readPos + len);
        readPos += len;
        return out;
    }

    /** Decodes {@code len} bytes as UTF-8 (malformed input becomes U+FFFD). */
    public String readUtf8(int len) {
        Preconditions.nonNegative(len, "len");
        ensureReadable(len);
        String s = new String(data, readPos, len, StandardCharsets.UTF_8);
        readPos += len;
        return s;
    }

    public ByteBuf skip(int n) {
        Preconditions.nonNegative(n, "n");
        ensureReadable(n);
        readPos += n;
        return this;
    }

    /** Next byte without consuming it. */
    public byte peekByte() { ensureReadable(1); return data[readPos]; }

    // ---- JDK bridges

    /** Copy of the unread bytes; does not consume them. */
    public byte[] toByteArray() { return Arrays.copyOfRange(data, readPos, writePos); }

    /**
     * Read-only JDK view of the unread bytes, sharing memory with this buffer. It is not invalidated by later
     * reads, but a write that reallocates leaves it pointing at the old array.
     */
    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(data, readPos, writePos - readPos).slice().asReadOnlyBuffer();
    }

    /** Writes and consumes all unread bytes. Returns how many were written. */
    public int writeTo(OutputStream out) throws IOException {
        Preconditions.notNull(out, "out");
        int n = writePos - readPos;
        out.write(data, readPos, n);
        readPos = writePos;
        return n;
    }

    @Override public String toString() {
        return "ByteBuf[readerIndex=" + readPos + ", writerIndex=" + writePos + ", capacity=" + data.length + "]";
    }
}
