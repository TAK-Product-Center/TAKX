package gov.takx.core.globe;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.takx.api.globe.IStateIcon;
import gov.takx.commons.swing.ImageUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashMap;

import static gov.takx.platform.globe.GlobeTextUtils.DEFAULT_GLOBE_FONT;

/**
 * Each GlobeStateIcon handles a StateIcon at a specified cardinal position for a parent {@link GlobeStateIconManager}.
 * These annotations don't need real positions because they are always rendered relative to the parent annotation.
 * The actual annotation is not instantiated until needed (and when the globe context is ready).
 *
 */
public class GlobeStateIcon implements IStateIcon
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Color BACKGROUND = new Color(0, 0, 0, 125);
    private static final Font DEFAULT_FONT = DEFAULT_GLOBE_FONT.deriveFont(14f);
    private static final Dimension DIMENSION_ZERO = new Dimension(0, 0);
    private static final Insets INSETS_ZERO = new Insets(0, 0, 0, 0);
    private static final Insets INSETS_FIVE = new Insets(5, 5, 5, 5);

    private boolean visible = true;
    private boolean wrapText = true;
    private boolean isText;
    private BufferedImage originalImage;
    private final HashMap<String, BufferedImage> imageCache = new HashMap<>();
    private int currentSize;

    private StateIconAnnotation annotation;
    private final AnnotationAttributes attributes = new AnnotationAttributes();
    private final IStateChangeListener stateChangeListener;
    private String text;
    private Font font = DEFAULT_FONT;

    /**
     * Setting constructor, set all data members.
     *
     * @param image       a valid {@code BufferedImage}; CANNOT be {@code null}
     * @param initialSize The initial size of the icon
     * @param stateChangeListener Listener for state change events
     */
    public GlobeStateIcon(@Nonnull BufferedImage image, int initialSize, @Nonnull IStateChangeListener stateChangeListener)
    {
        this(initialSize, stateChangeListener);

        setImageAttributes();
        setImage(image);
    }

    /**
     * Setting constructor, set all data members.
     *
     * @param imagePathURL a valid {@code URL}; CANNOT be {@code null}
     * @param initialSize  The initial size of the icon.
     * @param stateChangeListener Listener for state change events
     */
    public GlobeStateIcon(@Nonnull URL imagePathURL, int initialSize, @Nonnull IStateChangeListener stateChangeListener)
    {
        this(initialSize, stateChangeListener);

        setImageAttributes();
        setImage(imagePathURL);
    }

    /**
     * Setting constructor, set all data members.
     *
     * @param text        The text to display.
     * @param initialSize The initial size of the icon.
     * @param stateChangeListener Listener for state change events
     */
    public GlobeStateIcon(@Nonnull String text, int initialSize, @Nonnull IStateChangeListener stateChangeListener)
    {
        this(initialSize, stateChangeListener);

        setLabel(text);
    }

    /**
     * Common internal constructor to prepare our initial size and attributes.
     *
     * @param initialSize Initial size of annotation
     */
    private GlobeStateIcon(int initialSize, @Nonnull IStateChangeListener stateChangeListener)
    {
        currentSize = initialSize;
        attributes.setDrawOffset(new Point()); // Override to ensure no offset
        attributes.setFont(font);
        this.stateChangeListener = stateChangeListener;
    }

    @Override
    public void setFont(@Nonnull Font font)
    {
        this.font = font;
        attributes.setFont(font);
        stateChangeListener.stateChanged();
    }

    @Nonnull
    @Override
    public Font getFont()
    {
        return font;
    }

    /**
     * Return the annotation that represents this state icon.  It is constructed on demand
     * to avoid problems with the WW context not being ready.
     *
     * @return globe annotation to render
     */
    public GlobeAnnotation getAnnotation()
    {
        if (annotation == null)
        {
            annotation = new StateIconAnnotation();
            annotation.setAttributes(attributes);

            if (isText) annotation.setText(text);
        }

        return annotation;
    }

    /**
     * @return The current annotation attributes
     */
    public AnnotationAttributes getAttributes()
    {
        return attributes;
    }

    @Override
    public void setLabel(String text)
    {
        originalImage = null;

        if (!isText) setTextAttributes();

        if (text != null && !text.equals(this.text))
        {
            this.text = text;
            if (annotation != null) annotation.setText(text);
            stateChangeListener.stateChanged();
        }
    }

    @Override
    public void setImage(@Nonnull BufferedImage image) throws IllegalArgumentException
    {
        if (image.getData() == null) throw new IllegalArgumentException("The image cannot be empty.");

        setImageAttributes();

        if (image != originalImage)
        {
            originalImage = image;
            resize(currentSize);
        }

        stateChangeListener.stateChanged();
    }

    @Override
    public void setImage(@Nonnull URL imagePathURL)
    {
        setImageAttributes();

        BufferedImage iconImage;

        final String urlPath = imagePathURL.getPath();

        if (imageCache.containsKey(urlPath))
        {
            // Get the cached image.
            iconImage = imageCache.get(urlPath);
        } else
        {
            // Read the image in.
            iconImage = ImageUtils.getImage(imagePathURL, false);

            if (iconImage == null)
            {
                logger.error("Could not read image from URL {}", imagePathURL);
                return;
            }

            imageCache.put(urlPath, iconImage); // Add it to the cache.
        }

        originalImage = iconImage;
        resize(currentSize);

        stateChangeListener.stateChanged();
    }

    @Override
    public boolean isVisible()
    {
        return visible;
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (this.visible != visible)
        {
            this.visible = visible;
            attributes.setVisible(visible);
        }

        stateChangeListener.stateChanged();
    }

    @Override
    public void setWrapText(boolean wrapText)
    {
        this.wrapText = wrapText;
        stateChangeListener.stateChanged();
    }

    @Override
    public boolean isWrapText()
    {
        return wrapText;
    }

    /**
     * Set the current size of the state icon.
     *
     * @param currentSize New size for the icon
     */
    public void setCurrentSize(int currentSize)
    {
        if (this.currentSize != currentSize)
        {
            this.currentSize = currentSize;
            resize(currentSize);
        }

        stateChangeListener.stateChanged();
    }

    private void setTextAttributes()
    {
        isText = true;
        attributes.setAdjustWidthToText(AVKey.SIZE_FIT_TEXT);
        attributes.setImageSource(null);
        attributes.setSize(DIMENSION_ZERO);
        attributes.setInsets(INSETS_FIVE);
        attributes.setBackgroundColor(BACKGROUND);
        attributes.setTextColor(Color.WHITE);
        attributes.setTextAlign(AVKey.CENTER);
        attributes.setLeader(AVKey.SHAPE_NONE);
        attributes.setEffect(AVKey.TEXT_EFFECT_OUTLINE);
        attributes.setLeader(AVKey.SHAPE_NONE);
        attributes.setFrameShape(AVKey.SHAPE_NONE);
    }

    private void setImageAttributes()
    {
        text = "";
        isText = false;

        attributes.setInsets(INSETS_ZERO);
        attributes.setImageRepeat(AVKey.REPEAT_NONE); // no tiling of image
    }

    @Override
    public boolean isImage()
    {
        return !isText;
    }

    /**
     * Scale the image according to the size of the map object.
     *
     * @param size The size to which this object should be scaled.
     */
    private void resize(int size)
    {
        if (originalImage != null)
        {
            int width = originalImage.getWidth();
            int height = originalImage.getHeight();

            if (width > height)
            {
                height = (int) Math.round(size * height / (double) width);
                width = size;
            } else
            {
                width = (int) Math.round(size * width / (double) height);
                height = size;
            }

            attributes.setSize(new Dimension(width, height));
            attributes.setImageSource(originalImage);
        }
    }

    /**
     * Simple extension of globe annotation to control how our content is drawn.
     */
    private class StateIconAnnotation extends GlobeAnnotation
    {
        /**
         * Construct the annotation.
         */
        public StateIconAnnotation()
        {
            super("", Position.ZERO);
        }

        @Override
        protected void drawContent(DrawContext drawContext, int width, int height, double opacity, Position position)
        {
            //noinspection VariableNotUsedInsideIf
            if (originalImage != null)
            {
                drawBackgroundImage(drawContext, width, height, opacity, position);
            } else if (text != null && !text.isEmpty())
            {
                drawBackground(drawContext, width, height, opacity, position);
                drawText(drawContext, width, height, opacity, position);
            }
        }

        /**
         * Overridden to control whether text is wrapped or not.
         *
         * @param dc     Drawing context
         * @param width  annotation width
         * @param height annotation height
         * @return Dimensions of text
         */
        @Override
        protected Dimension adjustSizeToText(DrawContext dc, int width, int height)
        {
            AnnotationAttributes attribs = getAttributes();

            String theText = text;

            if (wrapText)
            {
                text = getWrappedText(dc, width, height, getText(), attribs.getFont(), attribs.getTextAlign());
            }

            Rectangle textBounds = getTextBounds(dc, theText, attribs.getFont(), attribs.getTextAlign());

            // If the attributes specify to fit the annotation to the wrapped text width, then set the inset width to
            // the wrapped text width.
            if (attribs.getAdjustWidthToText().equals(AVKey.SIZE_FIT_TEXT) && !text.isEmpty())
            {
                width = textBounds.width;
            }

            // If the inset height is less than or equal to zero, then override the inset height with the wrapped text height.
            if (height <= 0) height = textBounds.height;

            return new Dimension(width, height);
        }
    }

    /**
     * Listener contract for a general notion of a state change.  This can be used when specific details of "what"
     * changed are not needed.  For those cases, a more typical {@link java.beans.PropertyChangeListener} should be used.
     *
     * @since 3.5
     */
    @FunctionalInterface
    public interface IStateChangeListener
    {
        /**
         * Called when the state of an object changed.
         */
        void stateChanged();
    }
}
