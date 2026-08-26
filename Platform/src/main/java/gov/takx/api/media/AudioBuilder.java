package gov.takx.api.media;

import gov.takx.api.mapobject.IMapObject;
import jakarta.annotation.Nonnull;

import java.net.URI;

public class AudioBuilder extends APlayableMediaBuilder<IAudio, AudioBuilder>
{
    public AudioBuilder(@Nonnull String name, @Nonnull String keyOfSource)
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
     * @see IMapObject#getKey()
     * @since 5.0
     */
    public AudioBuilder(@Nonnull String name, @Nonnull String keyOfSource, @Nonnull URI sourceUri)
    {
        super(name, keyOfSource, sourceUri);
    }

    @Override
    @Nonnull
    public IAudio build()
    {
        return mediaFactory.build(this);
    }
}
