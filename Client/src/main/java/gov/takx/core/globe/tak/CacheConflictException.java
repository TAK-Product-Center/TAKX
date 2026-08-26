package gov.takx.core.globe.tak;

import jakarta.annotation.Nonnull;

import java.io.File;

/**
 * Exception indicating that multiple layers are trying to use the same offline cache location.
 *
 * @since 5.5
 */
public class CacheConflictException extends RuntimeException
{
    /**
     * @param cacheFile The cache file in conflict
     */
    public CacheConflictException(@Nonnull String layerName, @Nonnull File cacheFile)
    {
        super("Layer '" + layerName + "' cache conflict on: " + cacheFile.getAbsolutePath());
    }
}
