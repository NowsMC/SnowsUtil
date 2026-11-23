package space.nows.util.json;

import space.nows.util.check.Preconditions;
import space.nows.util.data.Arr;
import space.nows.util.data.Obj;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A JSON Pointer (RFC 6901), such as {@code /store/book/0/title}. The empty pointer refers to the whole document.
 * {@code ~1} stands for {@code /} and {@code ~0} for {@code ~} inside a token.
 */
public final class JsonPointer {
    private static final JsonPointer ROOT = new JsonPointer(List.of());

    private final List<String> tokens;

    private JsonPointer(List<String> tokens) {
        this.tokens = Collections.unmodifiableList(tokens);
    }

    public static JsonPointer root() { return ROOT; }

    /** Parses an RFC 6901 pointer. Tokens are unescaped; {@code ~} must be followed by {@code 0} or {@code 1}. */
    public static JsonPointer parse(String pointer) {
        Preconditions.notNull(pointer, "pointer");
        if (pointer.isEmpty()) return ROOT;
        if (pointer.charAt(0) != '/') throw new DataException("JSON Pointer must be empty or start with '/': " + pointer);
        List<String> tokens = new ArrayList<>();
        for (String raw : pointer.substring(1).split("/", -1)) tokens.add(unescape(raw, pointer));
        return new JsonPointer(tokens);
    }

    private static String unescape(String raw, String pointer) {
        if (raw.indexOf('~') < 0) return raw;
        StringBuilder out = new StringBuilder(raw.length());
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (c != '~') {
                out.append(c);
            } else if (i + 1 < raw.length() && raw.charAt(i + 1) == '0') {
                out.append('~');
                i++;
            } else if (i + 1 < raw.length() && raw.charAt(i + 1) == '1') {
                out.append('/');
                i++;
            } else {
                throw new DataException("invalid '~' escape in JSON Pointer: " + pointer);
            }
        }
        return out.toString();
    }

    /** The unescaped reference tokens, in order. */
    public List<String> tokens() { return tokens; }

    public boolean isRoot() { return tokens.isEmpty(); }

    /** Value at this pointer.
     *
     * @throws DataException if the pointer does not resolve in {@code root}
     */
    public Value get(Value root) {
        Value found = find(root);
        if (found == null) throw new DataException("JSON Pointer does not resolve: " + this);
        return found;
    }

    /** Value at this pointer, or {@code null} if it does not resolve. */
    public Value find(Value root) {
        Preconditions.notNull(root, "root");
        Value current = root;
        for (String token : tokens) {
            if (current instanceof Obj object) {
                current = object.get(token);
            } else if (current instanceof Arr array) {
                int index = arrayIndex(token, array.size());
                if (index < 0 || index >= array.size()) return null;
                current = array.get(index);
            } else {
                return null;
            }
            if (current == null) return null;
        }
        return current;
    }

    /**
     * Array index for {@code token} in an array of {@code size} elements, or -1 if the token is not a valid index
     * (leading zeros are not allowed, and {@code -} means "past the end").
     */
    static int arrayIndex(String token, int size) {
        if (token.isEmpty() || (token.length() > 1 && token.charAt(0) == '0')) return -1;
        for (int i = 0; i < token.length(); i++) {
            if (token.charAt(i) < '0' || token.charAt(i) > '9') return -1;
        }
        if (token.length() > 10) return -1;
        long value = Long.parseLong(token);
        return value > Integer.MAX_VALUE ? -1 : (int) value;
    }

    @Override public boolean equals(Object other) {
        return other instanceof JsonPointer pointer && tokens.equals(pointer.tokens);
    }

    @Override public int hashCode() { return tokens.hashCode(); }

    @Override public String toString() {
        StringBuilder out = new StringBuilder();
        for (String token : tokens) {
            out.append('/').append(token.replace("~", "~0").replace("/", "~1"));
        }
        return out.toString();
    }
}
