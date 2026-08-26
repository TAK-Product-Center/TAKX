package gov.takx.api.plugin.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Qualifying annotation specifying that a jar within a plugin deployment's {@code /lib}
 * directory should have all the same Aspect handling as the core code receives.
 * <p>
 * Developers should use this with caution, as unexpected behavior can occur when applying our aspects to arbitrary
 * code bases.
 * <p>
 * The most common case for needing this annotation is where a sub-jar submits Runnables to the fork/join
 * pool and need to ensure that the class loader for the plugin is installed as the TCCL (in the case that the
 * runnable is performing dynamic class loading).
 * <p>
 * There are two basic modes for matching the name of the jar to be processed.  By default, the string provided
 * as the value of this annotation will be used to perform a prefix match, which makes it easy to avoid having
 * to include a version number in the jar name.  So, for example, specifying {@code "some-lib-"} as the value could
 * match a contained jar by the name {@code "some-lib-1.2.3.jar"}.
 * <p>
 * If there is no simple prefix that will match exactly the single jar in question, then you can specify that the
 * value should be interpreted as a regular expression to match against all the sub-jars.  See {@link #regex()} below.
 * If using regex matching, the pattern provided must match the entire filename, including the trailing
 * {@code '.jar'} extension.
 * <p>
 * It is an error for either method to match more than a single jar or to match no jars.
 * <p>
 * This annotation may be repeated if multiple jars need to be declared.
 *
 * @since 2.18
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(value = ApplyAspectsToJars.class)
public @interface ApplyAspectsToJar
{
    /**
     * @return The string to match against sub-jar names
     */
    String value();

    /**
     * @return {@code true} to perform a regex match instead of a prefix match
     */
    boolean regex() default false;
}
