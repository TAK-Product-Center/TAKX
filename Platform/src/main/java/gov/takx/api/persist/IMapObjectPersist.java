package gov.takx.api.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IStructure;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.hibernate.ObjectNotFoundException;

import java.util.List;
import java.util.function.Consumer;

/**
 * Persistence helper for map objects, map entities, and structures.
 */
@Internal
@NoPluginImplementations
public interface IMapObjectPersist extends IMapObjectFinder
{
    /**
     * Save a map object in persistence.
     */
    void persistMapObject(@Nonnull IMapObject mapObject);

    /**
     * Find a MapObject by its id.
     *
     * @param id ID of MO to retrieve
     * @return located MapObject, {@code null} if it doesn't exist
     * @since 2.28
     */
    @Nullable
    IMapObject findMapObjectById(int id);

    /**
     * Delete the specified map object from the database.  Any owned map objects will also be deleted.
     * For example, shapes created by a device plugin will be deleted when the device map object is deleted.
     *
     * @param mapObject The map object to delete
     * @return List of keys for owned map objects deleted
     */
    @Nonnull
    List<String> deleteMapObject(@Nonnull IMapObject mapObject);

    /**
     * Find the MapEntity with the given id.
     *
     * @param id MapEntity id
     * @return Map Entity
     * @since 2.29
     * @throws ObjectNotFoundException If there is no MapEntity with that ID
     */
    @Nonnull
    IMapEntity findMapEntityById(int id) throws ObjectNotFoundException;

    /**
     * Get a map entity, creating one if it does not exist.
     *
     * @param family Map object family
     * @param type   Map object type
     * @return Map Entity
     * @since 2.29
     */
    @Nonnull
    default IMapEntity getOrCreateMapEntity(@Nonnull String family, @Nonnull String type)
    {
        return getOrCreateMapEntity(family, type, null, null);
    }

    /**
     * Get a map entity, creating one if it does not exist.
     *
     * @param family   Map object family
     * @param type     Map object type
     * @param onCreate Optional Consumer to handle a Map Entity if it is newly created. Pass {@code null} for no handler.
     * @param onExisting Optional Consumer to handle a Map Entity if it already existed. Pass {@code null} for no handler.
     * @return Map Entity
     * @since 2.29 (signature change in 3.4)
     */
    @Nonnull
    IMapEntity getOrCreateMapEntity(@Nonnull String family, @Nonnull String type, @Nullable Consumer<IMapEntity> onCreate,
                                    @Nullable Consumer<IMapEntity> onExisting);

    /**
     * Find the Structure with the given id.
     *
     * @param id Structure Id
     * @return Structure
     * @since 2.29
     * @throws ObjectNotFoundException If there is no Structure with that ID
     */
    @Nonnull
    IStructure findStructureById(int id) throws ObjectNotFoundException;

    /**
     * Find all the map objects directly owned by the given map object.
     *
     * @param owningMapObject MapObject of interest
     * @return List of owned map objects, never {@code null}, may be empty if there are no owned objects
     * @since 2.28 (signature change in 3.3)
     */
    @Nonnull
    List<IMapObject> getOwned(@Nonnull IMapObject owningMapObject);

    /**
     * @return A PersistService instance
     * @since 2.29
     */
    @Nonnull
    IPersistService getPersistService();

    /**
     * Load our initial data from the DB.
     * @since 3.2
     */
    void doInitialLoad();
}
