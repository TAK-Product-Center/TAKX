package gov.takx.commons.swing;

import com.google.common.cache.CacheStats;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for CanonicalImageCache.
 *
 * @since 2.7
 */
public class CanonicalImageCacheTest
{
    @Test
    public void testBasicProperties()
    {
        final CanonicalImageCache def = new CanonicalImageCache();
        assertThat(def.isEmpty()).isTrue();

        final CanonicalImageCache cache = new CanonicalImageCache(true);

        assertThat(cache.isEmpty()).isTrue();
        assertThat(0).isEqualTo(cache.size());

        final BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);

        final BufferedImage canonicalImage = cache.getCanonicalImage(image);
        assertThat(canonicalImage).isSameAs(image);
        assertThat(cache.isEmpty()).isFalse();
        assertThat(cache.size()).isEqualTo(1);

        CacheStats stats = cache.stats();
        assertThat(stats.loadCount()).isEqualTo(1);
        assertThat(stats.hitCount()).isEqualTo(0);

        final BufferedImage image2 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage canonicalImage2 = cache.getCanonicalImage(image2);
        assertThat(canonicalImage2).isSameAs(image);

        stats = cache.stats();
        assertThat(stats.loadCount()).isEqualTo(1);
        assertThat(stats.hitCount()).isEqualTo(1);

        cache.invalidateAll();
        assertThat(cache.size()).isEqualTo(0);
    }

    @Test
    public void testSingleCanonicalInstance()
    {
        final CanonicalImageCache cache = new CanonicalImageCache(true);

        // Install the original image
        final BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage canonicalImage = cache.getCanonicalImage(image);
        assertThat(canonicalImage).isSameAs(image);

        // Loop and create a bunch of BI instances and ensure they all return the same canonical instance
        for (int i = 0; i < 20; i++)
        {
            final BufferedImage image2 = new BufferedImage(10, 10, BufferedImage.TYPE_INT_ARGB);
            final BufferedImage canonicalImage2 = cache.getCanonicalImage(image2);
            assertThat(canonicalImage2).isNotSameAs(image2);
            assertThat(canonicalImage2).isSameAs(canonicalImage);
        }

        CacheStats stats = cache.stats();
        assertThat(stats.loadCount()).isEqualTo(1);
        assertThat(stats.hitCount()).isEqualTo(20);
    }

    @Test
    public void testMultipleDifferentInstances()
    {
        final CanonicalImageCache cache = new CanonicalImageCache(true);
        final BufferedImage[] originalImages = new BufferedImage[10];

        // Loop and create multiple, different images and ensure they don't result in any stored canonical instance
        for (int i = 0; i < 10; i++)
        {
            final BufferedImage image = new BufferedImage(i + 1, i + 1, BufferedImage.TYPE_INT_ARGB);
            originalImages[i] = image;

            final BufferedImage canonicalImage = cache.getCanonicalImage(image);
            assertThat(canonicalImage).isSameAs(image);
        }

        CacheStats stats = cache.stats();
        assertThat(stats.loadCount()).isEqualTo(10);
        assertThat(stats.hitCount()).isEqualTo(0);

        for (int i = 0; i < 10; i++)
        {
            final BufferedImage image = new BufferedImage(i + 1, i + 1, BufferedImage.TYPE_INT_ARGB);
            final BufferedImage canonicalImage = cache.getCanonicalImage(image);
            assertThat(canonicalImage).isNotSameAs(image);
            assertThat(canonicalImage).isSameAs(originalImages[i]);
        }

        stats = cache.stats();
        assertThat(stats.loadCount()).isEqualTo(10);
        assertThat(stats.hitCount()).isEqualTo(10);
    }

    @Test
    public void testScaledCanonicalInstance()
    {
        final CanonicalImageCache cache = new CanonicalImageCache(true);

        // Install the original image
        final BufferedImage image = new BufferedImage(20, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setPaint(Color.WHITE);
        graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
        graphics.dispose();

        // This image should be all white
        final BufferedImage canonicalImage = cache.getCanonicalScaledImage(image, 40);
        assertThat(canonicalImage.getRGB(0, 0)).isEqualTo(Color.WHITE.getRGB());

        // This image should be padded
        final BufferedImage square = cache.getCanonicalScaledImageWithPadding(image, 40);
        assertThat(square.getRGB(0, 0)).isEqualTo(0);

        CacheStats stats = cache.stats();
        assertThat(stats.loadCount()).isEqualTo(2);
    }
}
