package gov.takx.api.plugin.annotations;

import gov.takx.api.plugin.support.SupportedPlatformVersion;
import jakarta.enterprise.util.AnnotationLiteral;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Core plugin specification shared among all plugin annotations.  This allows for common handling of these
 * attributes.
 * <p>
 * The {@link #family() family} and {@link #type() type} are specified as values on this annotation, and
 * together are used to form unique keys and to group plugins within families.
 *
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginSpecification
{
    /**
     * @return The family identifier for this plugin (required)
     */
    String family();

    /**
     * @return The type identifier for this plugin (required)
     */
    String type();

    /**
     * Get the version number of this plugin in the form of MAJOR.MINOR.PATCH, each of which are expected to be
     * zero or any positive integer.
     * <p>
     * This is a required property.
     *
     * @return Version string for the plugin (for example, "1.0.0").
     */
    String version();

    /**
     * Get the minimum version of TAKX that is required to run this plugin. As new features are added to the
     * plugin API, this settings allows for better runtime error reporting when an attempt to use a given plugin
     * is made on a platform older than that which the plugins was developed against.
     *
     * @return The minimum version of TAKX required to run this plugin
     */
    SupportedPlatformVersion minimumSupportedVersion() default SupportedPlatformVersion.V3_0;

    /**
     * Specify if the plugin is export controlled according to the rules of the ITAR
     * or the EAR. It will be the responsibility of the plugin to properly notify the user of this restriction and
     * any caveats that might be associated with it (such as individual countries). This flag will be used by TAKX
     * and/or other plugins to possibly display an ITAR/EAR restriction to the user.
     *
     * @return {@code true} if this plugin is ITAR/EAR controlled.  Defaults to false.
     */
    boolean exportControlled() default false;

    /**
     * Specify the path to the icon to display in UI to represent this plugin.  This is used in various UI panels to
     * represent this plugin.  This value is required.
     *
     * @return The icon path in the plugin's jar.
     */
    String iconPath();

    /**
     * Annotation Literal for programmatic creation/lookup.
     * This is specifically to support "core" defined types, like Map Tools and File.
     *
     * @since 3.0
     */
    @SuppressWarnings("ClassExplicitlyAnnotation")
    class Literal extends AnnotationLiteral<PluginSpecification> implements PluginSpecification
    {
        private final String family;
        private final String type;
        private final String version;

        public Literal(String family, String type, String version)
        {
            this.family = family;
            this.type = type;
            this.version = version;
        }

        @Override
        public String family()
        {
            return family;
        }

        @Override
        public String type()
        {
            return type;
        }

        @Override
        public String version()
        {
            return version;
        }

        @Override
        public SupportedPlatformVersion minimumSupportedVersion()
        {
            return SupportedPlatformVersion.V3_0;
        }

        @Override
        public boolean exportControlled()
        {
            return false;
        }

        @Override
        public String iconPath()
        {
            return null;
        }
    }
}
