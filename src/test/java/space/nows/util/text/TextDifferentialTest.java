package space.nows.util.text;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.Set;
import org.junit.jupiter.api.Test;
import space.nows.util.array.ArrayEdit;
import space.nows.util.array.ArraySearch;
import space.nows.util.array.ArraySort;
import space.nows.util.collections.IntArrayDeque;
import space.nows.util.collections.IntHashSet;
import space.nows.util.collections.IntIntMap;
import space.nows.util.collections.IntList;
import space.nows.util.collections.IntMinHeap;
import space.nows.util.math.Modulo;
import space.nows.util.math.PowersOfTwo;
import space.nows.util.math.Rounding;
import space.nows.util.random.FastRandom;
import space.nows.util.text.Hex;
import space.nows.util.text.Padding;
import space.nows.util.text.Strings;
import space.nows.util.text.StringSplit;
import static org.junit.jupiter.api.Assertions.*;

class TextDifferentialTest {
    @Test
    void stringSplitMatchesStringSplit() {
        String[] samples = {"", ",", ",,", "a", "a,", ",a", "a,,b", "a,b,,", "x"};
        for (String s : samples) {
            List<String> expected = Arrays.asList(s.split(Pattern.quote(","), -1));
            assertEquals(expected, StringSplit.split(s, ','), "split '" + s + "'");
            assertEquals(s.chars().filter(c -> c == ',').count(), StringSplit.count(s, ','));
        }
    }

    @Test
    void stringsAndPaddingEdgeCases() {
        assertEquals("", Strings.truncate("", 0));
        assertEquals("", Strings.truncate("ab", 0));
        String emoji = "a😀b";
        for (int max = 0; max <= emoji.length() + 1; max++) {
            String t = Strings.truncate(emoji, max);
            assertFalse(t.length() > 0 && Character.isHighSurrogate(t.charAt(t.length() - 1)), "dangling high surrogate at " + max);
            assertTrue(emoji.startsWith(t));
        }
        assertEquals("ab", Padding.padLeft("ab", 1, '0'));
        assertEquals("00ab", Padding.padLeft("ab", 4, '0'));
        assertEquals("ab00", Padding.padRight("ab", 4, '0'));
        assertEquals("", Strings.defaultIfBlank("  ", "") );
        assertEquals("ǅ", Strings.capitalize("ǆ"));
        assertEquals("", Strings.capitalize(""));
    }

    @Test
    void hexMatchesHexFormat() {
        Random r = new Random(10);
        HexFormat hf = HexFormat.of();
        for (int trial = 0; trial < 500; trial++) {
            byte[] data = new byte[r.nextInt(20)];
            r.nextBytes(data);
            assertEquals(hf.formatHex(data), Hex.toHex(data));
            assertEquals(hf.formatHex(data).toUpperCase(), Hex.toUpperHex(data));
            assertArrayEquals(data, Hex.fromHex(hf.formatHex(data)));
            assertArrayEquals(data, Hex.fromHex(hf.formatHex(data).toUpperCase()));
            assertTrue(Hex.isHex(hf.formatHex(data)));
        }
        assertFalse(Hex.isHex("abc"));
        assertFalse(Hex.isHex("zz"));
    }

}
