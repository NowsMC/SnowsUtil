package space.nows.util.text;

import org.junit.jupiter.api.Test;
import space.nows.util.error.InvalidUtf8Exception;
import space.nows.util.io.ByteBuf;

import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class Utf8Test {
    private static String randomText(Random r, boolean allowLoneSurrogates) {
        StringBuilder sb = new StringBuilder();
        for (int k = r.nextInt(12); k > 0; k--) {
            switch (r.nextInt(4)) {
                case 0 -> sb.append((char) r.nextInt(0x80));
                case 1 -> sb.append((char) (0x80 + r.nextInt(0x800 - 0x80)));
                case 2 -> sb.appendCodePoint(0x10000 + r.nextInt(0x100000));
                default -> {
                    char c = (char) r.nextInt(0x10000);
                    if (allowLoneSurrogates || !Character.isSurrogate(c)) sb.append(c);
                }
            }
        }
        return sb.toString();
    }

    @Test
    void encodesLikeTheJdkIncludingLoneSurrogates() {
        Random r = new Random(7);
        for (int i = 0; i < 20_000; i++) {
            String s = randomText(r, true);
            byte[] expected = s.getBytes(StandardCharsets.UTF_8);
            byte[] actual = new byte[Utf8.encodedLength(s)];
            assertEquals(expected.length, Utf8.encode(s, actual, 0), s);
            assertArrayEquals(expected, actual, s);
        }
    }

    @Test
    void roundTripsValidText() {
        Random r = new Random(9);
        for (int i = 0; i < 20_000; i++) {
            String s = randomText(r, false);
            byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
            assertEquals(s, Utf8.decode(bytes, 0, bytes.length), s);
        }
    }

    @Test
    void strictDecodeAcceptsAndRejectsTheSameInputsAsTheJdk() {
        Random r = new Random(11);
        for (int i = 0; i < 200_000; i++) {
            byte[] bytes = new byte[r.nextInt(8)];
            for (int k = 0; k < bytes.length; k++) {
                int bucket = r.nextInt(3);
                bytes[k] = (byte) (bucket == 0 ? r.nextInt(0x80) : bucket == 1 ? 0x80 + r.nextInt(0x40) : 0xC0 + r.nextInt(0x40));
            }
            String expected = null;
            try {
                expected = StandardCharsets.UTF_8.newDecoder()
                        .onMalformedInput(CodingErrorAction.REPORT)
                        .onUnmappableCharacter(CodingErrorAction.REPORT)
                        .decode(ByteBuffer.wrap(bytes)).toString();
            } catch (CharacterCodingException ignored) {
                // JDK rejects these bytes
            }
            try {
                String actual = Utf8.decode(bytes, 0, bytes.length);
                assertNotNull(expected, Arrays.toString(bytes));
                assertEquals(expected, actual, Arrays.toString(bytes));
            } catch (InvalidUtf8Exception e) {
                assertNull(expected, Arrays.toString(bytes));
            }
        }
    }

    @Test
    void rejectsMalformedSequencesAtTheOffendingByte() {
        assertInvalid(new byte[]{(byte) 0xC0, (byte) 0x80}, 0);
        assertInvalid(new byte[]{'a', (byte) 0xE0, (byte) 0x80, (byte) 0x80}, 1);
        assertInvalid(new byte[]{(byte) 0xED, (byte) 0xA0, (byte) 0x80}, 0);
        assertInvalid(new byte[]{(byte) 0xF4, (byte) 0x90, (byte) 0x80, (byte) 0x80}, 0);
        assertInvalid(new byte[]{(byte) 0xE2, (byte) 0x82}, 0);
        assertInvalid(new byte[]{(byte) 0x80}, 0);
        assertInvalid(new byte[]{(byte) 0xC3, '"'}, 1);
    }

    private static void assertInvalid(byte[] bytes, int index) {
        InvalidUtf8Exception e = assertThrows(InvalidUtf8Exception.class, () -> Utf8.decode(bytes, 0, bytes.length));
        assertEquals(index, e.index());
    }

    @Test
    void byteBufTextMatchesTheJdkEncoding() {
        String s = "สวัสดี 😀 abc";
        ByteBuf buf = new ByteBuf();
        assertEquals(s.getBytes(StandardCharsets.UTF_8).length, buf.writeUtf8(s));
        assertArrayEquals(s.getBytes(StandardCharsets.UTF_8), buf.readBytes(buf.readableBytes()));

        ByteBuf prefixed = new ByteBuf().writeString(s);
        assertEquals(s, prefixed.readString(1024));
    }
}
