package space.nows.util.json;

import space.nows.util.data.Arr;
import space.nows.util.data.Bool;
import space.nows.util.data.Null;
import space.nows.util.data.Num;
import space.nows.util.data.Obj;
import space.nows.util.data.Str;
import space.nows.util.data.Value;
import space.nows.util.error.DataException;
import space.nows.util.text.Escapes;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Canonical JSON per RFC 8785 (JCS): members sorted by UTF-16 code units, no insignificant whitespace, strings escaped
 * minimally, and numbers written as ECMAScript prints them. A number is first converted to the nearest IEEE double, so a
 * number outside the double range is rejected rather than rounded silently.
 */
final class Canonical {
    private Canonical() {}

    static String write(Value value) {
        StringBuilder out = new StringBuilder();
        append(value, out, 0);
        return out.toString();
    }

    private static void append(Value value, StringBuilder out, int depth) {
        if (depth > Value.MAX_DEPTH) throw new DataException("maximum nesting depth " + Value.MAX_DEPTH + " exceeded");
        if (value instanceof Obj object) {
            List<String> keys = new ArrayList<>(object.keys());
            keys.sort(null);
            out.append('{');
            boolean first = true;
            for (String key : keys) {
                if (!first) out.append(',');
                first = false;
                out.append(Escapes.quote(key)).append(':');
                append(object.get(key), out, depth + 1);
            }
            out.append('}');
        } else if (value instanceof Arr array) {
            out.append('[');
            boolean first = true;
            for (Value element : array) {
                if (!first) out.append(',');
                first = false;
                append(element, out, depth + 1);
            }
            out.append(']');
        } else if (value instanceof Str string) {
            out.append(Escapes.quote(string.value()));
        } else if (value instanceof Num number) {
            out.append(formatNumber(number.asDouble()));
        } else if (value instanceof Bool bool) {
            out.append(bool.value() ? "true" : "false");
        } else if (value instanceof Null) {
            out.append("null");
        } else {
            throw new DataException("unsupported value for canonical JSON: " + value.type().label());
        }
    }

    /** ECMAScript {@code Number.prototype.toString} for a finite double (RFC 8785 section 3.2.2.3). */
    static String formatNumber(double d) {
        if (d == 0) return "0";
        if (Double.isNaN(d) || Double.isInfinite(d)) throw new DataException("non-finite numbers have no canonical form");
        StringBuilder out = new StringBuilder();
        if (d < 0) {
            out.append('-');
            d = -d;
        }
        // Double.toString yields the shortest digits that round-trip (JDK 19 and later).
        BigDecimal shortest = new BigDecimal(Double.toString(d)).stripTrailingZeros();
        while (shortest.precision() > 1) {
            BigDecimal fewer = shortest.round(new MathContext(shortest.precision() - 1, RoundingMode.HALF_EVEN));
            if (fewer.doubleValue() != d) break;
            shortest = fewer.stripTrailingZeros();
        }
        String digits = shortest.unscaledValue().toString();
        int k = digits.length();
        int n = k - shortest.scale();
        if (k <= n && n <= 21) {
            out.append(digits);
            out.append("0".repeat(n - k));
        } else if (0 < n && n <= 21) {
            out.append(digits, 0, n).append('.').append(digits, n, k);
        } else if (-6 < n && n <= 0) {
            out.append("0.").append("0".repeat(-n)).append(digits);
        } else {
            int exponent = n - 1;
            out.append(digits.charAt(0));
            if (k > 1) out.append('.').append(digits, 1, k);
            out.append('e').append(exponent < 0 ? '-' : '+').append(Math.abs(exponent));
        }
        return out.toString();
    }

}
