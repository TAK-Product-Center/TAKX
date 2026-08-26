package gov.takx.test.extension;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for {@link org.junit.jupiter.api.Test} methods which require other test methods to run beforehand.
 *
 * @since 3.4.0
 * @see DependsOnOrdering
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface DependsOn
{
    /**
     * @return Method names the test depends on.
     */
    String[] value();
}