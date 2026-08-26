package gov.takx.api.plugin;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.annotation.PluginDocumentation;

/**
 * Contract for data extracted from a {@link PluginDocumentation} Annotation.
 *
 * @since 2.26 (reworked completely in 3.0)
 */
@NoPluginImplementations
public interface IPluginDocumentation
{
    /**
     * @see PluginDocumentation#displayName()
     */
    String getDisplayName();

    /**
     * @see PluginDocumentation#tooltip()
     */
    String getTooltip();

    /**
     * @see PluginDocumentation#filePath()
     */
    String getFilePath();

    /**
     * @see PluginDocumentation#external()
     */
    boolean isExternal();

    /**
     * @see PluginDocumentation#htmlDoc()
     *
     * @since 5.1
     */
    boolean isHtmlDoc();

    /**
     * @see PluginDocumentation#iconPath()
     */
    String getIconPath();
}
