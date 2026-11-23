package space.nows.util.json;

import space.nows.util.data.DataArray;
import space.nows.util.data.DataBoolean;
import space.nows.util.data.DataNumber;
import space.nows.util.data.DataObject;
import space.nows.util.data.DataString;
import space.nows.util.data.DataValue;
import space.nows.util.error.DataException;
import space.nows.util.text.Hex;

import java.io.IOException;
import java.util.Map;

final class JsonWriter {
    private final Appendable output;
    private final boolean pretty;

    JsonWriter(Appendable output, boolean pretty) {
        this.output = output;
        this.pretty = pretty;
    }

    void write(DataValue value) throws IOException {
        write(value, 0);
    }

    private void write(DataValue value, int depth) throws IOException {
        if (depth > DataValue.MAX_DEPTH) throw new DataException("maximum nesting depth " + DataValue.MAX_DEPTH + " exceeded");
        if (value instanceof DataObject object) {
            writeObject(object, depth);
        } else if (value instanceof DataArray array) {
            writeArray(array, depth);
        } else if (value instanceof DataString string) {
            writeString(string.value());
        } else if (value instanceof DataNumber number) {
            output.append(number.asDecimal().toString());
        } else if (value instanceof DataBoolean bool) {
            output.append(bool.value() ? "true" : "false");
        } else {
            output.append("null");
        }
    }

    private void writeObject(DataObject object, int depth) throws IOException {
        output.append('{');
        boolean first = true;
        for (Map.Entry<String, DataValue> entry : object) {
            if (!first) output.append(',');
            first = false;
            newline(depth + 1);
            writeString(entry.getKey());
            output.append(pretty ? ": " : ":");
            write(entry.getValue(), depth + 1);
        }
        if (!object.isEmpty()) newline(depth);
        output.append('}');
    }

    private void writeArray(DataArray array, int depth) throws IOException {
        output.append('[');
        boolean first = true;
        for (DataValue element : array) {
            if (!first) output.append(',');
            first = false;
            newline(depth + 1);
            write(element, depth + 1);
        }
        if (!array.isEmpty()) newline(depth);
        output.append(']');
    }

    private void newline(int depth) throws IOException {
        if (!pretty) return;
        output.append('\n');
        for (int i = 0; i < depth; i++) output.append("  ");
    }

    private void writeString(String value) throws IOException {
        output.append('"');
        for (int i = 0, n = value.length(); i < n; i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> output.append("\\\"");
                case '\\' -> output.append("\\\\");
                case '\b' -> output.append("\\b");
                case '\f' -> output.append("\\f");
                case '\n' -> output.append("\\n");
                case '\r' -> output.append("\\r");
                case '\t' -> output.append("\\t");
                default -> {
                    if (c < 0x20) {
                        appendUnicodeEscape(c);
                    } else if (!Character.isSurrogate(c)) {
                        output.append(c);
                    } else if (Character.isHighSurrogate(c) && i + 1 < n && Character.isLowSurrogate(value.charAt(i + 1))) {
                        output.append(c).append(value.charAt(++i));
                    } else {
                        appendUnicodeEscape(c);
                    }
                }
            }
        }
        output.append('"');
    }

    private void appendUnicodeEscape(char c) throws IOException {
        output.append("\\u")
                .append(Hex.lowerDigit(c >>> 12))
                .append(Hex.lowerDigit(c >>> 8))
                .append(Hex.lowerDigit(c >>> 4))
                .append(Hex.lowerDigit(c));
    }
}
