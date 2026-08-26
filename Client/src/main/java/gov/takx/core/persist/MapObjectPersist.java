package gov.takx.core.persist;

import gov.takx.api.event.StopProjectServices;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IBackgroundDeleteManager;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.tracks.SmartTrackManager;
import gov.takx.dataservices.persist.MapEntity;
import gov.takx.dataservices.persist.MapObject;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Map Object persistence services.
 * <p>
 * As of 2.29 this now manages an in-memory cache of MapEntity, Structure, and MapObject instances.
 *
 */
@ApplicationScoped
public class MapObjectPersist implements IMapObjectPersist
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String UID_INDEX = "uid";

    private final IBackgroundDeleteManager backgroundDeleteManager;
    private final IPersistServiceProvider persistServiceProvider;
    private final StructureAssociationManager structureAssociationManager;
    private final SmartTrackManager smartTrackManager;

    private final EntityStore<IMapEntity> mapEntityStore = new EntityStore<>(IMapEntity::getUniqueKey);
    private final EntityStore<IStructure> structureStore = new EntityStore<>(IStructure::getHashValue);
    private final EntityStore<IMapObject> mapObjectStore = new EntityStore<>(IMapObject::getKey)
            .addSecondaryIndex(UID_INDEX, IMapObject::getUid);

    // Tracks MOs that own other MOs
    private final Set<Integer> ownsMapObjects = ConcurrentHashMap.newKeySet();

    // Transient Map Objects have ids < 0, and we generate them here since they are not physically persisted.
    // Some older code used -1 as an indicator that a MO had no ID.  I've tried to track those down, but it seems
    // simple to just start at a lower number here, and avoid any potential conflict with crufty old code.
    private final AtomicInteger transientIdGenerator = new AtomicInteger(-100);
    private Field idField;

    private IStructure manualLocationStructure;

    @Inject
    MapObjectPersist(IBackgroundDeleteManager backgroundDeleteManager,
                     IPersistServiceProvider persistServiceProvider,
                     StructureAssociationManager structureAssociationManager,
                     SmartTrackManager smartTrackManager)
    {
        this.backgroundDeleteManager = backgroundDeleteManager;
        this.persistServiceProvider = persistServiceProvider;
        this.structureAssociationManager = structureAssociationManager;
        this.smartTrackManager = smartTrackManager;

        findIdField();
    }

    /**
     * The "id" field of a Map Object is private, and there's no way to set it directly. So, we'll use reflection
     * to set the id value for transient MOs.
     *
     * @since 5.6
     */
    private void findIdField()
    {
        try
        {
            idField = MapObject.class.getDeclaredField("id");
            idField.setAccessible(true);
        } catch (NoSuchFieldException e)
        {
            logger.error("Unable to get 'id' field from MapObject", e);
        }
    }

    @Override
    public void doInitialLoad()
    {
        final IPersistService persistService = getPersistService();

        // Load all the structure/ME associations
        final Map<Integer, Set<Integer>> mapEntityStructureIds = structureAssociationManager.doInitialLoad();

        structureStore.addAll(persistService.query("from Structure")); // Load all the structures

        smartTrackManager.doInitialLoad(); // Load all the TSDs/CLDs

        // Load all the MapEntities and relink to structures
        final List<IMapEntity> mapEntities = persistService.query("from MapEntity");
        mapEntityStore.addAll(mapEntities);

        for (IMapEntity mapEntity : mapEntities)
        {
            // To relink structures, we need to find all the structures associated with this ME and add them to it
            mapEntityStructureIds.get(mapEntity.getId())
                    .forEach(structureId -> mapEntity.addStructure(structureStore.find(structureId)));
        }

        // Load all the MapObjects
        final List<MapObject> mapObjects = persistService.query("from MapObject");

        mapObjectStore.addAll(mapObjects);

        // Now invoke the extended lifecycle method to tell them they're all loaded.
        // This allows them to restore inter-MO relationships, like ownership and containment.
        mapObjects.forEach(MapObject::onAllLoaded);
    }

    /**
     * Add a MapEntity to the cache.
     *
     * @param me object to add
     * @since 2.29
     */
    public void addToCache(IMapEntity me)
    {
        mapEntityStore.add(me);
    }

    /**
     * Add a Structure to the cache.
     *
     * @param structure object to add
     * @since 2.29
     */
    public void addToCache(IStructure structure)
    {
        structureStore.add(structure);
    }

    /**
     * Add a Map Object to the cache.
     *
     * @param mapObject object to add
     * @since 2.29
     */
    public void addToCache(IMapObject mapObject)
    {
        final var owner = mapObject.getOwner();
        if (owner != null) ownsMapObjects.add(owner.getId());

        mapObjectStore.add(mapObject);
    }

    /**
     * Remove a Map Object from the cache.
     *
     * @param mapObject Map Object to remove
     */
    public void removeFromCache(IMapObject mapObject)
    {
        logger.debug("Remove MO: {}", mapObject);

        mapObjectStore.remove(mapObject.getId());
    }

    @Override
    public void persistMapObject(@Nonnull IMapObject mapObject)
    {
        final var owner = mapObject.getOwner();
        if (owner != null) ownsMapObjects.add(owner.getId());

        if (mapObject.isTransient())
        {
            try
            {
                idField.set(mapObject, transientIdGenerator.getAndDecrement());
                addToCache(mapObject);
            } catch (IllegalAccessException e)
            {
                logger.error("Failed to set id on {}", mapObject.getKey(), e);
            }
        } else
        {
            getPersistService().persistEntity(mapObject);
        }
    }

    @Nonnull
    @Override
    public List<IMapObject> findAllMapObjects()
    {
        return mapObjectStore.findAll();
    }

    @Override
    public IMapObject findMapObject(@Nonnull String key)
    {
        return mapObjectStore.findByKey(key);
    }

    @Override
    public IMapObject findMapObjectByUid(@Nonnull String uid)
    {
        return mapObjectStore.findByKey(UID_INDEX, uid);
    }

    @Override
    public IMapObject findMapObjectById(int id)
    {
        try
        {
            return mapObjectStore.find(id);
        } catch (ObjectNotFoundException ignored)
        {
        }
        return null;
    }

    @Nonnull
    @Override
    public <T extends IMapObject> List<T> findMapObjectsOfType(@Nonnull Class<T> type)
    {
        return mapObjectStore.findAllMatching(mo -> type.isAssignableFrom(mo.getClass()))
                .stream()
                .map(type::cast)
                .collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public List<IMapObject> findMapObjectsByFamily(@Nonnull String family)
    {
        return mapObjectStore.findAllMatching(mo -> mo.getFamily().equals(family));
    }

    @Nonnull
    @Override
    public List<IMapObject> findMapObjectsByFamilyAndType(@Nonnull String family, @Nonnull String type)
    {
        return mapObjectStore.findAllMatching(mo -> mo.getFamily().equals(family) && mo.getType().equals(type));
    }

    @Nonnull
    @Override
    public List<IMapEntity> findAllMapEntities()
    {
        return mapEntityStore.findAll();
    }

    @Override
    public IMapEntity findMapEntity(@Nonnull String family, @Nonnull String type)
    {
        final String key = KeyUtils.getUniqueKey(family, type);

        return findMapEntity(key);
    }

    @Override
    public IMapEntity findMapEntity(@Nonnull String key)
    {
        return mapEntityStore.findByKey(key);
    }

    @Nonnull
    @Override
    public IMapEntity findMapEntityById(int id) throws ObjectNotFoundException
    {
        return mapEntityStore.find(id);
    }

    @Nonnull
    @Override
    public synchronized IMapEntity getOrCreateMapEntity(@Nonnull String family, @Nonnull String type,
                                                        @Nullable Consumer<IMapEntity> onCreate,
                                                        @Nullable Consumer<IMapEntity> onExisting)
    {
        IMapEntity mapEntity = findMapEntity(family, type);

        if (mapEntity == null) // Create and persist a new one
        {
            mapEntity = new MapEntity(family, type);
            getPersistService().persistEntity(mapEntity); // Get it persisted

            logger.debug("Created new MapEntity: {}", mapEntity);

            // Ensure that every MapEntity has a "manual location" structure
            if (manualLocationStructure == null)
            {
                manualLocationStructure = DataStructureBuilder.createManualLocationStructure(family, type)
                        .getStructure();
            }

            mapEntity.addStructure(manualLocationStructure);

            if (onCreate != null) onCreate.accept(mapEntity);
        } else
        {
            if (onExisting != null) onExisting.accept(mapEntity);
        }

        return mapEntity;
    }

    @Nonnull
    @Override
    public List<String> deleteMapObject(@Nonnull IMapObject mapObject)
    {
        logger.debug("Delete: {}", mapObject);

        final List<String> keys = new ArrayList<>();

        if (mapObject.isTransient())
        {
            removeFromCache(mapObject);
        } else
        {
            final var moHierarchy = new ArrayList<IMapObject>();
            getMapObjectHierarchy(mapObject, moHierarchy, keys);

            logger.debug("Deleting owned objects: {}", keys);

            moHierarchy.forEach(mo -> ownsMapObjects.remove(mo.getId()));
            getPersistService().batchDeleteMapObjects(moHierarchy)
                    .forEach(backgroundDeleteManager::add); // Queue up our background work
        }

        return keys;
    }

    /**
     * Add the MapObject and all the Map Objects owned by the given Map Object to a list, recursively.
     * Also add the unique key of each of those MOs to the key list.
     *
     * @param mo        MapObject of interest
     * @param hierarchy Updated with MO and recursive set of owned MOs
     * @param keys      Updated with unique keys of those MOs
     */
    private void getMapObjectHierarchy(IMapObject mo, List<IMapObject> hierarchy, List<String> keys)
    {
        if (ownsMapObjects.contains(mo.getId()))
        {
            for (var ownedMapObject : getOwned(mo))
            {
                keys.add(ownedMapObject.getKey()); // Add the key
                getMapObjectHierarchy(ownedMapObject, hierarchy, keys); // And recurse
            }
        }

        hierarchy.add(mo);
    }

    @Nonnull
    @Override
    public List<IMapObject> getOwned(@Nonnull IMapObject owningMapObject)
    {
        return mapObjectStore.findAllMatching(mo -> mo.getOwner() == owningMapObject);
    }

    @Nonnull
    @Override
    public IStructure findStructureById(int id)
    {
        return structureStore.find(id);
    }

    @Override
    public IStructure findStructureByHash(@Nonnull String hash)
    {
        return structureStore.findByKey(hash);
    }

    @Nonnull
    @Override
    public List<IStructure> findStructuresByMessageType(@Nonnull String messageType)
    {
        return structureStore.findAllMatching(s -> s.getMessageType().equals(messageType));
    }

    @Nonnull
    @Override
    public List<IStructure> findAllStructures()
    {
        return structureStore.findAll();
    }

    @Nonnull
    @Override
    public IPersistService getPersistService()
    {
        return persistServiceProvider.getPersistService();
    }

    /**
     * Clean out all our caches.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_PersistServiceProvider) StopProjectServices event)
    {
        logger.debug("CLEAR STORES");
        mapEntityStore.clear();
        structureStore.clear();
        mapObjectStore.clear();

        manualLocationStructure = null;
    }
}
