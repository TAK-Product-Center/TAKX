package gov.takx.api.mapobject;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.cot.CotTypes;
import gov.tak.api.maps.MapItem;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.drifter.DrifterConfiguration;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IForceUpdatable;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.tracks.ISmartTrack;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.commons.constants.State;
import gov.takx.commons.lang.IPropertyChangeListenerSupport;
import gov.takx.dataservices.shapes.FieldOfView;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.ImageIcon;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.net.URI;
import java.util.Collection;
import java.util.Set;

/**
 */
@NoPluginImplementations
@SuppressWarnings("unused")
public interface IMapObject extends MapItem, IForceUpdatable, IPropertyChangeListenerSupport
{
    /**
     * Values control when a Map Object's tooltip is shown on the map.
     * @since 5.6
     */
    enum TooltipVisibility
    {
        /** Tooltip is always on (shown), regardless of mouse hover. */
        ON,
        /** Tooltip is always off (not shown), regardless of mouse hover. */
        OFF,
        /** Tooltip visibility is controlled by the {@link PreferenceEntry#MAP_OBJECT_TOOLTIPS} setting. */
        DEFAULT
    }

    String ALLOW_DEVICE_CONTAINMENT = "ALLOW_DEVICE_CONTAINMENT";
    String ALLOW_DRAG = "Allow Dragging of Map Object";
    String ATTRIBUTE_SET = "AttributeSet";
    String CONNECTING_LINE = "Connecting Line";
    String CONNECTING_LINE_VISIBILITY = "Connecting Line Visibility";
    String COT_TYPE = "CoT Type";
    String DEFAULT_PREFERENCES_TAB = "DEFAULT_PREFERENCES_TAB";
    String DEFAULT_TRACK_COLOR = "Default Track Color";
    String DESCRIPTION = "Description";
    String ELEVATION_MODE = "Elevation Mode";
    /**
     * Fired when the field of view is added or removed.
     * @since 5.8
     */
    String FIELD_OF_VIEW = "Field of View";
    /**
     * Fired when the field of view properties are updated.
     * @since 5.8
     */
    String FIELD_OF_VIEW_PROPERTIES = "Field of View Updated";
    /**
     * Fired when the location changes, payload is the {@link GeoPointMetaData} from the Map Object.
     * @since 5.5
     */
    String GEOPOINT = "GeoPoint";
    String HAS_HEADING = "Has Heading";
    String HEADING = "HeadingDegreesTrue";
    String HIGHEST_STATE = "Highest State";
    String ICON = "Icon";
    String ICON_SCALE = "Icon Scale";
    /** @since 3.5 */
    String REMARKS = "Remarks";
    String LOCATION = "Location";
    String MOST_RECENT_COMMUNICATION_TIME = "MOST_RECENT_COMMUNICATION_TIME";
    String NAME = "Name";
    String OLDEST_COMMUNICATION_TIME = "OLDEST_RECENT_COMMUNICATION_TIME";
    String PLAYBACK_MODE = "Playback Mode";
    /** @since 5.1 */
    String SELECTABLE = "selectable";
    String SNAP_ICON_TO_SURFACE = "Snap Icon to Surface";
    String SPEED = "SpeedInMps";
    String TRACK_STYLE = "Track Style";
    String UID = "UID";

    /**
     * Fired when the tooltip visibility is changed.
     * @since 5.6
     */
    String TOOLTIP_VISIBILITY = StandardAttributeNames.TOOLTIP_VISIBILITY;

    String VISIBILITY = "Visibility";
    /** @since 5.6 */
    String DRIFTER = "Drifter";

    double MIN_ICON_SCALE = 0.2;
    double MAX_ICON_SCALE = 2.0;

    /**
     * Name of VFS directory in which to store Icon images received remotely.
     *
     * @since 2.11
     */
    String MAP_OBJECT_ICONS_DIR = "MapObjectIcons";

    /**
     * @return {@code true} if this Map Object is the "self" location marker
     * @since 5.3
     */
    default boolean isSelfMarker()
    {
        return CotTypes.SELF.equals(getCotType());
    }

    /**
     * @return {@code true} if this Map Object was restored from persistence, {@code false} otherwise
     * @since 2.37 (moved down from {@link IShapeMapObject})
     */
    boolean isRestoring();

    /**
     * @return The {@link IMapObjectController controller} for this Map Object
     * @since 3.0
     */
    IMapObjectController getController();

    /**
     * @return The {@link IPluginDefinition} defining this map object
     * @since 2.29
     */
    IPluginDefinition<? extends IRaptorPlugin> getPluginDefinition();

    void setVisible(boolean visible);

    /**
     * Set the GeoPoint and associated metadata for this map item.
     *
     * @param geoPointMetaData Source to copy in
     * @since 5.5
     */
    void setGeoPointMetaData(@Nonnull GeoPointMetaData geoPointMetaData);

