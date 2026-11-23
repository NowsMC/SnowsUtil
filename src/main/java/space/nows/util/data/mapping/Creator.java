package space.nows.util.data.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks the constructor used to create instances when decoding. Each parameter must carry {@link Property} with the
 * JSON name it is bound to, because Java does not keep parameter names at run time. Use this for immutable classes
 * that have no no-argument constructor.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.CONSTRUCTOR)
public @interface Creator {}
