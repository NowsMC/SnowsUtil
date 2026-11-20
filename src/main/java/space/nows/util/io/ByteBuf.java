package space.nows.util.io;

import java.util.Arrays;

public final class ByteBuf {
    private byte[] data;
    private int readPos;
    private int writePos;

    public ByteBuf() { this(64); }

    public ByteBuf(int initialCapacity) { data = new byte[Math.max(16, initialCapacity)]; }

    public ByteBuf(byte[] wrap) { data = wrap; writePos = wrap.length; }

    public int readableBytes() { return writePos - readPos; }

    public int capacity() { return data.length; }

    private void ensureWritable(int n) {
        long need = (long) writePos + n;
        if (need > data.length) {
            long grown = Math.max(need, data.length + (data.length >> 1) + 1L);
            if (grown > Integer.MAX_VALUE - 8) {
                if (need > Integer.MAX_VALUE - 8) throw new OutOfMemoryError("ByteBuf too large");
                grown = Integer.MAX_VALUE - 8;
            }
            data = Arrays.copyOf(data, (int) grown);
        }
    }

    private void ensureReadable(int n) {
        if (writePos - readPos < n) throw new IndexOutOfBoundsException("need " + n + " bytes, have " + (writePos - readPos));
    }

    public ByteBuf writeByte(int v) { ensureWritable(1); data[writePos++] = (byte) v; return this; }

    public ByteBuf writeShort(int v) { ensureWritable(2); ByteAccess.writeShort(data, writePos, (short) v); writePos += 2; return this; }

    public ByteBuf writeInt(int v) { ensureWritable(4); ByteAccess.writeInt(data, writePos, v); writePos += 4; return this; }

    public ByteBuf writeLong(long v) { ensureWritable(8); ByteAccess.writeLong(data, writePos, v); writePos += 8; return this; }

    public ByteBuf writeBytes(byte[] src) { return writeBytes(src, 0, src.length); }

    public ByteBuf writeBytes(byte[] src, int off, int len) {
        ensureWritable(len);
        System.arraycopy(src, off, data, writePos, len);
        writePos += len;
        return this;
    }

    public byte readByte() { ensureReadable(1); return data[readPos++]; }

    public short readShort() { ensureReadable(2); short v = ByteAccess.readShort(data, readPos); readPos += 2; return v; }

    public int readInt() { ensureReadable(4); int v = ByteAccess.readInt(data, readPos); readPos += 4; return v; }

    public long readLong() { ensureReadable(8); long v = ByteAccess.readLong(data, readPos); readPos += 8; return v; }

    public void readBytes(byte[] dst) { readBytes(dst, 0, dst.length); }

    public void readBytes(byte[] dst, int off, int len) {
        ensureReadable(len);
        System.arraycopy(data, readPos, dst, off, len);
        readPos += len;
    }

    public ByteBuf reset() { readPos = 0; writePos = 0; return this; }

    public byte[] toByteArray() { return Arrays.copyOfRange(data, readPos, writePos); }
}
