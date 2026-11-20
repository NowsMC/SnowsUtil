package space.nows.util.bits;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BitsTest {
    @Test void bitSets() {
        long[] bits = BitSets.create(130);
        BitSets.set(bits, 0); BitSets.set(bits, 64); BitSets.set(bits, 129);
        assertTrue(BitSets.get(bits, 64));
        assertEquals(3, BitSets.count(bits));
        assertEquals(64, BitSets.nextSetBit(bits, 1));
        assertEquals(-1, BitSets.nextSetBit(bits, 130));
        BitSets.clear(bits, 64);
        assertEquals(129, BitSets.nextSetBit(bits, 1));
    }

    @Test void bitOpsAndUnsigned() {
        assertEquals(5, BitOps.setBit(1, 2));
        assertEquals(4, BitOps.lowestSetBit(12));
        assertEquals(255, Unsigned.toInt((byte) -1));
        assertEquals(0xFFFFFFFFL, Unsigned.toLong(-1));
    }
}
