package gov.takx.api.plugin.annotations;

import gov.takx.api.annotation.PluginDocumentation;
import gov.takx.api.plugin.IPluginDescriptor;
import jakarta.enterprise.util.AnnotationLiteral;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to provide additional descriptive information about a plugin.  This includes elements
 * like name, descriptions, vendor, copyright, etc.  These annotation values correspond to the properties available
 * via {@link IPluginDescriptor}.
 *
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface PluginDescriptor
{
    /**
     * Provides a human readable plugin name.
     * This is used in various places in the UI and other informational displays intended for the user.
     * <p>
     * This is a required property.
     *
     * @return plugin name
     */
    String name();

    /**
     * Provides a human-readable short description describing the capabilities and the uses of the plugin.
     * This is used in various places in the UI and other informational displays intended for the user.
     *
     * @return short description, defaults to empty
     */
    String description() default "";

    /**
     * Provides a human-readable long description describing the capabilities and uses of the plugin.
     * This is used in various places in the UI and other informational displays intended for the user.
     *
     * @return long description, defaults to empty
     */
    String longDescription() default "";

    /**
     * Provides an optional vendor name responsible for the plugin.
     * This is used in various places in the UI and other informational displays intended for the user.
     *
     * @return vendor name, defaults to empty
     */
    String vendor() default "";

    /**
     * Provides an optional copyright notice to display for the plugin.
     * This is used in various places in the UI and other informational displays intended for the user.
     *
     * @return copyright notice, defaults to empty
     */
    String copyright() default "";

    /**
     * Provides an optional set of documentation provided by the plugin
     *
     * @return array of plugin documentation specifications
     * @since 2.26
     */
    PluginDocumentation[] documentation() default {};

    /**
     * Annotation Literal for programmatic creation/lookup.
     * This is specifically to support "core" defined types, like Map Tools and File.
     *
     * @since 3.0
     */
    class Literal extends AnnotationLiteral<PluginDescriptor> implements PluginDescriptor
    {
        private final String name;
        private final String description;
        private final String longDescription;
        private final String vendor;
        private final String copyright;
        private final PluginDocumentation[] pluginDoc = new PluginDocumentation[0];

        /**
         * Construct from a given {@link IPluginDescriptor}.  Extract data so we don't hang on to a reference to the
         * plugin instance longer than needed.
         *
         * @param descriptor Descriptor from which to extract data, may be {@code null}
         */
        public Literal(IPluginDescriptor descriptor)
        {
            if (descriptor != null)
            {
                name = descriptor.getName();
                description = descriptor.getDescription();
                longDescription = descriptor.getLongDescription();
                vendor = descriptor.getVendor();
                copyright = descriptor.getCopyrightNotice();
            } else
            {
                name = description = longDescription = vendor = copyright = null;
            }
        }

        public Literal(String name)
        {
            this.name = name;
            description = longDescription = vendor = copyright = "";
        }

        @Override
        public String name()
        {
            return name;
        }

        @Override
        public String description()
        {
            return description;
        }

        @Override
        public String longDescription()
        {
            return longDescription;
        }

        @Override
        public String vendor()
        {
            return vendor;
        }

        @Override
        public String copyright()
        {
            return copyright;
        }

        @Override
        public PluginDocumentation[] documentation()
        {
            return pluginDoc.clone();
        }
    }
}
