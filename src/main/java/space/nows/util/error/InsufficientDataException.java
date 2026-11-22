package space.nows.util.error;

/** A read needed more bytes than the buffer holds. */
public class InsufficientDataException extends IndexOutOfBoundsException {
    private static final long serialVersionUID = 1L;
    private final int needed;
    private final int available;

    public InsufficientDataException(int needed, int available) {
        super("need " + needed + " bytes but only " + available + " available");
        this.needed = needed;
        this.available = available;
    }

    public int needed() { return needed; }

    public int available() { return available; }
}
