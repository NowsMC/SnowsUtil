package space.nows.util.error;

/** Hex text is malformed; {@link #index()} is the offending character offset (or the length if odd). */
public class InvalidHexException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;
    private final int index;

    public InvalidHexException(String message, int index) {
        super(message + " at index " + index);
        this.index = index;
    }

    public int index() { return index; }
}
