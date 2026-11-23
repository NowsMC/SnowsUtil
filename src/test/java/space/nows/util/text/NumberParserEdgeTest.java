package space.nows.util.text;

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

class NumberParserEdgeTest {
    private static <T> T parseOrNull(java.util.function.Supplier<T> parser) {
        try {
            return parser.get();
        } catch (NumberFormatException e) {
            return null;
        }
    }


    @Test
    void tryParseMatchesJdkParsers() {
        java.util.Random r = new java.util.Random(24);
        String alphabet = "0123456789+-x ";
        String[] extremes = {"9223372036854775807", "9223372036854775808", "-9223372036854775808", "-9223372036854775809",
                "2147483647", "2147483648", "-2147483648", "-2147483649", "+0", "-0", "", "+", "-", "00012"};
        for (int trial = 0; trial < 200_000; trial++) {
            String s;
            if (trial < extremes.length) {
                s = extremes[trial];
            } else {
                StringBuilder sb = new StringBuilder();
                for (int k = r.nextInt(22); k > 0; k--) sb.append(alphabet.charAt(r.nextInt(r.nextInt(8) == 0 ? alphabet.length() : 12)));
                s = sb.toString();
            }
            java.util.OptionalLong l = space.nows.util.text.NumberParser.tryParseLong(s);
            Long jdkLong = parseOrNull(() -> Long.parseLong(s));
            assertEquals(jdkLong == null ? java.util.OptionalLong.empty() : java.util.OptionalLong.of(jdkLong), l, "long '" + s + "'");
            java.util.OptionalInt i = space.nows.util.text.NumberParser.tryParseInt(s);
            Integer jdkInt = parseOrNull(() -> Integer.parseInt(s));
            assertEquals(jdkInt == null ? java.util.OptionalInt.empty() : java.util.OptionalInt.of(jdkInt), i, "int '" + s + "'");
        }
        assertTrue(space.nows.util.text.NumberParser.tryParseLong(null).isEmpty());
    }

}
