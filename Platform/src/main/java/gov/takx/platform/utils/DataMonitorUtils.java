package gov.takx.platform.utils;

import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.UIResources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandles;
import java.net.URL;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Utility class used by data monitors to construct identifier images.
 *
 * @since 1.11
 */
public class DataMonitorUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final URL MUTE_ICON_URL = UIResources.getUrl(PLATFORM_ICONS, "volume_off.png");

    /**
     * Overlay one image onto another. There is no return value from this method because
     * Java2D will directly alter the image defined by {@code baseImage}.
     *
     * @param baseImage The original image.
     * @param overlay   The image to overlay onto the {@code baseImage}
     * @return A buffered image with the visual state image overlaid onto the lower right corner.
     * @since 1.10
     */
    public static BufferedImage overlaySecondaryStatus(BufferedImage baseImage, BufferedImage overlay)
    {
        BufferedImage result = copy(baseImage);

        // Perform the overlay.
        // ------------------------------------------------------------------------
        Graphics2D g = result.createGraphics();

        // The 1.0f is the alpha factor.
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));

        int x = (baseImage.getWidth() - overlay.getWidth());
        int y = (baseImage.getHeight() - overlay.getHeight());

        g.drawImage(overlay, x, y, null);
        g.dispose();
        // ------------------------------------------------------------------------

        return result;
    }

    public static BufferedImage overlayMuteStatus(BufferedImage baseImage, BufferedImage muteImage)
    {
        BufferedImage result = copy(baseImage);

        // Perform the overlay.
        // ------------------------------------------------------------------------
        Graphics2D g = result.createGraphics();

        // The 0.9f is the alpha factor.
//        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.9f));

        int y = (baseImage.getHeight() - muteImage.getHeight());

        g.drawImage(muteImage, 0, y, null);
        g.dispose();
        // ------------------------------------------------------------------------

        return result;
    }

    public static BufferedImage getMutedStateImage(int size)
    {
        return ImageUtils.getScaledImage(MUTE_ICON_URL, size, size, false);
    }

    /**
     * Overlay one image onto another. There is no return value from this method because
     * Java2D will directly alter the image defined by {@code baseImage}.
     *
     * @param baseImage The original image.
     * @param overlay   The image to overlay onto the {@code baseImage}
     * @since 1.10
     */
    public static void overlayImage(BufferedImage baseImage, BufferedImage overlay)
    {
        // This will be the baseline width and height.
        // The final image will be these dimensions.
        int statusWidth = baseImage.getWidth();
        int statusHeight = baseImage.getHeight();

        // Get the dimensions of the image that will
        // be overlaid.
        int overlayWidth = overlay.getWidth();
        int overlayHeight = overlay.getHeight();

        int newOverlayWidth;
        int newOverlayHeight;

        // Calculate the new dimensions for the overlay image.
        // The larger edge (width/height) will provide the scaling
        // factor for both edges.
        if (overlayHeight > overlayWidth)
        {
            // Scale the larger edge relative to the baseline.
            newOverlayHeight = (int) Math.round(statusHeight * 0.80);

            // Scale the smaller edge relative to the larger edge.
            newOverlayWidth = (int) Math.round((double) newOverlayHeight / overlayHeight * overlayWidth);
        } else if (overlayWidth > overlayHeight)
        {
            // Scale the larger edge relative to the baseline.
            newOverlayWidth = (int) Math.round(statusWidth * 0.80);

            // Scale the smaller edge relative to the larger edge.
            newOverlayHeight = (int) Math.round((double) newOverlayWidth / overlayWidth * overlayHeight);
        } else
        {
            // Both edges are the same length, so scale them both
            // relative to the baseline.
            newOverlayHeight = (int) Math.round(statusHeight * 0.92);
            newOverlayWidth = (int) Math.round(statusWidth * 0.92);
        }

        // Resize the overlay image so that it will fit inside of the
        // status image.
        overlay = resize(overlay, newOverlayWidth, newOverlayHeight);

        // Perform the overlay.
        // ------------------------------------------------------------------------
        Graphics2D g = baseImage.createGraphics();

        // The 1.0f is the alpha factor.
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

        // The x-offset should be 1/2 the difference between the width of the
        // status image and the smaller overlay image.
        int x = (baseImage.getWidth() - overlay.getWidth()) / 2;

        // The y-offset should be 1/2 the difference between the height of the
        // status image and the smaller overlay image.
        int y = (baseImage.getHeight() - overlay.getHeight()) / 2;

        g.drawImage(overlay, x, y, null);
        g.dispose();
        // ------------------------------------------------------------------------
    }

    /**
     * Create a deep copy of a buffered image.
     *
     * @param bufferedImage The image to copy.
     * @return A copy of the original image.
     */
    private static BufferedImage copy(BufferedImage bufferedImage)
    {
        int w = bufferedImage.getWidth();
        int h = bufferedImage.getHeight();
        int type = bufferedImage.getType();
        if (type == BufferedImage.TYPE_CUSTOM)
        {
            type = bufferedImage.getColorModel().hasAlpha()
                    ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        }
        BufferedImage result = new BufferedImage(w, h, type);
        Graphics2D g = result.createGraphics();
        g.drawRenderedImage(bufferedImage, null);
        g.dispose();
        return result;
    }

    /**
     * Use an affine transformation to rescale an image.
     *
     * @param image  The image to rescale.
     * @param width  The target width.
     * @param height The target height.
     * @return A scaled version of the original image.
     * @since 1.10
     */
    private static BufferedImage resize(BufferedImage image, int width, int height)
    {
        // Determine the scaling factors.
        double widthScaleFactor = (double) width / image.getWidth();
        double heightScaleFactor = (double) height / image.getHeight();

        // Create a new image of the appropriate size.
        BufferedImage scaledImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics2D = scaledImage.createGraphics();

        // Get an affine transformation to perform the scaling.
        AffineTransform transform = AffineTransform.getScaleInstance(widthScaleFactor, heightScaleFactor);

        // Draw the new image.
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics2D.drawImage(image, transform, null);
        graphics2D.dispose();

        return scaledImage;
    }
}
