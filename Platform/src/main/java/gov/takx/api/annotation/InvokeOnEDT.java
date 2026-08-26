package gov.takx.api.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to mark any method whose execution should be placed onto the EDT if it was not called on the EDT.
 * The method will be wrapped in an anonymous {@link Runnable} and then executed using {@link javax.swing.SwingUtilities#invokeLater(Runnable)}.
 * <p/>
 * Implicit in this is the requirement that the method where this annotation is applied <b>MUST HAVE A VOID RETURN TYPE.</b>
 * Specifically, due to the fact that {@link Runnable}s do not have a return type and methods annotated with this will be
 * wrapped in an anonymous runnable, any method annotated with {@link InvokeOnEDT} must have a void return type.
 * <p/>
 * This annotation is used to mark join points for handling by AspectJ.
 *
 * @since 1.25
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InvokeOnEDT
{
}
