package gov.takx.api.annotation;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.ANNOTATION_TYPE;
import static java.lang.annotation.ElementType.CONSTRUCTOR;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Qualifier on an {@link org.greenrobot.eventbus.EventBus} injection point to select the secondary UI event
 * service instead of the basic Thread-safe event service. Event dispatches using this qualified
 * {@link org.greenrobot.eventbus.EventBus} will be handled on the EDT.
 *
 * @since 2.0.0
 */
@Target({PARAMETER, FIELD, METHOD, CONSTRUCTOR, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface UiEventBus
{
    class Literal extends AnnotationLiteral<UiEventBus> implements UiEventBus
    {
        public static final Literal INSTANCE = new Literal();

        private Literal()
        {
        }
    }
}
