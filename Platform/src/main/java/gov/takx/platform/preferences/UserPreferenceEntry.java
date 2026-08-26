package gov.takx.platform.preferences;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.maps.coords.NorthReference;
import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.PreferenceDescriptor;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.globe.GlobeNavigationStyle;
import gov.takx.api.globe.render.RendererConstants;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.IUserPreferenceManager;
import gov.takx.commons.media.AudioAlert;
import gov.takx.platform.cdi.InjectableContainerSupport;
import gov.takx.platform.toolbar.ToolbarButtonLabelLocation;
import gov.takx.platform.utils.ILengthMeasurer;
import gov.takx.platform.utils.StandardTimeFormatting;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.SpeedUnits;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Definitions for all of our User preference keys and default values.
 *
 * @implNote TAKX no longer has a concept of separate users. All new preferences should be placed in {@link PreferenceEntry}
 * @since 2.35
 */
public enum UserPreferenceEntry implements IPreferenceEntry
{
    AUDIO_ALERT("AudioAlerts", AudioAlert.ALERTS_ON.getName()),
    @PreferenceDescriptor(title = "Auto-expand plugin panels", description = "Automatically expand panel(s) from app plugins when they start")
    AUTO_EXPAND("AutoExpand", "true"),
    @PreferenceDescriptor(title = "Auto-pin plugin panels", description = "Automatically pin panel(s) from app plugins when they start")
    AUTO_PIN("AutoPin", "false"),
    COMM_PORTS_SERIAL_NODE("CommPorts/Serial", null),
    @PreferenceDescriptor(title = "Coordinate System", description = "")
    COORDINATE_SYSTEM("CoordinateSystem", CoordinateSystem.DECIMAL_DEGREES.getName()),
    @PreferenceDescriptor(title = "Confirm Quit", description = "Query the user before allowing an application exit")
    CONFIRM_QUIT("ConfirmQuit", "true"),
    DATE_FORMAT("DateFormat", StandardTimeFormatting.DEFAULT_FORMAT),
    /**
     * @since 5.0
     * @deprecated Use {@link #DISPLAY_BEARING_UNIT} instead ("UNIT", <em>not</em> "UNITS", to differentiate from deprecated entry)
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    @PreferenceDescriptor(title = "Bearing Units", description = "")
    DISPLAY_BEARING_UNITS("BearingUnits", Angle.DEGREE.name()),

    /** @since 5.5 */
    @PreferenceDescriptor(title = "Bearing Units", description = "The unit to use for bearing")
    DISPLAY_BEARING_UNIT("units/bearingUnits", BearingUnits.DEGREE.name()),
    @PreferenceDescriptor(title = "Display Local Time", description = "Display time values in the local time zone vs. UTC")
    DISPLAY_LOCAL_TIME("displayLocalTime", "false"),

    /**
     * Replaced by individual units of measure for altitude, range and speed.
     *
     * @deprecated Use {@link #DISPLAY_ALTITUDE_UNITS}, {@link #DISPLAY_RANGE_UNITS} and {@link #DISPLAY_SPEED_UNITS} instead
     */
    @Deprecated(since = "5.5", forRemoval = true)
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    @PreferenceDescriptor(title = "Unit of Measure", description = "The units of measure to use for distance/length/area values")
    DISPLAY_UNIT_OF_MEASURE("lastDisplayUoM", ILengthMeasurer.DEFAULT_UNIT_OF_MEASURE),
    /** @since 5.5 */
    @PreferenceDescriptor(title = "Altitude Units", description = "The units of measure to use for altitude")
    DISPLAY_ALTITUDE_UNITS("units/altitudeUnits", AltitudeUnits.METERS.name()),
    /** @since 5.5 */
    @PreferenceDescriptor(title = "Range Units", description = "The units of measure to use for range")
    DISPLAY_RANGE_UNITS("units/rangeUnits", RangeUnits.METERS.name()),
    /** @since 5.5 */
    @PreferenceDescriptor(title = "Speed Units", description = "The units of measure to use for speed")
    DISPLAY_SPEED_UNITS("units/speedUnits", SpeedUnits.MPS.name()),

    /** @since 3.4 */
    @PreferenceDescriptor(title = "North Reference", description = "")
    DISPLAY_NORTH_REFERENCE("NorthReference", NorthReference.TRUE.name()),

