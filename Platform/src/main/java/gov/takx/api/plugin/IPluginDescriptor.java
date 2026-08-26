package gov.takx.api.plugin;

import gov.takx.api.annotation.NoPluginImplementations;

/**
 * Contract to provide detailed human-readable descriptive data about the plugin. The
 * data provided here is used by the TAKX application to present the user with more details about the plugin
 * beyond simply its type and version.
 *
 * @since 1.3
 */
@NoPluginImplementations
public interface IPluginDescriptor
{
    /**
     * Identifies a human-readable plugin name. For Device, Connection, and Import/Export plugin, this value will be
     * used as the label in the toolbar under the icon.
     *
     * @return plugin name
     */
    String getName();

    /**
     * Presents a human-readable short description describing the capabilities and the uses of the plugin. For Device,
     * Connection, Form, App, and Import/Export plugin, this value will be used as the tooltip in the toolbar.
     *
     * @return short description, or {@code null} if no short description is provided
     */
    String getDescription();

    /**
     * Presents a human-readable long description describing the capabilities and uses of the plugin.
     *
     * @return long description, or {@code null} if no long description is provided
     */
    String getLongDescription();

    /**
     * Provides an optional copyright notice to display for the plugin.
     *
     * @return copyright notice, or {@code null} if no notice is provided
     */
    String getCopyrightNotice();

    /**
     * Provides an optional vendor name responsible for the plugin.
     *
     * @return vendor name, or {@code null} if no vendor name is provided
     */
    String getVendor();

    /**
     * Returns an array of {@link IPluginDocumentation} objects. By default, it returns an empty array.
     *
     * @return array of {@link IPluginDocumentation} objects
     * @since 2.26
     */
    default IPluginDocumentation[] getPluginDocumentation()
    {
        return new IPluginDocumentation[0];
    }
}
