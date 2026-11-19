package space.nows.util;

import org.junit.jupiter.api.Test;
import space.nows.util.array.*;
import space.nows.util.concurrent.*;
import space.nows.util.io.*;
import space.nows.util.math.*;
import space.nows.util.text.StringUtils;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class UtilTest {
    @Test void fastMath() {
        assertEquals(5, FastMath.clamp(9, 0, 5));
        assertEquals(15f, FastMath.lerp(10f, 20f, 0.5f), 1e-6);
        assertEquals(0.5f, FastMath.inverseLerp(10f, 20f, 15f), 1e-6);
        assertEquals(0.5f, FastMath.smoothstep(0f, 1f, 0.5f), 1e-6);
        assertEquals(-2, FastMath.fastFloor(-1.5f));
        assertEquals(-1, FastMath.fastCeil(-1.5f));
        assertEquals(3, FastMath.fastAbs(-3));
        assertEquals(2.5f, FastMath.fastAbs(-2.5f));
        assertEquals(16, FastMath.nextPowerOfTwo(9));
        assertEquals(3, FastMath.log2(8));
        assertEquals(Math.sin(1.0), FastMath.sin(1f), 0.005);
        assertEquals(Math.cos(-2.0), FastMath.cos(-2f), 0.005);
    }

    @Test void vectorsAndMatrices() {
        Vec3 v = new Vec3(1, 0, 0).cross(new Vec3(0, 1, 0));
        assertEquals(new Vec3(0, 0, 1), v);

        Quaternion q = new Quaternion().setFromAxisAngle(0, 0, 1, FastMath.HALF_PI);
        Vec3 r = q.rotate(new Vec3(1, 0, 0));
        assertEquals(0f, r.x, 1e-5);
        assertEquals(1f, r.y, 1e-5);

        Mat4 t = new Mat4().setTranslation(1, 2, 3);
        Vec3 p = t.transformPoint(new Vec3(1, 1, 1));
        assertEquals(new Vec3(2, 3, 4), p);
        assertEquals(1f, new Mat4().determinant(), 1e-6);

        Mat4 rot = new Mat4().setRotation(q);
        Vec3 d = rot.transformDirection(new Vec3(1, 0, 0));
        assertEquals(1f, d.y, 1e-5);
    }

    @Test void geometry() {
        assertTrue(Geometry.circlesOverlap(0, 0, 1, 1.5f, 0, 1));
        assertFalse(Geometry.rectsOverlap(0, 0, 1, 1, 5, 5, 1, 1));
        float[] square = {0, 0, 4, 0, 4, 4, 0, 4};
        assertTrue(Geometry.pointInPolygon(2, 2, square));
        assertFalse(Geometry.pointInPolygon(5, 2, square));
        assertEquals(16f, Geometry.polygonArea(square), 1e-6);
    }

    @Test void random() {
        FastRandom a = new FastRandom(123), b = new FastRandom(123);
        for (int i = 0; i < 100; i++) assertEquals(a.nextLong(), b.nextLong());
        for (int i = 0; i < 10_000; i++) {
            int v = a.nextInt(10);
            assertTrue(v >= 0 && v < 10);
            double d = a.nextDouble();
            assertTrue(d >= 0 && d < 1);
        }
    }

    @Test void arraysBytesBits() {
        int[] a = {1, 2, 3, 4};
        assertArrayEquals(new int[]{1, 3, 4}, ArrayUtils.remove(a, 1));
        assertArrayEquals(new int[]{1, 9, 2, 3, 4}, ArrayUtils.insert(a, 1, 9));
        ArrayUtils.swap(a, 0, 3);
        assertArrayEquals(new int[]{4, 2, 3, 1}, a);
        assertEquals(2, ArrayUtils.indexOf(a, 3));
        assertEquals(3, ArrayUtils.swapRemove(a, 0, 4));
        assertEquals(1, a[0]);

        byte[] b = new byte[16];
        ByteUtils.writeInt(b, 0, 0x01020304);
        ByteUtils.writeLongLE(b, 8, 0x1122334455667788L);
        assertEquals(1, b[0]);
        assertEquals(0x01020304, ByteUtils.readInt(b, 0));
        assertEquals(0x04030201, ByteUtils.readIntLE(b, 0));
        assertEquals(0x1122334455667788L, ByteUtils.readLongLE(b, 8));
        assertEquals(2, ByteUtils.indexOf(new byte[]{9, 8, 7, 6}, new byte[]{7, 6}));
        assertTrue(ByteUtils.compare(new byte[]{(byte) 0xFF}, new byte[]{0x01}) > 0);

        long[] bits = BitUtils.create(130);
        BitUtils.set(bits, 0); BitUtils.set(bits, 64); BitUtils.set(bits, 129);
        assertTrue(BitUtils.get(bits, 64));
        assertEquals(3, BitUtils.count(bits));
        assertEquals(64, BitUtils.nextSetBit(bits, 1));
        assertEquals(-1, BitUtils.nextSetBit(bits, 130));
        BitUtils.clear(bits, 64);
        assertEquals(129, BitUtils.nextSetBit(bits, 1));
    }

    @Test void ioAndText() throws IOException {
        ByteBuf buf = new ByteBuf(4);
        buf.writeInt(42).writeLong(-1L).writeByte(7).writeShort(500);
        assertEquals(42, buf.readInt());
        assertEquals(-1L, buf.readLong());
        assertEquals(7, buf.readByte());
        assertEquals(500, buf.readShort());
        assertThrows(IndexOutOfBoundsException.class, buf::readByte);

        byte[] data = "hello hello hello hello".getBytes();
        assertArrayEquals(data, IOUtils.gunzip(IOUtils.gzip(data)));

        assertEquals(123, StringUtils.parseInt("123", -1));
        assertEquals(-9, StringUtils.parseInt("-9", 0));
        assertEquals(-1, StringUtils.parseInt("12x", -1));
        assertEquals(-1, StringUtils.parseInt("99999999999", -1));
        assertEquals(Integer.MIN_VALUE, StringUtils.parseInt("-2147483648", 0));
        assertEquals(3, StringUtils.split("a,,b", ',').size());
        assertEquals("00ff", StringUtils.toHex(new byte[]{0, (byte) 0xFF}));
        assertArrayEquals(new byte[]{0, (byte) 0xFF}, StringUtils.fromHex("00FF"));
        assertEquals("007", StringUtils.padLeft("7", 3, '0'));
        assertTrue(StringUtils.isBlank("  \t"));
    }

    @Test void concurrency() throws Exception {
        SpscQueue<Integer> q = new SpscQueue<>(8);
        assertEquals(8, q.capacity());
        for (int i = 0; i < 8; i++) assertTrue(q.offer(i));
        assertFalse(q.offer(99));
        assertEquals(0, q.poll());

        final int n = 200_000;
        SpscQueue<Integer> pipe = new SpscQueue<>(1024);
        Thread producer = new Thread(() -> {
            for (int i = 0; i < n; i++) while (!pipe.offer(i)) Thread.onSpinWait();
        });
        producer.start();
        for (int expected = 0; expected < n; ) {
            Integer v = pipe.poll();
            if (v == null) { Thread.onSpinWait(); continue; }
            assertEquals(expected++, v);
        }
        producer.join();

        AtomicInteger sum = new AtomicInteger();
        Parallel.forRange(0, 100_000, 100, sum::addAndGet);
        assertEquals((int) (99_999L * 100_000L / 2), sum.get());
    }
}
