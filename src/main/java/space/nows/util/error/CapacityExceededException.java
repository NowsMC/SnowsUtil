package space.nows.util.error;

/** A collection or buffer would have to grow past its maximum size. */
public class CapacityExceededException extends IllegalStateException {
    private static final long serialVersionUID = 1L;
    private final long requested;
    private final long limit;

    public CapacityExceededException(String what, long requested, long limit) {
        super(what + ": requested capacity " + requested + " exceeds limit " + limit);
        this.requested = requested;
        this.limit = limit;
    }

    public long requested() { return requested; }

    public long limit() { return limit; }
}
