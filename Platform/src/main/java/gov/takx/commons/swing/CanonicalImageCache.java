package gov.takx.commons.swing;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.util.concurrent.ExecutionException;

/**
 * This class manages a cache of {@link java.awt.image.BufferedImage} instances, keyed by the MD5 hash of the
 * image content.  This supports a number of use cases, primarily for MapObjects where the icon image could
 * potentially be shared across multiple MO instances (consider hundreds of MO instances, like radar hits, that
 * would all typically have the same icon image).
 * <p>
 * This cache performs a function very similar to {@link String#intern()} in that it supports the maintenance of a
 * canonical image instance to be shared across any number of consumers.
 *
 * @since 2.7
 */
public class CanonicalImageCache
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The default size of the cache allocated by an instance of this class.
     */
    public static final int DEFAULT_CACHE_SIZE = 2048;

    // Cache from MD5 hash to BufferedImage
    private final Cache<String, BufferedImage> imageCache;

    /**
     * Construct using the default cache size.
     *
     * @see #DEFAULT_CACHE_SIZE
     */
    public CanonicalImageCache()
    {
        this(DEFAULT_CACHE_SIZE);
    }

    /**
     * Construct with statistics recording enabled.  This is primarily for testing.
     *
     * @param recordStats Pass {@code true} to record access statistics
     */
    public CanonicalImageCache(boolean recordStats)
    {
        final CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
                .maximumSize(DEFAULT_CACHE_SIZE);

        if (recordStats) builder.recordStats();

        imageCache = builder.build();
    }

    /**
     * Construct using the given cache size.
     *
     * @param cacheSize Maximum number of images to store in the cache
     */
    public CanonicalImageCache(int cacheSize)
    {
        imageCache = CacheBuilder.newBuilder()
                .maximumSize(cacheSize)
                .build();
    }

    /**
     * We manage a cache of buffered images based on their MD5 hash.  This essentially "interns" the image in such a
     * way that no matter how the image was obtained, we always refer to a canonical instance.  This can dramatically
     * reduce memory usage when the same image is used on multiple Map Object's or when a series of icons are used
     * repeatedly.
     *
     * @param originalImage The image for which we want the canonical instance
     * @return Canonical instance
     */
    public BufferedImage getCanonicalImage(BufferedImage originalImage)
    {
        if (originalImage == null) return null;

        final String hash = ImageUtils.generateHexMD5HashFromImage(originalImage);

        try
        {
            // Either return the cached instance, or add this new image to the cache
            return imageCache.get(hash, () -> originalImage);
        } catch (ExecutionException e)
        {
            // This should never happen given we just return the original image in the callable
            logger.error("Cache Failure");
        }

        return originalImage;
    }

    /**
     * Create a scaled instance of the given original image and then return the canonical instance for that scaled image.
     *
     * @param originalImage Image to scale
     * @param size          Size to scale to
     * @return Canonical instance for that scaled image
     */
    public BufferedImage getCanonicalScaledImage(BufferedImage originalImage, int size)
    {
        return getCanonicalImage(ImageUtils.getScaledInstance(originalImage, size, size, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true));
    }

    /**
     * Create a scaled instance of the given original image, respecting the aspect ratio, but padding to the given max size
     * such that the result is square, and then return the canonical instance for that scaled image.
     *
     * @param originalImage Image to scale
     * @param size          Size to scale to
     * @return Canonical instance for that scaled image
     * @since 5.6
     */
    public BufferedImage getCanonicalScaledImageWithPadding(BufferedImage originalImage, int size)
    {
        return getCanonicalImage(ImageUtils.getScaledInstanceRespectingAspectRatioWithPadding(originalImage, size, size, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true));
    }

    /**
     * Invalidate (remove) the cache entry associated with the given image.  The MD5 hash of the image will be
     * generated, and that will be used to remove the canonical mapping from the cache.
     *
     * @param image Image for which to invalidate the cache entry
     */
    public void invalidate(BufferedImage image)
    {
        final String hash = ImageUtils.generateHexMD5HashFromImage(image);

        imageCache.invalidate(hash);
    }

    /**
     * Discards all entries in the cache.
     */
    public void invalidateAll()
    {
        imageCache.invalidateAll();
    }

    /**
     * Returns a current snapshot of this cache's cumulative statistics. All stats are initialized
     * to zero, and are monotonically increasing over the lifetime of the cache.
     */
    public CacheStats stats()
    {
        return imageCache.stats();
    }

    /**
     * @return Size of underlying cache
     */
    public long size()
    {
        return imageCache.size();
    }

    /**
     * @return {@code true} if this cache is empty (size() == 0)
     */
    public boolean isEmpty()
    {
        return imageCache.asMap().isEmpty();
    }
}
