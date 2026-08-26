package gov.takx.core.globe.tak.features;

import gov.nasa.worldwind.avlist.AVKey;
import jakarta.enterprise.context.ApplicationScoped;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.nio.DoubleBuffer;
import java.util.HashMap;

/**
 * A service that renders and caches WW {@link gov.nasa.worldwind.render.Annotation} frames to {@link BufferedImage}.
 * @since 3.5
 */
@ApplicationScoped
public class FrameFactory
{
    private final HashMap<FrameFillCacheEntry, BufferedImage> frameFillCache = new HashMap<>();
    private final HashMap<FrameStrokeCacheEntry, BufferedImage> frameStrokeCache = new HashMap<>();

    /**
     * The shape type to produce.
     * @since 5.5
     */
    public enum ShapeType
    {
        Ellipse(AVKey.SHAPE_ELLIPSE),
        Rectangle(AVKey.SHAPE_RECTANGLE),
        None(AVKey.SHAPE_NONE);

        private final String avkey;

        ShapeType(String avkey)
        {
            this.avkey = avkey;
        }

        /**
         * Convert a WW {@link AVKey} to {@link ShapeType}.
         * @param key the key to convery
         * @return the shape type
         */
        static ShapeType toShapeType(String key)
        {
            if (Ellipse.avkey.equals(key))
            {
                return Ellipse;
            } else if (Rectangle.avkey.equals(key))
            {
                return Rectangle;
            } else
            {
                return None;
            }
        }
    }

    /**
     * Render the provided frame fill specification to an image.
     * @param shapeType the WW shape type
     * @param width the width
     * @param height the height
     * @param cornerRadius the corner radius
     * @param color the color
     * @return the image
     */
    public BufferedImage createShapeFill(ShapeType shapeType, int width, int height, int cornerRadius, int color)
    {
        FrameFillCacheEntry cacheEntry = new FrameFillCacheEntry(shapeType, width, height, cornerRadius, color);
        BufferedImage image = frameFillCache.get(cacheEntry);
        if (image != null) return image;

        DoubleBuffer shapeBuffer = gov.nasa.worldwind.render.FrameFactory.createShapeBuffer(shapeType.avkey, width, height, cornerRadius, null);
        image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D g2d = null;
        try
        {
            g2d = image.createGraphics();
            int[] x = new int[3];
            int[] y = new int[3];
            int x1 = (int) shapeBuffer.get();
            int y1 = (int) shapeBuffer.get();
            int x2 = (int) shapeBuffer.get();
            int y2 = (int) shapeBuffer.get();
            g2d.setColor(new Color(color, true));
            // GL_TRIANGLE_FAN
            while (shapeBuffer.remaining() > 0)
            {
                int x3 = (int) shapeBuffer.get();
                int y3 = (int) shapeBuffer.get();
                x[0] = x1;
                y[0] = y1;
                x[1] = x2;
                y[1] = y2;
                x[2] = x3;
                y[2] = y3;
                g2d.fillPolygon(x, y, 3);
                x2 = x3;
                y2 = y3;
            }
        } finally
        {
            g2d.dispose();
        }
        frameFillCache.put(cacheEntry, image);
        return image;
    }

    /**
     /**
     * Render the provided frame stroke specification to an image.
     * @param shapeType the WW shape type
     * @param width the width
     * @param height the height
     * @param cornerRadius the corner radius
     * @param color the color
     * @param lineWidth the line width
     * @return the image
     */
    public BufferedImage createShapeStroke(ShapeType shapeType, int width, int height, int cornerRadius, int color, int lineWidth)
    {
        FrameStrokeCacheEntry cacheEntry = new FrameStrokeCacheEntry(shapeType, width, height, cornerRadius, color, lineWidth);
        BufferedImage image = frameStrokeCache.get(cacheEntry);
        if (image != null) return image;

        DoubleBuffer shapeBuffer = gov.nasa.worldwind.render.FrameFactory.createShapeBuffer(shapeType.avkey, width, height, cornerRadius, null);
        image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D g2d = null;
        try
        {
            g2d = image.createGraphics();
            int x1 = (int) shapeBuffer.get();
            int y1 = (int) shapeBuffer.get();
            g2d.setColor(new Color(color));
            g2d.setStroke(new BasicStroke(lineWidth));
            // GL_LINE_STRIP
            while (shapeBuffer.remaining() > 0)
            {
                int x2 = (int) shapeBuffer.get();
                int y2 = (int) shapeBuffer.get();
                g2d.drawLine(x1, y1, x2, y2);
                g2d.drawLine(x1, y1, x2, y2);
                x1 = x2;
                y1 = y2;
            }
        } finally
        {
            g2d.dispose();
        }
        frameStrokeCache.put(cacheEntry, image);
        return image;
    }

    private record FrameFillCacheEntry(ShapeType shapeType, int width, int height, int cornerRadius, int color)
    {
    }

    private record FrameStrokeCacheEntry(ShapeType shapeType, int width, int height, int cornerRadius, int color, int lineWidth)
    {
    }
}
