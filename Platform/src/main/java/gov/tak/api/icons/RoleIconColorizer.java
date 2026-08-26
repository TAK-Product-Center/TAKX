package gov.tak.api.icons;

import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import java.awt.Color;
import java.awt.image.BufferedImage;

import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_ICONS;

/**
 * Processes a given team/role image and blends in a given color.  This process is very specialized according
 * to the icons in tak-assets.  If those assets are changed from being basically grey scale, then this will need
 * to be reworked.
 *
 * @since 3.0
 */
public class RoleIconColorizer
{
    /**
     * Read a given image in {@link gov.takx.platform.ui.ResourceDirectory#TAK_ASSETS_ICONS} and blend in the given color.
     *
     * @param imagePath Path of image to load
     * @param fillColor Color to blend with source image
     * @return colorized image
     */
    public static BufferedImage colorize(@Nonnull String imagePath, @Nonnull Color fillColor)
    {
        final BufferedImage image = UIResources.getImage(TAK_ASSETS_ICONS, imagePath);

        // Don't colorize to white (the source images are already white)
        return Color.white.equals(fillColor)
                ? image
                : ImageUtils.colorize(image, fillColor);
    }

}
