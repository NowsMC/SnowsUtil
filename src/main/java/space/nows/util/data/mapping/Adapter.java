package space.nows.util.data.mapping;

import space.nows.util.data.Value;

/** Converts one type to and from a {@link Value} when the reflective mapping is not wanted for it. */
public interface Adapter<T> {
    Value write(T value);

    T read(Value value);
}
