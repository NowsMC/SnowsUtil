package space.nows.util.bits;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.CRC32C;
import org.junit.jupiter.api.Test;
import space.nows.util.bits.BitRanges;
import space.nows.util.bits.BitSearch;
import space.nows.util.concurrent.MpmcQueue;
import space.nows.util.concurrent.ObjectPool;
import space.nows.util.concurrent.SpscQueue;
import space.nows.util.hash.Hashing;
import space.nows.util.net.HostPort;
import static org.junit.jupiter.api.Assertions.*;

class BitsEdgeTest {
    @Test
    void bitRangesAndSearchMatchBitSet() {
        Random r = new Random(21);
        for (int trial = 0; trial < 3000; trial++) {
            int words = 1 + r.nextInt(4);
            long[] bits = new long[words];
            BitSet ref = new BitSet();
            int cap = words * 64;
            for (int op = 0; op < 20; op++) {
                int from = r.nextInt(cap + 1);
                int to = from + r.nextInt(cap - from + 1);
                if (r.nextBoolean()) {
                    BitRanges.setRange(bits, from, to);
                    ref.set(from, to);
                } else {
                    BitRanges.clearRange(bits, from, to);
                    ref.clear(from, to);
                }
                assertEquals(ref.cardinality(), java.util.Arrays.stream(bits).map(Long::bitCount).sum(), "cardinality");
                int probe = r.nextInt(cap);
                assertEquals(ref.nextSetBit(probe), BitSearch.nextSetBit(bits, probe), "nextSetBit " + probe);
                assertEquals(ref.previousSetBit(probe), BitSearch.previousSetBit(bits, probe), "previousSetBit " + probe);
                int nbits = 1 + r.nextInt(cap);
                int expected = ref.nextClearBit(probe);
                if (expected >= nbits || probe >= nbits) expected = -1;
                assertEquals(expected, BitSearch.nextClearBit(bits, probe, nbits), "nextClearBit " + probe + " nbits " + nbits);
            }
        }
    }

}
