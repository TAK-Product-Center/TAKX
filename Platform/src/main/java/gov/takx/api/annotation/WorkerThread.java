package gov.takx.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Denotes that the annotated method should only be called on a worker (not the EDT) thread. If the annotated element
 * is a class, then all methods in the class should be called on a worker thread.
 * <p/>
 * If the method is invoked on the EDT, an {@link IllegalStateException} will be thrown.
 * <p/>
 * This annotation is used to mark join points for handling by AspectJ.
 *
 * @since 5.8
 */
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.CONSTRUCTOR})
@Retention(RetentionPolicy.RUNTIME)
public @interface WorkerThread
{
}
