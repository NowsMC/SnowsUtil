package space.nows.util.array;

import space.nows.util.check.Preconditions;

import java.util.Arrays;

/** Structural edits that return a new array: insert, remove, append and concatenate. Swap-remove works in place. Generated from {@code ArrayEdit.tpl}; edit the template, not this file. */
public final class ArrayEdit {
    private ArrayEdit() {}

#for byte,int,long,T
    public static $GEN$$ARR$ remove($ARR$ a, int index) {
        Preconditions.index(index, Preconditions.notNull(a, "a").length);
#if PRIM
        $E$[] out = new $E$[a.length - 1];
        System.arraycopy(a, 0, out, 0, index);
        System.arraycopy(a, index + 1, out, index, a.length - index - 1);
#else
        T[] out = Arrays.copyOf(a, a.length - 1);
        System.arraycopy(a, index + 1, out, index, a.length - index - 1);
#end
        return out;
    }
#endfor

#for byte,int,long,T
#if PRIM
    /** Overwrites {@code index} with the last live element and returns the new logical size. Order is not kept. */
#else
    /** Overwrites {@code index} with the last live element, nulls the vacated slot and returns the new logical size. */
#end
    public static $GEN$int swapRemove($ARR$ a, int index, int size) {
        Preconditions.position(size, Preconditions.notNull(a, "a").length);
        Preconditions.index(index, size);
        a[index] = a[size - 1];
#if OBJ
        a[size - 1] = null;
#end
        return size - 1;
    }
#endfor

#for byte,int,long,T
    public static $GEN$$ARR$ insert($ARR$ a, int index, $E$ value) {
        Preconditions.position(index, Preconditions.notNull(a, "a").length);
#if PRIM
        $E$[] out = new $E$[ArrayLimits.checkedLength(a.length + 1L)];
        System.arraycopy(a, 0, out, 0, index);
        out[index] = value;
        System.arraycopy(a, index, out, index + 1, a.length - index);
#else
        T[] out = Arrays.copyOf(a, ArrayLimits.checkedLength(a.length + 1L));
        System.arraycopy(a, index, out, index + 1, a.length - index);
        out[index] = value;
#end
        return out;
    }
#endfor

#for byte,int,long,T
    public static $GEN$$ARR$ append($ARR$ a, $E$ value) {
        $ARR$ out = Arrays.copyOf(Preconditions.notNull(a, "a"), ArrayLimits.checkedLength(a.length + 1L));
        out[a.length] = value;
        return out;
    }
#endfor

#for byte,int,long,T
    public static $GEN$$ARR$ concat($ARR$ a, $ARR$ b) {
        Preconditions.notNull(a, "a");
        Preconditions.notNull(b, "b");
        $ARR$ out = Arrays.copyOf(a, ArrayLimits.checkedLength((long) a.length + b.length));
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }
#endfor
}
