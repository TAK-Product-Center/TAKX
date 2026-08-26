package gov.takx.api.mapobject;

import gov.tak.api.maps.PointMapItem;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 */
@InjectableService
@NoPluginImplementations
public interface IMapObjectManager
{
    /**
     * Shutdown all the MO controllers associated with a given plugin definition.
     * This is typically invoked during the shutdown of the deployment.
     *
     * @since 2.0.0
     */
    @Internal
    void shutdownPluginControllers(IPluginDefinition<?> pluginDefinition);

    /**
     * Cause the selection of a map object on the globe.
     *
     * @param mapObjectKey unique key of a map object
     */
    void setSelected(String mapObjectKey);

    /**
     * Create a device at a specified location.  This method will either prompt the user to enter a valid unit id or
     * generate one automatically if the plugin configuration specifies auto-generation.
     *
     * @param definition device plugin definition
     * @param location   location to drop the device
     * @return boolean success creating device and dropping on map
     * @see IDevicePluginDefinition#isAutoGenerateUnitId()
     */
    boolean createDevice(IDevicePluginDefinition definition, ILocation location);

    /**
     * Deletes a map object.
     * If a device receives new data after the deletion, a new device will be created as if it is receiving data
     * for the first time.
     *
     * @param mapObject map object to delete
     * @since 1.25
     */
    void deleteMapObject(@Nonnull IMapObject mapObject);

    /**
     * Delete a part of the data associated with a given MapObject, but don't actually delete the MO.
     *
     * @param mapObject      MapObject being affected
     * @param timeOfDeletion The time of deletion, all data older than this time is getting erased
     * @since 1.34
     */
    void partialDeleteMapObject(IMapObject mapObject, long timeOfDeletion);

    void addMapObjectManagerListener(IMapObjectManagerListener listener);

    void removeMapObjectManagerListener(IMapObjectManagerListener listener);

    /**
     * Get the plugin definition for the given family/type.  Only search in the Device, Form, and Synthetic registries.
     * If the given family is {@link KeyUtils#FILE_FAMILY}, then we manufacture a synthetic definition for it.
     *
     * @param family Family of plugin
     * @param type   Typeof plugin
     * @return Optional plugin definition
     * @since 2.0.0
     */
    Optional<? extends IPluginDefinition<? extends IRaptorPlugin>> getPluginDefinition(@Nonnull String family, @Nonnull String type);

    /**
     * Get a map object for the given map object key.
     *
     * @param key Map object key
     * @return Map Object or {@code null} if not found
     * @since 3.4
     */
    @Nullable
    IMapObject getMapObject(@Nonnull String key);

    /**
     * @return Collection of all {@link IMapObject} instances, may be empty
     * @since 3.4
     */
    @Nonnull
    Collection<? extends IMapObject> getMapObjects();

    /**
     * Get a map object controller for the given map object key.
     *
     * @param key Map object key
     * @return controller or {@code null} if not found
     */
    @Nullable
    IMapObjectController getMapObjectController(@Nonnull String key);

    /**
     * Get the MOC for a map object given the ID of the MapObject.
     *
     * @param moId ID of MapObject to locate
     * @return controller, {@code null} if not found
     */
    @Internal
    @Nullable
    IMapObjectController getMapObjectController(Integer moId);

    /**
     * @return Collection of all {@link IMapObjectController} instances, may be empty
     */
    @Nonnull
    Collection<? extends IMapObjectController> getMapObjectControllers();

    /**
     * Get or create a Map Object using the configuration in the given MapObjectSpec.  If a Map Object already
     * exists with the same family, type, and unit Id as configured in the spec, then a new Map Object will not
     * be created, and the existing Map Object will be returned.
     *
     * @param mapObjectSpec Map Object Specification holding the configuration to use
     * @return The constructed (or existing) Map Object
     * @see MapObjectSpec#builder(String, String)
     * @see MapObjectSpec#builder(String, String, String)
     * @see MapObjectSpec#builder(IMapEntity, String)
     * @since 2.29 (renamed in 3.4)
     * @throws CreationFailedException If an exception occurs during construction of the MO
     */
    @Nonnull
    IMapObject getOrCreateMapObject(@Nonnull MapObjectSpec mapObjectSpec) throws CreationFailedException;

    /**
     * Find or create a MapEntity with the given family/type.  If created, it will not have any structures associated.
     *
     * @param family Map Object family
     * @param type   Map Object type
     * @return MapEntity
     * @since 2.29
     */
    IMapEntity getOrCreateMapEntity(@Nonnull String family, @Nonnull String type);

    /**
     * Find or create a MapEntity with the given family/type. Add the given data structures to the MapEntity, whether
     * created or existing.
     *
     * @param family Map Object family
     * @param type   Map Object type
     * @param structures  Data structures to add to the MapEntity
     * @return MapEntity
     * @since 3.4
     */
    IMapEntity getOrCreateMapEntity(@Nonnull String family, @Nonnull String type, @Nonnull List<IRaptorDataStructure> structures);

    boolean keyExists(String key);

    /**
     * Check for preexisting objects with the same name. The goal is to avoid collisions between user named objects and
     * generated names.
     *
     * @param name The target name for a new object.
     * @return true if the name is already associated with a map object, false if not.
     * @since 2.6
     */
    boolean nameExists(String name);

    /**
     * Generates a unique name for a map object. The name returned will be "baseName N" where N is the lowest unused
     * integer
     *
     * @param baseName Base name to use for generation
     * @return Generated name
     */
    String generateUniqueName(String baseName);

    String getNextUnitID(String family, String mapObjectType);

    IMapObject getSelectedMapObject();

    /**
     * Convenience method for determining if the user is permitted to change the visibility of the map object icon
     *
     * @param key Key of map object
     * @return true, if locked
     */
    boolean isVisibilityLocked(String key);

    /**
     * Set the visibility of the map object. This is mostly a pass through to the MO. This will also be the place that
     * we look for the shapes created by this MO and hide them as well.
     *
     * @param key Key of map object
     * @param vis new visible setting
     * @return true, if the request to toggle visibility was permitted and occurred as requested
     */
    boolean setVisible(String key, boolean vis);

    /**
     * Set the visibility of the map object. This is mostly a pass through to the MO. This will also be the place that
     * we look for the shapes created by this MO and hide them as well.
     *
     * @param key   Key of map object
     * @param vis   new visible setting
     * @param force When true, does not permit the end-user to toggle the visibility
     */
    void setVisible(String key, boolean vis, boolean force);

    /**
     * @return The MapObject that represents the "self" location
     * @since 5.3
     */
    PointMapItem getSelfMarker();
}
