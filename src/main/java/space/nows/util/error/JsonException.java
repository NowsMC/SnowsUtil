package space.nows.util.error;

/** JSON text is malformed, a value has an unexpected type, or object binding failed. */
public class JsonException extends IllegalArgumentException {
    private static final long serialVersionUID = 1L;

    public JsonException(String message) { super(message); }

    public JsonException(String message, Throwable cause) { super(message, cause); }
}
