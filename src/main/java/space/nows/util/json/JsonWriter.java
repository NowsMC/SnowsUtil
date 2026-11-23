package space.nows.util.json;

import space.nows.util.data.Arr;
import space.nows.util.data.Bool;
import space.nows.util.data.Num;
import space.nows.util.data.Obj;
import space.nows.util.data.Str;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;
import space.nows.util.text.Escapes;

import java.io.IOException;
import java.util.Map;

final class JsonWriter {
    private final Appendable output;
    private final boolean pretty;

    JsonWriter(Appendable output, boolean pretty) {
        this.output = output;
        this.pretty = pretty;
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
        Escapes.appendQuoted(value, output);
    }
}
