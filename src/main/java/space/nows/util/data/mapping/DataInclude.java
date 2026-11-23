package space.nows.util.data.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Controls whether {@code null} properties of a type are written. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DataInclude {
    Include value();

    enum Include { ALWAYS, NON_NULL }
}