    @PreferenceDescriptor(title = "Enable Critical Tab Notifications", description = "Flash a panels tab color when it is marked as critical")
    DOCK_BUTTON_NOTIFICATIONS("ButtonFlashing", "true"),
    EVENT_LOG_NODE("EventLog", null),
    FILE_STORES_DEFAULT_LOCATIONS("fileStores/locations", ""),
    FILE_STORES_ELEVATION_LOCATIONS("fileStores/ElevationLocations", ""),
    FILE_STORES_IMAGERY_LOCATIONS("fileStores/ImageryLocations", ""),

    @PreferenceDescriptor(title = "Global Marker Scale", description = "Multiplier (as a percentage) to scale marker icon size on the globe")
    GLOBAL_ICON_SCALE("GlobalIconScale", "1.0"),
    GLOBE_IMPORTERS_NODE("GlobeImporters", null),

    /** @since 5.6 */
    @PreferenceDescriptor(title = "Globe Navigation Style", description = "Change which keys and buttons control globe tilt, rotate, and zoom")
    GLOBE_NAVIGATION_STYLE("GlobeNavigationStyle", GlobeNavigationStyle.TAKX.name()),

    LAST_OPEN_PROJECT("lastOpenProject", ""),

    /**
     * @since 3.4
     */
    MOBAC_SERVERS("mobac/servers", ""),
    PKI_USER_KEY("PKIKeys/UserKey", ""),

    @PreferenceDescriptor(title = "Restore Last View", description = "Restore the globe view (location, heading, pitch, eye altitude) upon restart")
    RESTORE_LAST_VIEW("RestoreLastView", "true"),

    @PreferenceDescriptor(title = "Show Shape Labels", description = "Show shape names as labels on the globe")
    SHOW_SHAPE_LABELS("showShapeLabels", "false"),
    @PreferenceDescriptor(title = "Show Range & bearing Labels", description = "Show range bearing labels on the globe")
    SHOW_RANGE_BEARING_LABELS("showRangeBearingLabels", "true"),
    SKIN("skin", ""),
    STATUS_NODE("Status", null),

    /**
     * @since 3.4
     */
    TOOLBAR_BUTTON_LABEL_LOCATION("ToolbarButtonLabelLocation", ToolbarButtonLabelLocation.BOTTOM.name()),
    TRACK_DETAIL_LEVEL("TrackDetailLevel", RendererConstants.LOW_TRACK_DETAIL_LEVEL),
    TIME_ZONE("TimeZone", "UTC"),
    VIEW_BOOKMARKS_NODE("ViewBookmarks", null),
    WCS_IMAGERY_SERVERS("wcs/ImageryWMSServers", ""),
    WCS_ELEVATION_SERVERS("wcs/ElevationWMSServers", ""),
    WMS_IMAGERY_SERVERS("wms/ImageryWMSServers", ""),
    WMS_ELEVATION_SERVERS("wms/ElevationWMSServers", ""),
    WW_PROXY_ENABLED("WorldWindProxy/ProxyEnabled", "false"),
    WW_PROXY_HOST("WorldWindProxy/ProxyHost", ""),
    WW_PROXY_PORT("WorldWindProxy/ProxyPort", ""),
    WW_PROXY_TYPE("WorldWindProxy/ProxyType", ""),

    NONE("NONE", "NONE"); // Never use this

    private final String key;
    private final String defaultValue;

    private IUserPreferenceManager userPreferenceManager;
    private List<IPreferenceEntryChangeListener> changeListeners;

    @Override
    public String getDefaultValue()
    {
        return defaultValue;
    }

    @Override
    public String getKey()
    {
        return key;
    }

    /**
     * Constructor for immutable preferences (can't change at runtime).
     *
     * @param key          Preference key
     * @param defaultValue Default value if not specified in preferences file
     */
    UserPreferenceEntry(String key, String defaultValue)
    {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @Override
    public IUserPreferenceManager getPreferenceManager()
    {
        return userPreferenceManager != null
                ? userPreferenceManager
                : (userPreferenceManager = new InjectableContainerSupport().getBean(IUserPreferenceManager.class));
    }

    @Override
    public List<IPreferenceEntryChangeListener> getChangeListeners()
    {
        return changeListeners != null
                ? changeListeners
                : (changeListeners = new CopyOnWriteArrayList<>());
    }
}
