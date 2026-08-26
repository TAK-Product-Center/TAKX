package gov.takx.core.persist;

import com.koloboke.collect.map.IntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import gov.takx.api.persist.IPersistable;
import gov.takx.commons.utils.StringUtils;
import jakarta.persistence.PersistenceException;
import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.ObjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Entity stores provide the in-memory cache of database entities.
 *
 * @since 2.29
 */
public class EntityStore<T extends IPersistable>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Lock readLock = lock.readLock();
    private final Lock writeLock = lock.writeLock();

    private final IntObjMap<T> entities = HashIntObjMaps.newMutableMap();
    private final Function<T, String> keyExtractor;
    private final Map<String, T> entitiesByKey = new HashMap<>();

    private final Map<String, Function<T, String>> secondaryKeyExtractors = new HashMap<>();
    private final Map<String, Map<String, T>> secondaryIndices = new ConcurrentHashMap<>();

    /**
     * Construct an entity store using the given key extractor function for the primary key.
     * Primary keys are not allowed to be {@code null}.
     *
     * @param keyExtractor Function to extract the primary key from an entity
     */
    public EntityStore(Function<T, String> keyExtractor)
    {
        this.keyExtractor = keyExtractor;
    }

    /**
     * Define a secondary index on the entities in this store.  A map will be maintained for the index
     * using the given keyExtractor to generate the map key with the entity as the value.
     *
     * @param indexName    Name of secondary index
     * @param keyExtractor Function to extract a String key from the entity
     * @return this, for chaining
     * @since 3.0
     */
    protected EntityStore<T> addSecondaryIndex(String indexName, Function<T, String> keyExtractor)
    {
        secondaryKeyExtractors.put(indexName, keyExtractor);
        secondaryIndices.put(indexName, new ConcurrentHashMap<>());

        return this;
    }

    public void clear()
    {
        entities.clear();
        entitiesByKey.clear();
        secondaryIndices.values().forEach(Map::clear);
    }

    public int size()
    {
        return entitiesByKey.size();
    }

    /**
     * @return {@code true} if this entity store contains no elements
     */
    public boolean isEmpty()
    {
        return entities.isEmpty();
    }

    /**
     * Determine if we have an entity with the given id in our store.
     *
     * @param id Id of entity to test
     * @return {@code true} if an entity with that id exists in the store
     */
    public boolean exists(int id)
    {
        readLock.lock();

        try
        {
            return entities.containsKey(id);
        } finally
        {
            readLock.unlock();
        }
    }

    /**
     * Get the entity with the given id.
     *
     * @param id Id of entity to retrieve
     * @return Entity, may be {@code null} if this entity has been deleted
     * @throws ObjectNotFoundException If no entity has that id
     */
    public T find(int id) throws ObjectNotFoundException
    {
        readLock.lock();

        try
        {
            final T entity = entities.get(id);

            if (entity == null) throw new ObjectNotFoundException(id, "ID=" + id);

            return entity;
        } finally
        {
            readLock.unlock();
        }
    }

    /**
     * Get the entity with the given unique key.
     *
     * @param key Unique key of entity to retrieve
     * @return Entity, may be {@code null} if this entity has been deleted
     */
    public T findByKey(String key)
    {
        return getFromIndex(entitiesByKey, key);
    }

    /**
     * Get the entity with the given secondary index key.
     *
     * @param indexName Name of index to use to locate the entity
     * @param key       Key of entity to retrieve
     * @return Entity, may be {@code null} if this entity has been deleted
     * @since 3.0
     */
    public T findByKey(String indexName, String key)
    {
        return getFromIndex(secondaryIndices.get(indexName), key);
    }

    /**
     * Get the entity with the given key from the given index.
     *
     * @param index Index from which to retrieve the entity
     * @param key   Key of entity to retrieve
     * @return Entity, may be {@code null} if this entity has been deleted or doesn't exist
     * @since 3.0
     */
    protected T getFromIndex(Map<String, T> index, String key)
    {
        readLock.lock();

        try
        {
            return index.get(key);
        } finally
        {
            readLock.unlock();
        }
    }

    /**
     * @return A copy of all the entities in our store
     */
    public List<T> findAll()
    {
        readLock.lock();

        try
        {
            return new ArrayList<>(entities.values());
        } finally
        {
            readLock.unlock();
        }
    }

    /**
     * Find all the entities that match the given matcher.
     *
     * @param matcher Matcher to test entities for inclusion in results
     * @return List of all entities that match
     */
    public List<T> findAllMatching(Predicate<T> matcher)
    {
        readLock.lock();

        try
        {
            return entities.values().stream().filter(matcher).collect(Collectors.toList());
        } finally
        {
            readLock.unlock();
        }
    }

    /**
     * Add all the given entities to the store.
     *
     * @since 3.2
     */
    public void addAll(Collection<? extends T> entities)
    {
        writeLock.lock();

        try
        {
            entities.forEach(this::addUnderLock);
        } finally
        {
            writeLock.unlock();
        }
    }

    /**
     * Add an entity to the store, updating all defined indices.
     *
     * @param entity Entity to add using its current id
     */
    public void add(T entity)
    {
        writeLock.lock();

        try
        {
            addUnderLock(entity);
        } finally
        {
            writeLock.unlock();
        }
    }

    /**
     * Add an entity to the store, updating all defined indices.  The write-lock must be held when calling this method.
     *
     * @since 3.2
     */
    private void addUnderLock(T entity)
    {
        final Integer id = entity.getId();

        if (id == null) throw new PersistenceException("NULL Id on: " + entity);

        if (logger.isDebugEnabled())
        {
            logger.debug("ADD @id {} : {}: {}", id, ObjectUtils.identityToString(entity), entity);
        }

        entities.put((int) id, entity);

        // Update the primary and secondary indices
        updateIndex(entitiesByKey, entity, keyExtractor, "primary");

        secondaryKeyExtractors.forEach((indexName, keyExtractor) -> updateIndex(secondaryIndices.get(indexName), entity, keyExtractor, indexName));
    }

    /**
     * Update the given index by adding the entity under the given key.
     *
     * @param index        Index to update
     * @param entity       Entity to add to index
     * @param keyExtractor Function to extract a String key from the entity
     * @param indexName    Name of the index
     * @since 3.0
     */
    private void updateIndex(Map<String, T> index, T entity, Function<T, String> keyExtractor, String indexName)
    {
        final String key = keyExtractor.apply(entity);

        if (!StringUtils.isEmpty(key))
        {
            final T previous = index.putIfAbsent(key, entity);

            // Only warn if the object wasn't replacing itself
            if (previous != null && previous != entity)
            {
                logger.error("Duplicate {} key: '{}' on {}", indexName, key, entity.getClass().getSimpleName());
            }
        } else
        {
            logger.debug("NULL {} key from {}", indexName, entity);
        }
    }

    /**
     * Remove the given entity.
     *
     * @param entity Entity to delete
     * @return Deleted entity
     */
    public T remove(T entity)
    {
        return remove(entity.getId());
    }

    /**
     * Remove entity with the given id.
     *
     * @param id Id of entity to delete
     * @return Deleted entity
     */
    public T remove(int id)
    {
        writeLock.lock();

        T entity;
        try
        {
            entity = entities.remove(id);
            if (entity != null)
            {
                // Cleanup primary and secondary indices
                entitiesByKey.remove(keyExtractor.apply(entity));

                secondaryKeyExtractors.forEach((keyName, keyExtractor) -> {
                    final String key = keyExtractor.apply(entity);

                    if (key != null) secondaryIndices.get(keyName).remove(key);
                });
            }
        } finally
        {
            writeLock.unlock();
        }

        return entity;
    }
}
