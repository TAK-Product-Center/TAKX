package gov.takx.core.milsym;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.IMilSymIconCache;
import gov.takx.api.milsym.MilSymIconScaling;
import gov.takx.commons.swing.ImageUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Cache for MIL-STD-2525 graphics built by a {@link IMilStd2525Service} instance.
 *
 * @since 5.1
 */
public class MilSymIconCache implements IMilSymIconCache
{
    private static final BufferedImage UNKNOWN_IMAGE = ImageUtils.getDefaultImage();
    private final Cache<String, BufferedImage> symbolCache = CacheBuilder.newBuilder().expireAfterAccess(15, TimeUnit.MINUTES).build();
    private final Set<String> failedSymbolCache = ConcurrentHashMap.newKeySet();

    /**
     * New symbol icon cache.
     */
    MilSymIconCache()
    {
        // Package-private to limit who can create a cache to service implementations.
    }

    @Override
    public void put(@Nonnull String code, int resolution, @Nonnull MilSymIconScaling iconScaling, @Nonnull BufferedImage image)
    {
        symbolCache.put(key(code, resolution, iconScaling), image);
    }

    @Override
    public void setFailure(@Nonnull String code)
    {
        failedSymbolCache.add(code);
    }

    @Override
    @Nullable
    public BufferedImage lookup(@Nonnull String code, int resolution, @Nonnull MilSymIconScaling iconScaling)
    {
        return symbolCache.getIfPresent(key(code, resolution, iconScaling));
    }

    @Override
    public boolean isCached(@Nullable String code)
    {
        return code != null && (isFailure(code) || isSuccess(code));
    }

    @Override
    public boolean isFailure(@Nullable String code)
    {
        return code == null || failedSymbolCache.contains(code);
    }

    @Override
    public boolean isSuccess(@Nullable String code)
    {
        return code != null && symbolCache.getIfPresent(code) != null;
    }

    @Override
    @Nonnull
    public BufferedImage getUnknownImage()
    {
        return UNKNOWN_IMAGE;
    }

    @Nonnull
    private static String key(@Nonnull String code, int resolution, @Nonnull MilSymIconScaling iconScaling)
    {
        return resolution + ":" + iconScaling.name() + ":" + code;
    }
}
