package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.persist.IPersistable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Color;
import java.beans.PropertyChangeListener;
import java.net.URI;
import java.util.Map;

/**
 * Represents a named handle for an audio, video, or image file or stream.
 *
 * @since 2.8
 */
@Experimental
@NoPluginImplementations
public interface IMedia extends IPersistable
{
    void setMetaData(@Nullable Map<String, String> metaData);

    /**
     * Add a property change listener on all properties.
     *
     * @param propertyChangeListener The listener to register
     */
    void addPropertyChangeListener(@Nonnull PropertyChangeListener propertyChangeListener);

    /**
     * Add a property change listener on the specified property.
     *
     * @param property Property of interest
     * @param propertyChangeListener Listener to register
     */
    void addPropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener propertyChangeListener);

    /**
     * Remove a previously registered listener.
     *
     * @param propertyChangeListener The listener to unregister
     */
    void removePropertyChangeListener(@Nonnull PropertyChangeListener propertyChangeListener);

    /**
     * Remove a property change listener on the specified property.
     *
     * @param property Property of interest
     * @param propertyChangeListener Listener to unregister
     */
    void removePropertyChangeListener(@Nonnull String property, @Nonnull PropertyChangeListener propertyChangeListener);

    /**
     * Get the name of the IMedia
     *
     * @return The media's name
     */
    @Nonnull
    String getName();

    /**
     * Get the key of the source of this media.
     *
     * @return the key
     */
    @Nonnull
    String getKeyOfSource();

    /**
     * Get the key of this media.
     *
     * @return the key
     * @since 2.37
     */
    @Nonnull
    String getKey();

    /**
     * Get the media's source URI.
     *
     * @return The media's URI
     * @see #setSourceUri(URI)
     * @since 5.0
     */
    @Nonnull
    URI getSourceUri();

    /**
     * Set the source URI of the media.
     * @since 5.0
     */
    void setSourceUri(@Nonnull URI sourceUri);

    /**
     * Get the local file path if our source URI has a "file" protocol.
     *
     * @return The path to the local file, or {@code null} if the source URI protocol isn't "file"
     * @see #getSourceUri()
     * @since 5.0
     */
    @Nullable
    String getLocalFilePath();

    /**
     * Get the URL to the media's thumbnail if it exists
     *
     * @return The URL of the thumbnail (may be {@code null})
     */
    @Nullable
    String getThumbnailURL();

    /**
     * Set the URL to the media's thumbnail
     *
     * @param thumbnailURL URL to the thumbnail image
     */
    void setThumbnailURL(@Nullable String thumbnailURL);

    /**
     * Get the file modification time
     *
     * @return The time in milliseconds since the epoch, UTC (may be {@code null})
     * @since 2.15
     */
    @Nullable
    Long getLastModified();

    /**
     * Set the file modification time
     *
     * @param lastModified The time in milliseconds since the epoch, UTC
     * @since 2.15
     */
    void setLastModified(@Nullable Long lastModified);

    /**
     * If it's a file this will return the size/length in bytes of the file
     *
     * @return The file size in bytes (may be {@code null})
     */
    @Nullable
    Long getFileSize();

    /**
     * Set the size/length of the file in bytes
     *
     * @param fileSize The file size in bytes
     */
    void setFileSize(@Nullable Long fileSize);

    /**
     * Get generic information field displayed in the media list
     *
     * @return The info string (may be {@code null})
     * @since 2.20
     */
    @Nullable
    String getInfo();

    /**
     * A generic field that will be displayed in the media list
     *
     * @param info The value to display
     * @since 2.20
     */
    void setInfo(@Nullable String info);

    /**
     * Set the geolocation
     *
     * @param location The location
     */
    void setLocation(@Nullable ILocation location);

    /**
     * Get the geolocation
     *
     * @return The geo-location (may be {@code null})
     */
    @Nullable
    ILocation getLocation();

    /**
     * Set the time (e.g., when the media was recorded)
     *
     * @param time The time in milliseconds since the epoch UTC
     * @since 2.21
     */
    void setTime(@Nullable Long time);

    /**
     * Get the time (e.g., when the media was recorded)
     *
     * @return The time in milliseconds since the epoch UTC (may be {@code null})
     * @since 2.21
     */
    @Nullable
    Long getTime();

    /**
     * Get the color associated with this media entity
     *
     * @return the color encode as ARGB
     * @since 2.37
     */
    @Nullable
    Color getColor();

    /**
     * Set the color associated with this media entity
     *
     * @param color encoded as ARGB
     * @since 2.37
     */
    void setColor(@Nullable Color color);

    /**
     * Metadata associated with the media that will be displayed in the UI
     *
     * @return key, value pairs of meta data
     */
    @Nullable
    Map<String, String> getMetaData();

    /**
     * Get the format for this media, for example "rawvideo".
     *
     * @return The format for this media. Or {@code null} if automatically determined.
     * @since 5.0.0
     */
    @Nullable
    String getFormat();

    /**
     * Set the format for this media. Only needed when there is not enough metadata to determine the format.
     *
     * @param format The format for the media.
     * @since 5.0.0
     */
    void setFormat(@Nullable String format);

    /**
     * Get the named playback options for the media.
     *
     * @return The names and values for playback options.
     * @since 5.0.0
     */
    @Nullable
    Map<String, String> getOptions();

    /**
     * Set the playback options for the media.
     *
     * @param options A map of names and values for this media's options.
     * @since 5.0.0
     */
    void setOptions(@Nullable Map<String, String> options);
}
