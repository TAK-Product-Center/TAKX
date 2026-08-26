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
 * Qualifying annotation used along with @Inject to have the "platform under test" injected into a boolean variable.
 *
 * @since 2.36
 */
@Target({PARAMETER, FIELD, METHOD, CONSTRUCTOR, ANNOTATION_TYPE})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface PlatformUnderTest
{
    /**
     * @since 3.4
     */
    class Literal extends AnnotationLiteral<PlatformUnderTest> implements PlatformUnderTest
    {
        public static final Literal INSTANCE = new Literal();

        private Literal()
        {
        }
    }
}
