package gov.takx.core.view.gui;

import gov.takx.api.toolbar.ButtonType;

/**
 * Enumeration of all drawing tool types provided by the core.
 *
 * @since 1.26
 */
public enum DrawingTools
{
    SELECTOR(ButtonType.SELECTOR.getDisplayText()),
    ANNOTATION(ButtonType.TEXT_BOX.getDisplayText()), // TODO KMB: It's confusing that this and flat annotation are identified with the tool names in the explorer
    SHAPE("Shape"),
    PING(ButtonType.GEO_PING.getDisplayText()),
    RANGE_FAN("Range Fan"),
    FLAT_ANNOTATION(ButtonType.TEXT_LABEL.getDisplayText()),
    PLUGIN_TOOL("Plugin Tool"),
    ;

    private final String displayName;

    DrawingTools(String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    @Override
    public String toString()
    {
        return displayName;
    }
}
