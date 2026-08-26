package gov.takx.api.ui;

import gov.tak.api.annotation.IncubatingApi;

/**
 * Keys that can be used to hide device panel (primary, properties) components. Setting a key to {@code false} via
 * {@link gov.takx.api.mapobject.IMapObject#setMetaBoolean(String, boolean)} will cause corresponding component to be
 * hidden.
 * <p>
 * Note: The device panels will be reworked in 5.8, so these keys are very likely to change or be removed in that version.
 *
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class DevicePanelMetadataKeys
{
    /**
     * Controls the visibility of the data monitors.
     */
    public static final String SHOW_DATA_MONITORS = "showDataMonitors";

    /**
     * Controls the visibility of the extra buttons (attachments, etc.).
     */
    public static final String SHOW_EXTRA_BUTTONS = "showExtraButtons";

    /**
     * Controls the visibility of the panel header.
     */
    public static final String SHOW_HEADER = "showHeader";

    /**
     * Controls the visibility of the "From You" range and bearing panel.
     */
    public static final String SHOW_RANGE_AND_BEARING = "showRangeAndBearing";

    /**
     * Controls the visibility of the icon sizing and milsym options.
     */
    public static final String SHOW_ICON_OPTIONS = "showIconOptions";

    private DevicePanelMetadataKeys()
    {
    }
}
