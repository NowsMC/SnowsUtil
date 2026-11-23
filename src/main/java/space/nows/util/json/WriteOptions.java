package space.nows.util.json;

/**
 * How JSON text is written. The defaults produce compact output with only the escapes RFC 8259 requires.
 *
 * <ul>
 *   <li>{@code escapeHtml} writes the characters {@code <}, {@code >} and {@code &} as four-digit hexadecimal
 *       escapes, so the output can be embedded in an HTML script element safely.</li>
 *   <li>{@code asciiOnly} writes every non-ASCII character as a four-digit hexadecimal escape, so the output is pure
 *       ASCII.</li>
 * </ul>
 */
public final class WriteOptions {
    public static final WriteOptions COMPACT = builder().build();
    public static final WriteOptions PRETTY = builder().pretty(true).build();

    private final boolean pretty;
    private final boolean escapeHtml;
    private final boolean asciiOnly;

    private WriteOptions(Builder b) {
        pretty = b.pretty;
        escapeHtml = b.escapeHtml;
        asciiOnly = b.asciiOnly;
    }

    public static Builder builder() { return new Builder(); }

    public boolean pretty() { return pretty; }

    public boolean escapeHtml() { return escapeHtml; }

    public boolean asciiOnly() { return asciiOnly; }

    public static final class Builder {
        private boolean pretty;
        private boolean escapeHtml;
        private boolean asciiOnly;

        private Builder() {}

        public Builder pretty(boolean value) { pretty = value; return this; }

        public Builder escapeHtml(boolean value) { escapeHtml = value; return this; }

        public Builder asciiOnly(boolean value) { asciiOnly = value; return this; }

        public WriteOptions build() { return new WriteOptions(this); }
    }
}
