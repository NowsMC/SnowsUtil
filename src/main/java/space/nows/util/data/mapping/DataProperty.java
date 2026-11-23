package space.nows.util.data.mapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Overrides the JSON property name of a field, record component or enum constant. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.RECORD_COMPONENT})
public @interface DataProperty {
    String value() default "";
}
