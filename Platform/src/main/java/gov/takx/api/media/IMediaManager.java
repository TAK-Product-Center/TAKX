package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
 * Interface to provide centralized management of media UI elements.
 *
 * @since 2.8
 */
@InjectableService
@Experimental
@NoPluginImplementations
public interface IMediaManager
{
    /**
     * Registers to listen for media manager events
     *
     * @param mediaManagerListener The listener to add
     * @since 2.15
     */
    void addMediaManagerListener(@Nonnull IMediaManagerListener mediaManagerListener);

    /**
     * Unregisters for media manager events
     *
     * @param mediaManagerListener The listener to remove
     * @since 2.15
     */
    void removeMediaManagerListener(@Nonnull IMediaManagerListener mediaManagerListener);

    /**
     * Add the given media object to the library, making it available for user interaction.
     * Media must be constructed using the appropriate builder.
     *
     * @param media The media to add
     * @see AudioBuilder
     * @see ImageBuilder
     * @see VideoBuilder
     * @since 5.0
     */
    void addMedia(@Nonnull IMedia media);

    /**
     * Add the given media, replacing any existing media if one exists with the same key.
     *
     * @param media new media
     * @see IMedia#getKey()
     * @see #removeMedia(IMedia)
     * @see #addMedia(IMedia)
     * @since 5.3
     */
    void addOrReplaceMedia(@Nonnull IMedia media);

    /**
     * Gets the media with the given source and name.
     *
     * @return The referenced media, or {@code null} if not found
     * @since 5.0
     */
    @Nullable
    <T extends IMedia> T getMedia(@Nonnull String keyOfSource, @Nonnull String name);

    /**
     * Gets the audio with the given source and name.
     *
     * @return The referenced audio, or {@code null} if not found
     * @since 2.15
     * @throws ClassCastException If a media exists with the given key, but is not of type {@link IAudio}
     */
    @Nullable
    default IAudio getAudio(@Nonnull String keyOfSource, @Nonnull String name) throws ClassCastException
    {
        return getMedia(keyOfSource, name);
    }

    /**
     * Gets the video with the given source and name.
     *
     * @return The referenced video, or {@code null} if not found
     * @since 2.15
     * @throws ClassCastException If a media exists with the given key, but is not of type {@link IVideo}
     */
    @Nullable
    default IVideo getVideo(@Nonnull String keyOfSource, @Nonnull String name) throws ClassCastException
    {
        return getMedia(keyOfSource, name);
    }

    /**
     * Gets the image with the given source and name.
     *
     * @return The referenced image, or {@code null} if not found
     * @since 2.15
     * @throws ClassCastException If a media exists with the given key, but is not of type {@link IImage}
     */
    @Nullable
    default IImage getImage(@Nonnull String keyOfSource, @Nonnull String name) throws ClassCastException
    {
        return getMedia(keyOfSource, name);
    }

    /**
     * Create and add a media object from the given local file.  The media type will be determined from the file's
     * extension.
     *
     * @param file Source of media
     * @param keyOfSource The key for the source of the media
     * @return Constructed media object
     * @throws IllegalArgumentException if the given file has an extension that is unsupported
     * @see #getType(String)
     * @see MediaFormats
     * @since 3.5
     */
    @Nonnull
    IMedia addMediaFromLocalFile(@Nonnull File file, @Nonnull String keyOfSource) throws IllegalArgumentException;

    /**
     * Creates a media object from the given local file.  The media type will be determined from the file's
     * extension.
     *
     * @param file Source of media
     * @param keyOfSource The key for the source of the media
     * @return Constructed media object
     * @throws IllegalArgumentException if the given file has an extension that is unsupported
     * @see #getType(String)
     * @see MediaFormats
     * @since 5.2
     */
    @Nonnull
    IMedia createMediaFromLocalFile(@Nonnull File file, @Nonnull String keyOfSource) throws IllegalArgumentException;

    /**
     * Remove media from the project.
     *
     * @param media The media to remove
     * @throws IllegalArgumentException if media does not exist
     */
    void removeMedia(@Nonnull IMedia media) throws IllegalArgumentException;

    /**
     * Get all media in the project.
     *
     * @return Unmodifiable collection of media.
     */
    @Nonnull
    List<IMedia> getAllMedia();

    /**
     * Generate an automatically numbered name from the base name.
     *
     * @param baseName The name to which a number will be appended
     * @return A unique name in the form baseName (i)
     */
    @Nonnull
    String createUniqueName(@Nonnull String baseName);

    /**
     * Create a panel for the provided {@link IMediaPlayer} and the provided initial player options defined in the
     * provided {@link MediaPlayerOptions}.
     *
     * @param options options for configuring the {@link IMediaPlayer}
     * @param player  A plugin is allowed to supply its own player for custom stream and/or file types, or for
     *                custom rendering requirements
     * @since 2.37
     */
    void createMediaPlayer(@Nonnull MediaPlayerOptions options, @Nonnull IMediaPlayer player);

    /**
     * Create a set of default options for playing or iterating a media resource.
     *
     * @return the default options
     * @since 5.0 (renamed from createDefaultMediaOptions, which was confusing since there is a MediaPlayerOptions class as well)
     */
    @Nonnull
    IDecodingOptions createDefaultDecodingOptions();

    /**
     * Create an iterator to get each frame of a media resource. Users MUST call {@link IMediaFrameIterator#close()}
     * when finished with the iterator.  The iterator will be created using default decoding options, which means
     * all frame types will be decoded.
     *
     * @param media   the media to iterate over
     * @return The media frame iterator for the provided media and configured with default decoding options
     * @since 5.0
     */
    @Nonnull
    default IMediaFrameIterator createIterator(@Nonnull IMedia media)
    {
        return createIterator(media, null);
    }

    /**
     * Create an iterator to get each frame of a media resource. Users MUST call {@link IMediaFrameIterator#close()}
     * when finished with the iterator.
     *
     * @param media   the media to iterate over
     * @param options The decoding options, specifying which stream types will be decoded.  If null, default options will be used.
     * @return The media frame iterator for the provided media and configured with the provided decoding options
     */
    @Nonnull
    IMediaFrameIterator createIterator(@Nonnull IMedia media, @Nullable IDecodingOptions options);

    /**
     * Get an array of supported media formats (file extensions known to work with TAKX).
     *
     * @return Array of supported file extensions
     */
    @Nonnull
    String[] getSupportedFormats();

    /**
     * Attempt to determine the type of media based on known extensions (.mpg, .jpg, etc.).
     * <p>
     * NOTE: URLs often do not contain file extensions, in which case this method will return {@link MediaType#Unknown}
     *
     * @param name The name whose file extension will be analyzed
     * @return The associated media type or {@link MediaType#Unknown}
     */
    @Nonnull
    MediaType getType(@Nonnull String name);

    /**
     * Attempt to extract metadata (e.g., EXIF) from the given file.
     *
     * @param file The file from which to extract metadata
     * @return Any extracted metadata, as (key, value) pairs, or empty map if none
     * @since 2.17
     */
    @Nonnull
    Map<String, String> extractMetaData(@Nonnull File file);

    /**
     * Check if a media object exists with the given source key and name.
     *
     * @param name        Media name
     * @param keyOfSource The key for the source of the media
     * @return {@code true} if media exists under that key, {@code false} otherwise
     * @since 5.0
     */
    boolean mediaExists(@Nonnull String name, @Nonnull String keyOfSource);
}
