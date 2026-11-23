package space.nows.util.data;

import java.util.Map;

/** Compact debug rendering used by {@code toString()}. It is not a serialization format; use a format's writer for output. */
final class Debug {
    private Debug() {}

    static String of(Value value) {
        StringBuilder out = new StringBuilder();
        append(value, out);
        return out.toString();
    }

    private static void append(Value value, StringBuilder out) {
        if (value instanceof Obj object) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<String, Value> entry : object) {
                if (!first) out.append(',');
                first = false;
                quote(entry.getKey(), out);
                out.append(':');
                append(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof Arr array) {
            out.append('[');
            boolean first = true;
            for (Value element : array) {
                if (!first) out.append(',');
                first = false;
                append(element, out);
            }
            out.append(']');
        } else if (value instanceof Str string) {
            quote(string.value(), out);
        } else {
            out.append(value);
        }
    }

    private static void quote(String text, StringBuilder out) {
        out.append('"');
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        out.append('"');
    }
}
