package space.nows.util.json;

import space.nows.util.check.Preconditions;
import space.nows.util.data.Arr;
import space.nows.util.data.Bool;
import space.nows.util.data.Null;
import space.nows.util.data.Num;
import space.nows.util.data.Obj;
import space.nows.util.data.Str;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A JSONPath query. Supported syntax: {@code $} root, {@code .name} and {@code ['name']} members, {@code [n]} and
 * {@code [-n]} indexes, {@code [*]} and {@code .*} wildcards, {@code [a,b]} unions, {@code [start:end:step]} slices,
 * {@code ..name} recursive descent, and {@code [?(expr)]} filters. Filter expressions combine comparisons
 * ({@code == != < <= > >=}) with {@code && || !}; {@code @} is the candidate node, and {@code @.path} alone tests existence.
 */
public final class JsonPath {
    private final List<Step> steps;
    private final String source;

    private JsonPath(List<Step> steps, String source) {
        this.steps = steps;
        this.source = source;
    }

    /**
     * Compiles a query.
     *
     * @throws DataException if the query is malformed
     */
    public static JsonPath compile(String expression) {
        Preconditions.notNull(expression, "expression");
        return new Compiler(expression).compile();
    }

    /** All matching nodes, in document order. */
    public List<Value> eval(Value root) {
        Preconditions.notNull(root, "root");
        List<Value> current = List.of(root);
        for (Step step : steps) {
            List<Value> next = new ArrayList<>();
            for (Value node : current) step.apply(node, next);
            current = next;
        }
        return current;
    }

    /** The first matching node, or {@code null} if nothing matches. */
    public Value evalOne(Value root) {
        List<Value> matches = eval(root);
        return matches.isEmpty() ? null : matches.get(0);
    }

    public boolean exists(Value root) { return !eval(root).isEmpty(); }

    @Override public String toString() { return source; }

    private interface Step {
        void apply(Value node, List<Value> out);
    }

    private record Member(String name) implements Step {
        @Override public void apply(Value node, List<Value> out) {
            if (node instanceof Obj object && object.get(name) != null) out.add(object.get(name));
        }
    }

    private record Index(int index) implements Step {
        @Override public void apply(Value node, List<Value> out) {
            if (!(node instanceof Arr array)) return;
            int i = index < 0 ? array.size() + index : index;
            if (i >= 0 && i < array.size()) out.add(array.get(i));
        }
    }

    private record Wildcard() implements Step {
        @Override public void apply(Value node, List<Value> out) { children(node, out); }
    }

    private record Union(List<Step> selectors) implements Step {
        @Override public void apply(Value node, List<Value> out) {
            for (Step selector : selectors) selector.apply(node, out);
        }
    }

    private record Slice(Integer start, Integer end, int step) implements Step {
        @Override public void apply(Value node, List<Value> out) {
            if (!(node instanceof Arr array)) return;
            int n = array.size();
            if (step > 0) {
                int from = clamp(start == null ? 0 : resolve(start, n), 0, n);
                int to = clamp(end == null ? n : resolve(end, n), 0, n);
                for (int i = from; i < to; i += step) out.add(array.get(i));
            } else {
                int from = clamp(start == null ? n - 1 : resolve(start, n), -1, n - 1);
                int to = end == null ? -1 : clamp(resolve(end, n), -1, n - 1);
                for (int i = from; i > to; i += step) out.add(array.get(i));
            }
        }

        private static int resolve(int bound, int length) {
            return bound < 0 ? length + bound : bound;
        }

        private static int clamp(int value, int low, int high) {
            return Math.max(low, Math.min(value, high));
        }
    }

    private record Recursive(Step selector) implements Step {
        @Override public void apply(Value node, List<Value> out) {
            descend(node, selector, out);
        }

        private static void descend(Value node, Step selector, List<Value> out) {
            selector.apply(node, out);
            List<Value> kids = new ArrayList<>();
            children(node, kids);
            for (Value kid : kids) descend(kid, selector, out);
        }
    }

    private record Filter(Predicate predicate) implements Step {
        @Override public void apply(Value node, List<Value> out) {
            List<Value> kids = new ArrayList<>();
            children(node, kids);
            for (Value kid : kids) if (predicate.test(kid)) out.add(kid);
        }
    }

    private static void children(Value node, List<Value> out) {
        if (node instanceof Arr array) {
            out.addAll(array.asList());
        } else if (node instanceof Obj object) {
            for (Map.Entry<String, Value> member : object) out.add(member.getValue());
        }
    }

    private static final class Compiler {
        private final String text;
        private int pos;

        Compiler(String text) {
            this.text = text;
        }

