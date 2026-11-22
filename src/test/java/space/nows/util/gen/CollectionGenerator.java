package space.nows.util.gen;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Generates the primitive-specialized collections in {@code space.nows.util.collections} from the templates in
 * {@code src/test/resources/templates}. Run {@code main} with the source root as the only argument (default
 * {@code src/main/java}) after editing a template; {@code GeneratedSourcesTest} fails while the two disagree.
 */
public final class CollectionGenerator {
    private CollectionGenerator() {}

    private static final String PACKAGE_PATH = "space/nows/util/collections/";
    private static final Pattern MACRO = Pattern.compile("@(HASH|KEQ|KHASH|VHASH|VNE)\\(([^()]*)\\)");
    private static final Pattern TOKEN = Pattern.compile("\\$([A-Z_]+)\\$");
    private static final Pattern TOKEN_ONLY_LINE = Pattern.compile("^\\s*(\\$[A-Z_]+\\$|@NNKEY)\\s*$");

    private enum Kind {
        INT("Int", "int", "Integer", "int", "0"),
        LONG("Long", "long", "Long", "long", "0L"),
        OBJECT("Object", "Object", "Object", "Object", "null");

        final String name, type, box, storage, zero;

        Kind(String name, String type, String box, String storage, String zero) {
            this.name = name;
            this.type = type;
            this.box = box;
            this.storage = storage;
            this.zero = zero;
        }

        boolean primitive() { return this != OBJECT; }

        String hash(String x) {
            return switch (this) {
                case INT -> x;
                case LONG -> "Long.hashCode(" + x + ")";
                case OBJECT -> x + ".hashCode()";
            };
        }
    }

    public static Map<String, String> generate() {
        Map<String, String> out = new LinkedHashMap<>();
        Kind[][] maps = {
            {Kind.INT, Kind.INT}, {Kind.INT, Kind.LONG}, {Kind.INT, Kind.OBJECT},
            {Kind.LONG, Kind.INT}, {Kind.LONG, Kind.LONG}, {Kind.LONG, Kind.OBJECT},
            {Kind.OBJECT, Kind.INT}, {Kind.OBJECT, Kind.LONG}
        };
        String mapTemplate = template("HashMap.tpl");
        for (Kind[] kv : maps) {
            String name = kv[0].name + kv[1].name + "Map";
            out.put(PACKAGE_PATH + name + ".java", renderMap(mapTemplate, kv[0], kv[1], name));
        }
        String setTemplate = template("HashSet.tpl");
        String listTemplate = template("PrimitiveList.tpl");
        for (Kind k : new Kind[] {Kind.INT, Kind.LONG}) {
            out.put(PACKAGE_PATH + k.name + "HashSet.java", renderSet(setTemplate, k));
            out.put(PACKAGE_PATH + k.name + "List.java", renderList(listTemplate, k));
        }
        return out;
    }

    private static String renderMap(String template, Kind k, Kind v, String name) {
        boolean keyObj = !k.primitive(), valObj = !v.primitive();
        String gen = keyObj ? "<K>" : valObj ? "<V>" : "";
        Map<String, String> t = new LinkedHashMap<>();
        Set<String> imports = new TreeSet<>(List.of("java.util.Arrays", "java.util.ConcurrentModificationException",
                "java.util.HashMap", "java.util.Map", "java.util.Objects"));
        t.put("CLASS", name);
        t.put("GEN", gen);
        t.put("DIAMOND", gen.isEmpty() ? "" : "<>");
        t.put("WILD", gen.isEmpty() ? "" : "<?>");
        t.put("KT", k.storage);
        t.put("VT", v.storage);
        t.put("KPUB", keyObj ? "K" : k.type);
        t.put("VPUB", valObj ? "V" : v.type);
        t.put("KDOC", keyObj ? "K" : k.type);
        t.put("VDOC", valObj ? "V" : v.type);
        t.put("KBOX", keyObj ? "K" : k.box);
        t.put("VBOX", valObj ? "V" : v.box);
        t.put("KEMPTY", k.zero);
        t.put("VCLEAR", v.zero);
        t.put("VNONE", valObj ? "null" : "noEntryValue");
        t.put("KCAST", keyObj ? "(K) " : "");
        t.put("VCAST", valObj ? "(V) " : "");
        t.put("SUPPRESS", keyObj || valObj ? "@SuppressWarnings(\"unchecked\")" : "");
        t.put("NOENTRYDOC", valObj ? "" : " * Absent keys read as the no-entry value given at construction ({@code " + v.zero + "} by default).\n");
        t.put("ABSENTDOC", valObj ? "{@code null}" : "the no-entry value");
        t.put("COPYDOC", keyObj ? " (keys are shared, not cloned)" : valObj ? " (values are shared, not cloned)" : "");
        String consumer;
        if (keyObj) {
            consumer = "Obj" + v.name + "Consumer<? super K>";
            imports.add("java.util.function.Obj" + v.name + "Consumer");
        } else if (valObj) {
            consumer = k.name + "ObjConsumer<? super V>";
            imports.add("space.nows.util.function." + k.name + "ObjConsumer");
        } else {
            consumer = k.name + v.name + "Consumer";
            imports.add("space.nows.util.function." + consumer);
        }
        t.put("CONSUMER", consumer);
        if (valObj) {
            String fn = k.name + "Function";
            t.put("MAPPING", fn + "<? extends V>");
            imports.add("java.util.function." + fn);
        } else {
            String op = v.name + "BinaryOperator";
            t.put("BINOP", op);
            t.put("BINAPPLY", "applyAs" + v.name);
            imports.add("java.util.function." + op);
        }
        t.put("IMPORTS", importBlock(imports));
        return render(template, t, flags(k, v), k, v);
    }

