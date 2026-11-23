package space.nows.util.json;

import space.nows.util.check.Preconditions;
import space.nows.util.data.Arr;
import space.nows.util.data.Null;
import space.nows.util.data.Obj;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;

import java.util.List;
import java.util.Map;

/**
 * JSON Patch (RFC 6902) and JSON Merge Patch (RFC 7386). Both return a new document and leave the input unchanged,
 * so a patch that fails part-way does not leave a half-applied document behind.
 */
public final class JsonPatch {
    private JsonPatch() {}

    /**
     * Applies an RFC 6902 patch: an array of {@code add}, {@code remove}, {@code replace}, {@code move}, {@code copy}
     * and {@code test} operations, applied in order.
     *
     * @throws DataException if an operation is malformed or does not apply
     */
    public static Value apply(Value document, Value patch) {
        Preconditions.notNull(document, "document");
        Value working = copy(document);
        for (Value entry : patch(patch)) {
            if (!(entry instanceof Obj op)) throw new DataException("each patch operation must be an object");
            String name = string(op, "op");
            JsonPointer path = JsonPointer.parse(string(op, "path"));
            switch (name) {
                case "add" -> working = add(working, path, copy(required(op, "value")));
                case "replace" -> {
                    path.get(working);
                    working = replace(working, path, copy(required(op, "value")));
                }
                case "remove" -> working = remove(working, path);
                case "move" -> {
                    JsonPointer from = JsonPointer.parse(string(op, "from"));
                    if (isPrefix(from, path)) throw new DataException("cannot move a location into one of its children");
                    Value moved = from.get(working);
                    working = add(remove(working, from), path, moved);
                }
                case "copy" -> {
                    JsonPointer from = JsonPointer.parse(string(op, "from"));
                    working = add(working, path, copy(from.get(working)));
                }
                case "test" -> {
                    Value actual = path.find(working);
                    Value expected = required(op, "value");
                    if (actual == null || !actual.equals(expected)) {
                        throw new DataException("test operation failed at " + path);
                    }
                }
                default -> throw new DataException("unknown patch operation: " + name);
            }
        }
        return working;
    }

    /**
     * Applies an RFC 7386 merge patch: object members set or, when {@code null}, remove members of the target
     * recursively; any non-object patch replaces the target.
     */
    public static Value merge(Value target, Value patch) {
        Preconditions.notNull(patch, "patch");
        if (!(patch instanceof Obj patchObject)) return copy(patch);
        Obj result = target instanceof Obj object ? (Obj) copy(object) : new Obj();
        for (Map.Entry<String, Value> member : patchObject) {
            if (member.getValue() instanceof Null) {
                result.remove(member.getKey());
            } else {
                result.put(member.getKey(), merge(result.get(member.getKey()), member.getValue()));
            }
        }
        return result;
    }

    /** Deep copy of a value tree, so patch operations never alias the caller's data. */
    static Value copy(Value value) {
        if (value instanceof Obj object) {
            Obj out = new Obj(object.size());
            for (Map.Entry<String, Value> member : object) out.put(member.getKey(), copy(member.getValue()));
            return out;
        }
        if (value instanceof Arr array) {
            Arr out = new Arr(array.size());
            for (Value element : array) out.add(copy(element));
            return out;
        }
        return value;
    }

    private static Arr patch(Value patch) {
        if (patch instanceof Arr array) return array;
        throw new DataException("a JSON Patch must be an array of operations");
    }

    private static String string(Obj op, String name) {
        Value value = op.get(name);
        if (value == null) throw new DataException("patch operation is missing \"" + name + "\"");
        return value.asString();
    }

    private static Value required(Obj op, String name) {
        Value value = op.get(name);
        if (value == null) throw new DataException("patch operation is missing \"" + name + "\"");
        return value;
    }

    private static boolean isPrefix(JsonPointer prefix, JsonPointer path) {
        List<String> a = prefix.tokens(), b = path.tokens();
        if (a.size() > b.size()) return false;
        for (int i = 0; i < a.size(); i++) if (!a.get(i).equals(b.get(i))) return false;
        return true;
    }

    private static Value add(Value root, JsonPointer path, Value value) {
        if (path.isRoot()) return value;
        Value parent = parentOf(root, path);
        String last = path.tokens().get(path.tokens().size() - 1);
        if (parent instanceof Obj object) {
            object.put(last, value);
        } else if (parent instanceof Arr array) {
            if (last.equals("-")) {
                array.add(value);
            } else {
                int index = JsonPointer.arrayIndex(last, array.size());
                if (index < 0 || index > array.size()) throw new DataException("array index out of range at " + path);
                array.add(index, value);
            }
        } else {
            throw new DataException("cannot add below a scalar at " + path);
        }
        return root;
    }

    private static Value replace(Value root, JsonPointer path, Value value) {
        if (path.isRoot()) return value;
        Value parent = parentOf(root, path);
        String last = path.tokens().get(path.tokens().size() - 1);
        if (parent instanceof Obj object) {
            if (!object.has(last)) throw new DataException("replace target does not exist: " + path);
            object.put(last, value);
        } else if (parent instanceof Arr array) {
            int index = JsonPointer.arrayIndex(last, array.size());
            if (index < 0 || index >= array.size()) throw new DataException("replace target does not exist: " + path);
            array.set(index, value);
        }
        return root;
    }

    private static Value remove(Value root, JsonPointer path) {
        if (path.isRoot()) throw new DataException("the whole document cannot be removed");
        Value parent = parentOf(root, path);
        String last = path.tokens().get(path.tokens().size() - 1);
        if (parent instanceof Obj object) {
            if (object.remove(last) == null) throw new DataException("remove target does not exist: " + path);
        } else if (parent instanceof Arr array) {
            int index = JsonPointer.arrayIndex(last, array.size());
            if (index < 0 || index >= array.size()) throw new DataException("remove target does not exist: " + path);
            array.remove(index);
        }
        return root;
    }

    private static Value parentOf(Value root, JsonPointer path) {
        List<String> tokens = path.tokens();
        JsonPointer parentPath = JsonPointer.parse(parentString(tokens));
        Value parent = parentPath.get(root);
        if (!(parent instanceof Obj) && !(parent instanceof Arr)) {
            throw new DataException("parent of " + path + " is a scalar");
        }
        return parent;
    }

    private static String parentString(List<String> tokens) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < tokens.size() - 1; i++) {
            out.append('/').append(tokens.get(i).replace("~", "~0").replace("/", "~1"));
        }
        return out.toString();
    }
}
