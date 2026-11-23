package space.nows.util.data.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Polymorphic types. A value of a listed subtype is written with an extra {@code property} whose text is the subtype's
 * {@link SubType#name()}; decoding a value of this type picks the subtype from that property.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TypeInfo {
    String property() default "type";

    SubType[] subtypes();

    @interface SubType {
        String name();

        Class<?> type();
    }
}
