package space.nows.util.io;

import space.nows.util.error.CapacityExceededException;
import space.nows.util.check.Preconditions;
import space.nows.util.error.InsufficientDataException;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
        if (n >= 0 && n <= data.length - writePos) return this;
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

    // ---- variable-length

    /** Unsigned LEB128, 1 to 5 bytes. Negative values always take 5; use {@link #writeZigZagInt} for values that are often negative. */
    public ByteBuf writeVarInt(int v) {
        ensureWritable(5);
        while ((v & ~0x7F) != 0) {
            data[writePos++] = (byte) ((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        data[writePos++] = (byte) v;
        return this;
    }

    /** Unsigned LEB128, 1 to 10 bytes. */
    public ByteBuf writeVarLong(long v) {
        ensureWritable(10);
        while ((v & ~0x7FL) != 0) {
            data[writePos++] = (byte) ((v & 0x7F) | 0x80);
            v >>>= 7;
        }
        data[writePos++] = (byte) v;
        return this;
    }

    public ByteBuf writeZigZagInt(int v) { return writeVarInt((v << 1) ^ (v >> 31)); }

    public ByteBuf writeZigZagLong(long v) { return writeVarLong((v << 1) ^ (v >> 63)); }

    /**
     * Reads an unsigned LEB128 {@code int}.
     *
     * @throws IllegalArgumentException if the encoding is longer than 5 bytes or overflows an {@code int}
     */
    public int readVarInt() {
        int result = 0;
        int pos = readPos;
        for (int shift = 0; shift < 35; shift += 7) {
            if (pos >= writePos) throw new InsufficientDataException(pos - readPos + 1, writePos - readPos);
            byte b = data[pos++];
            if (shift == 28 && (b & 0xF0) != 0) throw new IllegalArgumentException("varint does not fit in an int");
            result |= (b & 0x7F) << shift;
            if (b >= 0) {
                readPos = pos;
                return result;
            }
        }
        throw new IllegalArgumentException("varint does not fit in an int");
    }

    /**
     * Reads an unsigned LEB128 {@code long}.
     *
     * @throws IllegalArgumentException if the encoding is longer than 10 bytes or overflows a {@code long}
     */
    public long readVarLong() {
        long result = 0;
        int pos = readPos;
        for (int shift = 0; shift < 70; shift += 7) {
            if (pos >= writePos) throw new InsufficientDataException(pos - readPos + 1, writePos - readPos);
            byte b = data[pos++];
            if (shift == 63 && (b & 0xFE) != 0) throw new IllegalArgumentException("varint does not fit in a long");
            result |= (long) (b & 0x7F) << shift;
            if (b >= 0) {
                readPos = pos;
                return result;
            }
        }
        throw new IllegalArgumentException("varint does not fit in a long");
    }

    public int readZigZagInt() {
        int v = readVarInt();
        return (v >>> 1) ^ -(v & 1);
    }

    public long readZigZagLong() {
        long v = readVarLong();
        return (v >>> 1) ^ -(v & 1);
    }

    /** Writes a varint byte length followed by the UTF-8 bytes of {@code s}. */
    public ByteBuf writeString(CharSequence s) {
        byte[] bytes = Preconditions.notNull(s, "s").toString().getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        return writeBytes(bytes);
    }

    /**
     * Reads a string written by {@link #writeString}. Nothing is consumed if the data is short or the length is
     * negative or above {@code maxBytes}.
     */
    public String readString(int maxBytes) {
        Preconditions.nonNegative(maxBytes, "maxBytes");
        int start = readPos;
        int len = readVarInt();
        if (len < 0 || len > maxBytes) {
            readPos = start;
            throw new IllegalArgumentException("string length " + len + " not in [0, " + maxBytes + "]");
        }
        int available = writePos - readPos;
        if (available < len) {
            readPos = start;
            throw new InsufficientDataException(len, available);
        }
        return readUtf8(len);
    }

    // ---- JDK bridges

    /** Copies a {@link ByteBuffer}'s remaining bytes into a new buffer without changing the source's position. */
    public static ByteBuf copyOf(ByteBuffer src) {
        byte[] bytes = new byte[Preconditions.notNull(src, "src").remaining()];
        src.duplicate().get(bytes);
        return new ByteBuf(bytes);
    }

    /** Appends all remaining bytes of {@code src}, advancing its position. Works for heap and direct buffers. */
    public ByteBuf writeFrom(ByteBuffer src) {
        int n = Preconditions.notNull(src, "src").remaining();
        ensureWritable(n);
        src.get(data, writePos, n);
        writePos += n;
        return this;
    }

    /** Fills {@code dst} with exactly {@code dst.remaining()} unread bytes. Works for heap and direct buffers. */
    public void readInto(ByteBuffer dst) {
        int n = Preconditions.notNull(dst, "dst").remaining();
        ensureReadable(n);
        dst.put(data, readPos, n);
        readPos += n;
    }

    /** Copies the unread bytes into a new direct buffer in native byte order, ready to read; does not consume them. */
    public ByteBuffer toDirectBuffer() {
        ByteBuffer out = ByteBuffer.allocateDirect(writePos - readPos).order(ByteOrder.nativeOrder());
        out.put(data, readPos, writePos - readPos);
        out.flip();
        return out;
    }

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
