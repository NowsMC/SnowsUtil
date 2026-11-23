package space.nows.util.text;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;

class EscapesTest {
    @Test
    void escapesQuotesBackslashesAndShortControls() {
        assertEquals("\"a\\\"b\\\\c\\n\\r\\t\\b\\f\"", Escapes.quote("a\"b\\c\n\r\t\b\f"));
    }

    @Test
    void escapesOtherControlsAsFourDigitEscapes() {
        assertEquals("\"\\u0000\\u001f\"", Escapes.quote("\u0000\u001F"));
        assertEquals("\"\u007f\"", Escapes.quote("\u007F"), "DEL is not a control character in these rules");
    }

    @Test
    void keepsValidSurrogatePairsAndEscapesUnpairedOnes() {
        assertEquals("\"\uD83D\uDE00\"", Escapes.quote("\uD83D\uDE00"));
        assertEquals("\"\\ud83d\"", Escapes.quote("\uD83D"));
        assertEquals("\"\\ude00\"", Escapes.quote("\uDE00"));
        assertEquals("\"\\ude00\\ud83d\"", Escapes.quote("\uDE00\uD83D"), "a reversed pair is two unpaired surrogates");
    }

    @Test
    void leavesPlainAndNonAsciiTextAlone() {
        assertEquals("\"สวัสดี abc\"", Escapes.quote("สวัสดี abc"));
        assertEquals("\"\"", Escapes.quote(""));
    }

    @Test
    void appendsToAnyAppendable() throws IOException {
        StringWriter out = new StringWriter();
        Escapes.appendQuoted("x\"y", out);
        assertEquals("\"x\\\"y\"", out.toString());
    }

    @Test
    void singleCharacterOutputNeverContainsRawSurrogates() {
        for (char c = 0; c < 0xFFFF; c++) {
            String quoted = Escapes.quote(String.valueOf(c));
            for (int i = 0; i < quoted.length(); i++) {
                assertFalse(Character.isSurrogate(quoted.charAt(i)), "raw surrogate for U+" + Integer.toHexString(c));
            }
        }
    }
}
