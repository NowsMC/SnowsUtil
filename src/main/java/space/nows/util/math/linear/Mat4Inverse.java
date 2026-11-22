package space.nows.util.math.linear;

import space.nows.util.error.SingularMatrixException;

/** Matrix inversion. */
public final class Mat4Inverse {
    private Mat4Inverse() {}

    private static final double SINGULAR_EPSILON = 1e-12;

    /**
     * Inverts {@code target} in place (Gauss-Jordan with scaled partial pivoting, computed in double precision) and
     * returns it. If the matrix is singular it is left unchanged.
     *
     * <p>Each row is judged against its own largest entry, so a matrix whose axes have very different scales, such as
     * a scale of 1e-14 on one axis, is still inverted; only rows that are numerically dependent on the others count
     * as singular.
     *
     * @throws SingularMatrixException if a row is zero or NaN, or a pivot is smaller than 1e-12 relative to its row
     */
    public static Mat4 invert(Mat4 target) {
        float[] m = target.m;
        double[] a = new double[32];
        double[] rowScale = new double[4];
        for (int r = 0; r < 4; r++) {
            double largest = 0.0;
            for (int c = 0; c < 4; c++) {
                double v = m[c * 4 + r];
                a[r * 8 + c] = v;
                largest = Math.max(largest, Math.abs(v));
            }
            if (!(largest > 0.0) || Double.isInfinite(largest)) {
                throw new SingularMatrixException("matrix is singular (row " + r + " is zero or not finite)");
            }
            a[r * 8 + 4 + r] = 1.0;
            rowScale[r] = 1.0 / largest;
        }
        for (int col = 0; col < 4; col++) {
            int pivot = col;
            double best = Math.abs(a[col * 8 + col]) * rowScale[col];
            for (int r = col + 1; r < 4; r++) {
                double v = Math.abs(a[r * 8 + col]) * rowScale[r];
                if (v > best) { best = v; pivot = r; }
            }
            if (!(best > SINGULAR_EPSILON)) {
                throw new SingularMatrixException("matrix is singular (relative pivot " + best + " in column " + col + ")");
            }
            if (pivot != col) {
                for (int k = 0; k < 8; k++) {
                    double t = a[pivot * 8 + k]; a[pivot * 8 + k] = a[col * 8 + k]; a[col * 8 + k] = t;
                }
                double s = rowScale[pivot]; rowScale[pivot] = rowScale[col]; rowScale[col] = s;
            }
            double inv = 1.0 / a[col * 8 + col];
            for (int k = 0; k < 8; k++) a[col * 8 + k] *= inv;
            for (int r = 0; r < 4; r++) {
                if (r == col) continue;
                double f = a[r * 8 + col];
                if (f != 0.0) for (int k = 0; k < 8; k++) a[r * 8 + k] -= f * a[col * 8 + k];
            }
        }
        for (int r = 0; r < 4; r++) for (int c = 0; c < 4; c++) m[c * 4 + r] = (float) a[r * 8 + 4 + c];
        return target;
    }
}
