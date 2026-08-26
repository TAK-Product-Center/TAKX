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
    // TODO DCW TAKX-4428 create a more precise way to control these, down to the field level for some components
    //  May even want to expose the fields directly via an interface, but need to ensure we properly modify the layouts
    //  when field visibility changes - similar to FormButtonBar

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
     * Controls the visibility of panel header fields other than icon, name, description, and location
     * @since 5.8
     */
    public static final String SHOW_EXTENDED_HEADER = "showExtendedHeader";

    /**
     * Controls the visibility of the "From You" range and bearing panel.
     */
    public static final String SHOW_RANGE_AND_BEARING = "showRangeAndBearing";

    /**
     * Controls the visibility of the icon sizing and milsym options.
     */
    public static final String SHOW_ICON_OPTIONS = "showIconOptions";

    /**
     * Controls the visibility of the device details panel.
     * @since 5.8
     */
    public static final String SHOW_DEVICE_DETAILS = "showDeviceDetails";

    /**
     * Controls the visibility of the device details panel.
     * @since 5.8
     */
    public static final String SHOW_LAST_REPORT_DETAILS = "showLastReportDetails";

    /**
     * Controls the visibility of the movement details panel.
     * @since 5.8
     */
    public static final String SHOW_MOVEMENT_DETAILS = "showMovementDetails";

    /**
     * Controls the visibility of the sensor details panel.
     * @since 5.8
     */
    public static final String SHOW_SENSOR_DETAILS = "showSensorDetails";

    private DevicePanelMetadataKeys()
    {
    }
}
