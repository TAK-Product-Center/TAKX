package gov.takx.core.mapobject.gui.widgets;

import gov.takx.platform.ui.ResourceDirectory;
import jakarta.annotation.Nonnull;

import javax.swing.ImageIcon;

import java.awt.Image;

/**
 * This class contains two images for use in the UI, with one specifically intended for the radial context menus that utilize a larger
 * icon than most of the rest of the UI.  The radial icons are generally  {@link gov.takx.platform.ui.UIResources#FEATURE_IMAGE_SIZE}
 * and can be generated with {@link gov.takx.platform.ui.UIResources#getFeatureIcon(ResourceDirectory, String)}
 * where most icons are {@link gov.takx.platform.ui.UIResources#STANDARD_IMAGE_SIZE} and can be generated from
 * {@link gov.takx.platform.ui.UIResources#getStandardSizedIcon(ResourceDirectory, String)}
 *
 * @since 3.3
 */
public class ContextMenuIcon extends ImageIcon
{
    private final Image radialImage;

    /**
     *
     * @param regularImage The image for most UIs, which should be {@link gov.takx.platform.ui.UIResources#STANDARD_IMAGE_SIZE}
     * @param radialImage The larger image for radial menu use, and should be {@link gov.takx.platform.ui.UIResources#FEATURE_IMAGE_SIZE}
     */
    public ContextMenuIcon(@Nonnull Image regularImage, @Nonnull Image radialImage)
    {
        super(regularImage);
        this.radialImage = radialImage;
    }

    /**
     * @return Image for use in the radial context menus
     */
    @Nonnull
    public Image getRadialImage()
    {
        return radialImage;
    }
}