    /**
     * @return Metadata enhanced wrapper on the GeoPoint for this map item
     * @since 5.5
     */
    @Nonnull
    GeoPointMetaData getGeoPointMetaData();

    /**
     * Set the location of the map object, using the current system time.
     *
     * @param location       the new location of the map object
     * @param manualLocation whether the user manually set the location
     * @return {@code true} if the new location was different from the current location, {@code false} otherwise
     * @since 5.5
     */
    default boolean setLocation(ILocation location, boolean manualLocation)
    {
        return setLocation(location, System.currentTimeMillis(), manualLocation);
    }

    /**
     * Set the location of the map object.
     *
     * @param location       the new location of the map object
     * @param time           time that the location is being set at
     * @param manualLocation whether the user manually set the location
     * @return {@code true} if the new location was different from the current location, {@code false} otherwise
     */
    boolean setLocation(ILocation location, long time, boolean manualLocation);

    /**
     * @return The location of the map object.
     */
    @Nonnull
    GeoPoint getLocation();

    /**
     * @return {@code true} if this Map Object has a valid location
     * @see GeoPoint#UNKNOWN_POINT
     * @since 3.0
     */
    default boolean hasLocation()
    {
        return !getLocation().equals(GeoPoint.UNKNOWN_POINT);
    }

    void setDescription(String description);

    void setName(@Nonnull String name);

    String getName();

    String getDescription();

    BufferedImage getIconImage();

    ImageIcon getSizedIcon(int size);

    void setDisplayableType(String type);

    String getDisplayableType();

    String getKey();

    String getFamily();

    String getType();

    String getUnitID();

    String getGroupName();

    /**
     * Specify the URI of an image to be used as the icon for this Map Object.  Specifying {@code null} causes the
     * Map Object to revert to using the Icon that was originally specified in its {@link IPluginDefinition}.
     *
     * @param imageURI New Image URI, {@code null} to revert to original definition
     */
    void setIconImage(@Nullable URI imageURI);

    /**
     * Specify the BufferedImage to be used as the icon for this Map Object.  The image will be internalized
     * (stored in VFS), and then installed as the icon image via {@link #setIconImage(URI)}.  This method is typically
     * used when the Icon image is received from an external source.
     *
     * @param image The image for the icon
     * @since 2.23
     */
    void setExternalIconImage(@Nonnull BufferedImage image);

    /**
     * Reset the icon to the Icon that was originally specified in its {@link IPluginDefinition}.  This is equivalent
     * to calling {@code setIconImage(null)}.
     *
     * @since 3.5
     */
    default void resetIconImage()
    {
        setIconImage(null);
    }

    /**
     * Gets the default icon buffered image
     *
     * @return the buffered image of the default icon
     * @since 2.33
     */
    BufferedImage getDefaultIcon();

    /**
     * Gets the default icon URI
     *
     * @return the URI of the default icon
     * @since 2.33
     */
    URI getDefaultIconURI();

    /**
     * Set the icon scaling size.  Valid range is from {@value #MIN_ICON_SCALE} to {@value #MAX_ICON_SCALE}. Values
     * outside that range will be ignored.
     *
     * @param scale amount of scaling to apply to the displayed icon
     */
    void setIconScale(double scale);

    double getIconScale();

    void initializeIcons();

    /**
     * Specify if the icon should be rendered on the globe surface, or at its actual elevation.
     *
     * @param snapIconToSurface Pass {@code true} to render on globe surface
     */
    void setSnapIconToSurface(boolean snapIconToSurface);

    /**
     * @return true if the icon should be rendered on the globe surface instead of at its actual elevation
     */
    boolean isSnapIconToSurface();

    /**
     * @return {@code true} if this type of client map object supports the use of the "snap icon to surface"
     * feature.
     */
    boolean supportsSnapIconToSurface();

    /**
     * Set whether the elevation is to be interpreted as MSL (mean sea level) or AGL (above ground level)
     *
     * @param elevationMode Elevation mode to use for rendering
     */
    void setElevationMode(ElevationMode elevationMode);

    /**
     * @return The map object's elevation mode (MSL or AGL)
     */
    ElevationMode getElevationMode();

    /**
     * Removes all references and cleans up all resources for this MapObject
     */
    void shutdown();

    void setHighestState(State highestState);

    State getHighestState();

    Collection<IRaptorDataStructure> getRaptorDataStructures();

    IRaptorDataStructure getRaptorDataStructure(String structureType);

    /**
     * Construct an RDM based on the data structure identified by structureType and using the
     * unit ID from this MapObject.
     *
     * @param structureType Type of data structure from which to create the RDM
     * @return newly minted RDM
     */
    IPrePersistRaptorDataMessage createRaptorDataMessage(String structureType);

