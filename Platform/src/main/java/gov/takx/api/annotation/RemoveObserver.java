package gov.takx.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark a method that will cause any observers registered due to the use of
 * {@link jakarta.enterprise.event.Observes} within a class annotated with {@link PseudoBean} to be unregistered.
 * This mechanism is used by dynamic objects that need to have lifecycle management that isn't driven by GC.
 * Without the use of this mechanism, the observer registration remains in effect until the PseudoBean is GCed.
 * <p/>
 * Typical usage is to place this annotation on some "lifecycle" method, called when the object instance is no longer
 * needed (likely some form of cleanup method), as shown below.
 * <pre>
 * {@code
 * @RemoveObserver
 * public void someCleanupMethod() {
 *     // Other cleanup actions
 * }
 * }
 * </pre>
 * If you need to terminate event observation at some mid-point in the lifecycle of the object, you can still create
 * a method annotated with this annotation, and simply call it when you want event observation to be terminated.
 *
 * @see PseudoBean
 * @since 2.37
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RemoveObserver
{
}
