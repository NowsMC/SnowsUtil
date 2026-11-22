package space.nows.util.array;

import java.util.NoSuchElementException;

/** Ordering queries: sortedness, minimum and maximum. Generated from {@code ArrayOrder.tpl}; edit the template, not this file. */
public final class ArrayOrder {
    private ArrayOrder() {}

#for byte,int,long
    public static boolean isSorted($ARR$ a) {
        for (int i = 1; i < a.length; i++) if (a[i - 1] > a[i]) return false;
        return true;
    }
#endfor

#for byte,int,long
    /** @throws NoSuchElementException if the array is empty */
    public static $E$ min($ARR$ a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        $E$ m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] < m) m = a[i];
        return m;
    }
#endfor

#for byte,int,long
    /** @throws NoSuchElementException if the array is empty */
    public static $E$ max($ARR$ a) {
        if (a.length == 0) throw new NoSuchElementException("empty array");
        $E$ m = a[0];
        for (int i = 1; i < a.length; i++) if (a[i] > m) m = a[i];
        return m;
    }
#endfor
}
