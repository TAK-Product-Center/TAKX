package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.graphics.ShadowRenderer;
import org.jdesktop.swingx.util.GraphicsUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Kernel;
import java.awt.image.Raster;
import java.awt.image.RescaleOp;
import java.awt.image.WritableRaster;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 */
@SuppressWarnings({"PMD.TooManyMethods", "unused"})
public final class GraphicsUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Map from color name (as defined by the fields in Color
    private static final Map<String, Color> knownColors = new ConcurrentHashMap<>();

    public enum Shape
    {
        CIRCLE(1.3D),
        GLOSSY_CIRCLE(1.3D),
        TRIANGLE(2D),
        GLOSSY_TRIANGLE(2D),
        OCTAGON(1D),
        GLOSSY_OCTAGON(1D);

        private final double scale;

        Shape(double scale)
        {
            this.scale = scale;
        }

        public double getScale()
        {
            return scale;
        }

        /**
         * Get the named Shape constant.
         *
         * @param name         Name of shape to obtain
         * @param defaultValue Default value to return if there is no shape with the requested name
         * @return Requested shape or defaultValue if not found
         * @since 2.35
         */
        @Nullable
        public static Shape forName(String name, @Nullable Shape defaultValue)
        {
            try
            {
                return valueOf(name.toUpperCase());
            } catch (IllegalArgumentException e)
            {
                logger.error("No Shape named: {}", name);
                return defaultValue;
            }
        }
    }

    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private GraphicsUtils()
    {
    }

    /**
     * Get a Color instance by finding a field on the Color class with the given name.
     *
     * @param colorName    Name of color to lookup
     * @param defaultColor Default color to return if the name is not found as a field on {@link Color}
     * @return Named color, or {@code defaultColor}
     * @since 2.35
     */
    @Nullable
    public static Color getColorByName(@Nonnull String colorName, @Nullable Color defaultColor)
    {
        return knownColors.computeIfAbsent(colorName, k -> {
            try
            {
                return (Color) Color.class.getField(colorName).get(null);
            } catch (Exception e)
            {
                logger.warn("No Color field: {}", colorName);
                return defaultColor;
            }
        });
    }

    /**
     * Returns a new translucent compatible image of the specified width and
     * height. That is, the returned {@code BufferedImage} is compatible with
     * the graphics hardware. If the method is called in a headless
     * environment, then the returned BufferedImage will be compatible with
     * the source image.
     *
     * @param width  Width of image to produce
     * @param height Height of image to produce
     * @return buffered image
     */
    @Nonnull
    private static BufferedImage createCompatibleTranslucentImage(int width, int height)
    {
        return GraphicsUtilities.createCompatibleTranslucentImage(width, height);
    }

    /**
     * Creates an edge detected image from the source image
     *
     * @param source - image whose edges will be detected
     * @return the new edge detected
     */
    @Nonnull
    public static BufferedImage createEdgeOnlyImage(@Nonnull BufferedImage source)
    {
        float[] edgeKernel =
                {0.0f, -1.0f, 0.0f,
                        -1.0f, 4.f, -1.0f,
                        0.0f, -1.0f, 0.0f};

        int type = source.getType();
        if (type == BufferedImage.TYPE_CUSTOM)
        {
            type = BufferedImage.TYPE_INT_RGB;
        }
        // We need to give the source a type
        BufferedImage img = new BufferedImage(source.getWidth(), source.getHeight(), type);
        img.setData(source.getData());

        BufferedImage imgCopy = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        ConvolveOp cop = new ConvolveOp(new Kernel(3, 3, edgeKernel), ConvolveOp.EDGE_ZERO_FILL, null);
        return cop.filter(img, imgCopy);
    }

    @Nonnull
    public static BufferedImage dropShadow(@Nonnull BufferedImage iconImage, int shadowSize, float shadowOpacity,
                                           @Nullable Color shadowColor)
    {
        // Create our selected image
        ShadowRenderer s = new ShadowRenderer(shadowSize, shadowOpacity, shadowColor);
        BufferedImage shadowedImage = s.createShadow(iconImage);
        Graphics2D g = (Graphics2D) shadowedImage.getGraphics();
        g.drawImage(iconImage, 0, 0, iconImage.getWidth(), iconImage.getHeight(), null);
        g.dispose();
        return shadowedImage;
    }

    @Nonnull
    public static List<BufferedImage> createPulsingImages(int numberOfFrames, double scaleFactor, @Nonnull Shape shape,
                                                          @Nonnull Color color)
    {
        List<BufferedImage> pulsingImages = new ArrayList<>(numberOfFrames);
        // Fade each octagon by this amount
        int initialOpacity = 100;
        int opacity1;

        // Amount to increment the smaller shape by so on the middle frame it is the starting size of the large shape

        for (int i = 0; i < numberOfFrames; i++)
        {
            opacity1 = initialOpacity - ((initialOpacity / numberOfFrames) * i);
            BufferedImage shapeImage = createShape(shape, ImageUtils.LARGE_ICON_WIDTH, opacity1, color);
            BufferedImage shapeWithGlow = createGlow(shapeImage, 7, 1F, color);
            pulsingImages.add(shapeWithGlow);
        }

        return pulsingImages;
    }

    @Nonnull
    public static BufferedImage createShape(@Nonnull GraphicsUtils.Shape shape, int imageWidth, int opacity, @Nonnull Color color)
    {
        return switch (shape)
        {
            case CIRCLE -> createCircle(imageWidth, opacity, color);
            case GLOSSY_CIRCLE -> createGlossyCircle(imageWidth, color);
            case GLOSSY_OCTAGON -> createGlossyOctagon(imageWidth, color);
            case GLOSSY_TRIANGLE -> createGlossyTriangle(imageWidth, color);
            case OCTAGON -> createOctagon(imageWidth, opacity, color);
            case TRIANGLE -> createTriangle(imageWidth, opacity, color);
        };
    }

    @Nonnull
    public static BufferedImage createGlow(@Nonnull BufferedImage iconImage, int glowSize, float glowOpacity, @Nullable Color glowColor)
    {
        // Create our selected image
        ShadowRenderer s = new ShadowRenderer(glowSize, glowOpacity, glowColor);
        return s.createShadow(iconImage);
    }

    @Nonnull
    public static BufferedImage offsetImage(@Nonnull BufferedImage image, int offsetX, int offsetY)
    {
        BufferedImage newImage = createCompatibleTranslucentImage(image.getWidth() + offsetX, image.getHeight() + offsetY);
        Graphics2D g2 = newImage.createGraphics();
        g2.drawImage(image, 0, 0, image.getWidth(), image.getHeight(), null);
        g2.dispose();
        return newImage;
    }

    @Nonnull
    public static BufferedImage createTriangle(int size, int opacity, @Nonnull Color color)
    {
        return createTriangle(size, g2 -> g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity)));
    }

    @Nonnull
    public static BufferedImage createGlossyTriangle(int size, @Nonnull Color color)
    {
        return createTriangle(size, g2 -> g2.setPaint(new GlossyPaint(color)));
    }

    @Nonnull
    private static BufferedImage createTriangle(int size, @Nonnull Consumer<Graphics2D> mutator)
    {
        // Create the x,y coordinates of our triangle
        final int[] xPoints = {0, size / 2, size};
        final int[] yPoints = {size, 0, size};

        BufferedImage image = createCompatibleTranslucentImage(size, size);

        return createClippedRectangularImage(image, size, mutator, new Polygon(xPoints, yPoints, 3));
    }

    @Nonnull
    public static BufferedImage createOctagon(int size, int opacity, @Nonnull Color color)
    {
        return createOctagon(size, g2 -> g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity)));
    }

    @Nonnull
    public static BufferedImage createGlossyOctagon(int size, @Nonnull Color color)
    {
        return createOctagon(size, g2 -> g2.setPaint(new GlossyPaint(color)));
    }

    @Nonnull
    private static BufferedImage createOctagon(int size, @Nonnull Consumer<Graphics2D> mutator)
    {
        // Calculate the length of the sides of the octagon
        // The sideLength is also the size of the hypotenuses of the triangles of an octagon
        final int h = (int) (size / (1 + (2 * Math.sin(Math.toRadians(45D)))));
        // This formula is used to calculate the length of the non-hypotenuses sides of the triangle
        final int s = (int) (h * Math.sin(Math.toRadians(45D)));

        // Create the x,y coordinates of our octagon
        final int[] xPoints = {0, s, s + h, size, size, s + h, s, 0};
        final int[] yPoints = {s, 0, 0, s, s + h, size, size, s + h};

        BufferedImage image = createCompatibleTranslucentImage(size, size);

        return createClippedRectangularImage(image, size, mutator, new Polygon(xPoints, yPoints, 8));
    }

    /**
     * Create a rectangular image with a specified clip shape.
     *
     * @param image   Source image
     * @param size    Size of image
     * @param mutator Mutator to apply to the graphics context
     * @param clip    Clip shape
     * @return constructed image
     * @since 2.35
     */
    @Nonnull
    private static BufferedImage createClippedRectangularImage(@Nonnull BufferedImage image, int size,
                                                               @Nonnull Consumer<Graphics2D> mutator, @Nullable Polygon clip)
    {
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        mutator.accept(g2);
        g2.setClip(clip);
        g2.fillRect(0, 0, size, size);
        g2.dispose();
        return image;
    }

    @Nonnull
    public static BufferedImage createSphere(@Nonnull Color color, int bsize)
    {
        byte[] data = new byte[bsize * 2 * bsize * 2];
        int maxR = 0;
        for (int Y = 2 * bsize; --Y >= 0;)
        {
            int x0 = (int) (Math.sqrt(bsize * bsize - (Y - bsize) * (Y - bsize)) + 0.5);
            int p = Y * (bsize * 2) + bsize - x0;
            for (int X = -x0; X < x0; X++)
            {
                int x = X + 15;
                int y = Y - bsize + 15;
                int r = (int) (Math.sqrt(x * x + y * y) + 0.5);
                if (r > maxR)
                {
                    maxR = r;
                }
                data[p++] = r <= 0 ? 1 : (byte) r;
            }
        }

        int bg = 255;
        byte[] red = new byte[256];
        red[0] = (byte) bg;
        byte[] green = new byte[256];
        green[0] = (byte) bg;
        byte[] blue = new byte[256];
        blue[0] = (byte) bg;

        // for each image, set its color
        //float b = 0.5f + ((1f) / 2f);
        float b = 1.0f; // It appears to me that the expression above is a constant 1.0, right?
        for (int i = maxR; i >= 1; --i)
        {
            float d = (float) i / maxR;
            red[i] = (byte) blend(blend(color.getRed(), 255, d), bg, b);
            green[i] = (byte) blend(blend(color.getGreen(), 255, d), bg, b);
            blue[i] = (byte) blend(blend(color.getBlue(), 255, d), bg, b);
        }
        IndexColorModel icm = new IndexColorModel(8, maxR + 1, red, green, blue, 0);
        DataBufferByte dbb = new DataBufferByte(data, data.length);
        int[] bandOffsets = {0};
        WritableRaster wr = Raster.createInterleavedRaster(dbb, bsize * 2, bsize * 2, bsize * 2, 1, bandOffsets, null);
        return new BufferedImage(icm, wr, icm.isAlphaPremultiplied(), null);
    }

    // performs the blending of white and color
    private static int blend(int fg, int bg, float fgFactor)
    {
        return (int) (bg + (fg - bg) * fgFactor);
    }

    @Nonnull
    public static BufferedImage createCircle(int size, int opacity, @Nonnull Color color)
    {
        return createCircle(size, g2 -> g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), opacity)));
    }

    @Nonnull
    public static BufferedImage createGlossyCircle(int size, @Nonnull Color color)
    {
        return createCircle(size, g2 -> g2.setPaint(new GlossyPaint(color)));
    }

    /**
     * Common code for creating a circle image.
     *
     * @param size    Size of circle
     * @param mutator Mutator to apply to the graphics context (typically, to set color or paint)
     * @return new image
     * @since 2.35
     */
    @Nonnull
    private static BufferedImage createCircle(int size, @Nonnull Consumer<Graphics2D> mutator)
    {
        BufferedImage image = createCompatibleTranslucentImage(size, size);
        Graphics2D g2 = image.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        mutator.accept(g2);
        g2.fillOval(0, 0, size, size);
        g2.dispose();
        return image;
    }

    /**
     * Overlays the provided image on the base image without changing the original image.
     *
     * @param originalImage The image to use as the base
     * @param overlaidImage The image to overlay on top of the base
     * @return The new image composed of the original image with the overlaid image on top, with the image type
     * coerced to {@link BufferedImage#TYPE_INT_ARGB}
     */
    @Nonnull
    public static BufferedImage overlayImage(@Nonnull BufferedImage originalImage, @Nonnull BufferedImage overlaidImage)
    {
        int widthOffset = (originalImage.getWidth() - overlaidImage.getWidth()) / 2;
        int heightOffset = (originalImage.getHeight() - overlaidImage.getHeight()) / 2;

        return overlayImage(originalImage, overlaidImage, widthOffset, heightOffset);
    }

    /**
     * Overlays the provided image on the base image at the specified offsets.
     *
     * @param originalImage The image to use as the base
     * @param overlaidImage The image to overlay on top of the base
     * @param widthOffset The amount to offset the overlaid image horizontally
     * @param heightOffset The amount to offset the overlaid image vertically
     * @return The new image composed of the original image with the overlaid image on top, at the specified offsets and
     * with the image type coerced to {@link BufferedImage#TYPE_INT_ARGB}
     */
    @Nonnull
    public static BufferedImage overlayImage(@Nonnull BufferedImage originalImage, @Nonnull BufferedImage overlaidImage,
                                             int widthOffset, int heightOffset)
    {
        BufferedImage newImage = copyImage(originalImage); // Copy original to avoid changing it
        Graphics2D g2 = newImage.createGraphics();
        g2.drawImage(overlaidImage, widthOffset, heightOffset, overlaidImage.getWidth(), overlaidImage.getHeight(), null);
        g2.dispose();
        return newImage;
    }

    /**
     * Overlays the provided image vertically above the base image at the specified offsets without changing the original image.
     *
     * @param originalImage The image to use as the base
     * @param overlaidImage The image to overlay on top of the base
     * @param widthOffset The amount to offset the overlaid image horizontally
     * @param heightOffset The amount to offset the overlaid image vertically
     * @return The new image composed of the original image with the overlaid image vertically above it, at the specified offsets,
     * with the image type coerced to {@link BufferedImage#TYPE_INT_ARGB}
     */
    @Nonnull
    public static BufferedImage overlayImageAboveOriginal(@Nonnull BufferedImage originalImage, @Nonnull BufferedImage overlaidImage,
                                                          int widthOffset, int heightOffset)
    {
        BufferedImage image = createCompatibleTranslucentImage(originalImage.getWidth(), originalImage.getHeight() + overlaidImage.getHeight());
        image = overlayImage(image, originalImage, 0, overlaidImage.getHeight());
        image = overlayImage(image, overlaidImage, widthOffset, heightOffset);
        return image;
    }

    /**
     * @param srcImage the original image to brighten
     * @param scaleFactor the amount to scale the brightness by (ex: 1.45 increases the brightness of all
     *                    color bands by 45%, capped to 255)
     * @return An image with all color bands scaled up by the {@code scaleFactor} and the image type
     * coerced to {@link BufferedImage#TYPE_INT_ARGB}
     */
    @Nonnull
    public static BufferedImage brightenImage(@Nonnull BufferedImage srcImage, float scaleFactor)
    {
        BufferedImage i = copyImage(srcImage);
        RescaleOp op = new RescaleOp(scaleFactor, 0, null);
        return op.filter(i, i);
    }

    /**
     * Applies {@link Color} shading to a given {@link BufferedImage}.
     * @param srcImage the original image to shade
     * @param color the {@link Color} to apply to the image
     * @return A copy of the image with the provided color shaded over the image and the image type coerced
     * to be {@link BufferedImage#TYPE_INT_ARGB}. If the source cannot be copied, then it is returned with no changes.
     * @since 5.3 (5.5 - image type coerced to {@link BufferedImage#TYPE_INT_ARGB}
     */
    @Nonnull
    public static BufferedImage colorImage(@Nonnull BufferedImage srcImage, @Nonnull Color color)
    {
        // If the type is CUSTOM, then we can't make a copy, so just return the original
        if (srcImage.getType() == BufferedImage.TYPE_CUSTOM) return srcImage;

        BufferedImage newImage = copyImage(srcImage);
        float blueShade = (float) color.getBlue() / 255;
        float greenShade = (float) color.getGreen() / 255;
        float redShade = (float) color.getRed() / 255;
        float alphaShade = (float) color.getAlpha() / 255;
        final int height = newImage.getHeight();
        final int width = newImage.getWidth();

        // similar algorithm to RoleIconColorizer, but distinct in that this doesn't overwrite the color - just shifts it
        // This is end-result identical to how WinTAK handles it
        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                final int oldColor = newImage.getRGB(x, y);
                final int a = (int) Math.max(0, ((oldColor >> 24) & 0xFF) * alphaShade);
                final int r = (int) Math.max(0, ((oldColor >> 16) & 0xFF) * redShade);
                final int g = (int) Math.max(0, ((oldColor >> 8) & 0xFF) * greenShade);
                final int b = (int) Math.max(0, (oldColor & 0xFF) * blueShade);
                newImage.setRGB(x, y, (a << 24) | (r << 16) | (g << 8) | b);
            }
        }
        newImage.flush();
        return newImage;
    }

    /**
     * @param image The {@link BufferedImage} to create a copy of
     * @return a copy of the image, with type {@link BufferedImage#TYPE_INT_ARGB}
     * @since 5.5 - return image no longer preserves the image type
     */
    @Nonnull
    public static BufferedImage copyImage(@Nonnull BufferedImage image)
    {
        BufferedImage result = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = result.createGraphics();
        g.drawRenderedImage(image, null);
        g.dispose();
        return result;
    }

    @Nonnull
    public static BufferedImage darkenImage(@Nonnull BufferedImage image, double factorDarker)
    {
        return scaleImagePixels(image, 1d - factorDarker, false);
    }

    @Nonnull
    public static BufferedImage grayscaleImage(@Nonnull BufferedImage image)
    {
        return scaleImagePixels(image, 1.0, true);
    }

    /**
     * Create an image by altering each pixel, applying a scale factor and optionally converting to a gray scale value.
     *
     * @param image        Source image
     * @param multiplier   Multiplier to apply to each pixel's color components
     * @param useGrayScale Pass {@code true} to map to a gray scale
     * @return New image, with type {@link BufferedImage#TYPE_INT_ARGB}
     * @since 2.35
     */
    @Nonnull
    private static BufferedImage scaleImagePixels(@Nonnull BufferedImage image, double multiplier, boolean useGrayScale)
    {
        int width = image.getWidth();
        int height = image.getHeight();
        BufferedImage scaled = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = scaled.createGraphics();
        g2.drawRenderedImage(image, null);
        g2.dispose();
        int[] pixels = scaled.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++)
        {
            int p = pixels[i];
            int a = (p >> 24) & 0xFF;
            int r = (int) (((p >> 16) & 0xFF) * multiplier);
            int g = (int) (((p >> 8) & 0xFF) * multiplier);
            int b = (int) ((p & 0xFF) * multiplier);

            if (useGrayScale)
            {
                int gray = Math.max(Math.min((int) (0.2126 * r + 0.7152 * g + 0.0722 * b), 255), 0);
                r = g = b = gray;
            }

            p = ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
            pixels[i] = p;
        }
        scaled.setRGB(0, 0, width, height, pixels, 0, width);
        return scaled;
    }

    @Nonnull
    public static BufferedImage drawBox(@Nonnull BufferedImage srcImage, @Nullable Color boxColor, float opacity)
    {
        BufferedImage result = new BufferedImage(srcImage.getWidth(), srcImage.getHeight(), srcImage.getType());
        Graphics2D g = result.createGraphics();
        Composite originalComposite = g.getComposite();
        g.setColor(boxColor);
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, opacity));
        g.fillRect(0, 0, srcImage.getWidth(), srcImage.getHeight());
        g.setComposite(originalComposite);
        g.drawImage(srcImage, 0, 0, srcImage.getWidth(), srcImage.getHeight(), null);
        g.setColor(boxColor);
        g.drawRect(0, 0, srcImage.getWidth() - 1, srcImage.getHeight() - 1);
        g.dispose();
        return result;
    }

    private static final int dashSize = 10;

    /**
     * This method uses a dashed stroke to draw an outline around the non-transparent regions of a picture.  Java2D
     * doesn't have a straightforward way of generating the proper stroke shape around a raster image, so this uses
     * a roundabout way to accomplish the task.  First, an Area object of no size is created.  Next, each horizontal
     * line of the image is scanned from top to bottom.  Any non-transparent region in a given line is then added to
     * the original area.  Finally, the area is drawn over the original image - hopefully giving a glitch-free blue
     * dashed outline to the image.  Thanks to Chris Campbell for the algorithm idea.
     * <p>
     * Note: This method is not designed to handle images with wildly different x and y dimensions.  The ratio between
     * height and width should be reasonably close to 1, otherwise rendering artifacts may occur when the image is drawn
     * on the globe.
     *
     * @param image the input image (not modified by this method)
     * @return a copy of the image with the drawn outline
     */
    @Nonnull
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public static BufferedImage createSelectedImage(@Nonnull BufferedImage image)
    {
        image = ImageUtils.getScaledInstance(image, 128, -1, RenderingHints.VALUE_INTERPOLATION_BILINEAR, true);
        BufferedImage newImage = new BufferedImage(image.getWidth() + dashSize, image.getHeight() + dashSize, BufferedImage.TYPE_INT_ARGB);

        //paste the re-sized image in the new image with some extra space around the margins for the stroke
        Graphics2D g2 = newImage.createGraphics();
        g2.drawImage(image, dashSize / 2, dashSize / 2, null);

        //set up stroke outline
        Area area = new Area();

        int width = newImage.getWidth();
        int height = newImage.getHeight();

        //pull pixel information out of the image
        int[] rgb = newImage.getRGB(0, 0, width, height, null, 0, width);

        //scan through image looking for transitions between transparent and non-transparent pixels
        //and add non transparent lines to the area object
        int pixel;
        int left = 0;
        int right = 0;
        boolean leftSet = false;
        Area tempArea;
        Rectangle rect;
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                pixel = rgb[y * width + x];

                if ((pixel >>> 24) != 0) // found a non transparent pixel
                {
                    if (!leftSet) // begin a line to add
                    {
                        left = right = x;
                        leftSet = true;
                    } else // stretch line to the right
                    {
                        right = x;
                    }
                }
            }
            if (leftSet && left != right) // make sure to add possible regions at the end of the line before moving to the next
            {
                rect = new Rectangle(left, y, right - left, 1);
                tempArea = new Area(rect);
                area.add(tempArea);
                leftSet = false;
                left = right = 0;
            }
        }

        //draw white glow
        float[] dash = {8};
        g2.setPaint(Color.WHITE);
        Stroke stroke = new BasicStroke(dashSize);
        g2.setStroke(stroke);
        g2.draw(area);

        //draw black checked stroke
        g2.setPaint(Color.BLUE);
        stroke = new BasicStroke(dashSize, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 10, dash, 0);
        g2.setStroke(stroke);
        g2.draw(area);

        return newImage;
    }

    @Nonnull
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public static BufferedImage createOutlinedImage(@Nonnull BufferedImage image, int ignoreColor, int alpha)
    {
        if (alpha < 0 || alpha > 255)
        {
            throw new IllegalArgumentException("Alpha value must be between 0 and 255.");
        }

        // Remove alpha.
        int normalizedIgnoreColor = ignoreColor & 0xFFFFFF;

        // Add alpha.
        int borderColor = (alpha << 24) | 0xFFFFFF;

        // Add 2 to each dimension to allow a 1-pixel space
        // from each edge.
        int height = image.getHeight() + 2;
        int width = image.getWidth() + 2;

        // Get the graphics for the new canvas.
        BufferedImage ret = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        // Draw the original image on the new canvas, shifting
        // it one pixel down and one pixel to the right to allow
        // space for the border.
        ret.createGraphics().drawImage(image, 1, 1, null);

        // Get the ARGB values for each pixel in the new
        // image drawn on the extended canvas.
        int[] rgb = new int[width * height];
        ret.getRGB(0, 0, width, height, rgb, 0, width);
        int nextPixel;

        // Scan the image.
        // Top to bottom. Ignore the transparent
        // edges we added.
        for (int y = 1; y < height - 1; y++)
        {
            // Left to right. Ignore the
            // transparent edges we added.
            for (int x = 1; x < width - 1; x++)
            {
                // Next pixel
                nextPixel = rgb[y * width + x];

                // Only process non-transparent pixels
                // and those that are not the ignore color.
                if ((nextPixel & 0xFFFFFF) != normalizedIgnoreColor && (nextPixel >>> 24) != 0)
                {
                    int pxAbove = (y - 1) * width + x;
                    int pxBelow = (y + 1) * width + x;
                    int pxLeft = y * width + x - 1;
                    int pxRight = y * width + x + 1;

                    // Check above
                    if ((rgb[pxAbove] >>> 24) == 0)
                    {
                        ret.setRGB(x, y - 1, borderColor);
                    }

                    // Check below
                    if ((rgb[pxBelow] >>> 24) == 0)
                    {
                        ret.setRGB(x, y + 1, borderColor);
                    }

                    // Check left
                    if ((rgb[pxLeft] >>> 24) == 0)
                    {
                        ret.setRGB(x - 1, y, borderColor);
                    }

                    // Check right
                    if ((rgb[pxRight] >>> 24) == 0)
                    {
                        ret.setRGB(x + 1, y, borderColor);
                    }
                }
            }
        }

        return ret;
    }

    /**
     * Rotates a copy of a given {@link BufferedImage} by a provided angle, returning the copy
     * @param image The image to be rotated
     * @param angle The angle by which to rotate the image
     * @return A rotated copy of the image
     *
     * @since 5.0
     */
    @Nonnull
    public static BufferedImage rotate(@Nonnull BufferedImage image, double angle)
    {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage imgCopy = new BufferedImage(w, h, image.getType());
        Graphics2D g = imgCopy.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.rotate(Math.toRadians(angle), w / 2.0, h / 2.0);
        g.drawImage(image, null, 0, 0);
        return imgCopy;
    }

    @Nonnull
    public static BufferedImage flipVertically(@Nonnull BufferedImage image)
    {
        AffineTransform tx1 = AffineTransform.getScaleInstance(1, -1);
        tx1.translate(0, -image.getWidth(null));
        AffineTransformOp op1 = new AffineTransformOp(tx1, AffineTransformOp.TYPE_NEAREST_NEIGHBOR);
        return op1.filter(image, null);
    }

    /**
     * Compares the provided images for equality.
     *
     * @param image1 The first image to compare
     * @param image2 The second image to compare
     * @return True if the images are equal.
     * @since 3.2
     */
    public static boolean compareImages(@Nullable BufferedImage image1, @Nullable BufferedImage image2)
    {
        if (image1 == null || image2 == null)
        {
            return image1 == null && image2 == null;
        }

        // The images must be the same size.
        int width = image1.getWidth();
        int height = image1.getHeight();
        if (width != image2.getWidth() || height != image2.getHeight()) return false;

        // Loop over every pixel.
        for (int y = 0; y < height; y++)
        {
            for (int x = 0; x < width; x++)
            {
                // Compare the pixels for equality.
                if (image1.getRGB(x, y) != image2.getRGB(x, y)) return false;
            }
        }

        return true;
    }

    public static void main(String[] args)
    {
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel p = new JPanel();
        p.add(new JLabel(new ImageIcon(createGlossyCircle(32, Color.GREEN))));
        final BufferedImage glossyCircle = createGlossyCircle(128, Color.GREEN);
        p.add(new JLabel(new ImageIcon(glossyCircle)));
        p.add(new JLabel(new ImageIcon(createGlossyOctagon(32, Color.RED))));
        p.add(new JLabel(new ImageIcon(createGlossyOctagon(128, Color.RED))));
        p.add(new JLabel(new ImageIcon(createOctagon(128, 100, Color.RED))));
        BufferedImage image = createOctagon(ImageUtils.LARGE_ICON_WIDTH, 120, Color.RED);
        image = createGlow(image, 7, 1F, Color.RED);
        p.add(new JLabel(new ImageIcon(image)));
        p.add(new JLabel(new ImageIcon(createGlossyTriangle(32, Color.YELLOW))));
        final BufferedImage glossyTriangle = createGlossyTriangle(128, Color.YELLOW);
        p.add(new JLabel(new ImageIcon(glossyTriangle)));
        final BufferedImage octagon = createShape(Shape.OCTAGON, 32, 0, Color.RED);
        p.add(new JLabel(new ImageIcon(octagon)));
        final BufferedImage overlayImage = overlayImageAboveOriginal(glossyTriangle, glossyCircle, glossyTriangle.getWidth() - glossyCircle.getWidth(), 0);
        p.add(new JLabel(new ImageIcon(overlayImage)));
        f.getContentPane().add(p);
        f.pack();
        f.setVisible(true);
    }
}
