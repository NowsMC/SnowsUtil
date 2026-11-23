package space.nows.util.json;

import space.nows.util.data.Value;
import space.nows.util.error.DataException;

/**
 * Which relaxations of RFC 8259 the parser accepts. {@link #STRICT} is the default and accepts only standard JSON.
 * Duplicate object keys are always rejected, because keeping one of two values would silently lose data.
 */
public final class ParseOptions {
    public static final ParseOptions STRICT = builder().build();

    private final boolean comments;
    private final boolean trailingCommas;
    private final boolean singleQuotes;
    private final boolean unquotedKeys;
    private final boolean leadingPlus;
    private final boolean leadingZeros;
    private final boolean hexNumbers;
    private final int maxDepth;

    private ParseOptions(Builder b) {
        comments = b.comments;
        trailingCommas = b.trailingCommas;
        singleQuotes = b.singleQuotes;
        unquotedKeys = b.unquotedKeys;
        leadingPlus = b.leadingPlus;
        leadingZeros = b.leadingZeros;
        hexNumbers = b.hexNumbers;
        maxDepth = b.maxDepth;
    }

    public static Builder builder() { return new Builder(); }

    /** Every relaxation below is enabled. */
    public static ParseOptions lenient() {
        return builder().lenient().build();
    }

    public boolean comments() { return comments; }
    public boolean trailingCommas() { return trailingCommas; }
    public boolean singleQuotes() { return singleQuotes; }
    public boolean unquotedKeys() { return unquotedKeys; }
    public boolean leadingPlus() { return leadingPlus; }
    public boolean leadingZeros() { return leadingZeros; }
    public boolean hexNumbers() { return hexNumbers; }
    public int maxDepth() { return maxDepth; }

    public static final class Builder {
        private boolean comments;
        private boolean trailingCommas;
        private boolean singleQuotes;
        private boolean unquotedKeys;
        private boolean leadingPlus;
        private boolean leadingZeros;
        private boolean hexNumbers;
        private int maxDepth = Value.MAX_DEPTH;

        private Builder() {}

        public Builder lenient() {
            comments = trailingCommas = singleQuotes = unquotedKeys = true;
            leadingPlus = leadingZeros = hexNumbers = true;
            return this;
        }

        /** Allows {@code // line} and {@code /* block *\/} comments wherever whitespace is allowed. */
        public Builder comments(boolean allow) { comments = allow; return this; }

        public Builder trailingCommas(boolean allow) { trailingCommas = allow; return this; }

        public Builder singleQuotes(boolean allow) { singleQuotes = allow; return this; }

        /** Allows identifiers such as {@code name} as object keys. */
        public Builder unquotedKeys(boolean allow) { unquotedKeys = allow; return this; }

        public Builder leadingPlus(boolean allow) { leadingPlus = allow; return this; }

        public Builder leadingZeros(boolean allow) { leadingZeros = allow; return this; }

        public Builder hexNumbers(boolean allow) { hexNumbers = allow; return this; }

        /** Nesting limit, at least 1 and at most {@link Value#MAX_DEPTH}. */
        public Builder maxDepth(int depth) {
            if (depth < 1 || depth > Value.MAX_DEPTH) {
                throw new DataException("maxDepth must be in [1, " + Value.MAX_DEPTH + "] but was " + depth);
            }
            maxDepth = depth;
            return this;
        }

        public ParseOptions build() { return new ParseOptions(this); }
    }
}
