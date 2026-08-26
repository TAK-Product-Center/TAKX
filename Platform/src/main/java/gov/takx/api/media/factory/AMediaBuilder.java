package gov.takx.api.media.factory;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.media.IMedia;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.jboss.weld.exceptions.IllegalArgumentException;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;

/**
 * Abstract base for builders to create media objects for registration with the {@link gov.takx.api.media.IMediaManager}.
 * Registered media will be available for display in the media viewer.
 *
 * @param <M> Media interface type
 * @param <T> Concrete builder type
 */
@Experimental
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public abstract class AMediaBuilder<M extends IMedia, T extends AMediaBuilder<M, T>>
{
    @Inject
    protected IMediaFactory mediaFactory;

    private final String name;
    private final String keyOfSource;

    private URI sourceUri = null;
    private String thumbnailURL = null;
    private Long lastModified = null;
    private Long fileSize = null;
    private Map<String, String> metaData = null;
    private ILocation location;
    private String info;

    /**
     * @param name        The display name for the media. Immutable.
     * @param keyOfSource The unique key for the source that created / provided the media. This is most likely a
     *                    plugin's key if created by a plugin or "user" if the user directly added the media.
     *                    If the keyOfSource is the unique key of a Map Object, then when/if the Map Object is deleted,
     *                    all the media with that key will also be deleted.
     *                    Immutable.
     */
    protected AMediaBuilder(@Nonnull String name, @Nonnull String keyOfSource)
    {
        if (name.isBlank()) throw new IllegalArgumentException("'name' must not be empty");
        if (keyOfSource.isBlank()) throw new IllegalArgumentException("'keyOfSource' must not be empty");

        this.name = name;
        this.keyOfSource = keyOfSource;
    }

    /**
     * @param name        The display name for the media. Immutable.
     * @param keyOfSource The unique key for the source that created / provided the media. This is most likely a
     *                    plugin's key if created by a plugin or "user" if the user directly added the media. Immutable.
     * @param sourceUri   media Source URI
     * @since 5.0
     */
    protected AMediaBuilder(@Nonnull String name, @Nonnull String keyOfSource, @Nonnull URI sourceUri)
    {
        this(name, keyOfSource);
        this.sourceUri = sourceUri;
    }

    /**
     * @see IMedia#setSourceUri(URI)
     * @since 5.0
     */
    @Nonnull
    public T setSourceUri(@Nonnull URI sourceUri)
    {
        this.sourceUri = sourceUri;
        //noinspection unchecked
        return (T) this;
    }

    /**
     * @see IMedia#setInfo(String)
     * @since 2.20
     */
    @Nonnull
    public T setInfo(@Nullable String info)
    {
        this.info = info;
        //noinspection unchecked
        return (T) this;
    }

    /**
     * @see IMedia#setThumbnailURL(String)
     * @throws IllegalArgumentException if the given url cannot be parsed by {@link URL}
     */
    @Nonnull
    public T setThumbnailURL(@Nullable String thumbnailURL) throws IllegalArgumentException
    {
        if (thumbnailURL != null)
        {
            try
            {
                //noinspection ResultOfObjectAllocationIgnored
                new URL(thumbnailURL);
            } catch (MalformedURLException e)
            {
                throw new IllegalArgumentException("Malformed URL: " + thumbnailURL, e);
            }
        }

        this.thumbnailURL = thumbnailURL;
        //noinspection unchecked
        return (T) this;
    }

    @Nonnull
    public T setLastModified(long lastModified)
    {
        this.lastModified = lastModified;
        //noinspection unchecked
        return (T) this;
    }

    @Nonnull
    public T setFileSize(long fileSize)
    {
        this.fileSize = fileSize;
        //noinspection unchecked
        return (T) this;
    }

    @Nonnull
    public T setLocation(@Nullable ILocation location)
    {
        this.location = location;
        //noinspection unchecked
        return (T) this;
    }

    @Nonnull
    public T setMetaData(@Nullable Map<String, String> metaData)
    {
        this.metaData = metaData == null ? null : Map.copyOf(metaData);
        //noinspection unchecked
        return (T) this;
    }

    @Nullable
    public ILocation getLocation()
    {
        return location;
    }

    @Nonnull
    public String getName()
    {
        return name;
    }

    @Nonnull
    public String getKeyOfSource()
    {
        return keyOfSource;
    }

    /** @since 5.0 */
    @Nonnull
    public URI getSourceUri()
    {
        return sourceUri;
    }

    /**
     * @see IMedia#setInfo(String)
     * @since 2.20
     */
    @Nullable
    public String getInfo()
    {
        return info;
    }

    @Nullable
    public String getThumbnailURL()
    {
        return thumbnailURL;
    }

    @Nullable
    public Long getLastModified()
    {
        return lastModified;
    }

    @Nullable
    public Long getFileSize()
    {
        return fileSize;
    }

    @Nullable
    public Map<String, String> getMetaData()
    {
        //noinspection AssignmentOrReturnOfFieldWithMutableType
        return metaData;
    }

    /**
     * @return media constructed from the builder configuration
     * @since 5.0
     */
    public abstract M build();
}