    /**
     * Construct an RDM based on the {@link DataStructureBuilder#LOCATION_MESSAGE_TYPE}
     * using the unit ID from this map object.  It will be marked as manually generated and have its device time
     * set to the current time.
     *
     * @return Newly created RDM
     * @see IPrePersistRaptorDataMessage#setManuallyGeneratedLocation(boolean)
     */
    IPrePersistRaptorDataMessage createLocationDataMessage();

    /**
     * @return The default rendering color for this map object's track points
     */
    Color getDefaultTrackColor();

    /**
     * Set the default color to be used for track points on this map object.  If no TrackStyleDefinition specifies
     * a color, then this color will be used to render the track point markers.  The initial setting for this
     * member is a random color (yes, that's the default default).
     *
     * @param defaultTrackColor The default rendering color for this map object's track points
     * @see ISmartTrack
     * @see ITrackStyleDefinition
     */
    void setDefaultTrackColor(Color defaultTrackColor);

    long getMostRecentCommunicationTime();

    boolean isVisible();

    /**
     * @return {@code true} if the Map Object can be selected by left mouse clicks, {@code false} if not.
     * @since 5.1
     */
    boolean isSelectable();

    /**
     * Set whether the Map Object can be selected by left mouse clicks.  If a Map Object is not selectable,
     * then no popup/radial menu, no Properties panel, and no Properties dialog can be accessed.
     *
     * @param selectable Pass {@code true} to allow selection, {@code false} to disallow.
     * @since 5.1
     */
    void setSelectable(boolean selectable);

    /**
     * @return {@code true} if this Map Object allows drag operations
     */
    boolean isAllowDrag();

    /**
     * Set whether a map object can be dragged.
     *
     * @param allowDrag true to allow dragging
     */
    void setAllowDrag(boolean allowDrag);

    boolean isAllowDeviceToBeContainer();

    void setAllowDeviceToBeContainer(boolean allowDeviceContainment);

    boolean hasElevation();

    long getOldestCommunicationTime();

    Set<IStructure> getAllDataStructures();

    /**
     * @return The Map Entity defining this Map Object
     * @since 2.29
     */
    IMapEntity getMapEntity();

    /**
     * @return if this map object is currently being played back
     */
    boolean inPlaybackMode();

    /**
     * @param playback true if the map object is in playback mode, false otherwise
     */
    void setPlaybackMode(boolean playback);

    /**
     * @return The Map Object that owns this Map Object, or {@code null} if none
     * @since 3.3 (signature change)
     */
    @Nullable
    IMapObject getOwner();

    /**
     * Set (or remove) the owning Map Object for this Map Object.
     *
     * @param owner New owner, pass {@code null} to remove any existing owner
     * @since 3.3 (signature change)
     */
    @Internal
    void setOwner(@Nullable IMapObject owner);

    /**
     * Return the object that contains the metadata describing the set of
     * tracks owned by this {@link IMapObject}.
     *
     * @return The metadata describing the set of tracks owned by this {@link IMapObject}.
     * @since 1.13
     */
    ITrackInfo getTrackInfo();

    /**
     * Set the name of the tab to be displayed when the preferences dialog is opened.
     *
     * @param tabName Name of tab
     */
    void setDefaultPreferencesTab(String tabName);

    /**
     * @return the name of the tab to be displayed when the preferences dialog is opened
     */
    String getDefaultPreferencesTab();

    /**
     * Gets the URI to the map object in the local system.
     *
     * @return The URI of the map object's icon image.
     */
    URI getIconImageURI();

    /**
     * {@inheritDoc}
     * @since 3.5
     */
    @Override
    String getRemarks();

    /**
     * {@inheritDoc}
     * @since 3.5
     */
    @Override
    void setRemarks(String remarks);

    /**
     * @return the drifter configuration for this map object
     * @since 5.6
     */
    @Nullable
    DrifterConfiguration getDrifterConfiguration();

    /**
     * @param drifterConfiguration the drifter configuration set on this map object
     * @since 5.6
     */
    void setDrifterConfiguration(@Nullable DrifterConfiguration drifterConfiguration);

    /**
     * @return This Map Object's tooltip visibility setting
     * @since 5.6
     */
    @Nonnull
    default TooltipVisibility getTooltipVisibility()
    {
        return TooltipVisibility.valueOf(getMetaString(StandardAttributeNames.TOOLTIP_VISIBILITY, TooltipVisibility.DEFAULT.name()));
    }

    /**
     * @param tooltipVisibility The tooltip visibility to apply
     * @since 5.6
     */
    void setTooltipVisibility(@Nonnull TooltipVisibility tooltipVisibility);

    /**
     * @param fieldOfView The field of view associated with this map object
     * @since 5.8
     */
    @IncubatingApi(since = "5.8")
    void setFieldOfView(@Nullable FieldOfView fieldOfView);

    /**
     * @return The map object's field of view or {@code null} if unset
     * @since 5.8
     */
    @IncubatingApi(since = "5.8")
    @Nullable
    FieldOfView getFieldOfView();
}
