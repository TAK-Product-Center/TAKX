package gov.takx.platform.utils;

import gov.nasa.worldwind.util.ImageUtil;
import gov.nasa.worldwind.util.WWMath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;

/**
 * The PowerOfTwoPaddedImage class converts images with non-power-of-two dimensions to images with power-of-two
 * dimensions. The original image is copied into a power-of-two image, where any pixels not covered by the original
 * image are completely transparent. This is a useful property when converting images to OpenGL textures.
 * Non-power-of-two textures are handled inconsistently by graphics hardware. Not all hardware supports them, and many
 * that do lack full support for the texturing functionality available for power-of-two textures.
 * <p/>
 * PowerOfTwoPaddedImage provides accessors to the converted power-of-two image, the power-of-two image's width and
 * height, and the original image's width and height.
 *
 * @author dcollins
 * @version $Id: PowerOfTwoPaddedImage.java 12848 2009-12-01 01:25:12Z dcollins $
 */
public class PowerOfTwoPaddedImage
{
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    protected BufferedImage image;
    protected int width;
    protected int height;

    protected PowerOfTwoPaddedImage(BufferedImage image, int width, int height)
    {
        if (image == null)
        {
            logger.error("Image is null.");
            throw new IllegalArgumentException("Image is null.");
        }

        if (width <= 0)
        {
            logger.error("Invalid geometry value: width.");
            throw new IllegalArgumentException("Invalid geometry value: width.");
        }

        if (height <= 0)
        {
            logger.error("Invalid geometry value: height.");
            throw new IllegalArgumentException("Invalid geometry value: height.");
        }

        this.image = image;
        this.width = width;
        this.height = height;
    }

    /**
     * Returns a new PowerOfTwoPaddedImage by converting the specified {@code image} to a new image with
     * power-of-two dimensions. Any pixels not covered by the original image are completely transparent. If the
     * specified {@code image} has power-of-two dimensions, this maintains a reference to the original image
     * instead of creating a copy. However, if the specified {@code image} has no alpha channel, this creates a
     * copy of the original image with an alpha channel, regardless of the specified {@code image's} dimensions.
     * This guarantees that the method {@link #getPowerOfTwoImage()} always returns a BufferedImage with an alpha
     * channel. This is a useful property when converting images to OpenGL texture's, when both power-of-two and
     * non-power-of-two textures must have an alpha channel for consistent handling.
     *
     * @param image the BufferedImage to convert to an image with power-of-two dimensions.
     * @return a new PowerOfTwoPaddedImage representing a power-of-two copy of the specified {@code image}.
     * @throws IllegalArgumentException if the image is {@code null}.
     */
    public static PowerOfTwoPaddedImage fromBufferedImage(BufferedImage image)
    {
        if (image == null)
        {
            logger.error("Image is null.");
            throw new IllegalArgumentException("Image is null.");
        }

        BufferedImage potImage = image;

        // Create a new image with power of two dimensions and an alpha channel. If the original image has non power
        // of two dimensions, or if it does not have alpha channel, it won't display correctly as an Annotation
        // background image.
        if (!WWMath.isPowerOfTwo(image.getWidth()) || !WWMath.isPowerOfTwo(image.getHeight())
                || image.getTransparency() == Transparency.OPAQUE)
        {
            int potWidth = WWMath.powerOfTwoCeiling(image.getWidth());
            int potHeight = WWMath.powerOfTwoCeiling(image.getHeight());

            potImage = ImageUtil.createCompatibleImage(potWidth, potHeight, BufferedImage.TRANSLUCENT);
            Graphics2D g2d = potImage.createGraphics();
            try
            {
                g2d.drawImage(image, 0, 0, null);
            } finally
            {
                g2d.dispose();
            }
        }

        return new PowerOfTwoPaddedImage(potImage, image.getWidth(), image.getHeight());
    }

    /**
     * Returns the width of the original non-power-of-two image, in pixels.
     *
     * @return the original image's width, in pixels.
     */
    public int getOriginalWidth()
    {
        return this.width;
    }

    /**
     * Returns the height of the original non-power-of-two image, in pixels.
     *
     * @return the original image's height, in pixels.
     */
    public int getOriginalHeight()
    {
        return this.height;
    }

    /**
     * Returns a copy of the original image as a {@link java.awt.image.BufferedImage} with power-of-two dimensions. Any
     * pixels not covered by the original image are completely transparent.
     *
     * @return a copy of the original image as a BufferedImage.
     */
    public BufferedImage getPowerOfTwoImage()
    {
        return this.image;
    }

    /**
     * Returns the width of the power-of-two image, in pixels.
     *
     * @return the power-of-two image's width, in pixels.
     */
    public int getPowerOfTwoWidth()
    {
        return this.image.getWidth();
    }

    /**
     * Returns the height of the power-of-two image, in pixels.
     *
     * @return the power-of-two image's height, in pixels.
     */
    public int getPowerOfTwoHeight()
    {
        return this.image.getHeight();
    }
}
