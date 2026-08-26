package gov.takx.api.mapobject;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.device.IDevicePluginController;
import gov.takx.api.plugin.device.IDevicePluginDelegate;
import gov.takx.api.query.IDeviceQueryService;
import gov.takx.api.ui.IMapObjectSwingUIOperations;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.platform.query.MessageStore;
import jakarta.annotation.Nonnull;

import java.io.Serializable;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Contract for Map Object Controllers.
 *
 * @since 2.0.0
 */
@NoPluginImplementations
public interface IMapObjectController extends IMapObjectSwingUIOperations, IDevicePluginDelegate, Serializable
{
    /**
     * @return The map object managed by this controller
     * @since 3.0 (eas previously in a base interface)
     */
    @Override
    IMapObject getMapObject();

    /**
     * Utility method to send an {@link IPrePersistRaptorDataMessage} to change the location of the map object
     * being managed by this controller.
     *
     * @param location The new location to send in the message
     * @since 3.0 (was previously in a base interface)
     */
    void sendLocationChangeMessage(ILocation location);

    void addMapObjectControllerListener(@Nonnull IMapObjectControllerListener mapObjectControllerListener);

    void removeMapObjectControllerListener(@Nonnull IMapObjectControllerListener mapObjectControllerListener);

    /**
     * Removes all the controller's listeners and any other references it contains.  For internal use only.
     */
    @Internal
    void shutdown();

    /**
     * Shutdown the device plugin and plugin controller. This is called as the plugin container is being destroyed.
     * For internal use only.
     * @since 5.7
     */
    @Internal
    void shutdownPlugin();

    IDevicePluginController getPluginController();

    Collection<IRaptorDataStructure> getDataStructures();

    void receiveMessage(IRaptorDataMessage rdm);

    /**
     * Use this method to make data changes to the MapObject that you want saved to the database
     *
     * @param rdm data message to send
     */
    void sendMessage(IPrePersistRaptorDataMessage rdm);

    /**
     * Called by MapObjectPreferencesDialog to determine what the default comm type should be
     *
     * @return String value corresponding to the default comm type, or {@code null} if none selected
     */
    CommServiceType getDefaultCommType();

    /**
     * Called by MapObjectPreferencesDialog to determine what the default comm name should be
     *
     * @return String value corresponding to the default comm name, or {@code null} if none selected
     */
    String getDefaultCommName();

    /**
     * Called by MapObjectPreferencesDialog when user has modified the default comm path value
     *
     * @param commType Value for comm type (eg: CommServiceTypes.SERIAL)
     * @param commName String value for comm name (eg: "COM2")
     */
    void updateDefaultCommPath(CommServiceType commType, String commName);

    /**
     * Get a list of click listeners registered for tracks created by this map object.
     *
     * @return List of listeners to be notified of single click events.
     */
    List<ITrackSelectListener> getSelectListeners();

    /**
     * Get a list of context menu listeners registered for tracks created by this map object.
     *
     * @return List of listeners to be notified of single click events.
     */
    List<ITrackContextMenuListener> getContextMenuListeners();

    /**
     * Resource management on client plugin delete.
     */
    void delete();

    Comparator<IDataMonitor> getComparator();

    /**
     * Specifies an alternate query service to use for operations such as the device log.  This allows plugins
     * to customize the way messages are stored and still support core services like the device log.
     * <p>
     * For example, one suite of plugins use the {@link MessageStore} to hold all RDMs
     * in memory.  The plugin then specifies a specialized query service that operates upon that in-memory store
     * instead of querying the database.
     *
     * @param queryService The query service to use for the device log
     * @since 2.29
     */
    @SuppressWarnings("unused")
    // Called from plugins
    void setAlternateQueryService(IDeviceQueryService queryService);

    /**
     * Undo the last user-initiated location change on the Map Object.
     *
     * @since 3.0
     */
    void undoLastUserLocationChange();

    /**
     * @return {@code true} if the last location change can be undone
     * @since 3.0
     */
    boolean canUndoLastLocation();
}
