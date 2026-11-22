package space.nows.util.error;

/** A matrix (or quaternion) cannot be inverted because it is singular or degenerate. */
public class SingularMatrixException extends ArithmeticException {
    private static final long serialVersionUID = 1L;

    public SingularMatrixException(String message) { super(message); }
}
