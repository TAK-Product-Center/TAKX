package gov.takx.api.media;

import gov.tak.api.video.ConnectionEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;

/**
 * @since 2.15
 */
public class VideoBuilder extends APlayableMediaBuilder<IVideo, VideoBuilder>
{
    private ConnectionEntry connectionEntry;

    public VideoBuilder(@Nonnull String name, @Nonnull String keyOfSource)
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
    public VideoBuilder(@Nonnull String name, @Nonnull String keyOfSource, @Nonnull URI sourceUri)
    {
        super(name, keyOfSource, sourceUri);
    }

    /**
     * @param connectionEntry the connection entry that is defining this media instance
     * @return This builder
     * @since 5.3
     */
    @Nonnull
    public VideoBuilder setConnectionEntry(@Nonnull ConnectionEntry connectionEntry)
    {
        this.connectionEntry = connectionEntry;
        return this;
    }

    /**
     *
     * @return the connection entry that is defining this media instance
     * @since 5.3
     */
    @Nullable
    public ConnectionEntry getConnectionEntry()
    {
        return connectionEntry;
    }

    @Override
    @Nonnull
    public IVideo build()
    {
        return mediaFactory.build(this);
    }
}
