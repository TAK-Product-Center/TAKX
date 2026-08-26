package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.media.IImage;
import gov.takx.api.media.ImageBuilder;
import gov.takx.api.media.MediaProperties;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.URI;

/**
 * @since 2.15
 */
@Internal
@Entity
@DiscriminatorValue("Image")
public class Image extends AMedia<IImage, ImageBuilder> implements IImage
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private Integer width;
    private Integer height;

    protected Image()
    {
        // JPA
    }

    public Image(ImageBuilder imageBuilder)
    {
        super(imageBuilder);

        final BufferedImage image = loadImage();
        if (image != null)
        {
            updateSizeFromImage(image);
        } else
        {
            // Only use the builder dimensions if we can't extract the width & height from the url / file
            width = imageBuilder.getWidth();
            height = imageBuilder.getHeight();
        }
    }

    /**
     * @return image loaded from our configured source URI
     * @since 5.0
     */
    @Nullable
    private BufferedImage loadImage()
    {
        try
        {
            return ImageIO.read(getSourceUri().toURL());
        } catch (IOException e)
        {
            logger.warn("Error loading image from: {}", getSourceUri(), e);
        }
        return null;
    }

    /**
     * Update our width/height from the given {@code image}.  If the image is {@code null}, null out the width and height.
     * @since 5.0
     */
    private void updateSizeFromImage(@Nullable BufferedImage image)
    {
        if (image != null)
        {
            setWidth(image.getWidth());
            setHeight(image.getHeight());
        } else
        {
            setWidth(null);
            setHeight(null);
        }
    }

    @Override
    public void setSourceUri(@Nonnull URI sourceUri)
    {
        super.setSourceUri(sourceUri);

        updateSizeFromImage(loadImage());
    }

    @Override
    @Nullable
    public Integer getWidth()
    {
        return width;
    }

    @Override
    @Nullable
    public Integer getHeight()
    {
        return height;
    }

    @Override
    public void setWidth(@Nullable Integer width)
    {
        propertyChangeSupport.updateAndFire(MediaProperties.Image.WIDTH, this.width, width, () -> this.width = width);
    }

    @Override
    public void setHeight(@Nullable Integer height)
    {
        propertyChangeSupport.updateAndFire(MediaProperties.Image.HEIGHT, this.height, height, () -> this.height = height);
    }
}
