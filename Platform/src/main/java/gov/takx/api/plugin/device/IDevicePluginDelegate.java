package gov.takx.api.plugin.device;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.commservices.ISerialPortManager;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.explorer.IExplorerManagerDelegate;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IStateIconManager;
import gov.takx.api.layer.ILayerManagerDelegate;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.ITrackContextMenuListener;
import gov.takx.api.mapobject.ITrackDisplayHandler;
import gov.takx.api.mapobject.ITrackSelectListener;
import gov.takx.api.messages.IPrePersistRaptorCommandMessage;
import gov.takx.api.platform.IDataMonitorServices;
import gov.takx.api.plugin.IPluginDelegate;
import gov.takx.api.query.IDeviceQueryService;
import gov.takx.api.shapes.IShapeService;
import gov.takx.api.surfaces.IAnalyticSurfaceService;
import gov.takx.api.tracks.ISmartTrack;
import gov.takx.api.tracks.ISmartTrackManager;
import gov.takx.api.ui.IDeviceMenuSpaceManager;
import gov.takx.api.ui.IDeviceSwingUIOperations;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.commons.constants.State;
import gov.takx.commons.security.IParserEncryptionKey;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.Action;

import java.awt.Color;
import java.io.IOException;
import java.net.URL;
import java.util.SortedSet;

/**
 * Contract for device plugin delegates.
 *
 * @since 2.0.0
 */
