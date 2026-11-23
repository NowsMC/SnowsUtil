package space.nows.util.data;

import java.util.Map;

/** Compact debug rendering used by {@code toString()}. It is not a serialization format; use a format's writer for output. */
final class DataText {
    private DataText() {}

    static String of(DataValue value) {
        StringBuilder out = new StringBuilder();
        append(value, out);
        return out.toString();
    }

    private static void append(DataValue value, StringBuilder out) {
        if (value instanceof DataObject object) {
            out.append('{');
            boolean first = true;
            for (Map.Entry<String, DataValue> entry : object) {
                if (!first) out.append(',');
                first = false;
                quote(entry.getKey(), out);
                out.append(':');
                append(entry.getValue(), out);
            }
            out.append('}');
        } else if (value instanceof DataArray array) {
            out.append('[');
            boolean first = true;
            for (DataValue element : array) {
                if (!first) out.append(',');
                first = false;
                append(element, out);
            }
            out.append(']');
        } else if (value instanceof DataString string) {
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
