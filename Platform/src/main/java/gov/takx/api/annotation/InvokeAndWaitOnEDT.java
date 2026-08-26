package gov.takx.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark any method whose execution should be placed onto the EDT if it was not called on the EDT.
 * The method will be wrapped in an anonymous {@link Runnable} and then executed using {@link javax.swing.SwingUtilities#invokeAndWait(Runnable)}.
 * Execution will wait for the EDT invocation to complete before continuing.  The return value, if any, from the target
 * method will be captured and returned as if the method had been called directly.
 * <p/>
 * Unlike the restriction on {@link InvokeOnEDT} to only work on "void" returning methods, this annotation may
 * be used on methods that have any return type.  This only works because we wait for the method execution to
 * complete before returning, thus allowing for the capture of the target methods return value.
 * <p/>
 * This annotation is used to mark join points for handling by AspectJ.
 *
 * @since 1.25
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InvokeAndWaitOnEDT
{
}
