package space.nows.util.error;

/** Byte sequence is not well-formed UTF-8; {@link #index()} is the offset of the offending byte. */
public class InvalidUtf8Exception extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;
    private final int index;

    public InvalidUtf8Exception(String message, int index) {
        super(message + " at byte index " + index);
        this.index = index;
    }

    public int index() { return index; }
}
