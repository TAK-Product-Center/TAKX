package gov.takx.api.media;

import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nullable;

/**
 * Reference to an image
 *
 * @since 2.15
 */
@NoPluginImplementations
public interface IImage extends IMedia
{
    /**
     * Gets the width of the image
     */
    @Nullable
    Integer getWidth();

    /**
     * Gets the height of the image
     */
    @Nullable
    Integer getHeight();

    /**
     * Sets the width of the image. To be used if the width can not be calculated
     */
    void setWidth(@Nullable Integer width);

    /**
     * Sets the height of the image. To be used if the height can not be calculated
     */
    void setHeight(@Nullable Integer height);
}
