package space.nows.util.error;

/** An int key lies outside the fixed range of a range-bound map. */
public class KeyOutOfRangeException extends IndexOutOfBoundsException {
    private static final long serialVersionUID = 1L;
    private final int key;
    private final int min;
    private final int max;

    public KeyOutOfRangeException(int key, int min, int max) {
        super("key " + key + " outside [" + min + ", " + max + "]");
        this.key = key;
        this.min = min;
        this.max = max;
    }

    public int key() { return key; }

    public int min() { return min; }

    public int max() { return max; }
}
