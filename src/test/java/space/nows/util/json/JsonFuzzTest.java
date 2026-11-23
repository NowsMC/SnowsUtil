package space.nows.util.json;

import org.junit.jupiter.api.Test;
import space.nows.util.data.Arr;
import space.nows.util.data.Bool;
import space.nows.util.data.Null;
import space.nows.util.data.Num;
import space.nows.util.data.Obj;
import space.nows.util.data.Str;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;

import java.math.BigDecimal;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class JsonFuzzTest {
    private static final String ALPHABET = "{}[]:,\"\\/ \t\r\n-+.eE0123456789tfnaulsr\u0000\u001F\uD83D\uDE00\uD800\uDC00\u00E9\uFEFF\uFFFD";

    private static void checkInput(String input) {
        Value parsed;
        try {
            parsed = Json.parse(input);
        } catch (DataException expected) {
            return;
        } catch (RuntimeException unexpected) {
            throw new AssertionError("unexpected " + unexpected + " for input " + escape(input), unexpected);
        }
        String compact = Json.write(parsed);
        String pretty = Json.writePretty(parsed);
        assertEquals(parsed, Json.parse(compact), "compact round trip of " + escape(input));
        assertEquals(parsed, Json.parse(pretty), "pretty round trip of " + escape(input));
    }

    private static String escape(String s) {
        StringBuilder out = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c < 0x20 || c > 0x7E) out.append(String.format("\\u%04X", (int) c)); else out.append(c);
        }
        return out.toString();
    }

    @Test
    void randomTextNeverEscapesDataException() {
        Random r = new Random(31);
        for (int trial = 0; trial < 60_000; trial++) {
            StringBuilder sb = new StringBuilder();
            for (int k = r.nextInt(24); k > 0; k--) sb.append(ALPHABET.charAt(r.nextInt(ALPHABET.length())));
            checkInput(sb.toString());
        }
    }

    @Test
    void mutatedValidDocumentsNeverEscapeDataException() {
        Random r = new Random(32);
        for (int trial = 0; trial < 30_000; trial++) {
            String base = Json.write(randomValue(r, 0));
            StringBuilder sb = new StringBuilder(base);
            int edits = 1 + r.nextInt(3);
            for (int e = 0; e < edits; e++) {
                int op = r.nextInt(3);
                int at = sb.length() == 0 ? 0 : r.nextInt(sb.length());
                if (op == 0 || sb.length() == 0) sb.insert(at, ALPHABET.charAt(r.nextInt(ALPHABET.length())));
                else if (op == 1) sb.deleteCharAt(at);
                else sb.setCharAt(at, ALPHABET.charAt(r.nextInt(ALPHABET.length())));
            }
            checkInput(sb.toString());
        }
    }

    @Test
    void randomBytesNeverEscapeDataException() {
        Random r = new Random(33);
        for (int trial = 0; trial < 60_000; trial++) {
            byte[] bytes = new byte[r.nextInt(16)];
            for (int i = 0; i < bytes.length; i++) bytes[i] = (byte) (r.nextBoolean() ? "{}[]\",:0-.1ea ".charAt(r.nextInt(14)) : r.nextInt());
            try {
                Value v = Json.parse(bytes);
                assertEquals(v, Json.parse(Json.write(v)));
            } catch (DataException expected) {
                // malformed input is reported through DataException
            } catch (RuntimeException unexpected) {
                throw new AssertionError("unexpected " + unexpected + " for bytes " + java.util.Arrays.toString(bytes), unexpected);
            }
        }
    }

    @Test
    void numbersRoundTripExactly() {
        Random r = new Random(34);
        for (int trial = 0; trial < 20_000; trial++) {
            BigDecimal d = new BigDecimal(new java.math.BigInteger(1 + r.nextInt(40), r), r.nextInt(60) - 30);
            if (r.nextBoolean()) d = d.negate();
            Num n = Num.of(d);
            assertEquals(n, Json.parse(Json.write(n)), d.toString());
            assertEquals(d.compareTo(Json.parse(Json.write(n)).asNumber().asDecimal()), 0);
        }
    }

    @Test
    void knownHardInputs() {
        assertThrows(DataException.class, () -> Json.parse("\uFEFF{}"), "BOM is rejected, not silently skipped");
        assertThrows(DataException.class, () -> Json.parse("\"\\u0000\\uD800\""));
        assertEquals(new Str("\u0000"), Json.parse("\"\\u0000\""));
        assertEquals(new Str("\uD83D\uDE00"), Json.parse("\"\\uD83D\\uDE00\""));
        assertThrows(DataException.class, () -> Json.parse("1e999999999999999999999"));
        assertEquals(Num.of(new BigDecimal("1E+3")), Json.parse("1e3").asNumber());
        assertEquals(new Obj().put("a", 1), Json.parse("{\"a\":1}"));
        assertEquals(new Arr().add(Null.INSTANCE), Json.parse("[null]"));
        assertEquals(Bool.TRUE, Json.parse("true"));
    }

    private static Value randomValue(Random r, int depth) {
        int kind = depth > 3 ? r.nextInt(5) : r.nextInt(7);
        return switch (kind) {
            case 0 -> Null.INSTANCE;
            case 1 -> Bool.of(r.nextBoolean());
            case 2 -> Num.of(r.nextLong());
            case 3 -> new Str(randomString(r));
            case 4 -> Num.of(r.nextDouble() * 1000 - 500);
            case 5 -> {
                Arr a = new Arr();
                for (int i = r.nextInt(4); i > 0; i--) a.add(randomValue(r, depth + 1));
                yield a;
            }
            default -> {
                Obj o = new Obj();
                for (int i = r.nextInt(4); i > 0; i--) o.put(randomString(r), randomValue(r, depth + 1));
                yield o;
            }
        };
    }

    private static String randomString(Random r) {
        StringBuilder sb = new StringBuilder();
        for (int i = r.nextInt(6); i > 0; i--) {
            int pick = r.nextInt(4);
            if (pick == 0) sb.append((char) r.nextInt(0x20));
            else if (pick == 1) sb.append("\"\\/".charAt(r.nextInt(3)));
            else if (pick == 2) sb.appendCodePoint(0x10000 + r.nextInt(0x10000));
            else sb.append((char) ('a' + r.nextInt(26)));
        }
        return sb.toString();
    }
}