@SuppressWarnings("unused") // called from plugins
@NoPluginImplementations
public interface IDevicePluginDelegate extends IDeviceSwingUIOperations, ISharedDeviceCapable, IDataMonitorServices, IPluginDelegate,
        ISerialPortManager, ISmartTrack
{
    /**
     * Get the IDevicePlugin that owns this {@link IDevicePluginDelegate} instance.
     *
     * @return IDevicePlugin Plugin that owns this delegate instance
     */
    IDevicePlugin getDevicePlugin();

    /**
     * Identifies the present location of the map object on the globe.  Useful for when a manual location has been
     * set.
     *
     * @return Present location on the globe.
     */
    ILocation getLocationOnGlobe();

    /**
     * Get the Smart Tracks Manager.
     *
     * @return SmartTrackManager instance
     * @since 1.12
     */
    ISmartTrackManager getSmartTrackManager();

    /**
     * Send a command message, usually for the reason of having the message framed and send out the
     * communications port
     *
     * @param rcm IRaptorCommandMessage
     * @throws IOException exception in sending the message
     */
    void sendMessage(IPrePersistRaptorCommandMessage rcm) throws IOException;

    /**
     * Writes a plugin event into the Event Log.
     *
     * @param message Plugin message
     * @param details Event details
     */
    void logDeviceEvent(String message, String details);

    /**
     * Writes a plugin event into the Event Log, with the further option of allowing the plugin to declare a "State"
     * for the message, i.e. Critical, Warning, etc. so that the text will be colored according to the state, and
     * not in the default color.
     *
     * @param message Plugin message
     * @param details Event details
     * @param state   State to use to color the text
     * @since 1.14.20
     */
    void logDeviceEvent(String message, String details, State state);

    /**
     * Sets the plugin's map object icon to the icon represented by the URL
     *
     * @param imagePathURL Path to image
     */
    void setIconImage(URL imagePathURL);

    /**
     * Sets the flag that controls if the elevation value is ignored, instead snapping the device's icon to the
     * globe surface.
     *
     * @param flag true, snap the icon to the surface, or false to accurately represent the actual elevation
     */
    void setSnapIconToSurface(boolean flag);

    /**
     * Get the shape service.
     *
     * @return Shape service
     */
    IShapeService getShapeService();

    /**
     * Get the query service.
     *
     * @return Query service
     * @since 1.2
     */
    IDeviceQueryService getQueryService();

    /**
     * Get the Menu Space Manager.
     *
     * @return Menu space manager
     * @since 1.4
     */
    IDeviceMenuSpaceManager getMenuSpaceManager();

    /**
     * Get the UI Space manager.
     *
     * @return Space manager instance
     * @since 1.3
     */
    IUISpaceManager getUISpaceManager();

    /**
     * Get the surface service.
     *
     * @return Surface service.
     * @since 1.4
     */
    IAnalyticSurfaceService getAnalyticSurfaceService();

    /**
     * Get a reference to the globe manager that can be used to manage globes, including creation and deletion.
     *
     * @return The globe manager
     * @since 1.49
     */
    IGlobeManager getGlobeManager();

    /**
     * Get the state icon manager.
     *
     * @return State icon manager instance
     * @since 1.4
     */
    IStateIconManager getStateIconManager();

    /**
     * @return the state icon manager used when in playback.
     * @since 5.7
     */
    IStateIconManager getPlaybackStateIconManager();

    /**
     * Stores a set of encryption keys for the map object.  The gateway parser will look up what key(s) are available
     * when attempting to decrypt incoming data.
     *
     * @param sortedKeyList List of encryption keys to try in well-defined order of precedence.  All keys stored in the
     *                      list must implement Comparable (since IParserEncryptionKey extends Comparable) and should implement Comparable
     *                      in a consistent way so that the most likely key is always at the head of the sorted set.  The use of a Set also
     *                      ensures that duplicate keys are not stored.
     * @since 1.2
     */
    void setParserKeys(SortedSet<IParserEncryptionKey> sortedKeyList);

    /**
     * Sets the description for the map object.  The description is the field that is displayed on the map object
     * dialog just below the location.
     *
     * @param description String description.
     * @since 1.2
     */
    void setDescription(String description);

    /**
     * Sets the current value in the Comms tab in the map object preferences panel
     *
     * @param commType String value from CommServiceTypes (ie "SERIAL", "ETHERNET")
     * @param commName String value for the comm name (i.e. "COM1")
     */
    void setDefaultCommPath(CommServiceType commType, String commName);

    /**
     * Set the flag to allow the device icon on the screen to be draggable.
     *
     * @param allowDrag false to lock the icon from being dragged by a user, true to allow dragging
     * @since 1.3
     */
    void setAllowDrag(boolean allowDrag);

    /**
     * Set the flag to allow the device to be a container.
     *
     * @param allowContainment Allow the device to be a container for other devices.
     * @since 1.24
     */
    void setAllowContainment(boolean allowContainment);

    /**
     * Set the name of the Map Object.  The name is displayed in numerous locations throughout the UI, so it should
     * be chosen with some care to ensure that it is unique, or at least distinguishable by the user.
     *
     * @param name New name
     * @since 1.25
     */
    void setName(String name);

    /**
     * @return The current name of the Map Object, as set by the user or the plugin or defaulted when the Map
     * Object was created.
     * @since 1.25
     */
    String getName();

    /**
     * Add a listener that will be invoked when the user selects a track created by this device.
     *
     * @param listener Listener to add. May not be {@code null}.
     * @see #removeSelectListener(ITrackSelectListener)
     */
    void addSelectListener(@Nonnull ITrackSelectListener listener);

    /**
     * Remove a select listener.
     *
     * @param listener Listener to remove.
     * @see #addSelectListener(ITrackSelectListener)
     */
    void removeSelectListener(@Nonnull ITrackSelectListener listener);

    /**
     * Add a listener that will be invoked when the user right-clicks 1 or more track points.  The registered listener
     * will get notified of any right-click operation on a track on any globe OR in the Device Log. The listener is
     * responsible for returning a list of menu items which are appropriate for the tracks selected.
     *
     * @param listener Listener to add, may not be {@code null}
     * @see #removeTrackContextMenuListener(ITrackContextMenuListener)
     * @since 2.26
     */
    void addTrackContextMenuListener(@Nonnull ITrackContextMenuListener listener);

    /**
     * Remove a select listener.
     *
     * @param listener Listener to remove.
     * @see #addTrackContextMenuListener(ITrackContextMenuListener)
     * @since 2.26
     */
    void removeTrackContextMenuListener(@Nonnull ITrackContextMenuListener listener);

    /**
     * Return the {@link IMapObject} that represents this device on the map.
     *
     * @return The {@link IMapObject} associated with this device.
     * @since 1.25.6
     */
    IMapObject getMapObject();

    /**
     * Toggles the display of a vector tail orbiting around the map object icon.  The vector consists of magnitude
     * and heading and is used to convey a sense of speed and direction to the end user.
     *
     * @param flag  True to enable, false to disable
     * @param color The color that the pointer should be. May be {@code null}.
     */
    void showVectorPointer(boolean flag, @Nullable Color color);

    /**
     * Rotates and scales the vector tail to convey a sense of speed and direction to the end user.  The magnitude and
     * heading scalars will be proportionally scaled, and there is a physical limit to the overall size of the rendered
     * magnitude representation.  At some arbitrary point expect the vector tail's length to stop growing, i.e. the
     * tail won't wrap half-way around the globe when it jumps from a value of 20.0 to 2500.0.  The actual number can't
     * be articulated here because it is proportionally based on the distance between the map object icon and the orbit
     * of its state icons, assuming state icons are enabled.
     *
     * @param magnitude Scalar value representing speed
     * @param heading   Scalar value representing heading.
     * @throws IllegalArgumentException when the heading scalar is outside the range 0.0 and 360.0, inclusive or the
     *                                  magnitude scalar is negative
     * @throws IllegalStateException    when called when the vector tail feature is disabled
     */
    void setVectorPointerAttributes(double magnitude, double heading);

    /**
     * Allows the map object to make itself (and its tracks) invisible and not displayed on the globe.  By default, the
     * user will have the ability to go into the Map Object Tree and re-establish visibility on the device's map object
     * icon.  If this behavior is undesirable, see overloaded setVisible() method.
     *
     * @param flag False, device's map object icon should be hidden
     * @return true, if the request to toggle visibility was permitted and occurred as requested
     */
    boolean setVisible(boolean flag);

    /**
     * Allows the map object to make lock itself into an invisible state.
     */
    void lockInvisible();

    /**
     * Allows the map object to unlock itself form a perpetually invisible state.
     */
    void unlockInvisible();

    /**
     * Add a handler that can override the default behavior when a user double-clicks on a device's tracks.
     *
     * @param trackDisplayHandler A handler that will be notified when a user double-clicks on this device's tracks.
     * @since 1.17
     */
    void addDisplayHandler(@Nonnull ITrackDisplayHandler trackDisplayHandler);

    /**
     * Remove a track selection handler.
     *
     * @param trackDisplayHandler The handler to remove.
     * @see #addDisplayHandler(ITrackDisplayHandler)
     * @since 1.17
     */
    void removeDisplayHandler(@Nonnull ITrackDisplayHandler trackDisplayHandler);

    /**
     * Get the Layer Manager delegate.
     *
     * @return The Layer manager delegate.
     * @since 1.41
     */
    ILayerManagerDelegate getLayerManagerDelegate();

    /**
     * Set the tooltip text for the map object icon on the globe.  Tooltip will override the default tooltip that
     * is usually displayed for a map object.  Setting the tooltip to {@code null} will revert to using the default.
     *
     * @param tooltip ToolTip string to display during mouseover.
     * @since 1.47
     */
    void setIconToolTipText(String tooltip);

    /**
     * Return the Explorer Manager delegate that provides access to all Explorers.
     *
     * @return The Explorer Manager delegate.
     * @since 2.4
     */
    IExplorerManagerDelegate getExplorerManagerDelegate();

    /**
     * Set a custom action to be performed when there is a request from anywhere to open the plugin dialog.  If
     * not {@code null}, this action will be performed instead.
     *
     * @param action custom action
     * @since 1.50
     */
    void setPluginDialogAction(@Nullable Action action);
}
