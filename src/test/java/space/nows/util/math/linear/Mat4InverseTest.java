package space.nows.util.math.linear;

import org.junit.jupiter.api.Test;
import space.nows.util.error.SingularMatrixException;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class Mat4InverseTest {
    @Test void invertsMatricesWithVeryDifferentAxisScales() {
        Mat4 m = new Mat4().setScale(1e-14f, 1f, 1f);
        Mat4Inverse.invert(m);
        assertEquals(1e14f, m.get(0, 0), 1e8f);
        assertEquals(1f, m.get(1, 1));
        assertEquals(1f, m.get(2, 2));
        assertEquals(1f, m.get(3, 3));
        Mat4 tiny = new Mat4().setScale(1e-14f, 1e-14f, 1e-14f);
        tiny.set(3, 3, 1e-14f);
        Mat4Inverse.invert(tiny);
        assertEquals(1e14f, tiny.get(2, 2), 1e8f);
        assertEquals(1e14f, tiny.get(3, 3), 1e8f);
        Mat4 mixed = new Mat4().setScale(1e12f, 1e-12f, 1f);
        Mat4Inverse.invert(mixed);
        assertEquals(1e-12f, mixed.get(0, 0), 1e-18f);
        assertEquals(1e12f, mixed.get(1, 1), 1e6f);
    }

    @Test void productWithTheInverseIsTheIdentity() {
        Random r = new Random(41);
        for (int round = 0; round < 500; round++) {
            Mat4 m = new Mat4();
            for (int row = 0; row < 4; row++) for (int col = 0; col < 4; col++) m.set(row, col, r.nextFloat() * 2f - 1f + (row == col ? 3f : 0f));
            Mat4 inverse = m.copy();
            Mat4Inverse.invert(inverse);
            Mat4 product = new Mat4();
            Mat4.multiply(m, inverse, product);
            for (int row = 0; row < 4; row++) {
                for (int col = 0; col < 4; col++) assertEquals(row == col ? 1f : 0f, product.get(row, col), 1e-4f);
            }
        }
    }

    @Test void singularMatricesThrowAndStayUnchanged() {
        Mat4 duplicateRows = new Mat4();
        for (int col = 0; col < 4; col++) {
            duplicateRows.set(0, col, col + 1);
            duplicateRows.set(1, col, col + 1);
            duplicateRows.set(2, col, col * 3 + 1);
            duplicateRows.set(3, col, col == 0 ? 5 : 2);
        }
        float[] before = duplicateRows.m.clone();
        assertThrows(SingularMatrixException.class, () -> Mat4Inverse.invert(duplicateRows));
        assertArrayEquals(before, duplicateRows.m);
        Mat4 zeroRow = new Mat4();
        zeroRow.set(2, 2, 0f);
        assertThrows(SingularMatrixException.class, () -> Mat4Inverse.invert(zeroRow));
        Mat4 zero = new Mat4();
        Arrays.fill(zero.m, 0f);
        assertThrows(SingularMatrixException.class, () -> Mat4Inverse.invert(zero));
        Mat4 nan = new Mat4();
        nan.set(1, 1, Float.NaN);
        assertThrows(SingularMatrixException.class, () -> Mat4Inverse.invert(nan));
        Mat4 zeroScale = new Mat4().setScale(1f, 0f, 1f);
        assertThrows(SingularMatrixException.class, () -> Mat4Inverse.invert(zeroScale));
    }
}
