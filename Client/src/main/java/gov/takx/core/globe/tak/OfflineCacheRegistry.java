package gov.takx.core.globe.tak;

import gov.takx.api.event.project.ProjectClosed;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry to track the offline cache locations for imagery layers and elevation models.  A centralized registry
 * is needed to avoid two separate layers referring to the same cache location, which would lead to various
 * errors and possibly data corruption.
 *
 * @since 5.5
 */
@ApplicationScoped
public class OfflineCacheRegistry
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Map from layer name to cache file path
    private final Map<String, String> cachePathsInUse = new ConcurrentHashMap<>();

    /**
     * Register the given {@code cacheFile}'s absolute path.  If the path has already been recorded,
     * a {@link CacheConflictException} will be thrown.
     *
     * @param layerName Name of layer to register
     * @param cacheFile Cache file location to register
     * @throws CacheConflictException If the layer name or cache file path has already been recorded
     * @since 5.6, signature changed
     */
    public void registerCacheFile(@Nonnull String layerName, @Nonnull File cacheFile)
    {
        final String cacheFilePath = cacheFile.getAbsolutePath();

        if (cachePathsInUse.containsKey(layerName))
        {
            logger.warn("Duplicate layer name registered; name '{}', cache {}", layerName, cacheFilePath);
            throw new CacheConflictException(layerName, cacheFile);
        } else
        {
            if (cachePathsInUse.containsValue(cacheFilePath)) throw new CacheConflictException(layerName, cacheFile);

            cachePathsInUse.put(layerName, cacheFilePath);
        }
    }

    /**
     * Remove a cache file registration.  We have to do this by layer name since the cache file a given layer uses
     * is buried deep inside specific implementations, and isn't really accessible after the layer is initially created.
     *
     * @param layerName Name of layer to unregister
     * @since 5.6
     */
    public void unregisterCacheFile(@Nonnull String layerName)
    {
        cachePathsInUse.remove(layerName);
    }

    private void onProjectClosed(@Observes ProjectClosed ignored)
    {
        cachePathsInUse.clear();
    }
}
