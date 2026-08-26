package gov.takx.api.plugin.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.PACKAGE;
import static java.lang.annotation.ElementType.TYPE;

/**
 * Annotation indicating that the implementation for a given type lives within another jar (and, thus, another
 * CDI container).  This is typically required when the plugin architect wants to hide the implementation classes
 * from the API consumer, meaning that there will be no {@link PluginDependency} from the consumer to the
 * implementation jar, just on the API jar.
 * <p>
 * It is common to apply this annotation to the {@code package-info.java} class in order to have it apply to an
 * entire collection of APIs.  This is not required, and this annotation can actually be used on any class.
 * It may not, however, be repeated on a single class.
 *
 * @since 2.25
 */
@Target({TYPE, PACKAGE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ImplementedBy
{
    /**
     * @return The name of the plugin deployment, such as {@code "plugin.foobar-1.0.0"}.  This can be
     * shortened to any unique matching prefix, like {@code "plugin.foobar"}, in order to avoid hard-coded
     * version numbers in the dependency.
     */
    String value();
}
