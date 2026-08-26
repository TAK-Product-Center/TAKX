package gov.takx.core.globe;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.takx.api.mapobject.IPreviewProvider;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.platform.globe.GlobeTextUtils;
import gov.takx.platform.ui.UIResources;

import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * A GlobeAnnotation that presents an image preview to the user.
 *
 */
public class PreviewAnnotation extends GlobeAnnotation
{
    private static final int INSET = 6;
    private static final int WIDTH = 200;
    private static final BufferedImage WARNING_IMAGE = UIResources.getImage(PLATFORM_ICONS, "missing_file_warning.png");

    private File imageFile;

    /**
     * Construct new preview annotation.
     *
     * @param previewProvider preview provider
     * @param position globe position at which to produce the annotation
     */
    public PreviewAnnotation(IPreviewProvider previewProvider, Position position)
    {
        super(previewProvider.getPreviewDescription(), position, GlobeTextUtils.PREVIEW_ANNOTATION_FONT, Color.white);

        imageFile = previewProvider.getExternalFile();

        configureAnnotation();
    }

    /**
     * Reconfigure this PreviewAnnotation for reuse.
     *
     * @param previewProvider preview provider
     * @param position globe position at which to produce the annotation
     * @since 3.5
     */
    public void reconfigureAnnotation(IPreviewProvider previewProvider, Position position)
    {
        setText(previewProvider.getPreviewDescription());
        setPosition(position);
        imageFile = previewProvider.getExternalFile();
        configureAnnotation();
    }

    /**
     * Configure the annotation attributes.
     */
    private void configureAnnotation()
    {
        setAlwaysOnTop(true);

        final AnnotationAttributes attributes = getAttributes();

        attributes.setBackgroundColor(Color.BLACK);
        attributes.setImageRepeat(AVKey.REPEAT_NONE);
        attributes.setCornerRadius(0);
        attributes.setDrawOffset(new Point(0, 5)); // Just a little up
        attributes.setLeader(AVKey.SHAPE_NONE);

        final BufferedImage previewImage = getScaledImage(WIDTH);

        attributes.setImageSource(previewImage);
        attributes.setImageOffset(new Point(INSET, INSET));
        attributes.setSize(new Dimension(WIDTH + INSET * 2, previewImage.getHeight() + INSET * 2));
        attributes.setInsets(new Insets(previewImage.getHeight() + INSET, INSET, INSET, INSET));
    }

    /**
     * Generate a scaled version of the image contained in our file.
     *
     * @param width  Desired width
     * @return scaled image
     */
    private BufferedImage getScaledImage(int width)
    {
        try
        {
            final BufferedImage orig = imageFile != null && imageFile.exists() && ImageUtils.isImageTypeReadable(imageFile)
                    ? ImageIO.read(imageFile)
                    : WARNING_IMAGE;

            final int height = (int) (orig.getHeight() * ((double) width) / orig.getWidth());

            final BufferedImage scaled = ImageTypeSpecifier.createFromRenderedImage(orig)
                    .createBufferedImage(width, height);

            final Graphics2D g2d = scaled.createGraphics();

            try
            {
                g2d.setComposite(AlphaComposite.Src);
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.drawImage(orig, 0, 0, width, height, null);

                return scaled;
            } finally
            {
                g2d.dispose();
            }
        } catch (IOException e)
        {
            return WARNING_IMAGE;
        }
    }
}
