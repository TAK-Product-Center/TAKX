package gov.takx.api.plugin;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.bootstrap.Verification;
import gov.takx.commons.utils.PlatformVersion;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.util.Optional;

/**
 * Contract for a "plugin definition."  A definition is created for every plugin discovered within the plugin
 * deployments.
 *
 * @since 2.0.0
 */
@NoPluginImplementations
public interface IPluginDefinition<T extends IRaptorPlugin> extends IPluginDescriptor
{
    /**
     * @return The namespace that this plugin is registered under
     */
    String getFamily();

    /**
     * @return The unique type/identifier for this plugin
     */
    String getType();

    /**
     * Plugins are organized into groups for some aspects of TAKX. For example, all device plugins have a group
     * of "Devices". Synthesized plugins for shapes (map tools) will have a group of "Map Tools" and synthetic plugins
     * derived from icon resources will have a group derived from the directory in which they live, like "Flags" or
     * "Forces."
     *
     * @return The group to which this plugin belongs
     */
    String getGroup();

    /**
     * @return The unique key for this plugin (comprised of family and type)
     */
    String getUniqueKey();

    /**
     * @return the verification status of the plugin
     * @since 5.5
     */
    @Nullable
    Verification getVerification();

    /**
     * @return The version string for this plugin
     */
    String getVersion();

    /**
     * @return the category of this plugin
     */
    PluginCategory getPluginCategory();

    /**
     * @return The fully qualified name of the plugin's class
     */
    default String getPluginClassName()
    {
        return getPluginClass().getName();
    }

    /**
     * @return The class instance of this plugin
     */
    Class<? extends IRaptorPlugin> getPluginClass();

    /**
     * @return The class loader to use to load resources defined by this plugin
     */
    default ClassLoader getResourceClassLoader()
    {
        return getPluginClass().getClassLoader();
    }

    /**
     * Get the minimum version of TAKX that is required to run this plugin. As new features are added to the
     * plugin API, this setting allows for better runtime error reporting when an attempt to use a given plugin
     * is made on a platform older than that which the plugin was developed against.
     *
     * @return The minimum version of takx required to run this plugin, {@code null} if unknown
     */
    @Nullable
    PlatformVersion getMinimumSupportedVersion();

    /**
     * Return if the plugin is marked as export controlled.
     *
     * @return {@code true} if this plugin is ITAR/EAR controlled.
     */
    boolean isExportControlled();

    /**
     * @return The resource path of the icon as defined by the plugin, may be empty
     */
    Optional<String> getIconPath();

    /**
     * @return The URI of the icon as defined by the plugin, may be empty
     */
    Optional<URI> getIconURI();

    /**
     * @return The image to use as an icon for this plugin, defaults to the unknown icon image if not specified or not found
     */
    BufferedImage getIcon();

    /**
     * @return File (jar) from which the plugin was loaded
     */
    File getPluginSource();

    /**
     * Obtain the actual plugin instance.  Note that calling this will create a new instance if one has not already
     * been created.  From the perspective of other plugins, all plugins will have been instantiated by the time
     * any method that returns this data can be called.
     *
     * @return Plugin instance
     */
    T getInstance();
}
