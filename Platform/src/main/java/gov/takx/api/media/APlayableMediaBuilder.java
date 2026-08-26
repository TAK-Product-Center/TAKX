package gov.takx.api.media;

import gov.takx.api.media.factory.AMediaBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;

/**
 * Abstract base for builders that handle {@link IPlayable playable} media types.
 *
 * @param <M> Media interface type
 * @param <B> Concrete builder type
 * @since 5.0
 */
public abstract class APlayableMediaBuilder<M extends IMedia & IPlayable, B extends AMediaBuilder<M, B>> extends AMediaBuilder<M, B>
{
    private Long duration;

    protected APlayableMediaBuilder(@Nonnull String name, @Nonnull String keyOfSource)
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
    protected APlayableMediaBuilder(@Nonnull String name, @Nonnull String keyOfSource, @Nonnull URI sourceUri)
    {
        super(name, keyOfSource, sourceUri);
    }

    /**
     * @see IPlayable#setDuration(Long)
     */
    @Nonnull
    public B setDuration(@Nullable Long duration)
    {
        this.duration = duration;
        //noinspection unchecked
        return (B) this;
    }

    /**
     * @see IPlayable#getDuration()
     */
    @Nullable
    public Long getDuration()
    {
        return duration;
    }
}
