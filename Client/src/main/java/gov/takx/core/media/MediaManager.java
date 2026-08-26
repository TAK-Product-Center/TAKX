package gov.takx.core.media;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.media.AudioBuilder;
import gov.takx.api.media.IDecodingOptions;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaFrameIterator;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.IMediaManagerListener;
import gov.takx.api.media.IMediaPlayer;
import gov.takx.api.media.IMediaPlayerManager;
import gov.takx.api.media.ImageBuilder;
import gov.takx.api.media.MediaFormats;
import gov.takx.api.media.MediaKey;
import gov.takx.api.media.MediaPlayerOptions;
import gov.takx.api.media.MediaType;
import gov.takx.api.media.VideoBuilder;
import gov.takx.api.persist.IMediaPersist;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.explorer.IExplorerManager;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.media.MediaExplorable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.weld.exceptions.IllegalArgumentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Manage Media and MediaPlayers
 *
 * @since 2.8
 */
@ApplicationScoped
public class MediaManager implements IMediaManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IMediaPersist mediaPersist;
    private final IExplorerManager explorerManager;
    private final IMediaPlayerManager mediaPlayerManager;

    private final List<IMediaManagerListener> mediaManagerListeners = new CopyOnWriteArrayList<>();
    private final Map<String, IMedia> mediaMap = new ConcurrentHashMap<>();
    private final Map<String, MediaExplorable> explorables = new ConcurrentHashMap<>();

    private volatile boolean running;

    @Inject
    MediaManager(IMediaPersist mediaPersist, IExplorerManager explorerManager, IMediaPlayerManager mediaPlayerManager)
    {
        this.mediaPersist = mediaPersist;
        this.explorerManager = explorerManager;
        this.mediaPlayerManager = mediaPlayerManager;
    }

    @Override
    public void addMediaManagerListener(@Nonnull IMediaManagerListener mediaManagerListener)
    {
        mediaManagerListeners.add(mediaManagerListener);
    }

    @Override
    public void removeMediaManagerListener(@Nonnull IMediaManagerListener mediaManagerListener)
    {
        mediaManagerListeners.remove(mediaManagerListener);
    }

    @Override
    public void addMedia(@Nonnull IMedia media) throws IllegalArgumentException
    {
        addMedia(media, true);
    }

    @Override
    public void addOrReplaceMedia(@Nonnull IMedia media)
    {
        final String key = media.getKey();

        final var existing = mediaMap.get(key);
        if (existing != null) removeMedia(existing);

        addMedia(media, true);
    }

    @Override
    @Nullable
    public <T extends IMedia> T getMedia(@Nonnull String keyOfSource, @Nonnull String name)
    {
        //noinspection unchecked
        return (T) mediaMap.get(MediaKey.makeKey(keyOfSource, name));
    }

    @Override
    public boolean mediaExists(@Nonnull String name, @Nonnull String keyOfSource)
    {
        return mediaMap.containsKey(MediaKey.makeKey(keyOfSource, name));
    }

    @Override
    public void removeMedia(@Nonnull IMedia media) throws NoSuchElementException
    {
        if (!running) return;  // Media maps are cleared when this service stops

        final String key = media.getKey();

        if (!mediaMap.containsKey(key)) throw new NoSuchElementException(key + " has not been registered");

        logger.debug("Remove media: {}", key);

        // Notify listeners
        mediaManagerListeners.forEach(listener -> listener.mediaRemoved(media));

        mediaMap.remove(key);

        // Remove from explorer tree
        explorerManager.getDefaultExplorerDelegate().removeExplorable(explorables.remove(key), false);

        mediaPlayerManager.removeMediaPlayer(media);

        // Remove from project
        mediaPersist.delete(media);
    }

    @Override
    @Nonnull
    public List<IMedia> getAllMedia()
    {
        return new ArrayList<>(mediaMap.values()); // Return a mutable list (in case the caller wants to sort it)
    }

    @Override
    @Nonnull
    public IDecodingOptions createDefaultDecodingOptions()
    {
        return new DecodingOptions();
    }

    @Override
    @Nonnull
    public IMediaFrameIterator createIterator(@Nonnull IMedia media, @Nullable IDecodingOptions options)
    {
        String localFilePath = media.getLocalFilePath();
        String mediaUrl = localFilePath != null ? localFilePath : media.getSourceUri().toString();
        return options == null
                ? new MediaFrameIterator(mediaUrl)
                : new MediaFrameIterator(mediaUrl, options);
    }

    @Override
    @Nonnull
    public String[] getSupportedFormats()
    {
        return MediaFormats.ALL_FORMATS.clone();
    }

    @Override
    @Nonnull
    public MediaType getType(@Nonnull String name)
    {
        // If name is a valid URL and it has no path, assume it's a video.  This also works around an issue where
        // FileUtils.getExtension barfs on Windows if the file name has a colon in it after the last slash, which
        // happens with URLs like 'udp://239.4.1.142:8208'.

        try
        {
            // RTSP URLs are not properly handled by the Java URL class (it throws a MalformedURLException), so
            // we handle it specifically before we get to that point.
            if (name.startsWith("rtsp://"))
            {
                return MediaType.Video;
            } else if (name.startsWith("udp://"))
            {
                // UDP has no path but can't be checked in the URL path check below because udp links aren't valid URLs
                // according to Java.
                return StringUtils.isEmpty(URI.create(name).getPath()) ? MediaType.Video : MediaType.Unknown;
            }

            final URL url = new URL(name);
            final String path = url.getPath();

            // Assume it's a video if no path (protocols like UDP and SRT have no path)
            if (StringUtils.isEmpty(path)) return MediaType.Video;

            // If it has a path AND that path has an extension, then lookup the type from that, else assume it's a video.
            final String extension = FileUtils.getExtension(name);
            return extension != null
                    ? MediaType.getMediaTypeForExtension(extension)
                    : MediaType.Video;
        } catch (MalformedURLException e)
        {
            // not a valid URL, ignore
            logger.debug("Bad URL: {}", name, e);
        }

        // Not a URL, so assume it is a local file path
        try
        {
            return MediaType.getMediaTypeForExtension(FileUtils.getExtension(name));
        } catch (Exception e)
        {
            logger.warn("Failed to get extension from '{}' : {}", name, e.getMessage());
            return MediaType.Unknown;
        }
    }

    @Override
    @Nonnull
    public String createUniqueName(@Nonnull String baseName)
    {
        String name = baseName;
        int i = 0;
        while (nameInUse(name)) name = baseName + " (" + ++i + ")";
        return name;
    }

    private boolean nameInUse(String name)
    {
        return mediaMap.keySet().stream().anyMatch(key -> MediaKey.nameFromKey(key).equals(name));
    }

    /**
     * Add the media to our collection, notify listeners and adds it to UIs.
     *
     * @param media   The media to add
     * @param persist If {@code true} the media will be persisted
     */
    private void addMedia(@Nonnull IMedia media, boolean persist) throws IllegalArgumentException
    {
        final String key = media.getKey();
        logger.debug("Add media: {}", key);

        if (mediaMap.containsKey(key)) throw new IllegalArgumentException("Media with the key '" + key + "' already exists");

        extractMetaData(media);
        if (persist) mediaPersist.persist(media);

        mediaMap.put(key, media);

        mediaManagerListeners.forEach(listener -> listener.mediaAdded(media)); // Notify listeners

        // Add an explorable for the new media
        final var explorable = new MediaExplorable(this, mediaPlayerManager, media);
        explorerManager.getDefaultExplorerDelegate().addExplorable(explorable);
        explorables.put(key, explorable);
    }

    /**
     * Start the media manager service on the project. Load media from the DB.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored) throws IllegalArgumentException
    {
        logger.debug("Starting media manager service");

        // Query the DB for the list of media to restore
        mediaPersist.queryAll().forEach(media -> addMedia(media, false));

        running = true;
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        logger.debug("Stopping media manager service");

        mediaMap.values().forEach(mediaPersist::persist); // Persist all our media

        mediaMap.clear();
        explorables.clear();

        mediaManagerListeners.forEach(IMediaManagerListener::serviceStopped);
        mediaManagerListeners.clear();

        running = false;
    }

    /**
     * Delete media associated with the Map Object when it is deleted.
     * @since 5.0
     */
    private void onMapObjectRemoved(@Observes MapObjectRemoved event) throws IllegalArgumentException
    {
        // This seems fundamentally busted.  AFAICT, none of the plugins that create media use the MOs key
        // as the "keyOfSource" when doing so.  So far, they all seem to use the MO family.  So, removing entries
        // that start with the MOs key would seem to be doing nothing.  But, if I just use the family, I could be
        // deleting a bunch of media not associated with the specific MO that has been deleted.
        //
        // Honestly, except by convention, there is no mechanism to associate a media object with a MO.

        final String targetKey = event.getMapObject().getKey();

        final List<IMedia> toDelete = mediaMap.values().stream()
                .filter(media -> media.getKeyOfSource().equals(targetKey))
                .toList();

        toDelete.forEach(this::removeMedia);
    }

    @Override
    public void createMediaPlayer(@Nonnull MediaPlayerOptions options, @Nonnull IMediaPlayer player)
    {
        player.getMedia().ifPresent(media -> {
            final String mediaKey = media.getKey();
            final IMedia existingMedia = mediaMap.get(mediaKey);

            if (existingMedia == null)
            {
                addMedia(media);
                mediaPlayerManager.createMediaPlayer(options, media, player);
            } else
            {
                // The panel already exists, so just replace the player inside of it
                final String uuid = options.getUUID();
                final String playerUuid = uuid != null ? uuid : mediaKey;
                mediaPlayerManager.setMediaPlayer(playerUuid, player);
            }
        });
    }

    @Override
    @Nonnull
    public IMedia addMediaFromLocalFile(@Nonnull File file, @Nonnull String keyOfSource) throws IllegalArgumentException
    {
        final var media = createMediaFromLocalFile(file, keyOfSource);
        addMedia(media);
        return media;
    }

    @Override
    @Nonnull
    public IMedia createMediaFromLocalFile(@Nonnull File file, @Nonnull String keyOfSource) throws IllegalArgumentException
    {
        if (!file.exists()) throw new IllegalArgumentException("File must exist: " + file);
        final String fileName = file.getName();
        final String mediaName = createUniqueName(fileName);
        final URI fileUri = file.toURI();

        logger.debug("adding '{}' as '{}'", fileName, mediaName);

        final var builder = switch (getType(file.getName()))
        {
            case Image -> new ImageBuilder(mediaName, keyOfSource, fileUri);
            case Video -> new VideoBuilder(mediaName, keyOfSource, fileUri);
            case Audio -> new AudioBuilder(mediaName, keyOfSource, fileUri);
            case Unknown -> throw new IllegalArgumentException("Unsupported media extension for file " + mediaName);
        };
        return builder.setMetaData(extractMetaData(file)).build();
    }

    @Override
    @Nonnull
    public Map<String, String> extractMetaData(@Nonnull File file)
    {
        return ImageUtils.extractMetaData(file);
    }

    /**
     * Analyze the media to extract specific metadata (date, time and location)
     *
     * @param media Media to analyze
     */
    private void extractMetaData(IMedia media)
    {
        // Currently supports local files only
        final String localFile = media.getLocalFilePath();
        if (localFile == null) return;

        try
        {
            final Metadata metadata = ImageMetadataReader.readMetadata(new File(localFile));

            final ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);

            if (directory != null)
            {
                // Get time
                final var date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                media.setTime(date != null ? date.getTime() : null);
            }

            // TODO: Get location

        } catch (Exception e)
        {
            // It is pretty common that we won't be able to read metadata- it may not be there, or we may not know
            // its format
            logger.debug("Could not read metadata from file", e);
        }
    }
}
