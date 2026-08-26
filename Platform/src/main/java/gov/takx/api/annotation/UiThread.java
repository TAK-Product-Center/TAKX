package gov.takx.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated method or constructor should only be called on the EDT. If the annotated element
 * is a class, then all methods in the class should be called on the EDT.
 * <p/>
 * If the method is NOT invoked on the EDT, an {@link IllegalStateException} will be thrown.
 * <p/>
 * This annotation is used to mark join points for handling by AspectJ.
 *
 * @apiNote This is named {@code UiThread} for conformity with the equivalent Android annotation.
 * @since 5.8
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface UiThread
{
}
