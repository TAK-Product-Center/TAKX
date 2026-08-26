package gov.takx.core.database.provider;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Specifies the type (implementation name) of a database provider.
 *
 * @since 3.0
 */
@Target({TYPE, FIELD, PARAMETER})
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface DatabaseProviderType
{
    String value();

    /**
     * Annotation literal for use in bean selection.
     */
    class Literal extends AnnotationLiteral<DatabaseProviderType> implements DatabaseProviderType
    {
        private final String value;

        public Literal(String value)
        {
            this.value = value;
        }

        @Override
        public String value()
        {
            return value;
        }
    }
}
