package gov.takx.api.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Container annotation to allow repeated use of {@link ApplyAspectsToJar}.
 *
 * @since 2.18
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplyAspectsToJars
{
    /**
     * @return Array of {@link ApplyAspectsToJar} entries
     */
    ApplyAspectsToJar[] value();
}
