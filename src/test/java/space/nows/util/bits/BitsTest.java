package space.nows.util.bits;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BitsTest {
    private static final int NBITS = 256;

    @Test void bitSetsMatchTheJdkBitSetUnderRandomOperations() {
        Random r = new Random(21);
        long[] bits = BitArrays.create(NBITS);
        BitSet ref = new BitSet(NBITS);
        for (int step = 0; step < 20_000; step++) {
            int i = r.nextInt(NBITS);
            switch (r.nextInt(8)) {
                case 0 -> { BitArrays.set(bits, i); ref.set(i); }
                case 1 -> { BitArrays.clear(bits, i); ref.clear(i); }
                case 2 -> { BitArrays.flip(bits, i); ref.flip(i); }
                case 3 -> { int j = r.nextInt(NBITS + 1); int lo = Math.min(i, j), hi = Math.max(i, j); BitRanges.setRange(bits, lo, hi); ref.set(lo, hi); }
                case 4 -> { int j = r.nextInt(NBITS + 1); int lo = Math.min(i, j), hi = Math.max(i, j); BitRanges.clearRange(bits, lo, hi); ref.clear(lo, hi); }
                case 5 -> { boolean v = r.nextBoolean(); BitArrays.set(bits, i, v); ref.set(i, v); }
                case 6 -> {
                    assertEquals(ref.get(i), BitArrays.get(bits, i));
                    int ns = ref.nextSetBit(i);
                    assertEquals(ns, BitSearch.nextSetBit(bits, i));
                    int nc = ref.nextClearBit(i);
                    assertEquals(nc >= NBITS ? -1 : nc, BitSearch.nextClearBit(bits, i, NBITS));
                    assertEquals(ref.previousSetBit(i), BitSearch.previousSetBit(bits, i));
                }
                default -> { assertEquals(ref.cardinality(), BitArrays.count(bits)); assertEquals(ref.isEmpty(), BitArrays.isEmpty(bits)); }
            }
        }
        assertEquals(ref, BitSetConversions.toBitSet(bits));
        assertEquals(ref.previousSetBit(10_000), BitSearch.previousSetBit(bits, 10_000));
        assertEquals(-1, BitSearch.previousSetBit(bits, -1));
    }

    @Test void setOperationsMatchTheJdk() {
        Random r = new Random(22);
        for (int round = 0; round < 200; round++) {
            long[] a = new long[4], b = new long[4];
            for (int i = 0; i < 4; i++) { a[i] = r.nextLong(); b[i] = r.nextLong(); }
            BitSet ra = BitSet.valueOf(a), rb = BitSet.valueOf(b);

            long[] x = a.clone(); BitLogic.or(x, b);     BitSet e = (BitSet) ra.clone(); e.or(rb);     assertEquals(e, BitSet.valueOf(x));
            x = a.clone();        BitLogic.and(x, b);    e = (BitSet) ra.clone(); e.and(rb);           assertEquals(e, BitSet.valueOf(x));
            x = a.clone();        BitLogic.xor(x, b);    e = (BitSet) ra.clone(); e.xor(rb);           assertEquals(e, BitSet.valueOf(x));
            x = a.clone();        BitLogic.andNot(x, b); e = (BitSet) ra.clone(); e.andNot(rb);        assertEquals(e, BitSet.valueOf(x));
            assertEquals(ra.intersects(rb), BitLogic.intersects(a, b));
        }
    }

    @Test void indexesAreCheckedAgainstTheArray() {
        long[] bits = BitArrays.create(NBITS);
        assertEquals(256L, BitArrays.capacity(bits));
        assertThrows(IndexOutOfBoundsException.class, () -> BitArrays.get(bits, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> BitArrays.get(bits, NBITS));
        assertThrows(IndexOutOfBoundsException.class, () -> BitArrays.set(bits, NBITS));
        assertThrows(IndexOutOfBoundsException.class, () -> BitArrays.flip(bits, Integer.MIN_VALUE));
        assertThrows(IndexOutOfBoundsException.class, () -> BitRanges.setRange(bits, 5, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> BitRanges.setRange(bits, 0, NBITS + 1));
        assertThrows(IndexOutOfBoundsException.class, () -> BitRanges.clearRange(bits, -1, 3));
        assertThrows(IndexOutOfBoundsException.class, () -> BitSearch.nextSetBit(bits, -1));
        assertThrows(IndexOutOfBoundsException.class, () -> BitSearch.previousSetBit(bits, -2));
        assertThrows(IllegalArgumentException.class, () -> BitSearch.nextClearBit(bits, 0, NBITS + 1));
        assertThrows(IllegalArgumentException.class, () -> BitArrays.create(-1));
        assertThrows(IllegalArgumentException.class, () -> BitLogic.or(new long[2], new long[3]));
        assertThrows(IllegalArgumentException.class, () -> BitLogic.intersects(new long[2], new long[3]));
        assertEquals(0, BitArrays.create(0).length);
        assertEquals(1, BitArrays.create(1).length);
        assertEquals(1, BitArrays.create(64).length);
        assertEquals(2, BitArrays.create(65).length);
        BitRanges.setRange(bits, 7, 7);
        assertTrue(BitArrays.isEmpty(bits));
        assertEquals(-1, BitSearch.nextSetBit(new long[0], 0));
        assertEquals(-1, BitSearch.previousSetBit(new long[0], 5));
    }

    @Test void bitSetRoundTripWithTheJdk() {
        long[] bits = BitArrays.create(100);
        BitArrays.set(bits, 3); BitArrays.set(bits, 99);
        BitSet jdk = BitSetConversions.toBitSet(bits);
        assertTrue(jdk.get(99));
        assertArrayEquals(bits, BitSetConversions.fromBitSet(jdk, 100));
        assertEquals(4, BitSetConversions.fromBitSet(jdk, 200).length);
        assertThrows(IllegalArgumentException.class, () -> BitSetConversions.fromBitSet(jdk, 99));
        assertThrows(NullPointerException.class, () -> BitSetConversions.fromBitSet(null, 10));
    }

    @Test void bitOpsRejectShiftsThatWouldSilentlyWrap() {
        assertEquals(5, IntBits.setBit(1, 2));
        assertEquals(1, IntBits.clearBit(5, 2));
        assertEquals(4, IntBits.toggleBit(0, 2));
        assertTrue(IntBits.isSet(0x80000000, 31));
        assertThrows(IndexOutOfBoundsException.class, () -> IntBits.setBit(0, 32));
        assertThrows(IndexOutOfBoundsException.class, () -> IntBits.isSet(1, 32));
        assertThrows(IndexOutOfBoundsException.class, () -> IntBits.clearBit(1, -1));
        assertEquals(4, IntBits.lowestSetBit(12));
        assertEquals(8, IntBits.highestSetBit(12));
        assertEquals(0, IntBits.highestSetBit(0));
        assertEquals(3, IntBits.count(7));
    }

    @Test void bitFields() {
        assertEquals(0, BitField.mask(0));
        assertEquals(0xFF, BitField.mask(8));
        assertEquals(-1, BitField.mask(32));
        assertThrows(IllegalArgumentException.class, () -> BitField.mask(33));
        assertThrows(IllegalArgumentException.class, () -> BitField.mask(-1));
        assertEquals(0xBC, BitField.extract(0xABCD, 4, 8));
        assertEquals(0xFFFF12FF, BitField.deposit(0xFFFFFFFF, 8, 8, 0x12));
        assertEquals(0x7, BitField.extract(0xFFFFFFFF, 29, 3));
        assertEquals(0xFFFFFFFF, BitField.extract(0xFFFFFFFF, 0, 32));
        assertEquals(0, BitField.extract(0xFFFFFFFF, 5, 0));
        assertThrows(IllegalArgumentException.class, () -> BitField.extract(0, 30, 5));
        assertThrows(IllegalArgumentException.class, () -> BitField.deposit(0, -1, 2, 1));
        Random r = new Random(23);
        for (int i = 0; i < 10_000; i++) {
            int offset = r.nextInt(32), width = r.nextInt(33 - offset), value = r.nextInt(), field = r.nextInt();
            int put = BitField.deposit(value, offset, width, field);
            assertEquals(field & BitField.mask(width), BitField.extract(put, offset, width));
            assertEquals(value & ~(BitField.mask(width) << offset), put & ~(BitField.mask(width) << offset));
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
