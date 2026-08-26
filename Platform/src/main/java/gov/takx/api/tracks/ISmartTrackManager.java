package gov.takx.api.tracks;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * @since 1.12
 */
@SuppressWarnings("unused") // Called from plugins
@InjectableService
@NoPluginImplementations
public interface ISmartTrackManager
{
    String GLOBAL_SMART_TRACK_FAMILY = "gov.takx.smarttrack";

    String GLOBAL_TRACK_STYLE_TYPE = "trackStyleDefinition";
    String GLOBAL_TRACK_STYLE_UUID = "a7c83f00-28cd-11e0-91fa-0800200c9a66";

    String GLOBAL_CONNECTING_LINE_TYPE = "connectingLineDefinition";
    String GLOBAL_CONNECTING_LINE_UUID = "fc54af90-28cd-11e0-91fa-0800200c9a66";

    /**
     * Registers a track style definition with the platform.
     *
     * @param trackStyleDefinition Track style definition
     */
    void registerTrackStyle(@Nonnull ITrackStyleDefinition trackStyleDefinition);

    /**
     * Unregisters a track style definition from the platform.
     *
     * @param trackStyleDefinition Track style definition
     * @return Old track style definition that was removed, or {@code null} if no match was found
     */
    @Nullable
    ITrackStyleDefinition unregisterTrackStyle(ITrackStyleDefinition trackStyleDefinition);

    /**
     * Determines if the given track style definition is presently assigned and in use.
     *
     * @param trackStyleDefinition Track style definition
     * @return {@code true}, if the definition is presently in use
     */
    boolean isTrackStyleAssigned(ITrackStyleDefinition trackStyleDefinition);

    /**
     * Get a registered TSD given its unique key.
     *
     * @param key Unique key of the TSD to retrieve
     * @return TSD or {@code null} if not found
     */
    @Nullable
    ITrackStyleDefinition getTrackStyleDefinition(String key);

    /**
     * Returns all of the registered TSDs.
     *
     * @return All of the registered TSDs.
     * @since 1.44
     */
    Collection<ITrackStyleDefinition> getTrackStyleDefinitions();

    /**
     * Registers a connecting line definition with the platform.
     *
     * @param connectingLineDefinition Connecting line definition
     */
    void registerConnectingLine(@Nonnull IConnectingLineDefinition connectingLineDefinition);

    /**
     * Unregisters a connecting line definition from the platform.
     *
     * @param connectingLineDefinition Connecting line definition
     * @return Old connecting line definition that was removed, or {@code null} if no match was found
     */
    @Nullable
    IConnectingLineDefinition unregisterConnectingLine(IConnectingLineDefinition connectingLineDefinition);

    /**
     * Determines if the given connecting line definition is presently assigned and in use.
     *
     * @param connectingLineDefinition Connecting line definition
     * @return true, if the definition is presently in use
     */
    boolean isConnectingLineAssigned(IConnectingLineDefinition connectingLineDefinition);

    /**
     * Registers a SmartTrack definition listener.
     *
     * @param listener Listener
     */
    void addDefinitionListener(@Nonnull ISmartTrackDefinitionListener listener);

    /**
     * Unregisters a SmartTrack definition listener.
     *
     * @param listener Listener
     */
    void removeDefinitionListener(@Nonnull ISmartTrackDefinitionListener listener);

    /**
     * Globally defined track style definition. This definition cannot be programmatically defined. It is entirely
     * controlled by the user and can only be read from.
     *
     * @return Global track style definition
     */
    ITrackStyleDefinition getGlobalTrackStyleDefinition();

    /**
     * Globally defined default connecting line definition. This definition cannot be programmatically defined. It is
     * entirely controlled by the user.
     * <p/>
     * The global connecting line definition is a template that is copied to each new device map object.
     *
     * @return Global connecting line definition
     */
    IConnectingLineDefinition getGlobalConnectingLineDefinition();

    /**
     * Get a track styling context for the given map object.  An instance will be created on demand as needed.
     * The returned context will refer to the TSD assigned to the map object and will reach back to the global
     * TSD for any values not specified on the map object level TSD.  The context will respond to property
     * changes on the map object and properly update itself if the TSD on the map object is replaced.
     *
     * @param mapObject The map object of interest
     * @return track styling context
     * @see IIconMapObject#setTrackStyleDefinition(ITrackStyleDefinition)
     */
    ITrackStylingContext getTrackStylingContext(@Nonnull IMapObject mapObject);

    /**
     * Get a track styling context for the given map object ID.  An instance will be created on demand as needed.
     * The returned context will refer to the TSD assigned to the map object and will reach back to the global
     * TSD for any values not specified on the map object level TSD.  The context will respond to property
     * changes on the map object and properly update itself if the TSD on the map object is replaced.
     *
     * @param mapObjectId The ID of the map object of interest
     * @return track styling context
     * @see #getTrackStylingContext(IMapObject)
     */
    ITrackStylingContext getTrackStylingContext(Integer mapObjectId);
}
