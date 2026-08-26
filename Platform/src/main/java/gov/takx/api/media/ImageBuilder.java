package gov.takx.api.media;

import gov.takx.api.media.factory.AMediaBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;

/**
 * @since 2.15
 */
public class ImageBuilder extends AMediaBuilder<IImage, ImageBuilder>
{
    private Integer width;
    private Integer height;

    public ImageBuilder(@Nonnull String name, @Nonnull String keyOfSource)
    {
        super(name, keyOfSource);
    }

    /**
     * @param name        The display name for the media. Immutable.
     * @param keyOfSource The unique key for the source that created / provided the media. This is most likely a
     *                    plugin's key if created by a plugin or "user" if the user directly added the media.
     *                    If the keyOfSource is the unique key of a Map Object, then when/if the Map Object is deleted,
     *                    all the media with that key will also be deleted.
     *                    Immutable.
     * @param sourceUri   media Source URI
     * @since 5.0
     */
    public ImageBuilder(@Nonnull String name, @Nonnull String keyOfSource, @Nonnull URI sourceUri)
    {
        super(name, keyOfSource, sourceUri);
    }

    /**
     * @see IImage#setHeight(Integer)
     */
    @Nonnull
    public ImageBuilder setHeight(@Nullable Integer height)
    {
        this.height = height;
        return this;
    }

    /**
     * @see IImage#setWidth(Integer)
     */
    @Nonnull
    public ImageBuilder setWidth(@Nullable Integer width)
    {
        this.width = width;
        return this;
    }

    /**
     * @see IImage#getWidth()
     */
    @Nullable
    public Integer getWidth()
    {
        return width;
    }

    /**
     * @see IImage#getHeight()
     */
    @Nullable
    public Integer getHeight()
    {
        return height;
    }

    @Override
    @Nonnull
    public IImage build()
    {
        return mediaFactory.build(this);
    }
}
