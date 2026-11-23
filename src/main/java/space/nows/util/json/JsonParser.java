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
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;

import static space.nows.util.text.CharCursor.EOF;

/**
 * Iterative parser: nesting is tracked on an explicit stack instead of the call stack, so the depth limit is enforced
 * by a counter and deep documents cannot overflow the thread's stack.
 */
final class JsonParser {
    private static final int BYTE_ORDER_MARK = 0xFEFF;

    private final CharCursor in;
    private final ParseOptions options;

    JsonParser(Reader input, ParseOptions options) {
        this.in = new CharCursor(input);
        this.options = options;
    }

    /** A container being filled: an object (with the key of the value in progress) or an array. */
    private static final class Frame {
        final Obj object;
        final Arr array;
        String key;

        private Frame(Obj object, Arr array) {
            this.object = object;
            this.array = array;
        }

        static Frame ofObject() { return new Frame(new Obj(), null); }

        static Frame ofArray() { return new Frame(null, new Arr()); }

        int closer() { return object != null ? '}' : ']'; }

        Value value() { return object != null ? object : array; }

        void attach(Value value) {
            if (object != null) object.put(key, value);
            else array.add(value);
        }
    }

    Value parseDocument() {
        try {
            if (in.peek() == BYTE_ORDER_MARK) in.read();
            skipWhitespace();
            Value value = parseDocumentValue();
            skipWhitespace();
            if (in.peek() != EOF) throw in.error("trailing content after JSON value");
            return value;
        } catch (IOException e) {
            throw new DataException("I/O error while reading JSON at line " + in.line() + ", column " + in.column(), e);
        }
    }

    private Value parseDocumentValue() throws IOException {
        Deque<Frame> stack = new ArrayDeque<>();
        while (true) {
            Value value;
            int c = in.peek();
            if (c == '{' || c == '[') {
                if (stack.size() + 1 > options.maxDepth()) {
                    throw in.error("maximum nesting depth " + options.maxDepth() + " exceeded");
                }
                in.read();
                Frame frame = c == '{' ? Frame.ofObject() : Frame.ofArray();
                stack.push(frame);
                skipWhitespace();
                if (in.peek() == frame.closer()) {
                    in.read();
                    stack.pop();
                    value = frame.value();
                } else {
                    if (frame.object != null) {
                        frame.key = parseMemberKey(frame);
                    }
                    continue;
                }
            } else {
                value = parseScalar();
            }
            while (true) {
                if (stack.isEmpty()) return value;
                Frame top = stack.peek();
                top.attach(value);
                skipWhitespace();
                int delimiter = in.read();
                if (delimiter == top.closer()) {
                    stack.pop();
                    value = top.value();
                    continue;
                }
                if (delimiter != ',') {
                    throw in.error(top.object != null ? "expected ',' or '}' in object" : "expected ',' or ']' in array");
                }
                skipWhitespace();
                if (in.peek() == top.closer() && options.trailingCommas()) {
                    in.read();
                    stack.pop();
                    value = top.value();
                    continue;
                }
                if (top.object != null) top.key = parseMemberKey(top);
                break;
            }
        }
    }

    /** Parses {@code key :} inside {@code frame}, rejecting duplicates. */
    private String parseMemberKey(Frame frame) throws IOException {
        String key = parseKey();
        if (frame.object.has(key)) throw in.error("duplicate object key: \"" + key + "\"");
        skipWhitespace();
        if (in.read() != ':') throw in.error("expected ':' after object key");
        skipWhitespace();
        return key;
    }

    private Value parseScalar() throws IOException {
        int c = in.peek();
        return switch (c) {
            case '"' -> new Str(parseString('"'));
            case '\'' -> {
                if (!options.singleQuotes()) throw in.error("single-quoted strings are not allowed");
                yield new Str(parseString('\''));
            }
            case 't' -> { expect("true"); yield Bool.TRUE; }
            case 'f' -> { expect("false"); yield Bool.FALSE; }
            case 'n' -> { expect("null"); yield Null.INSTANCE; }
            case EOF -> throw in.error("unexpected end of input");
            default -> {
                if (c == '-' || c == '+' || isDigit(c)) yield parseNumber();
                throw in.error("unexpected character '" + (char) c + "'");
            }
        };
    }

