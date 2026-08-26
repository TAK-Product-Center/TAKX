package gov.takx.api.persist;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IStructure;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Contract for service to locate {@link IMapObject MapObjects}, {@link IMapEntity MapEntities},
 * and {@link IStructure Structures}.
 *
 * @since 3.2
 */
@InjectableService
@NoPluginImplementations
public interface IMapObjectFinder
{
    /**
     * @return list of ALL MapObjects in the system, never {@code null}, may be empty
     */
    @Nonnull
    List<IMapObject> findAllMapObjects();

    /**
     * Find the MapObjects with the given family.
     *
     * @param family The family for the map objects of interest
     * @return list of map objects, never {@code null}, may be empty
     */
    @Nonnull
    List<IMapObject> findMapObjectsByFamily(@Nonnull String family);

    /**
     * Find the MapObjects with the given family and type.
     *
     * @param family The family for the map objects of interest
     * @return list of map objects, never {@code null}, may be empty
     */
    @Nonnull
    List<IMapObject> findMapObjectsByFamilyAndType(@Nonnull String family, @Nonnull String type);

    /**
     * Find a MapObject by its key.
     *
     * @param key The key to search for
     * @return located MapObject or {@code null}
     */
    @Nullable
    IMapObject findMapObject(@Nonnull String key);

    /**
     * Find a MapObject by its UID.
     *
     * @param uid The UID to search for
     * @return located MapObject or {@code null}
     */
    @Nullable
    IMapObject findMapObjectByUid(@Nonnull String uid);

    /**
     * Find all the Map Objects of the given type.
     *
     * @param type Type of MapObject to find
     * @return Collection of MapObjects that are assignable to the given type, never {@code null}, may be empty
     */
    @Nonnull
    <T extends IMapObject> List<T> findMapObjectsOfType(@Nonnull Class<T> type);

    /**
     * @return list of ALL MapEntity instances in the system, never {@code null}, may be empty
     */
    @Nonnull
    List<IMapEntity> findAllMapEntities();

    /**
     * Find a MapEntity by its family and type.
     *
     * @param family Map entity family
     * @param type   Map entity type
     * @return located MapEntity, {@code null} if not found
     */
    @Nullable
    IMapEntity findMapEntity(@Nonnull String family, @Nonnull String type);

    /**
     * Find a MapEntity by its unique key (containing family and type)
     *
     * @param key Unique key of MapEntity
     * @return located MapEntity, {@code null} if not found
     */
    @Nullable
    IMapEntity findMapEntity(@Nonnull String key);

    /**
     * @return List of ALL structures in the system, never {@code null}, may be empty
     */
    @Nonnull
    List<IStructure> findAllStructures();

    /**
     * Find a structure by its hash.
     *
     * @param hash The structure's hash. This uniquely based on the type, version, and field definitions.
     * @return The structure with the given hash, or {@code null} if not found
     */
    @Nullable
    IStructure findStructureByHash(@Nonnull String hash);

    /**
     * Find all the structures with the given message type.
     *
     * @param messageType Message type of interest
     * @return List of matching structures, never {@code null}, may be empty
     */
    @Nonnull
    List<IStructure> findStructuresByMessageType(@Nonnull String messageType);
}
