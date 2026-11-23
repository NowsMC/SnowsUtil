package space.nows.util.json;

import space.nows.util.data.Arr;
import space.nows.util.data.Bool;
import space.nows.util.data.Num;
import space.nows.util.data.Obj;
import space.nows.util.data.Str;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;
import space.nows.util.text.Escapes;
import space.nows.util.text.Hex;

import java.io.IOException;
import java.util.Map;

final class JsonWriter {
    private final Appendable output;
    private final boolean pretty;
    private final WriteOptions options;

    JsonWriter(Appendable output, boolean pretty) {
        this(output, pretty ? WriteOptions.PRETTY : WriteOptions.COMPACT);
    }

    JsonWriter(Appendable output, WriteOptions options) {
        this.options = options;
        this.output = output;
        this.pretty = options.pretty();
    }

    void write(Value value) throws IOException {
        write(value, 0);
    }

    private void write(Value value, int depth) throws IOException {
        if (depth > Value.MAX_DEPTH) throw new DataException("maximum nesting depth " + Value.MAX_DEPTH + " exceeded");
        if (value instanceof Obj object) {
            writeObject(object, depth);
        } else if (value instanceof Arr array) {
            writeArray(array, depth);
        } else if (value instanceof Str string) {
            writeString(string.value());
        } else if (value instanceof Num number) {
            output.append(number.toString());
        } else if (value instanceof Bool bool) {
            output.append(bool.value() ? "true" : "false");
        } else {
            output.append("null");
        }
    }

    private void writeObject(Obj object, int depth) throws IOException {
        output.append('{');
        boolean first = true;
        for (Map.Entry<String, Value> entry : object) {
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

    private void writeArray(Arr array, int depth) throws IOException {
        output.append('[');
        boolean first = true;
        for (Value element : array) {
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
        if (!options.escapeHtml() && !options.asciiOnly()) {
            Escapes.appendQuoted(value, output);
            return;
        }
        String quoted = Escapes.quote(value);
        for (int i = 0, n = quoted.length(); i < n; i++) {
            char c = quoted.charAt(i);
            if (options.escapeHtml() && (c == '<' || c == '>' || c == '&')) {
                appendHexEscape(c);
            } else if (options.asciiOnly() && c > 0x7F) {
                appendHexEscape(c);
            } else {
                output.append(c);
            }
        }
    }

    private void appendHexEscape(char c) throws IOException {
        output.append("\\u")
                .append(Hex.lowerDigit(c >>> 12))
                .append(Hex.lowerDigit(c >>> 8))
                .append(Hex.lowerDigit(c >>> 4))
                .append(Hex.lowerDigit(c));
    }
}
