package space.nows.util.json;

import space.nows.util.data.Arr;
import space.nows.util.data.Bool;
import space.nows.util.data.Null;
import space.nows.util.data.Num;
import space.nows.util.data.Obj;
import space.nows.util.data.Str;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;
import space.nows.util.text.CharCursor;
import space.nows.util.text.Hex;

import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;

import static space.nows.util.text.CharCursor.EOF;

final class JsonParser {
    private final CharCursor in;
    private int depth;

    JsonParser(Reader input) {
        this.in = new CharCursor(input);
    }

    Value parseDocument() {
        try {
            skipWhitespace();
            Value value = parseValue();
            skipWhitespace();
            if (in.peek() != EOF) throw in.error("trailing content after JSON value");
            return value;
        } catch (IOException e) {
            throw new DataException("I/O error while reading JSON at char offset " + in.offset(), e);
        }
    }

    private Value parseValue() throws IOException {
        int c = in.peek();
        return switch (c) {
            case '{' -> parseObject();
            case '[' -> parseArray();
            case '"' -> new Str(parseString());
            case 't' -> { expect("true"); yield Bool.TRUE; }
            case 'f' -> { expect("false"); yield Bool.FALSE; }
            case 'n' -> { expect("null"); yield Null.INSTANCE; }
            case EOF -> throw in.error("unexpected end of input");
            default -> {
                if (c == '-' || isDigit(c)) yield parseNumber();
                throw in.error("unexpected character '" + (char) c + "'");
            }
        };
    }

    private Obj parseObject() throws IOException {
        enterContainer();
        in.read();
        Obj object = new Obj();
        skipWhitespace();
        if (in.peek() == '}') {
            in.read();
            depth--;
            return object;
        }
        while (true) {
            if (in.peek() != '"') throw in.error("expected '\"' to start object key");
            String key = parseString();
            if (object.has(key)) throw in.error("duplicate object key: \"" + key + "\"");
            skipWhitespace();
            if (in.read() != ':') throw in.error("expected ':' after object key");
            skipWhitespace();
            object.put(key, parseValue());
            skipWhitespace();
            int delimiter = in.read();
            if (delimiter == '}') {
                depth--;
                return object;
            }
            if (delimiter != ',') throw in.error("expected ',' or '}' in object");
            skipWhitespace();
        }
    }

    private Arr parseArray() throws IOException {
        enterContainer();
        in.read();
        Arr array = new Arr();
        skipWhitespace();
        if (in.peek() == ']') {
            in.read();
            depth--;
            return array;
        }
        while (true) {
            array.add(parseValue());
            skipWhitespace();
            int delimiter = in.read();
            if (delimiter == ']') {
                depth--;
                return array;
            }
            if (delimiter != ',') throw in.error("expected ',' or ']' in array");
            skipWhitespace();
        }
    }

    private void enterContainer() {
        if (++depth > Value.MAX_DEPTH) throw in.error("maximum nesting depth " + Value.MAX_DEPTH + " exceeded");
    }

    private String parseString() throws IOException {
        if (in.read() != '"') throw in.error("expected string");
        StringBuilder result = new StringBuilder();
        while (true) {
            int c = in.read();
            if (c == EOF) throw in.error("unterminated string");
            if (c == '"') return result.toString();
            if (c == '\\') {
                switch (in.read()) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> appendUnicodeEscape(result);
                    default -> throw in.error("invalid string escape");
                }
            } else if (c < 0x20) {
                throw in.error(String.format("unescaped control character U+%04X", c));
            } else if (Character.isHighSurrogate((char) c)) {
                int low = in.read();
                if (low == EOF || !Character.isLowSurrogate((char) low)) throw in.error("unpaired high surrogate in string");
                result.append((char) c).append((char) low);
            } else if (Character.isLowSurrogate((char) c)) {
                throw in.error("unpaired low surrogate in string");
            } else {
                result.append((char) c);
            }
        }
    }

    private void appendUnicodeEscape(StringBuilder result) throws IOException {
        char first = readHexChar();
        if (Character.isHighSurrogate(first)) {
            if (in.read() != '\\' || in.read() != 'u') throw in.error("expected low surrogate escape");
            char second = readHexChar();
            if (!Character.isLowSurrogate(second)) throw in.error("expected low surrogate escape");
            result.append(first).append(second);
        } else if (Character.isLowSurrogate(first)) {
            throw in.error("unexpected low surrogate escape");
        } else {
            result.append(first);
        }
    }

    private char readHexChar() throws IOException {
        int value = 0;
        for (int i = 0; i < 4; i++) {
            int digit = Hex.digitValue(in.read());
            if (digit < 0) throw in.error("invalid hexadecimal digit in Unicode escape");
            value = (value << 4) | digit;
        }
        return (char) value;
    }

    private Num parseNumber() throws IOException {
        StringBuilder number = new StringBuilder();
        if (in.peek() == '-') number.append((char) in.read());
        if (in.peek() == '0') {
            number.append((char) in.read());
            if (isDigit(in.peek())) throw in.error("leading zero in number");
        } else {
            if (!isNonZeroDigit(in.peek())) throw in.error("expected digit in number");
            appendDigits(number);
        }
        if (in.peek() == '.') {
            number.append((char) in.read());
            if (!isDigit(in.peek())) throw in.error("expected digit after decimal point");
            appendDigits(number);
        }
        if (in.peek() == 'e' || in.peek() == 'E') {
            number.append((char) in.read());
            if (in.peek() == '+' || in.peek() == '-') number.append((char) in.read());
            if (!isDigit(in.peek())) throw in.error("expected digit in exponent");
            appendDigits(number);
        }
        try {
            return Num.of(new BigDecimal(number.toString()));
        } catch (NumberFormatException e) {
            throw new DataException("number out of range at char offset " + in.offset(), e);
        }
    }

    private void appendDigits(StringBuilder number) throws IOException {
        while (isDigit(in.peek())) number.append((char) in.read());
    }

    private void expect(String expected) throws IOException {
        for (int i = 0; i < expected.length(); i++) {
            if (in.read() != expected.charAt(i)) throw in.error("expected keyword \"" + expected + "\"");
        }
    }

    private void skipWhitespace() throws IOException {
        while (in.peek() == ' ' || in.peek() == '\t' || in.peek() == '\n' || in.peek() == '\r') in.read();
    }

    private static boolean isDigit(int c) { return c >= '0' && c <= '9'; }

    private static boolean isNonZeroDigit(int c) { return c >= '1' && c <= '9'; }
}
