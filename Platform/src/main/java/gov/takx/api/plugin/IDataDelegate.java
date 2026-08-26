package gov.takx.api.plugin;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.CreationFailedException;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.platform.IDataMonitorServices;
import gov.takx.api.plugin.device.IDataStructureFactory;
import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Delegate to create map objects, and send data messages to those map objects. This delegate
 * allows the caller to inject messages into the TAKX data flow.
 *
 */
@SuppressWarnings({"UnusedDeclaration"}) // Many of these methods are only called by plugins
@NoPluginImplementations
public interface IDataDelegate
{
    /**
     * Get the MapEntity defined by the given family and type. {@code null} is returned if there is no such
     * map entity defined.
     *
     * @param family Map entity family
     * @param type   Map entity type
     * @return Map entity, or {@code null} if not found
     * @since 2.29
     */
    IMapEntity getMapEntity(String family, String type);

    /**
     * Find or create a MapEntity with the given family/type. Add the given data structures to the MapEntity, whether
     * created or existing.
     *
     * @param mapObjectFamily Family of the map object.
     * @param mapObjectType   Type of the map object.
     * @param dataStructures  Data structures to add to the MapEntity
     * @return MapEntity
     * @since 2.29 (renamed in 3.4)
     */
    IMapEntity getOrCreateMapEntity(String mapObjectFamily, String mapObjectType, List<IRaptorDataStructure> dataStructures);

    /**
     * Get or create a Map Object using the configuration in the given MapObjectSpec.  If a Map Object already
     * exists with the same family, type, and unit Id as configured in the spec, then a new Map Object will not
     * be created, and the existing Map Object will be returned.
     *
     * @param mapObjectSpec Map Object Specification holding the configuration to use
     * @return The constructed (or existing) Map Object
     * @see MapObjectSpec#builder(IMapEntity, String)
     * @see gov.takx.api.mapobject.IMapObjectManager#getOrCreateMapObject(MapObjectSpec)
     * @since 2.29 (renamed in 3.4)
     * @throws CreationFailedException If an exception occurs during construction of the MO
     */
    @Nonnull
    IMapObject getOrCreateMapObject(@Nonnull MapObjectSpec mapObjectSpec) throws CreationFailedException;

    /**
     * Send a data message to the TAKX core.
     *
     * @param rdm The message to send.
     * @since 1.4
     */
    void sendRaptorDataMessage(IPrePersistRaptorDataMessage rdm);

    /**
     * Get a collection of all the map objects in the project.
     *
     * @return List of map objects.
     */
    Collection<? extends IMapObject> getMapObjects();

    /**
     * @return List of Map Object Controllers for all Map Objects in the system
     * @since 3.0
     */
    Collection<? extends IMapObjectController> getMapObjectControllers();

    /**
     * Get the map object for the given unique key. May be {@code null} if the MO doesn't exist.
     *
     * @param key Unique key of map object
     * @return map object or {@code null} if not found
     * @since 1.25
     */
    IMapObject getMapObject(String key);

    /**
     * Provides a manner to verify that a map object type exists prior to invoking getOrCreateMapEntity().
     *
     * @param category Plugin Category
     * @param family   Family name
     * @param type     Type name
     * @return true if plugin exists, false otherwise
     */
    boolean doesPluginExist(PluginCategory category, String family, String type);

    /**
     * Access the data monitor services map for all active map objects.
     *
     * @return Map of data monitor services keyed by IMapObject.
     */
    Map<IMapObject, IDataMonitorServices> getDataMonitorServices();

    /**
     * Delete a map object. Will delete the map object and its historical data, but if new messages come in for the
     * device, it will be recreated and dropped back onto the globe as if it were a new device.
     * <p/>
     * Note: It is very advisable to spawn a thread to call this method, since deleting can take awhile to complete
     * and return if there are a significant amount of tracks.
     *
     * @param mapObject map object to delete
     * @since 1.25
     */
    void deleteMapObject(IMapObject mapObject);

    /**
     * Delete all RDMs for a map object that have timestamps older to the given timestampOfDeletionMillis.
     *
     * @param mapObject                 map object for which RDMs will be deleted
     * @param timestampOfDeletionMillis The latest timestamp in milliseconds of the data to be deleted
     * @since 1.49
     */
    void truncateMapObjectRdmsByTimestamp(IMapObject mapObject, long timestampOfDeletionMillis);

    /**
     * Obtain the next unit ID available for assignment for the given device plugin family and type.
     *
     * @param family Plugin Family
     * @param type   Plugin Type
     * @return next available unit id suitable for assignment
     * @since 1.14
     */
    String getNextUnitID(String family, String type);

    /**
     * Get a unique data structure name for a plugin, with a base name provided. If the name already exists in the list
     * of structures for this plugin, this method will generate a unique name based on the provided name. Currently TAKX
     * does not allow 2 structures with the same name (message type) for the same plugin, and this helps guarantee
     * before creating a structure on the fly that the name will be unique.
     *
     * @param family plugin family
     * @param type   plugin type
     * @param name   base name for data structure.
     * @return name if it is already unique, or a permutation of the name guaranteed to be unique.
     * @since 1.31; version parameter removed in 3.0
     */
    String getUniqueDataStructureName(String family, String type, String name);

    /**
     * Obtain the data structure factory for the given device plugin family and type.
     *
     * @param family Plugin Family
     * @param type   Plugin Type
     * @return Data structure factory
     * @since 1.14
     */
    IDataStructureFactory getDataStructureFactory(String family, String type);
}
