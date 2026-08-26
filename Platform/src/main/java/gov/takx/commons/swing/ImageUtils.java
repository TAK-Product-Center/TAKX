package gov.takx.commons.swing;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.google.common.annotations.VisibleForTesting;
import gov.takx.api.annotation.Internal;
import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 */
@SuppressWarnings({"unused", "SameParameterValue", "NonFinalStaticVariableUsedInClassInitialization", "NonConstantFieldWithUpperCaseName"})
public final class ImageUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String DEFAULT_IMAGE_FILE_NAME = "unknown.png";

    public static final int SMALL_ICON_WIDTH = 16;
    public static final int SMALL_ICON_HEIGHT = 16;
    public static final int MEDIUM_ICON_WIDTH = 24;
    public static final int MEDIUM_ICON_HEIGHT = 24;
    public static final int DEFAULT_LED_WIDTH = 28;
    public static final int DEFAULT_LED_HEIGHT = 28;
    public static final int DEFAULT_ICON_WIDTH = 32;
    public static final int DEFAULT_ICON_HEIGHT = 32;
    public static final int FEATURE_ICON_WIDTH = 48;
    public static final int FEATURE_ICON_HEIGHT = 48;
    public static final int LARGE_ICON_WIDTH = 128;
    public static final int LARGE_ICON_HEIGHT = 128;

    private static final ThreadLocal<ByteArrayOutputStream> BYTE_STREAMS = ThreadLocal.withInitial(() -> new ByteArrayOutputStream(4096));

    private static final char[] HEX_CHARS = "0123456789ABCDEF".toCharArray();

    /**
     * Relative path to default image for use in tests.
     * @since 3.4
     */
    @Internal
    @VisibleForTesting
    public static String DEFAULT_IMAGE_PATH;

    /**
     * Default image (in JAR URI nomenclature).
     */
    public static URI DEFAULT_IMAGE_URI;

    /**
     * @since 2.37
     */
    public static URL DEFAULT_IMAGE_URL;

    private static BufferedImage defaultImage;

    static
    {
        ImageIO.setUseCache(false); // Avoid using a disk based cache (keep it in memory)

        try
        {
            // WARNING: Don't use UIResources here.  When under test, it short-circuits some calls (like getPath)
            // and returns ImageUtils.DEFAULT_IMAGE_URL, which hasn't been initialized yet, leading to an NPE
            // during this class' initialization.

            DEFAULT_IMAGE_PATH = PLATFORM_ICONS.getDirectoryPath() + DEFAULT_IMAGE_FILE_NAME;
            DEFAULT_IMAGE_URL = ImageUtils.class.getClassLoader().getResource(PLATFORM_ICONS.getDirectoryPath() + DEFAULT_IMAGE_FILE_NAME);
            if (DEFAULT_IMAGE_URL != null)
            {
                DEFAULT_IMAGE_URI = DEFAULT_IMAGE_URL.toURI();
                defaultImage = ImageIO.read(DEFAULT_IMAGE_URL);
            } else
            {
                logger.error("Missing image resource: {}", DEFAULT_IMAGE_FILE_NAME);
            }
        } catch (URISyntaxException | IOException e)
        {
            logger.error("Unable to load {}.", DEFAULT_IMAGE_FILE_NAME, e);
        }
    }

    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private ImageUtils()
    {
    }

    /**
     * This method takes an image and converts it to a MD5 hash hex string. This allows for an image comparison without having to
     * directly compare file sizes and bytes of the images.
     *
     * @param bufferedImage The image to convert to an MD5 hash hex string
     * @return The converted hex string
     */
    @Nonnull
    public static String generateHexMD5HashFromImage(@Nonnull BufferedImage bufferedImage)
    {
        StringBuilder hexString = new StringBuilder();
        try
        {
            for (byte b : MessageDigest.getInstance("MD5").digest(getRawByteArray(bufferedImage)))
            {
                hexString.append(HEX_CHARS[(b & 0xF0) >> 4]);
                hexString.append(HEX_CHARS[b & 0x0F]);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e)
        {
            logger.error("Error converting image:{}", bufferedImage, e);
        }

        return "";
    }

    /**
     * Convenience method that returns a scaled instance of the provided {@link BufferedImage} with
     * the maximum width/height scaled to fit the given target size. The image width/height ratio is kept.
     *
     * @param image      The original image to be scaled.
     * @param targetSize Maximum dimension size (width or height, whichever is larger).
     * @return A scaled image where the largest dimension (width or height) is scaled down to the given size.
     * @since 5.1
     */
    @Nonnull
    public static BufferedImage getScaledInstanceToMaxSize(@Nonnull BufferedImage image, int targetSize)
    {
        // Auto-scale so the maximum dimension is 'size'.
        int width = image.getWidth();
        int height = image.getHeight();
        if (width < height)
        {
            // Skip if already scaled to fit.
            return height == targetSize ? image : getScaledInstance(image, -1, targetSize);
        } else if (width > height)
        {
            // Skip if already scaled to fit.
            return width == targetSize ? image : getScaledInstance(image, targetSize, -1);
        }
        return getScaledInstance(image, targetSize, targetSize);
    }

    /**
     * Convenience method that returns a scaled instance of the provided {@link BufferedImage} with
     * the minimum width/height scaled to fit the given target size. The image width/height ratio is kept.
     *
     * @param image      The original image to be scaled.
     * @param targetSize Minimum dimension size (width or height, whichever is smaller).
     * @return A scaled image where the smallest dimension (width or height) is scaled up to the given size.
     * @since 5.5
     */
    @Nonnull
    public static BufferedImage getScaledInstanceToMinSize(@Nonnull BufferedImage image, int targetSize)
    {
        // Auto-scale so the minimum dimension is 'size'.
        int width = image.getWidth();
        int height = image.getHeight();
        if (width > height)
        {
            // Skip if already scaled to fit.
            return height == targetSize ? image : getScaledInstance(image, -1, targetSize);
        } else if (width < height)
        {
            // Skip if already scaled to fit.
            return width == targetSize ? image : getScaledInstance(image, targetSize, -1);
        }
        return getScaledInstance(image, targetSize, targetSize);
    }

    /**
     * Convenience method that returns a scaled instance of the provided {@code BufferedImage} with the same width and
     * height.
     *
     * @param image      the original image to be scaled
     * @param targetSize the desired width and height of the scaled instance, in pixels
     * @return a scaled version of the original {@code BufferedImage}
     * @since 3.0
     */
    @Nonnull
    public static BufferedImage getScaledInstance(@Nonnull BufferedImage image, int targetSize)
    {
        return getScaledInstance(image, targetSize, targetSize);
    }

    /**
     * Convenience method that returns a scaled instance of the provided {@code BufferedImage} using the default
     * rendering hint and quality.
     *
     * @param image      the original image to be scaled
     * @param targetWidth   the desired width of the scaled instance, in pixels
     * @param targetHeight  the desired height of the scaled instance, in pixels
     * @return a scaled version of the original {@code BufferedImage}
     * @since 3.3
     */
    @Nonnull
    public static BufferedImage getScaledInstance(@Nonnull BufferedImage image, int targetWidth, int targetHeight)
    {
        return getScaledInstance(image, targetWidth, targetHeight, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
    }

    /**
     * Convenience method that returns a scaled instance of the provided {@code BufferedImage}.
     *
     * @param image         the original image to be scaled
     * @param targetWidth   the desired width of the scaled instance, in pixels
     * @param targetHeight  the desired height of the scaled instance, in pixels
     *                      if the value is -1 then the height will be scaled proportionally to the width
     * @param hint          one of the rendering hints that corresponds to {@code RenderingHints.KEY_INTERPOLATION}
     *                      (e.g. {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR}, {@code
     *                      RenderingHints.VALUE_INTERPOLATION_BILINEAR}, {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if true, this method will use a multi-step scaling technique that provides higher quality
     *                      than the usual one-step technique (only useful in downscaling cases, where {@code
     *                      targetWidth} or {@code targetHeight} is smaller than the original dimensions, and generally
     *                      only when the {@code BILINEAR} hint is specified)
     * @return a scaled version of the original {@code BufferedImage}
     */
    @Nonnull
    public static BufferedImage getScaledInstance(@Nonnull BufferedImage image, int targetWidth, int targetHeight,
                                                  @Nonnull Object hint, boolean higherQuality)
    {
        // Use a proportional height if the parameter is a -1
        if (targetHeight == -1)
        {
            targetHeight = (int) (image.getHeight() * ((double) targetWidth) / image.getWidth());
        } else if (targetWidth == -1)
        {
            targetWidth = (int) (image.getWidth() * ((double) targetHeight) / image.getHeight());
        }

        // Short-circuit if the size is not changing
        if (targetWidth == image.getWidth() && targetHeight == image.getHeight()) return image;

        int type = (image.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        int w;
        int h;
        if (higherQuality)
        {
            // Use multi-step technique: start with original size, then scale down in multiple passes with drawImage()
            // until the target size is reached
            w = image.getWidth();
            h = image.getHeight();
        } else
        {
            // Use one-step technique: scale directly from original size to target size with a single drawImage() call
            w = targetWidth;
            h = targetHeight;
        }

        BufferedImage ret = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = createHighQualityGraphics(ret);
        g2.drawImage(image, 0, 0, w, h, null);
        g2.dispose();

        if (!higherQuality) return ret;
        BufferedImage tmp;
        do
        {
            if (w != targetWidth) w = Math.max(targetWidth, w / 2);
            if (h != targetHeight) h = Math.max(targetHeight, h / 2);

            tmp = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            g2 = createHighQualityGraphics(tmp);
            g2.drawImage(ret, 0, 0, w, h, null);
            g2.dispose();
            ret = tmp;
            if (w == 1 || h == 1) break;
        } while (w != targetWidth || h != targetHeight);

        return ret;
    }

    /**
     * Create a {@link Graphics2D} object with high-quality rendering hints set
     * 
     * @param bi The {@link BufferedImage} to create the graphics object for
     * @return A graphics object with various high-quality flags set
     * 
     * @since 5.4
     */
    public static Graphics2D createHighQualityGraphics(BufferedImage bi)
    {
        Graphics2D g = bi.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        return g;
    }

    /**
     * Get an icon for the provided image.
     *
     * @param image  The image to use for the icon
     * @param width  The width for the scaled icon
     * @param height The height for the scaled icon
     * @return new icon at the given size.
     * @since 3.3
     */
    @Nonnull
    public static ImageIcon getScaledIcon(@Nonnull BufferedImage image, int width, int height)
    {
        return new ImageIcon(getScaledInstance(image, width, height));
    }

    /**
     * Get an icon for the provided file path.
     *
     * @param filePath The file path of the image to use for the icon
     * @param width  The width for the scaled icon
     * @param height The height for the scaled icon
     * @param defaultOnError Whether to return the default image icon if an error occurs
     * @return new icon at the given size. If failure occurs and defaultOnError is true, the default image is returned.
     * Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    public static ImageIcon getScaledIcon(@Nonnull String filePath, int width, int height, boolean defaultOnError)
    {
        return getScaledIcon(new File(filePath), width, height, defaultOnError);
    }

    /**
     * Get an icon for the provided file.
     *
     * @param file   The file containing the image to use for the icon
     * @param width  The width for the scaled icon
     * @param height The height for the scaled icon
     * @param defaultOnError Whether to return the default image icon if an error occurs
     * @return new icon at the given size. If failure occurs and defaultOnError is true, the default image is returned.
     * Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    public static ImageIcon getScaledIcon(@Nonnull File file, int width, int height, boolean defaultOnError)
    {
        final BufferedImage image = getImage(file, defaultOnError);
        final BufferedImage scaledImage = image != null ? getScaledInstance(image, width, height) : null;
        return scaledImage == null ? null : new ImageIcon(scaledImage);
    }

    /**
     * @param size Desired size of icon
     * @return an icon from the default image, scaled to the specified width and height.
     * @since 3.3
     */
    @Nonnull
    public static ImageIcon getScaledDefaultIcon(@Nonnull Dimension size)
    {
        return getScaledDefaultIcon(size.width);
    }

    /**
     * @param size Desired width &amp; height of icon, in pixels
     * @return an icon from the default image, scaled to the specified size.
     * @since 3.3
     */
    @Nonnull
    public static ImageIcon getScaledDefaultIcon(int size)
    {
        return new ImageIcon(getScaledDefaultImage(size));
    }

    /**
     * @param size Desired size of image
     * @return the default image, scaled to the specified width and height.
     * @since 3.3
     */
    @Nonnull
    public static BufferedImage getScaledDefaultImage(@Nonnull Dimension size)
    {
        return getScaledDefaultImage(size.width);
    }

    /**
     * @param size Desired width &amp; height of image, in pixels
     * @return the default image, scaled to the specified size.
     * @since 3.3
     */
    @Nonnull
    public static BufferedImage getScaledDefaultImage(int size)
    {
        return getScaledInstance(getDefaultImage(), size, size, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
    }

    /**
     * @return The default icon (from {@value #DEFAULT_IMAGE_FILE_NAME})
     * @since 3.3 (renamed from getDefaultIcon to better reflect return value)
     */
    @Nonnull // This would only be null in the very rare case that we couldn't even find our unknown.png
    public static BufferedImage getDefaultImage()
    {
        return defaultImage;
    }

    /**
     * Transforms the image to cap off color values to those defined in the given color.
     * For instance, given a completely white image, using {@link Color#RED} will cap off
     * the green and blue values to {@code 0} while keeping the red channel, making the
     * image red. If the image was completely green, then the resulting image would be
     * black since there is no value in the red channel in a strictly green image.
     *
     * @param image Image to transform.
     * @param color Color to mask with bitwise AND, acting as a cap for each red, green, blue, and alpha components of the color.
     * @return Copy of the transformed image.
     * @since 3.5
     */
    public static BufferedImage applyColorMask(@Nonnull BufferedImage image, @Nonnull Color color)
    {
        int rgbMask = color.getRGB();
        return transform(image, (img, x, y, argb) -> argb & rgbMask);
    }

    /**
     * Applies a transformation to the given image, returning the modified image.
     *
     * @param image     Image to transform.
     * @param transform Transformer to apply on a per-pixel basis.
     * @return Copy of image with transformation applied.
     * @since 3.5
     */
    @Nonnull
    public static BufferedImage transform(@Nonnull BufferedImage image, @Nonnull ImageTransform transform)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage copy = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                int color = image.getRGB(x, y);
                int transformed = transform.mapPixel(image, x, y, color);
                copy.setRGB(x, y, transformed);
            }
        }
        return copy;
    }

    /**
     * Creates a copy of the provided image with the specified opacity
     *
     * @param image   The image to copy
     * @param opacity The desired opacity
     * @return The image with the specified opacity
     */
    @Nonnull
    public static BufferedImage createTransparentImage(@Nonnull BufferedImage image, float opacity)
    {
        GraphicsConfiguration gc = image.createGraphics().getDeviceConfiguration();
        BufferedImage out = gc.createCompatibleImage(image.getWidth(), image.getHeight(), Transparency.TRANSLUCENT);
        Graphics2D g2d = out.createGraphics();
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g2d.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g2d.dispose();
        return out;
    }

    /**
     * Converts the provided image to a buffered images using the provided color model
     *
     * @param image      The image to convert
     * @param colorModel The color model for the image
     * @return The buffered image representing the provided image
     */
    @Nonnull
    public static BufferedImage toBufferedImage(@Nonnull Image image, @Nonnull ColorModel colorModel)
    {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        boolean alphaPremultiplied = colorModel.isAlphaPremultiplied();
        WritableRaster raster = colorModel.createCompatibleWritableRaster(w, h);
        BufferedImage result = new BufferedImage(colorModel, raster, alphaPremultiplied, null);
        Graphics2D g = result.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.dispose();
        return result;
    }

    /**
     * Creates a new image icon from the provided image with the specified size and fill color
     *
     * @param iconImage The original image
     * @param size      The size for the scaled image icon
     * @param fillColor The fill color
     * @return The new ImageIcon with the specified size and fill color
     */
    @Nonnull
    public static ImageIcon getNewIconImage(@Nonnull BufferedImage iconImage, int size, @Nonnull Color fillColor)
    {
        return new ImageIcon(getScaledInstance(iconImage, size, fillColor));
    }

    /**
     * Creates a scaled version of the provided image with the specified size and fill color
     *
     * @param originalImage The original image
     * @param size          The size for the scaled image icon
     * @param fillColor     The fill color
     * @return The new buffered image with the specified size and fill color
     */
    @Nonnull
    public static BufferedImage getScaledInstance(@Nonnull BufferedImage originalImage, int size, @Nonnull Color fillColor)
    {
        BufferedImage image = getScaledInstance(originalImage, size, size, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
        return getRecoloredInstance(originalImage, fillColor);
    }

    /**
     * Creates a version of the provided image with the specified fill color.
     *
     * @param originalImage The original image
     * @param fillColor The fill color to apply to the image
     * @return The new buffered image with the specified fill color
     * @since 3.4
     */
    @Nonnull
    public static BufferedImage getRecoloredInstance(@Nonnull BufferedImage originalImage, @Nonnull Color fillColor)
    {
        // Create a copy of the original image to avoid recoloring it
        final BufferedImage newImage = toBufferedImage(originalImage, originalImage.getColorModel());
        int height = newImage.getHeight();
        int width = newImage.getWidth();
        final int fillRGB = fillColor.getRGB();

        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                final int alpha = (newImage.getRGB(x, y) >> 24) & 0xff;

                // If more than 50% opaque, substitute our fill color
                if (alpha > 127) newImage.setRGB(x, y, fillRGB);
            }
        }

        newImage.flush();
        return newImage;
    }

    /**
     * Overlays an image onto the upper left or right corner of the base image.
     *
     * @param baseImageURL The URL of the base image.
     * @param overlayURL   The URL of the overlay image. This is expected to be a square image.
     * @param left         {@code true} indicates that the image should be drawn on the left.
     * @return The original image with the overlay image overlaid onto its upper left or right corner or {@code null} if
     * an error occurred reading the base image URL or the overlay URL.
     * @since 1.14
     */
    @Nullable
    public static BufferedImage getOverlayedImage(@Nonnull URL baseImageURL, @Nonnull URL overlayURL, boolean left)
    {
        BufferedImage resultImage = getImage(baseImageURL, false);
        if (resultImage == null) return null;

        int sideLength = (int) Math.round(resultImage.getWidth() / 2.0);

        final BufferedImage overlayImage = getImage(overlayURL, false);
        if (overlayImage == null) return null;

        BufferedImage scaledOverlayImage = getScaledInstance(
                overlayImage,
                sideLength,
                sideLength,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);

        Graphics2D g = resultImage.createGraphics();
        g.drawImage(scaledOverlayImage, left ? 0 : resultImage.getWidth() - scaledOverlayImage.getWidth(), 0, null);
        g.dispose();

        return resultImage;
    }

    /**
     * Get the bytes from a {@code BufferedImage}.  The result can not be used to re-constitute the image, at least
     * not as far I can determine.  But, it is useful for determining equivalence of two images.  In the case
     * that the underlying raster's data buffer is already a byte array, this method performs about 10,000 times
     * faster than {@link #toByteArray(BufferedImage)}.
     *
     * @param image The BufferedImage to be converted to a byte[]
     * @return A byte array that represents the png of the given image
     * @since 2.27
     */
    @Nonnull
    private static byte[] getRawByteArray(@Nonnull BufferedImage image)
    {
        final DataBuffer dataBuffer = image.getRaster().getDataBuffer();
        final int dataType = dataBuffer.getDataType();

        // Custom handling for the case where the data buffer is already a simple byte or int array.
        // This optimization makes obtaining the image bytes 10,000 times faster.
        if (dataBuffer instanceof DataBufferByte dataBufferByte)
        {
            return dataBufferByte.getData();
        } else if (dataBuffer instanceof DataBufferInt dataBufferInt)
        {
            final var intArray = dataBufferInt.getData();
            final var byteBuffer = ByteBuffer.allocate(intArray.length * Integer.BYTES);
            byteBuffer.asIntBuffer().put(intArray);
            return byteBuffer.array();
        } else
        {
            return toByteArray(image);
        }
    }

    /**
     * Converts a BufferedImage to a byte array encoded in PNG format.
     *
     * @param image The BufferedImage to be converted to a byte[]
     * @return A byte array that represents the png of the given image
     */
    @Nonnull
    public static byte[] toByteArray(@Nonnull BufferedImage image)
    {
        final ByteArrayOutputStream baos = BYTE_STREAMS.get();
        baos.reset();

        try
        {
            ImageIO.write(image, "png", baos);
        } catch (IOException e)
        {
            logger.error("Error converting image to byte array", e);
        }
        return baos.toByteArray();
    }

    /**
     * Converts a BufferedImage to a byte array.  The BufferedImage is first converted to a png of the desired size
     * (width and height) in pixels and then to a byte array to optimize the size of the array.
     *
     * @param image The BufferedImage to be converted to a byte[]
     * @param size  The icon size to convert the image to (result will be a square image)
     * @return A byte array that represents the png of the image for the given size
     */
    @Nonnull
    public static byte[] toByteArray(@Nonnull BufferedImage image, int size)
    {
        return toByteArray(getScaledInstance(image, size, size, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true));
    }

    /**
     * Converts a byte array to a BufferedImage
     *
     * @param byteArray The byte array that holds a png version of an image
     * @return the BufferedImage created from the byte array
     */
    @Nullable
    public static BufferedImage toBufferedImage(@Nonnull byte[] byteArray)
    {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(byteArray))
        {
            return ImageIO.read(inputStream);
        } catch (IOException e)
        {
            logger.error("Error converting byte array to image", e);
        }
        return null;
    }

    /**
     * Writes the buffered image to the given file as a png
     *
     * @param image image to write to file as a png
     * @param file  file to be written to
     * @return whether the write operation was successful
     */
    public static boolean writeBufferedImageToPNGFile(@Nonnull BufferedImage image, @Nonnull File file)
    {
        try
        {
            ImageIO.write(image, "png", file);
        } catch (IOException e)
        {
            logger.error("Error writing image to PNG: {}", file.getAbsolutePath(), e);
            return false;
        }
        return true;
    }

    /**
     * Write an image to a canonically named file (based on the MD5 hash of the image content) in the given directory.
     * The image will be written as a PNG.
     * <p>
     * If the file already exists, then the URI for that existing file is returned, the file is not updated since
     * the name of the file is based on the content.
     *
     * @param dir   The directory in which to write the image
     * @param image The image to write
     * @return URI of the file
     * @since 2.11
     */
    @Nonnull
    public static URI writeImageToCanonicalFile(@Nonnull File dir, @Nonnull BufferedImage image)
    {
        final var file = new File(dir, generateHexMD5HashFromImage(image) + ".png");

        // This file may already exist
        if (!file.exists())
        {
            // Only check the dir not the file. Otherwise, the file gets created as a dir.
            if (!dir.exists() && !dir.mkdirs()) logger.error("Failed to create directory to hold icon: {}", dir);

            writeBufferedImageToPNGFile(image, file);
        }
        return file.toURI();
    }

    /**
     * Resize the image to fit in a square of the specified size without distorting the image.
     *
     * @param image The image to convert into a square icon
     * @param size  The desired width and height of the icon
     * @return The image as a square ImageIcon
     * @see ImageUtils#getSquareIcon(BufferedImage, int)
     * @since 2.13.1
     */
    @Nonnull
    public static ImageIcon getSquareIcon(@Nonnull BufferedImage image, int size)
    {
        return new ImageIcon(getSquareImage(image, size));
    }

    /**
     * Resize the image to fit in a square of the specified size, filling any extra area with transparent background.
     * The new image is created using the highest known quality java2d rendering.
     *
     * @param image The image to convert into a square image
     * @param size  The desired width and height of the image
     * @return The new square image
     */
    @Nonnull
    public static BufferedImage getSquareImage(@Nonnull BufferedImage image, int size)
    {
        // Resize the image if necessary.
        int width = image.getWidth();
        int height = image.getHeight();

        if (width != size || height != size)
        {
            image =
                    getScaledInstance(
                            image,
                            width < height ? (size * width / height) : size,
                            height < width ? (size * height / width) : size,
                            RenderingHints.VALUE_INTERPOLATION_BILINEAR,
                            true);
        }

        BufferedImage result = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = result.createGraphics();
        g.drawImage(image, (size - image.getWidth()) / 2, (size - image.getHeight()) / 2, null);
        g.dispose();
        return result;
    }

    /**
     * Creates a scaled version of the provided image with the specified maximum width and height while maintaining the
     * current aspect ratio.
     *
     * @param image     The original image
     * @param maxWidth  The max width for the new image
     * @param maxHeight The max height for the new image
     * @return The scaled image
     */
    @Nonnull
    public static BufferedImage getScaledInstanceRespectingAspectRatio(@Nonnull BufferedImage image,
                                                                       double maxWidth,
                                                                       double maxHeight)
    {
        if (image.getWidth() <= maxWidth && image.getHeight() <= maxHeight) return image;

        Dimension d = getScaledRespectingAspectRatio(
                image.getWidth(),
                image.getHeight(),
                maxWidth,
                maxHeight);
        return getScaledInstance(image, d.width, d.height, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
    }

    /**
     * Creates a scaled version of the provided image, respecting the aspect ratio and padding it as necessary to match the
     * given width and height.
     *
     * @param image the original image
     * @param width the width for the new image
     * @param height the height for the new image
     * @return the scaled image
     * @since 5.6
     */
    @Nonnull
    public static BufferedImage getScaledInstanceRespectingAspectRatioWithPadding(@Nonnull BufferedImage image, int width, int height)
    {
        return getScaledInstanceRespectingAspectRatioWithPadding(image, width, height, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
    }

    /**
     * Creates a scaled version of the provided image, respecting the aspect ratio and padding it as necessary to match the
     * given width and height.
     *
     * @param image the original image
     * @param width the width for the new image
     * @param height the height for the new image
     * @param hint one of the rendering hints that corresponds to {@code RenderingHints.KEY_INTERPOLATION} (e.g.
     * {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR}, {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
     * {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
     * @param higherQuality if {@code true}, this method will use a multistep scaling technique that provides higher quality
     * than the usual one-step technique (only useful in downscaling cases, where {@code targetWidth} or {@code targetHeight}
     * is smaller than the original dimensions, and generally only when the {@code BILINEAR} hint is specified)
     * @return the scaled image
     * @since 5.6
     */
    @Nonnull
    public static BufferedImage getScaledInstanceRespectingAspectRatioWithPadding(@Nonnull BufferedImage image, int width, int height, @Nonnull Object hint, boolean higherQuality)
    {
        Dimension d = getScaledRespectingAspectRatio(
                image.getWidth(),
                image.getHeight(),
                width,
                height);
        BufferedImage scaledImage = getScaledInstance(image, d.width, d.height, hint, higherQuality);
        if (scaledImage.getWidth() != width || scaledImage.getHeight() != height)
        {
            BufferedImage paddedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = paddedImage.createGraphics();
            int xOffset = (width - scaledImage.getWidth()) / 2;
            int yOffset = (height - scaledImage.getHeight()) / 2;
            g2d.drawImage(scaledImage, xOffset, yOffset, null);
            g2d.dispose();
            scaledImage = paddedImage;
        }
        return scaledImage;
    }

    /**
     * This code was taken from:
     * <a href="http://stackoverflow.com/questions/10245220/java-image-resize-maintain-aspect-ratio">Stack Overflow</a>
     * <p>
     * Get a scaled dimension from dimensions, while respecting the original aspect ratio.
     *
     * @param originalWidth  original width image.
     * @param originalHeight original height of image.
     * @param maxWidth       bounding width (largest possible scaled width).
     * @param maxHeight      bounding height (largest possible scaled height).
     * @return a scaled dimension for the buffered image, respecting the image's aspect ratio.
     */
    @Nonnull
    public static Dimension getScaledRespectingAspectRatio(int originalWidth,
                                                           int originalHeight,
                                                           double maxWidth,
                                                           double maxHeight)
    {
        double newWidth = originalWidth;
        double newHeight = originalHeight;

        if (originalWidth <= maxWidth && originalHeight <= maxHeight)
        {
            return new Dimension(originalWidth, originalHeight);
        }

        // first check if we need to scale width
        if (originalWidth > maxWidth)
        {
            // scale width to fit
            newWidth = maxWidth;
            // scale height to maintain aspect ratio
            newHeight = (newWidth * originalHeight) / originalWidth;
        }

        // then check if we need to scale even with the new height
        if (newHeight > maxHeight)
        {
            // scale height to fit instead
            newHeight = maxHeight;
            // scale width to maintain aspect ratio
            newWidth = (newHeight * originalWidth) / originalHeight;
        }

        return new Dimension((int) newWidth, (int) newHeight);
    }

    /**
     * Return a color that is brighter than the given color by the specified factor.  Basically, the RGB
     * components of the color are all divided by the factor, clamping at 255.
     * <p>
     * Notes from the 2D discussion group:
     * <OL>
     * <li>black.brighter() should return grey</li>
     * <li>applying brighter to blue will always return blue, brighter</li>
     * <li>non pure color (non zero rgb) will eventually return white</li>
     * </OL>
     *
     * @param color  Input color
     * @param factor Factor by which to make it brighter
     * @return New color
     * @since 1.47
     */
    @Nonnull
    public static Color brighter(@Nonnull Color color, double factor)
    {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        /* From 2D group:
         * 1. black.brighter() should return grey
         * 2. applying brighter to blue will always return blue, brighter
         * 3. non pure color (non zero rgb) will eventually return white
         */
        int i = (int) (1.0 / (1.0 - factor));
        if (r == 0 && g == 0 && b == 0) return new Color(i, i, i);

        if (r > 0 && r < i) r = i;
        if (g > 0 && g < i) g = i;
        if (b > 0 && b < i) b = i;

        return new Color(Math.min((int) (r / factor), 255),
                Math.min((int) (g / factor), 255),
                Math.min((int) (b / factor), 255));
    }

    /**
     * Return a color that is darker than the given color by the specified factor.  Basically, the RGB
     * components are each multiplied by the factor, clamped to a minimum of 0.
     *
     * @param color  Input color
     * @param factor Factor by which to make it darker
     * @return New color
     * @since 1.47
     */
    @Nonnull
    public static Color darker(@Nonnull Color color, double factor)
    {
        return new Color(Math.max((int) (color.getRed() * factor), 0),
                Math.max((int) (color.getGreen() * factor), 0),
                Math.max((int) (color.getBlue() * factor), 0));
    }

    /**
     * Create a random color with the RGB channels randomly chosen between 0 and 255.
     *
     * @return a randomly generated color
     * @since 2.29
     */
    @Nonnull
    public static Color generateRandomColor()
    {
        return generateRandomColor(0, 256);
    }

    /**
     * Create a random color with the RGB channels randomly chosen between the specified {@code lowThreshold} and 255.
     *
     * @param lowThreshold low color channel threshold - all color channels created with this method will have a value
     *                     greater than or equal to this threshold.
     * @return a randomly generated color
     * @since 1.47
     */
    @Nonnull
    public static Color generateRandomColor(int lowThreshold)
    {
        return generateRandomColor(lowThreshold, 256);
    }

    /**
     * Create a random color with the RGB channels randomly chosen between the specified {@code lowThreshold} and
     * {@code highThreshold}.
     *
     * @param lowThreshold  low color channel threshold - all color channels created with this method will have a value
     *                      greater than or equal to this threshold.
     * @param highThreshold high color channel threshold - all color channels created with this method will have a value
     *                      less than or equal to this threshold.
     * @return a randomly generated color
     * @since 1.47
     */
    @Nonnull
    public static Color generateRandomColor(int lowThreshold, int highThreshold)
    {
        int range = highThreshold - lowThreshold;
        int r = (int) (Math.random() * range) + lowThreshold;
        int g = (int) (Math.random() * range) + lowThreshold;
        int b = (int) (Math.random() * range) + lowThreshold;
        return new Color(r, g, b);
    }

    /**
     * Extract meta data including EXIF from a media file.
     *
     * @param localFile The file to extract the meta-data from
     * @return A map of the meta-data key value pairs
     */
    @Nonnull
    public static Map<String, String> extractMetaData(@Nonnull File localFile)
    {
        Map<String, String> exif = new HashMap<>();
        try
        {
            final Metadata metaDataDictionary = ImageMetadataReader.readMetadata(localFile);
            for (Directory directory : metaDataDictionary.getDirectories())
            {
                for (Tag tag : directory.getTags())
                {
                    String description = Objects.requireNonNullElse(tag.getDescription(), "");
                    exif.put("[" + directory.getName() + "] " + tag.getTagName(), description);
                }
            }
        } catch (ImageProcessingException e)
        {
            if (e.getMessage().contains("not supported") || e.getMessage().contains("could not be determined"))
            {
                if (logger.isDebugEnabled()) logger.debug("{}: {}", e.getMessage(), localFile);
            } else
            {
                logger.warn("Unable to extract image metadata from: {}", localFile, e);
            }
        } catch (Exception e)
        {
            // Not all data formats are supported so just log as info
            logger.info("Unable to extract image meta data from {}", localFile);
        }
        return exif;
    }

    /**
     * Determine if a given file is readable by the Image IO subsystem.
     *
     * @param file The file to test
     * @return {@code true} if the file has a suffix that is supported by the image IO subsystem
     * @since 3.1
     */
    public static boolean isImageTypeReadable(@Nonnull File file)
    {
        // See if our file has a suffix that is supported by the image IO subsystem
        final String extension = FileUtils.getExtension(file);

        return Arrays.stream(ImageIO.getReaderFileSuffixes()).anyMatch(ext -> ext.equalsIgnoreCase(extension));
    }

    /**
     * Get a scaled icon for the provided URL.
     *
     * @param imageUrl       URL of image to load
     * @param width          Desired width of image, in pixels
     * @param height         Desired height of image, in pixels
     * @param defaultOnError Whether to return the default image icon if an error occurs
     * @return new icon at the given size. If failure occurs and defaultOnError is true, the default image is returned.
     * Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    public static ImageIcon getScaledIcon(@Nonnull URL imageUrl, int width, int height, boolean defaultOnError)
    {
        final BufferedImage image = getScaledImage(imageUrl, width, height, defaultOnError);
        return image == null ? null : new ImageIcon(image);
    }

    /**
     * Get a scaled and colorized icon for the provided URL.
     *
     * @param imageUrl the URL of image to load
     * @param width the desired width of image, in pixels
     * @param height the desired height of image, in pixels
     * @param defaultOnError whether to return the default image icon if an error occurs
     * @param color the Color to use for colorizing the icon or {@code null} for no colorization
     * @return new icon at the given size. If failure occurs and defaultOnError is true, the default image is returned.
     * Otherwise, {@code null} is returned.
     * @since 5.5
     */
    @Nullable
    public static ImageIcon getScaledColorizedIcon(@Nonnull URL imageUrl, int width, int height, boolean defaultOnError, @Nullable Color color)
    {
        BufferedImage image = getScaledImage(imageUrl, width, height, defaultOnError);
        if (image != null && color != null)
        {
            image = colorize(image, color);
        }
        return image == null ? null : new ImageIcon(image);
    }

    /**
     * Get an image from the given URL resized to the given dimensions.
     *
     * @param imageUrl       URL of image
     * @param width          Desired width of image, in pixels
     * @param height         Desired height of image, in pixels
     * @param defaultOnError Whether to return the default image if an error occurs
     * @return new image at the given size. If failure occurs and defaultOnError is true, the default image is returned.
     * Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    public static BufferedImage getScaledImage(@Nonnull URL imageUrl, int width, int height, boolean defaultOnError)
    {
        final BufferedImage image = getImage(imageUrl, defaultOnError);

        return image != null ? getScaledInstance(image, width, height, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true) : null;
    }

    /**
     * Get an image from the given URL at the original size.
     *
     * @param imageUrl       URL of image
     * @param defaultOnError Whether to return the default image if an error occurs
     * @return image at the original size. If failure occurs and defaultOnError is true, the default image is returned.
     * Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    public static BufferedImage getImage(@Nonnull URL imageUrl, boolean defaultOnError)
    {
        return getImage(imageUrl, ImageIO::read, defaultOnError);
    }

    /**
     * Get an image from the provided input stream at the original size.
     *
     * @param imageInputStream The input stream for the image
     * @param defaultOnError Whether to return the default image if an error occurs
     * @return image at the original size. If failure occurs and defaultOnError is true, the default image is returned.
     * Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    public static BufferedImage getImage(@Nonnull InputStream imageInputStream, boolean defaultOnError)
    {
        return getImage(imageInputStream, ImageIO::read, defaultOnError);
    }

    /**
     * Get an image from the provided file at the original size.
     *
     * @param imageFile The file containing the image
     * @param defaultOnError Whether to return the default image if an error occurs
     * @return image at the original size. If failure occurs and defaultOnError is true, the default image is returned.
     * Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    public static BufferedImage getImage(@Nonnull File imageFile, boolean defaultOnError)
    {
        return getImage(imageFile, ImageIO::read, defaultOnError);
    }

    /**
     * Helper method for performing the common logic to retrieve an image using {@link ImageIO}.
     *
     * @param imageSource The source of the image
     * @param imageReader The function for reading in the image from the image source
     * @param defaultOnError Whether to return the default image if an error occurs
     * @param <T> The type of the image source (i.e. URL, InputStream, File)
     * @return image at the original size. If failure occurs and defaultOnError is true, the default image is returned.
     * Otherwise, {@code null} is returned.
     * @since 3.3
     */
    @Nullable
    private static <T> BufferedImage getImage(@Nonnull T imageSource, @Nonnull ImageReader<T> imageReader, boolean defaultOnError)
    {
        try
        {
            BufferedImage image = imageReader.apply(imageSource);
            if (image == null && defaultOnError) image = getDefaultImage();
            return image;
        } catch (IOException e)
        {
            logger.error("Error initializing image {}, returning default image: {}", imageSource, defaultOnError, e);
        }

        return defaultOnError ? getDefaultImage() : null;
    }

    /**
     * Determines an images predominant color by scanning each pixel and tallying the number of times each color is used.
     * Fully transparent pixels are ignored. If an image is very large, it should be scaled before calling this method.
     *
     * @param image The image to scan
     * @param defaultColor The color to use if a predominant color cannot be determined (e.g. if the entire image is transparent)
     * @return The image's predominant color
     * @since 5.3
     */
    public static int getPredominantColor(BufferedImage image, int defaultColor)
    {
        return getPredominantColor(image, defaultColor, 1); // ignore pixels with alpha == 0
    }

    /**
     * Determines an images predominant color by scanning each pixel and tallying the number of times each color is used.
     * Pixels below the specified {@code alphaThreshold} are ignored. If an image is very large, it should be scaled before
     * calling this method.
     * 
     * @param image The image to scan
     * @param defaultColor The color to use if a predominant color cannot be determined (e.g. if no color is above the
     *                     alpha threshold)
     * @param alphaThreshold The minimum acceptable alpha value for pixels to be included in the scan on a scale from
     *                       0-255. e.g. 1 will only exclude completely transparent pixels, 255 will exclude any pixels
     *                       that aren't completely opaque
     * @return The image's predominant color
     * @since 5.4
     */
    public static int getPredominantColor(BufferedImage image, int defaultColor, int alphaThreshold)
    {
        final Map<Integer, Integer> colorCounts = new HashMap<>();

        for (int x = 0; x < image.getWidth(); x++)
        {
            for (int y = 0; y < image.getHeight(); y++)
            {
                int color = image.getRGB(x, y);
                if ((color >>> 24) < alphaThreshold) continue;
                colorCounts.put(color, colorCounts.getOrDefault(color, 0) + 1);
            }
        }

        return colorCounts.entrySet().stream()
                .max(Comparator.comparingInt(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(defaultColor);
    }

    /**
     * Modify the pixels in the given image to blend in the given color.  The BufferedImage returned may not be the
     * same as the one passed in if it does not have the required type of {@link BufferedImage#TYPE_INT_ARGB}.
     *
     * @param image     Image to process
     * @param fillColor Color to blend with source image
     * @return Updated or created BufferedImage
     * @since 5.5
     */
    @Nonnull
    public static BufferedImage colorize(@Nonnull BufferedImage image, @Nonnull Color fillColor)
    {
        // We need an ARGB color model to work with, so convert the source image if needed
        if (image.getType() != BufferedImage.TYPE_INT_ARGB)
        {
            image = toBufferedImage(image, ColorModel.getRGBdefault());
        }

        final int fillRed = fillColor.getRed();
        final int fillGreen = fillColor.getGreen();
        final int fillBlue = fillColor.getBlue();

        final int height = image.getHeight();
        final int width = image.getWidth();

        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                final int color = image.getRGB(x, y);

                final int r = (color >> 16) & 0xFF;
                final int g = (color >> 8) & 0xFF;
                final int b = (color) & 0xFF;

                // If this pixel is not grey scale (r == g == b), then don't colorize it.
                // This allows the source image to contain elements (like a red cross) that will remain unaltered.
                if (r != g || g != b) continue;

                // Scale color to 0.0 - 1.0 and then multiply with fill color components
                final float scale = r / 255f;
                final int alpha = (color >> 24) & 0xFF; // Retain source alpha

                final int newColor = (alpha << 24) |
                        (((int) (scale * fillRed) & 0xFF) << 16) |
                        (((int) (scale * fillGreen) & 0xFF) << 8) |
                        (((int) (scale * fillBlue) & 0xFF));

                image.setRGB(x, y, newColor);
            }
        }

        image.flush();
        return image;
    }

    /**
     * A functional interface to represent one of the {@link ImageIO} methods for reading in an image. This is required
     * because ImageIO throws an {@link IOException}.
     *
     * @param <T> The type of the image source
     * @since 3.3
     */
    @SuppressWarnings("NewClassNamingConvention")
    @FunctionalInterface
    public interface ImageReader<T>
    {
        BufferedImage apply(T t) throws IOException;
    }

    /**
     * Transformer for basic ARGB manipulations.
     *
     * @since 3.5
     */
    @FunctionalInterface
    public interface ImageTransform
    {
        /**
         * @param image Original image being transformed.
         * @param x     Pixel x coordinate.
         * @param y     Pixel y coordinate.
         * @param argb  Input ARGB masked value.
         * @return Mapped ARGB value.
         */
        int mapPixel(@Nonnull BufferedImage image, int x, int y, int argb);
    }
}