        JsonPath compile() {
            if (text.isEmpty() || text.charAt(0) != '$') throw error("JSONPath must start with '$'");
            pos = 1;
            List<Step> steps = new ArrayList<>();
            while (pos < text.length()) {
                char c = text.charAt(pos);
                if (c == '.') {
                    pos++;
                    if (peek() == '.') {
                        pos++;
                        steps.add(new Recursive(peek() == '[' ? bracketStep() : dotSelector()));
                    } else {
                        steps.add(dotSelector());
                    }
                } else if (c == '[') {
                    steps.add(bracketStep());
                } else {
                    throw error("unexpected character '" + c + "'");
                }
            }
            return new JsonPath(steps, text);
        }

        private Step dotSelector() {
            if (peek() == '*') {
                pos++;
                return new Wildcard();
            }
            int start = pos;
            while (pos < text.length() && isName(text.charAt(pos))) pos++;
            if (start == pos) throw error("expected a member name");
            return new Member(text.substring(start, pos));
        }

        private Step bracketStep() {
            pos++;
            skipSpaces();
            if (peek() == '?') {
                pos++;
                skipSpaces();
                expect('(');
                Predicate predicate = new PredicateParser(this).parseAll();
                skipSpaces();
                expect(')');
                skipSpaces();
                expect(']');
                return new Filter(predicate);
            }
            List<Step> selectors = new ArrayList<>();
            do {
                skipSpaces();
                selectors.add(oneSelector());
                skipSpaces();
            } while (consume(','));
            expect(']');
            return selectors.size() == 1 ? selectors.get(0) : new Union(selectors);
        }

        private Step oneSelector() {
            int c = peek();
            if (c == '*') {
                pos++;
                return new Wildcard();
            }
            if (c == '\'' || c == '"') return new Member(quoted((char) c));
            return numberOrSlice();
        }

        private Step numberOrSlice() {
            Integer first = optionalInt();
            if (!consume(':')) {
                if (first == null) throw error("expected an index, name, wildcard or slice");
                return new Index(first);
            }
            Integer end = optionalInt();
            int step = 1;
            if (consume(':')) {
                Integer s = optionalInt();
                if (s != null) step = s;
                if (step == 0) throw error("slice step must not be zero");
            }
            return new Slice(first, end, step);
        }

        private Integer optionalInt() {
            skipSpaces();
            int start = pos;
            if (peek() == '-') pos++;
            while (pos < text.length() && Character.isDigit(text.charAt(pos))) pos++;
            String digits = text.substring(start, pos);
            if (digits.isEmpty() || digits.equals("-")) {
                pos = start;
                return null;
            }
            try {
                return Integer.parseInt(digits);
            } catch (NumberFormatException e) {
                throw error("index out of range: " + digits);
            }
        }

        String quoted(char quote) {
            pos++;
            StringBuilder out = new StringBuilder();
            while (true) {
                if (pos >= text.length()) throw error("unterminated quoted name");
                char c = text.charAt(pos++);
                if (c == quote) return out.toString();
                if (c == '\\' && pos < text.length()) c = text.charAt(pos++);
                out.append(c);
            }
        }

        int peek() { return pos < text.length() ? text.charAt(pos) : -1; }

        String text() { return text; }

        int position() { return pos; }

        void advance(int count) { pos += count; }

        boolean startsWith(String token) { return text.startsWith(token, pos); }

        int optionalIntOrFail() {
            Integer value = optionalInt();
            if (value == null) throw error("expected an index");
            return value;
        }

        boolean consume(char c) {
            if (peek() == c) {
                pos++;
                return true;
            }
            return false;
        }

        void expect(char c) {
            if (!consume(c)) throw error("expected '" + c + "'");
        }

        void skipSpaces() {
            while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) pos++;
        }

        DataException error(String message) {
            return new DataException("invalid JSONPath \"" + text + "\" at position " + pos + ": " + message);
        }

