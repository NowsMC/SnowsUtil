package space.nows.util.bits;

import space.nows.util.check.Preconditions;

import java.util.BitSet;

/** Conversions between {@code long[]} bit arrays and the JDK's {@link BitSet}. */
public final class BitSetConversions {
    private BitSetConversions() {}

    /** Copies into a JDK {@link BitSet}. */
    public static BitSet toBitSet(long[] bits) { return BitSet.valueOf(bits); }

    /** Copies a JDK {@link BitSet} into a new array sized for {@code nbits} bits. */
    public static long[] fromBitSet(BitSet source, int nbits) {
        Preconditions.notNull(source, "source");
        if (source.length() > nbits) {
            throw new IllegalArgumentException("bit set has bits beyond nbits=" + nbits + " (length " + source.length() + ")");
        }
        long[] out = BitArrays.create(nbits);
        long[] words = source.toLongArray();
        System.arraycopy(words, 0, out, 0, words.length);
        return out;
    }
}
