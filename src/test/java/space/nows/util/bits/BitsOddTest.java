package space.nows.util.bits;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class BitsOddTest {
    private static BitSet toBitSet(long[] words) {
        BitSet set = new BitSet();
        for (int w = 0; w < words.length; w++) {
            for (int b = 0; b < 64; b++) if ((words[w] & (1L << b)) != 0) set.set(w * 64 + b);
        }
        return set;
    }

    @Test
    void bitFieldExtractAndDepositRoundTripWithoutTouchingOtherBits() {
        Random r = new Random(91);
        for (int trial = 0; trial < 20_000; trial++) {
            int width = 1 + r.nextInt(31);
            int offset = r.nextInt(33 - width);
            int original = r.nextInt();
            int field = r.nextInt() & BitField.mask(width);
            int deposited = BitField.deposit(original, offset, width, field);
            assertEquals(field, BitField.extract(deposited, offset, width));
            int outsideMask = ~(BitField.mask(width) << offset);
            assertEquals(original & outsideMask, deposited & outsideMask, "bits outside the field changed");
        }
    }

    @Test
    void bitFieldFullWidthAndInvalidWidths() {
        assertEquals(-1, BitField.mask(32));
        assertEquals(0, BitField.mask(0));
        assertThrows(IllegalArgumentException.class, () -> BitField.mask(33));
        assertThrows(IllegalArgumentException.class, () -> BitField.mask(-1));
        assertEquals(1, BitField.extract(Integer.MIN_VALUE, 31, 1));
    }

    @Test
    void bitLogicMatchesBitSet() {
        Random r = new Random(92);
        for (int trial = 0; trial < 2000; trial++) {
            int words = 1 + r.nextInt(4);
            long[] a = new long[words];
            long[] b = new long[words];
            for (int i = 0; i < words; i++) { a[i] = r.nextLong(); b[i] = r.nextLong(); }
            BitSet sa = toBitSet(a), sb = toBitSet(b);

            long[] or = a.clone();
            BitLogic.or(or, b);
            BitSet expOr = (BitSet) sa.clone();
            expOr.or(sb);
            assertEquals(expOr, toBitSet(or));

            long[] and = a.clone();
            BitLogic.and(and, b);
            BitSet expAnd = (BitSet) sa.clone();
            expAnd.and(sb);
            assertEquals(expAnd, toBitSet(and));

            long[] andNot = a.clone();
            BitLogic.andNot(andNot, b);
            BitSet expAndNot = (BitSet) sa.clone();
            expAndNot.andNot(sb);
            assertEquals(expAndNot, toBitSet(andNot));

            long[] xor = a.clone();
            BitLogic.xor(xor, b);
            BitSet expXor = (BitSet) sa.clone();
            expXor.xor(sb);
            assertEquals(expXor, toBitSet(xor));

            assertEquals(sa.intersects(sb), BitLogic.intersects(a, b));
        }
    }

    @Test
    void bitLogicRejectsMismatchedLengths() {
        assertThrows(RuntimeException.class, () -> BitLogic.or(new long[2], new long[3]));
    }

    @Test
    void searchesAtWordBoundaries() {
        long[] bits = new long[3];
        BitRanges.setRange(bits, 63, 65);
        assertEquals(63, BitSearch.nextSetBit(bits, 0));
        assertEquals(64, BitSearch.nextSetBit(bits, 64));
        assertEquals(-1, BitSearch.nextSetBit(bits, 65));
        assertEquals(64, BitSearch.previousSetBit(bits, 127));
        assertEquals(-1, BitSearch.previousSetBit(bits, -1));
        assertEquals(-1, BitSearch.nextSetBit(new long[0], 0));
        assertEquals(-1, BitSearch.previousSetBit(new long[0], 5));
    }

    @Test
    void intBitsSingleBitOperations() {
        assertTrue(IntBits.isSet(1 << 31, 31));
        assertEquals(1 << 5, IntBits.setBit(0, 5));
        assertEquals(0, IntBits.clearBit(1 << 5, 5));
        assertEquals(0, IntBits.toggleBit(1, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> IntBits.setBit(0, 32));
    }
}
