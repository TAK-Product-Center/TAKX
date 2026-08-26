package gov.takx.api.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifying annotation specifying that a given plugin has a dependency on a library (module) provided as part
 * of the TAKX distribution.
 * <p>
 * By default, all plugins start with a "clean" class path. No libraries, other than the TAKX plugin libraries (Platform,
 * log4j, etc.) are added. This annotation allows a plugin to call out the libraries it needs (such as apache commons,
 * lucene, etc.)
 * <p>
 * If you require a very specific version of a given library, you should include that library within your plugin
 * deployment instead of relying on the one provided by TAKX, which may be updated from time to time.
 * <p>
 * See the current TAKX documentation to find a complete list of the libraries provided with the TAKX installation.
 * Specify the library name (such as {@code "net.sf.opencsv"} as the value of this annotation.
 * <p>
 * This annotation may be repeated if multiple plugin dependencies need to be declared.
 * <p>
 * For example, a class needing access to OpenCSV and Apache Commons Lang3 would look like this:
 * <pre>
 * {@code
 *     @LibraryDependency("net.sf.opencsv")
 *     @LibraryDependency("org.apache.commons.commons-lang3")
 *     public class FooPlugin ...}
 * </pre>
 *
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(value = LibraryDependencies.class)
public @interface LibraryDependency
{
    /**
     * @return The array of dependency names
     */
    String value();

    /**
     * @return {@code true} if this is an optional dependency
     */
    boolean optional() default false;

    /**
     * @return {@code true} if the dependencies resources should be exported from the depending module
     */
    boolean export() default false;
}