    private static String renderSet(String template, Kind k) {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("CLASS", k.name + "HashSet");
        t.put("K", k.type);
        t.put("ZERO", k.zero);
        t.put("BOX", k.box);
        t.put("CONSUMER", k.name + "Consumer");
        t.put("ITER", "Of" + k.name);
        t.put("NEXT", "next" + k.name);
        t.put("STREAM", k.name + "Stream");
        t.put("IMPORTS", importBlock(Set.of("java.util.Arrays", "java.util.ConcurrentModificationException",
                "java.util.HashSet", "java.util.NoSuchElementException", "java.util.Objects", "java.util.PrimitiveIterator",
                "java.util.Set", "java.util.function." + k.name + "Consumer", "java.util.stream." + k.name + "Stream")));
        return render(template, t, Set.of(), k, k);
    }

    private static String renderList(String template, Kind k) {
        Map<String, String> t = new LinkedHashMap<>();
        t.put("CLASS", k.name + "List");
        t.put("T", k.type);
        t.put("BOX", k.box);
        t.put("CONSUMER", k.name + "Consumer");
        t.put("ITER", "Of" + k.name);
        t.put("NEXT", "next" + k.name);
        t.put("STREAM", k.name + "Stream");
        t.put("IMPORTS", importBlock(Set.of("space.nows.util.check.Preconditions", "java.util.Arrays",
                "java.util.ConcurrentModificationException", "java.util.NoSuchElementException", "java.util.Objects",
                "java.util.PrimitiveIterator", "java.util.function." + k.name + "Consumer", "java.util.stream." + k.name + "Stream")));
        return render(template, t, Set.of(), k, k);
    }

    private static Set<String> flags(Kind k, Kind v) {
        Set<String> f = new TreeSet<>();
        f.add(k.primitive() ? "KEY_PRIM" : "KEY_OBJ");
        f.add(v.primitive() ? "VAL_PRIM" : "VAL_OBJ");
        return f;
    }

    private static String importBlock(Set<String> imports) {
        List<String> java = new ArrayList<>(), project = new ArrayList<>();
        for (String i : new TreeSet<>(imports)) (i.startsWith("java.") ? java : project).add("import " + i + ";");
        StringBuilder sb = new StringBuilder();
        if (!project.isEmpty()) sb.append(String.join("\n", project)).append("\n\n");
        sb.append(String.join("\n", java));
        return sb.toString();
    }

    private static String render(String template, Map<String, String> tokens, Set<String> flags, Kind k, Kind v) {
        List<String> out = new ArrayList<>();
        Deque<Boolean> active = new ArrayDeque<>();
        active.push(true);
        for (String line : template.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#if ")) {
                active.push(active.peek() && flags.contains(trimmed.substring(4).trim()));
                continue;
            }
            if (trimmed.equals("#else")) {
                boolean cur = active.pop();
                boolean parent = active.peek();
                active.push(parent && !cur);
                continue;
            }
            if (trimmed.equals("#end")) {
                active.pop();
                continue;
            }
            if (!active.peek()) continue;
            String expanded = expand(line, tokens, k, v);
            if (TOKEN_ONLY_LINE.matcher(line).matches() && expanded.isBlank()) continue;
            out.add(expanded);
        }
        String text = String.join("\n", out);
        return text.endsWith("\n") ? text : text + "\n";
    }

    private static String expand(String line, Map<String, String> tokens, Kind k, Kind v) {
        String s = line;
        if (s.contains("@NNKEY")) s = s.replace("@NNKEY", k.primitive() ? "" : "Objects.requireNonNull(key, \"key\");");
        Matcher m = MACRO.matcher(s);
        s = m.replaceAll((MatchResult r) -> Matcher.quoteReplacement(macro(r.group(1), r.group(2), k, v)));
        Matcher tm = TOKEN.matcher(s);
        return tm.replaceAll((MatchResult r) -> {
            String value = tokens.get(r.group(1));
            if (value == null) throw new IllegalStateException("unknown token $" + r.group(1) + "$ in: " + line);
            return Matcher.quoteReplacement(value);
        });
    }

    private static String macro(String name, String args, Kind k, Kind v) {
        String[] a = args.split(",\\s*");
        return switch (name) {
            case "HASH" -> k.primitive() ? "HashTables.mix(" + a[0] + ")" : "HashTables.mix(" + a[0] + ".hashCode())";
            case "KEQ" -> k.primitive() ? a[0] + " == " + a[1] : a[0] + " == " + a[1] + " || " + a[0] + ".equals(" + a[1] + ")";
            case "KHASH" -> k.hash(a[0]);
            case "VHASH" -> v.primitive() ? v.hash(a[0]) : "Objects.hashCode(" + a[0] + ")";
            case "VNE" -> v.primitive() ? a[0] + " != " + a[1] : "!Objects.equals(" + a[0] + ", " + a[1] + ")";
            default -> throw new IllegalStateException("unknown macro @" + name);
        };
    }

    private static String template(String name) {
        try (InputStream in = CollectionGenerator.class.getResourceAsStream("/templates/" + name)) {
            if (in == null) throw new IllegalStateException("missing template " + name);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8).replace("\r\n", "\n");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static void main(String[] args) throws IOException {
        Path root = Path.of(args.length > 0 ? args[0] : "src/main/java");
        for (Map.Entry<String, String> e : generate().entrySet()) {
            Path target = root.resolve(e.getKey());
            Files.createDirectories(target.getParent());
            Files.writeString(target, e.getValue(), StandardCharsets.UTF_8);
        }
    }
}
