package space.nows.util.json;

import space.nows.util.error.JsonException;
import space.nows.util.text.Hex;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;

final class JsonParser {
    private static final int EOF = -1;
    private static final int NONE = Integer.MIN_VALUE;

    private final BufferedReader input;
    private int lookahead = NONE;
    private long offset;
    private int depth;

    JsonParser(Reader input) {
        this.input = input instanceof BufferedReader buffered ? buffered : new BufferedReader(input);
    }

    JsonValue parseDocument() {
        try {
            skipWhitespace();
            JsonValue value = parseValue();
            skipWhitespace();
            if (peek() != EOF) throw error("trailing content after JSON value");
            return value;
        } catch (IOException e) {
            throw new JsonException("I/O error while reading JSON at char offset " + offset, e);
        }
    }

    private JsonValue parseValue() throws IOException {
        int c = peek();
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> new JsonString(parseString());
            case 't' -> { expect("true"); yield JsonBoolean.TRUE; }
            case 'f' -> { expect("false"); yield JsonBoolean.FALSE; }
            case 'n' -> { expect("null"); yield JsonNull.INSTANCE; }
            case EOF -> throw error("unexpected end of input");
            default -> {
                if (c == '-' || isDigit(c)) yield parseNumber();
                throw error("unexpected character '" + (char) c + "'");
            }
        };
    }

    private JsonObject parseObject() throws IOException {
        enterContainer();
        read();
        JsonObject object = new JsonObject();
        skipWhitespace();
        if (peek() == '}') {
            read();
            depth--;
            return object;
        }
        while (true) {
            if (peek() != '"') throw error("expected '\"' to start object key");
            String key = parseString();
            if (object.has(key)) throw error("duplicate object key: \"" + key + "\"");
            skipWhitespace();
            if (read() != ':') throw error("expected ':' after object key");
            skipWhitespace();
            object.put(key, parseValue());
            skipWhitespace();
            int delimiter = read();
            if (delimiter == '}') {
                depth--;
                return object;
            }
            if (delimiter != ',') throw error("expected ',' or '}' in object");
            skipWhitespace();
        }
    }

    private JsonArray parseArray() throws IOException {
        enterContainer();
        read();
        JsonArray array = new JsonArray();
        skipWhitespace();
        if (peek() == ']') {
            read();
            depth--;
            return array;
        }
        while (true) {
            array.add(parseValue());
            skipWhitespace();
            int delimiter = read();
            if (delimiter == ']') {
                depth--;
                return array;
            }
            if (delimiter != ',') throw error("expected ',' or ']' in array");
            skipWhitespace();
        }
    }

    private void enterContainer() {
        if (++depth > Json.MAX_DEPTH) throw error("maximum nesting depth " + Json.MAX_DEPTH + " exceeded");
    }

    private String parseString() throws IOException {
        if (read() != '"') throw error("expected string");
        StringBuilder result = new StringBuilder();
        while (true) {
            int c = read();
            if (c == EOF) throw error("unterminated string");
            if (c == '"') return result.toString();
            if (c == '\\') {
                switch (read()) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> appendUnicodeEscape(result);
                    default -> throw error("invalid string escape");
                }
            } else if (c < 0x20) {
                throw error(String.format("unescaped control character U+%04X", c));
            } else if (Character.isHighSurrogate((char) c)) {
                int low = read();
                if (low == EOF || !Character.isLowSurrogate((char) low)) throw error("unpaired high surrogate in string");
                result.append((char) c).append((char) low);
            } else if (Character.isLowSurrogate((char) c)) {
                throw error("unpaired low surrogate in string");
            } else {
                result.append((char) c);
            }
        }
    }

    private void appendUnicodeEscape(StringBuilder result) throws IOException {
        char first = readHexChar();
        if (Character.isHighSurrogate(first)) {
            if (read() != '\\' || read() != 'u') throw error("expected low surrogate escape");
            char second = readHexChar();
            if (!Character.isLowSurrogate(second)) throw error("expected low surrogate escape");
            result.append(first).append(second);
        } else if (Character.isLowSurrogate(first)) {
            throw error("unexpected low surrogate escape");
        } else {
            result.append(first);
        }
    }

    private char readHexChar() throws IOException {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int digit = Hex.digitValue(read());
            if (digit < 0) throw error("invalid hexadecimal digit in Unicode escape");
            value = (value << 4) | digit;
        }
        return (char) value;
    }

    private JsonNumber parseNumber() throws IOException {
        StringBuilder number = new StringBuilder();
        if (peek() == '-') number.append((char) read());
        if (peek() == '0') {
            number.append((char) read());
            if (isDigit(peek())) throw error("leading zero in number");
        } else {
            if (!isNonZeroDigit(peek())) throw error("expected digit in number");
            appendDigits(number);
        }
        if (peek() == '.') {
            number.append((char) read());
            if (!isDigit(peek())) throw error("expected digit after decimal point");
            appendDigits(number);
        }
        if (peek() == 'e' || peek() == 'E') {
            number.append((char) read());
            if (peek() == '+' || peek() == '-') number.append((char) read());
            if (!isDigit(peek())) throw error("expected digit in exponent");
            appendDigits(number);
        }
        try {
            return JsonNumber.of(new BigDecimal(number.toString()));
        } catch (NumberFormatException e) {
            throw new JsonException("number out of range at char offset " + offset, e);
        }
    }

    private void appendDigits(StringBuilder number) throws IOException {
        while (isDigit(peek())) number.append((char) read());
    }

    private void expect(String expected) throws IOException {
        for (int i = 0; i < expected.length(); i++) {
            if (read() != expected.charAt(i)) throw error("expected keyword \"" + expected + "\"");
        }
    }

    private void skipWhitespace() throws IOException {
        while (peek() == ' ' || peek() == '\t' || peek() == '\n' || peek() == '\r') read();
    }

    private int peek() throws IOException {
        if (lookahead == NONE) lookahead = input.read();
        return lookahead;
    }

    private int read() throws IOException {
        int value = peek();
        lookahead = NONE;
        if (value != EOF) offset++;
        return value;
    }

    private JsonException error(String message) {
        return new JsonException(message + " (at char offset " + offset + ")");
    }

    private static boolean isDigit(int c) { return c >= '0' && c <= '9'; }

    private static boolean isNonZeroDigit(int c) { return c >= '1' && c <= '9'; }
}
