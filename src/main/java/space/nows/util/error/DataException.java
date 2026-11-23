package space.nows.util.error;

/** JSON text is malformed, a value has an unexpected type, or object binding failed. */
public class DataException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public DataException(String message) { super(message); }

    public DataException(String message, Throwable cause) { super(message, cause); }
}
