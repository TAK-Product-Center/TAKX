package gov.takx.api.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation to allow repeated use of {@link LibraryDependency}.
 *
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface LibraryDependencies
{
    /**
     * @return Array of {@link LibraryDependency} entries
     */
    LibraryDependency[] value();
}