    private String parseKey() throws IOException {
        int c = in.peek();
        if (c == '"') return parseString('"');
        if (c == '\'' && options.singleQuotes()) return parseString('\'');
        if (options.unquotedKeys() && isIdentifierStart(c)) {
            StringBuilder name = new StringBuilder();
            while (isIdentifierPart(in.peek())) name.append((char) in.read());
            return name.toString();
        }
        throw in.error("expected '\"' to start object key");
    }

    private String parseString(int quote) throws IOException {
        if (in.read() != quote) throw in.error("expected string");
        StringBuilder result = new StringBuilder();
        while (true) {
            int c = in.read();
            if (c == EOF) throw in.error("unterminated string");
            if (c == quote) return result.toString();
            if (c == '\\') {
                int escaped = in.read();
                switch (escaped) {
                    case '"' -> result.append('"');
                    case '\\' -> result.append('\\');
                    case '/' -> result.append('/');
                    case 'b' -> result.append('\b');
                    case 'f' -> result.append('\f');
                    case 'n' -> result.append('\n');
                    case 'r' -> result.append('\r');
                    case 't' -> result.append('\t');
                    case 'u' -> appendUnicodeEscape(result);
                    default -> {
                        if (escaped == '\'' && quote == '\'') result.append('\'');
                        else throw in.error("invalid string escape");
                    }
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
        boolean negative = false;
        if (in.peek() == '-' || in.peek() == '+') {
            if (in.peek() == '+' && !options.leadingPlus()) throw in.error("leading '+' is not allowed");
            negative = in.read() == '-';
        }
        if (in.peek() == '0' && options.hexNumbers()) {
            in.read();
            if (in.peek() == 'x' || in.peek() == 'X') {
                in.read();
                return hexNumber(negative);
            }
            return decimalNumber(negative, "0");
        }
        return decimalNumber(negative, "");
    }

    private Num hexNumber(boolean negative) throws IOException {
        StringBuilder digits = new StringBuilder();
        while (Hex.digitValue(in.peek()) >= 0) digits.append((char) in.read());
        if (digits.isEmpty()) throw in.error("expected hexadecimal digit after 0x");
        BigInteger value = new BigInteger(digits.toString(), 16);
        return Num.of(negative ? value.negate() : value);
    }

    private Num decimalNumber(boolean negative, String prefix) throws IOException {
        StringBuilder number = new StringBuilder(prefix);
        if (prefix.isEmpty()) {
            if (!isDigit(in.peek())) throw in.error("expected digit in number");
            if (in.peek() == '0' && !options.leadingZeros()) {
                number.append((char) in.read());
                if (isDigit(in.peek())) throw in.error("leading zero in number");
            } else {
                appendDigits(number);
            }
        } else if (isDigit(in.peek())) {
            if (!options.leadingZeros()) throw in.error("leading zero in number");
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
            BigDecimal decimal = new BigDecimal(number.toString());
            if (negative) decimal = decimal.negate();
            return decimal.signum() == 0 && negative ? Num.negativeZero() : Num.of(decimal);
        } catch (NumberFormatException e) {
            throw in.error("number out of range");
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
        while (true) {
            int c = in.peek();
            if (c == ' ' || c == '\t' || c == '\n' || c == '\r') {
                in.read();
            } else if (c == '/' && options.comments()) {
                in.read();
                skipComment();
            } else {
                return;
            }
        }
    }

    private void skipComment() throws IOException {
        int kind = in.read();
        if (kind == '/') {
            while (in.peek() != '\n' && in.peek() != EOF) in.read();
        } else if (kind == '*') {
            while (true) {
                int c = in.read();
                if (c == EOF) throw in.error("unterminated block comment");
                if (c == '*' && in.peek() == '/') {
                    in.read();
                    return;
                }
            }
        } else {
            throw in.error("expected comment after '/'");
        }
    }

    private static boolean isDigit(int c) { return c >= '0' && c <= '9'; }

    private static boolean isIdentifierStart(int c) {
        return c == '_' || c == '$' || (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z');
    }

    private static boolean isIdentifierPart(int c) {
        return isIdentifierStart(c) || isDigit(c);
    }
}
