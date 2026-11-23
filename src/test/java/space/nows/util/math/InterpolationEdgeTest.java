package space.nows.util.math;

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

class InterpolationEdgeTest {
    @Test
    void lerpIsExactAtEndpointsEvenWithInfiniteEndpoints() {
        assertEquals(0.0, space.nows.util.math.Interpolation.lerp(0.0, Double.POSITIVE_INFINITY, 0.0));
        assertEquals(Double.POSITIVE_INFINITY, space.nows.util.math.Interpolation.lerp(0.0, Double.POSITIVE_INFINITY, 1.0));
        assertEquals(0f, space.nows.util.math.Interpolation.lerp(0f, Float.POSITIVE_INFINITY, 0f));
    }

}
