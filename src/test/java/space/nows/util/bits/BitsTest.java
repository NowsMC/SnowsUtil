package space.nows.util.bits;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BitsTest {
    private static final int NBITS = 256;

    @Test void bitSetsMatchTheJdkBitSetUnderRandomOperations() {
        Random r = new Random(21);
        long[] bits = BitSets.create(NBITS);
        BitSet ref = new BitSet(NBITS);
        for (int step = 0; step < 20_000; step++) {
            int i = r.nextInt(NBITS);
            switch (r.nextInt(8)) {
                case 0 -> { BitSets.set(bits, i); ref.set(i); }
                case 1 -> { BitSets.clear(bits, i); ref.clear(i); }
                case 2 -> { BitSets.flip(bits, i); ref.flip(i); }
                case 3 -> { int j = r.nextInt(NBITS + 1); int lo = Math.min(i, j), hi = Math.max(i, j); BitSets.setRange(bits, lo, hi); ref.set(lo, hi); }
                case 4 -> { int j = r.nextInt(NBITS + 1); int lo = Math.min(i, j), hi = Math.max(i, j); BitSets.clearRange(bits, lo, hi); ref.clear(lo, hi); }
                case 5 -> { boolean v = r.nextBoolean(); BitSets.set(bits, i, v); ref.set(i, v); }
                case 6 -> {
                    assertEquals(ref.get(i), BitSets.get(bits, i));
                    int ns = ref.nextSetBit(i);
                    assertEquals(ns, BitSets.nextSetBit(bits, i));
                    int nc = ref.nextClearBit(i);
                    assertEquals(nc >= NBITS ? -1 : nc, BitSets.nextClearBit(bits, i, NBITS));
                    assertEquals(ref.previousSetBit(i), BitSets.previousSetBit(bits, i));
                }
                default -> { assertEquals(ref.cardinality(), BitSets.count(bits)); assertEquals(ref.isEmpty(), BitSets.isEmpty(bits)); }
            }
        }
        assertEquals(ref, BitSets.toBitSet(bits));
        assertEquals(ref.previousSetBit(10_000), BitSets.previousSetBit(bits, 10_000));   // from beyond the end is clamped
        assertEquals(-1, BitSets.previousSetBit(bits, -1));
    }

    @Test void setOperationsMatchTheJdk() {
        Random r = new Random(22);
        for (int round = 0; round < 200; round++) {
            long[] a = new long[4], b = new long[4];
            for (int i = 0; i < 4; i++) { a[i] = r.nextLong(); b[i] = r.nextLong(); }
            BitSet ra = BitSet.valueOf(a), rb = BitSet.valueOf(b);

            long[] x = a.clone(); BitSets.or(x, b);     BitSet e = (BitSet) ra.clone(); e.or(rb);     assertEquals(e, BitSet.valueOf(x));
            x = a.clone();        BitSets.and(x, b);    e = (BitSet) ra.clone(); e.and(rb);           assertEquals(e, BitSet.valueOf(x));
            x = a.clone();        BitSets.xor(x, b);    e = (BitSet) ra.clone(); e.xor(rb);           assertEquals(e, BitSet.valueOf(x));
            x = a.clone();        BitSets.andNot(x, b); e = (BitSet) ra.clone(); e.andNot(rb);        assertEquals(e, BitSet.valueOf(x));
            assertEquals(ra.intersects(rb), BitSets.intersects(a, b));
        }
    }

    @Test void indexesAreCheckedAgainstTheArray() {
        long[] bits = BitSets.create(NBITS);
        assertEquals(256L, BitSets.capacity(bits));
        assertThrows(IndexOutOfBoundsException.class, () -> BitSets.get(bits, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> BitSets.get(bits, NBITS));
        assertThrows(IndexOutOfBoundsException.class, () -> BitSets.set(bits, NBITS));
        assertThrows(IndexOutOfBoundsException.class, () -> BitSets.flip(bits, Integer.MIN_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> BitSets.setRange(bits, 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> BitSets.setRange(bits, 0, NBITS + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> BitSets.clearRange(bits, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> BitSets.nextSetBit(bits, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> BitSets.previousSetBit(bits, -2));
        assertThrows(IllegalArgumentException.class, () -> BitSets.nextClearBit(bits, 0, NBITS + 1));
        assertThrows(IllegalArgumentException.class, () -> BitSets.create(-1));
        assertThrows(IllegalArgumentException.class, () -> BitSets.or(new long[2], new long[3]));
        assertThrows(IllegalArgumentException.class, () -> BitSets.intersects(new long[2], new long[3]));
        assertEquals(0, BitSets.create(0).length);
        assertEquals(1, BitSets.create(1).length);
        assertEquals(1, BitSets.create(64).length);
        assertEquals(2, BitSets.create(65).length);
        BitSets.setRange(bits, 7, 7);                          // empty range is a no-op
        assertTrue(BitSets.isEmpty(bits));
        assertEquals(-1, BitSets.nextSetBit(new long[0], 0));
        assertEquals(-1, BitSets.previousSetBit(new long[0], 5));
    }

    @Test void bitSetRoundTripWithTheJdk() {
        long[] bits = BitSets.create(100);
        BitSets.set(bits, 3); BitSets.set(bits, 99);
        BitSet jdk = BitSets.toBitSet(bits);
        assertTrue(jdk.get(99));
        assertArrayEquals(bits, BitSets.fromBitSet(jdk, 100));
        assertEquals(4, BitSets.fromBitSet(jdk, 200).length);                  // larger target pads with zero words
        assertThrows(IllegalArgumentException.class, () -> BitSets.fromBitSet(jdk, 99));
        assertThrows(NullPointerException.class, () -> BitSets.fromBitSet(null, 10));
    }

    @Test void bitOpsRejectShiftsThatWouldSilentlyWrap() {
        assertEquals(5, BitOps.setBit(1, 2));
        assertEquals(1, BitOps.clearBit(5, 2));
        assertEquals(4, BitOps.toggleBit(0, 2));
        assertTrue(BitOps.isSet(0x80000000, 31));
        assertThrows(IndexOutOfBoundsException.class, () -> BitOps.setBit(0, 32));    // used to set bit 0
        assertThrows(IndexOutOfBoundsException.class, () -> BitOps.isSet(1, 32));
        assertThrows(IndexOutOfBoundsException.class, () -> BitOps.clearBit(1, -1));
        assertEquals(4, BitOps.lowestSetBit(12));
        assertEquals(8, BitOps.highestSetBit(12));
        assertEquals(0, BitOps.highestSetBit(0));
        assertEquals(3, BitOps.count(7));
    }

    @Test void bitFields() {
        assertEquals(0, BitOps.mask(0));
        assertEquals(0xFF, BitOps.mask(8));
        assertEquals(-1, BitOps.mask(32));
        assertThrows(IllegalArgumentException.class, () -> BitOps.mask(33));
        assertThrows(IllegalArgumentException.class, () -> BitOps.mask(-1));
        assertEquals(0xBC, BitOps.extract(0xABCD, 4, 8));
        assertEquals(0xFFFF12FF, BitOps.deposit(0xFFFFFFFF, 8, 8, 0x12));
        assertEquals(0x7, BitOps.extract(0xFFFFFFFF, 29, 3));
        assertEquals(0xFFFFFFFF, BitOps.extract(0xFFFFFFFF, 0, 32));
        assertEquals(0, BitOps.extract(0xFFFFFFFF, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> BitOps.extract(0, 30, 5));
        assertThrows(IllegalArgumentException.class, () -> BitOps.deposit(0, -1, 2, 1));
        Random r = new Random(23);
        for (int i = 0; i < 10_000; i++) {
            int offset = r.nextInt(32), width = r.nextInt(33 - offset), value = r.nextInt(), field = r.nextInt();
            int put = BitOps.deposit(value, offset, width, field);
            assertEquals(field & BitOps.mask(width), BitOps.extract(put, offset, width));
            assertEquals(value & ~(BitOps.mask(width) << offset), put & ~(BitOps.mask(width) << offset));   // bits outside the field are untouched
        }
    }

    @Test void unsignedConversions() {
        assertEquals(255, Unsigned.toInt((byte) -1));
        assertEquals(65535, Unsigned.toInt((short) -1));
        assertEquals(255L, Unsigned.toLong((byte) -1));
        assertEquals(65535L, Unsigned.toLong((short) -1));
        assertEquals(0xFFFFFFFFL, Unsigned.toLong(-1));
        assertEquals(0L, Unsigned.toLong(0));
    }
}
