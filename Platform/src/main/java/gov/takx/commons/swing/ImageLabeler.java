package gov.takx.commons.swing;

import gov.takx.platform.globe.GlobeTextUtils;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;

/**
 * Provides image manipulation utilities.
 *
 */
public final class ImageLabeler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Orientations
    // --------------------------------------------------------------------

    /**
     * Place label North of original image (top).
     */
    public static final int NORTH = 0;

    /**
     * Place label South of the original image (bottom).
     */
    public static final int SOUTH = 1;

    /**
     * Overlay label on center of image.
     */
    public static final int CENTER = 2;

    // Justifications
    // --------------------------------------------------------------------

    /**
     * Place text on the left side of the label.
     */
    public static final int JUSTIFY_LEFT = 0;

    /**
     * Place text on the right side of the label.
     */
    public static final int JUSTIFY_RIGHT = 1;

    /**
     * Place text at the center of the label.
     */
    public static final int JUSTIFY_CENTER = 2;

    // Local Constants
    // --------------------------------------------------------------------

    /**
     * Default transparent background for the label.
     */
    private static final Color DEFAULT_BACKGROUND = new Color(0, 0, 0, 0);

    /**
     * Total border size (top and bottom) for the label.
     */
    private static final int BORDER = 2;

    /**
     * The smallest allowable font, regardless of image size.
     */
    private static final int MIN_FONT_SIZE = 9;

    /**
     * The maximum font size, regardless of image size.
     */
    private static final int MAX_FONT_SIZE = 50;

    /**
     * Private constructor. Accessible through static methods only.
     */
    private ImageLabeler()
    {
        /*hide*/
    }

    /**
     * Adds a label to an image at the specified location and justification.
     * <p>
     * Images should be at least 16x16 for the results to be usable. If the value passed in for {@code location} is
     * invalid, {@link #SOUTH} is used. If the value passed in for {@code justification} is invalid, {@link #JUSTIFY_LEFT}
     * is used.
     * <p/>
     * A default Arial, bold, white font with a size based on the original image width is used.
     *
     * @param originalImage The original image
     * @param text          The text to add to the image
     * @param location      The location where the text should be added
     * @param justification The justification of the text being added
     * @return A new image including the original image and the specified text
     */
    @Nonnull
    public static BufferedImage getLabeledImage(@Nonnull BufferedImage originalImage, @Nonnull String text, int location, int justification)
    {
        return getLabeledImage(originalImage, text, location, justification, 0, 0);
    }

    /**
     * Adds a label to an image at the specified location and justification, further adjusting the vertical and horizontal
     * offsets of the text using the given offsets.
     * <p>
     * Images should be at least 16x16 for the results to be usable. If the value passed in for {@code location} is
     * invalid, {@link #SOUTH} is used. If the value passed in for {@code justification} is invalid, {@link #JUSTIFY_LEFT}
     * is used.
     * <p/>
     * A default Arial, bold, white font with a size based on the original image width is used.
     *
     * @param originalImage    The original image
     * @param text             The text to add to the image
     * @param location         The location where the text should be added
     * @param justification    The justification of the text being added
     * @param verticalOffset   The vertical offset to further adjust the text by (in px). Positive moves the text down,
     *                         negative moves the text up.
     * @param horizontalOffset The horizontal offset to further adjust the text by (in px). Positive moves the text right,
     *                         negative moves the text left.
     * @return A new image including the original image and the specified text
     * @since 3.2
     */
    @Nonnull
    public static BufferedImage getLabeledImage(@Nonnull BufferedImage originalImage, @Nonnull String text, int location, int justification,
                                                int verticalOffset, int horizontalOffset)
    {
        Font font = GlobeTextUtils.DEFAULT_GLOBE_FONT.deriveFont((float) Math.max(Math.min(originalImage.getWidth() / 2, MAX_FONT_SIZE), MIN_FONT_SIZE));
        return getLabeledImage(originalImage, text, location, justification, verticalOffset, horizontalOffset, font);
    }

    /**
     * Adds a label to an image at the specified location and justification, further adjusting the vertical and horizontal
     * offsets of the text using the given offsets. The given font will be applied to the image text with a white font
     * color and transparent background.
     * <p>
     * Images should be at least 16x16 for the results to be usable. If the value passed in for {@code location} is
     * invalid, {@link #SOUTH} is used. If the value passed in for {@code justification} is invalid, {@link #JUSTIFY_LEFT}
     * is used.
     *
     * @param originalImage    The original image
     * @param text             The text to add to the image
     * @param location         The location where the text should be added
     * @param justification    The justification of the text being added
     * @param verticalOffset   The vertical offset to further adjust the text by (in px). Positive moves the text down,
     *                         negative moves the text up.
     * @param horizontalOffset The horizontal offset to further adjust the text by (in px). Positive moves the text right,
     *                         negative moves the text left.
     * @param font             The font to use when drawing the image text
     * @return A new image including the original image and the specified text
     * @since 3.2
     */
    @Nonnull
    public static BufferedImage getLabeledImage(@Nonnull BufferedImage originalImage, @Nonnull String text, int location, int justification,
                                                int verticalOffset, int horizontalOffset, @Nonnull Font font)
    {
        return getLabeledImage(originalImage, text, location, justification, verticalOffset, horizontalOffset, font, Color.WHITE, DEFAULT_BACKGROUND);
    }

    /**
     * Adds a label to an image at the specified location and justification, further adjusting the vertical and horizontal
     * offsets of the text using the given offsets. The given font, font color, and background color will be applied to
     * the image text.
     * <p>
     * Images should be at least 16x16 for the results to be usable. If the value passed in for {@code location} is
     * invalid, {@link #SOUTH} is used. If the value passed in for {@code justification} is invalid, {@link #JUSTIFY_LEFT}
     * is used.
     *
     * @param originalImage    The original image
     * @param text             The text to add to the image
     * @param location         The location where the text should be added
     * @param justification    The justification of the text being added
     * @param verticalOffset   The vertical offset to further adjust the text by (in px). Positive moves the text down,
     *                         negative moves the text up.
     * @param horizontalOffset The horizontal offset to further adjust the text by (in px). Positive moves the text right,
     *                         negative moves the text left.
     * @param font             The font to use when drawing the image text
     * @param fontColor        The font color to use when drawing the image text
     * @param backgroundColor  The background color to use when drawing the image text
     * @return A new image including the original image and the specified text
     * @since 3.2
     */
    @Nonnull
    public static BufferedImage getLabeledImage(@Nonnull BufferedImage originalImage, @Nonnull String text, int location, int justification,
                                                int verticalOffset, int horizontalOffset, @Nonnull Font font, @Nonnull Color fontColor,
                                                @Nonnull Color backgroundColor)
    {
        // Original image info.
        Graphics2D originalGraphics = (Graphics2D) originalImage.getGraphics();
        int originalImageWidth = originalImage.getWidth();
        int originalImageHeight = originalImage.getHeight();

        // Text info.
        Rectangle2D textRectangle = originalGraphics.getFontMetrics(font).getStringBounds(text, originalGraphics);
        int textDescent = originalGraphics.getFontMetrics(font).getDescent(); // descent is space needed to display letter tails (like 'g' and 'j')

        // Derived text info.
        int widthDelta = 0;
        int heightDelta = (int) Math.ceil(textRectangle.getHeight());

        if (textRectangle.getWidth() > originalImage.getWidth())
        {
            widthDelta = (int) Math.ceil(textRectangle.getWidth()) - originalImageWidth;
        }

        // Make a new image that will contain the original image and the text. Resize the image if necessary.
        BufferedImage result = new BufferedImage(originalImageWidth + widthDelta,
                location == CENTER ? originalImageHeight : originalImageHeight + heightDelta + BORDER,
                BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D resultGraphics = (Graphics2D) result.getGraphics();

        // Use our custom font.
        resultGraphics.setFont(font);

        // Draw original image on the new image.
        resultGraphics.drawImage(originalImage,                                      // original image
                widthDelta / 2,                                                      // if label is wider than image, move right by 1/2 delta
                location == NORTH ? heightDelta + BORDER : 0,                        // if label is at NORTH position, move image down label height plus label border
                originalImageWidth,                                                  // draw full image width
                originalImageHeight,                                                 // draw full image height
                null);

        // Draw label's background.
        resultGraphics.setColor(backgroundColor);
        resultGraphics.fillRect(0,                                                        // horizontal offset
                getVerticalBackgroundOffset(originalImageHeight, heightDelta, location),  // vertical offset
                originalImageWidth + widthDelta,                                          // width will be image width plus label border (if it is bigger than the image)
                heightDelta + BORDER);                                                    // height is text height plus border

        // Draw text.
        resultGraphics.setColor(fontColor);
        resultGraphics.drawString(text,
                getHorizontalTextOffset(originalImageWidth, (int) Math.ceil(textRectangle.getWidth()), justification) + horizontalOffset,
                getVerticalTextOffset(originalImageHeight, heightDelta, textDescent, location) + verticalOffset);

        return result;
    }

    /**
     * Determine the horizontal location of the text based on the specified justification.
     *
     * @param imageWidth    The width of the new image.
     * @param textWidth     The width of the text.
     * @param justification The specified justification.
     * @return The horizontal offset at which the text should be placed.
     */
    private static int getHorizontalTextOffset(int imageWidth, int textWidth, int justification)
    {
        int offset;

        switch (justification)
        {
            // String starts all the way to the left, offset 0.
            case JUSTIFY_LEFT:
            {
                offset = 0;
                break;
            }
            // If string width is smaller than image width, image starts at 1/2 width delta.
            // Otherwise, the text is the same size as the image and starts at offset 0.
            case JUSTIFY_CENTER:
            {
                offset = textWidth >= imageWidth ? 0 : Math.round((imageWidth - textWidth) / 2.0f);
                break;
            }
            // If string width is smaller than image width, image starts at image width less text width.
            // Otherwise, the text is the same size as the image and starts at offset 0.
            case JUSTIFY_RIGHT:
            {
                offset = textWidth >= imageWidth ? 0 : imageWidth - textWidth;
                break;
            }
            // Default behavior is to be left justified.
            default:
            {
                logger.warn("Invalid justification specified. Using default justificaiton: LEFT");
                offset = 0;
            }
        }

        return offset;
    }

    /**
     * Determine the horizontal location of the text based on the specified location.
     *
     * @param originalImageHeight The height of the original image.
     * @param labelHeight         The height of the label.
     * @param textDescent         The size of the text descent.
     * @param location            The specified location.
     * @return The location at which the text should be placed, based on lower left corner.
     */
    private static int getVerticalTextOffset(int originalImageHeight, int labelHeight, int textDescent, int location)
    {
        int offset;

        switch (location)
        {
            // Bottom of string starts at offset of label height less the border and text descent
            case NORTH:
            {
                offset = labelHeight - BORDER / 2 - textDescent;
                break;
            }
            // Bottom of string starts at half image height and half label height, moved up by border and text descent
            case CENTER:
            {
                offset = (originalImageHeight / 2 + labelHeight / 2) - BORDER / 2 - textDescent;
                break;
            }
            // Bottom of string starts at bottom of new image less the border and text descent
            case SOUTH:
            {
                offset = originalImageHeight + labelHeight - BORDER / 2 - textDescent;
                break;
            }
            default:
            {
                logger.warn("Invalid location specified. Using default location: SOUTH");
                offset = originalImageHeight + labelHeight - BORDER / 2 - textDescent;
            }
        }

        return offset;
    }

    /**
     * Determine the lower bound of the label's background.
     *
     * @param originalImageHeight The height of the original image.
     * @param heightDelta         The height of the text being added.
     * @param location            The location at which the label will be placed.
     * @return The lower bound of the label's background.
     */
    private static int getVerticalBackgroundOffset(int originalImageHeight, int heightDelta, int location)
    {
        int offset;

        switch (location)
        {
            // NORTH is at 0 offset from top.
            case NORTH:
            {
                offset = 0;
                break;
            }
            // CENTER starts at the middle of image, moves down half
            // of the text's height plus half of the borders height
            case CENTER:
            {
                offset = (originalImageHeight / 2) - (heightDelta / 2) - (BORDER / 2);
                break;
            }
            // SOUTH is at the bottom of the original image.
            case SOUTH:
            {
                offset = originalImageHeight;
                break;
            }
            // Default behavior is to place the label SOUTH of the image.
            default:
            {
                logger.warn("Invalid location specified. Using default location: SOUTH");
                offset = originalImageHeight;
            }
        }

        return offset;
    }
}