        static boolean isName(char c) {
            return Character.isLetterOrDigit(c) || c == '_' || c == '-';
        }
    }

    /** Boolean filter expression with {@code @} bound to the candidate node. */
    private static final class Predicate {
        private final Expr expr;

        Predicate(Expr expr) {
            this.expr = expr;
        }

        boolean test(Value candidate) {
            return expr.eval(candidate);
        }
    }

    private interface Expr {
        boolean eval(Value at);
    }

    private static final class PredicateParser {
        private final Compiler compiler;

        PredicateParser(Compiler compiler) {
            this.compiler = compiler;
        }

        Predicate parseAll() {
            Expr expr = or();
            return new Predicate(expr);
        }

        private Expr or() {
            Expr left = and();
            while (true) {
                compiler.skipSpaces();
                if (!compiler.startsWith("||")) return left;
                compiler.advance(2);
                Expr a = left, b = and();
                left = at -> a.eval(at) || b.eval(at);
            }
        }

        private Expr and() {
            Expr left = unary();
            while (true) {
                compiler.skipSpaces();
                if (!compiler.startsWith("&&")) return left;
                compiler.advance(2);
                Expr a = left, b = unary();
                left = at -> a.eval(at) && b.eval(at);
            }
        }

        private Expr unary() {
            compiler.skipSpaces();
            if (compiler.consume('!')) {
                Expr inner = unary();
                return at -> !inner.eval(at);
            }
            if (compiler.consume('(')) {
                Expr inner = or();
                compiler.skipSpaces();
                compiler.expect(')');
                return inner;
            }
            return comparison();
        }

        private Expr comparison() {
            Operand left = operand();
            compiler.skipSpaces();
            String op = operator();
            if (op == null) {
                if (!left.isQuery()) throw compiler.error("a bare literal is not a valid filter");
                return at -> left.resolve(at) != null;
            }
            Operand right = operand();
            return at -> compare(left.resolve(at), op, right.resolve(at));
        }

        private String operator() {
            for (String op : new String[]{"==", "!=", "<=", ">=", "<", ">"}) {
                if (compiler.text().startsWith(op, compiler.position())) {
                    compiler.advance(op.length());
                    return op;
                }
            }
            return null;
        }

        private Operand operand() {
            compiler.skipSpaces();
            int c = compiler.peek();
            if (c == '@') {
                compiler.advance(1);
                List<Step> steps = new ArrayList<>();
                while (true) {
                    if (compiler.consume('.')) {
                        int start = compiler.position();
                        while (compiler.position() < compiler.text().length() && Compiler.isName(compiler.text().charAt(compiler.position()))) {
                            compiler.advance(1);
                        }
                        if (start == compiler.position()) throw compiler.error("expected a member name after '@.'");
                        steps.add(new Member(compiler.text().substring(start, compiler.position())));
                    } else if (compiler.peek() == '[') {
                        compiler.advance(1);
                        int inner = compiler.peek();
                        if (inner == '\'' || inner == '"') {
                            steps.add(new Member(compiler.quoted((char) inner)));
                        } else {
                            steps.add(new Index(compiler.optionalIntOrFail()));
                        }
                        compiler.skipSpaces();
                        compiler.expect(']');
                    } else {
                        break;
                    }
                }
                return new Operand(steps, null);
            }
            if (c == '\'' || c == '"') return new Operand(List.of(), new Str(compiler.quoted((char) c)));
            int start = compiler.position();
            while (compiler.position() < compiler.text().length()
                    && "+-.0123456789eEtruefalsn".indexOf(compiler.text().charAt(compiler.position())) >= 0
                    && !compiler.text().startsWith("&&", compiler.position())
                    && !compiler.text().startsWith("||", compiler.position())) {
                compiler.advance(1);
            }
            String token = compiler.text().substring(start, compiler.position());
            return switch (token) {
                case "true" -> new Operand(List.of(), Bool.TRUE);
                case "false" -> new Operand(List.of(), Bool.FALSE);
                case "null" -> new Operand(List.of(), Null.INSTANCE);
                default -> {
                    try {
                        yield new Operand(List.of(), Num.of(new BigDecimal(token)));
                    } catch (NumberFormatException e) {
                        throw compiler.error("invalid literal \"" + token + "\"");
                    }
                }
            };
        }

        private static boolean compare(Value a, String op, Value b) {
            if (a == null || b == null) return false;
            if (op.equals("==")) return equal(a, b);
            if (op.equals("!=")) return !equal(a, b);
            int order;
            if (a instanceof Num x && b instanceof Num y) {
                order = x.asDecimal().compareTo(y.asDecimal());
            } else if (a instanceof Str x && b instanceof Str y) {
                order = x.value().compareTo(y.value());
            } else {
                return false;
            }
            return switch (op) {
                case "<" -> order < 0;
                case "<=" -> order <= 0;
                case ">" -> order > 0;
                case ">=" -> order >= 0;
                default -> false;
            };
        }

        private static boolean equal(Value a, Value b) {
            if (a instanceof Num x && b instanceof Num y) return x.asDecimal().compareTo(y.asDecimal()) == 0;
            return a.equals(b);
        }
    }

    private static final class Operand {
        private final List<Step> path;
        private final Value literal;

        Operand(List<Step> path, Value literal) {
            this.path = path;
            this.literal = literal;
        }

        boolean isQuery() { return literal == null; }

        Value resolve(Value at) {
            if (literal != null) return literal;
            List<Value> nodes = List.of(at);
            for (Step step : path) {
                List<Value> next = new ArrayList<>();
                for (Value node : nodes) step.apply(node, next);
                nodes = next;
            }
            return nodes.isEmpty() ? null : nodes.get(0);
        }
    }
}
