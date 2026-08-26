package gov.takx.core.media;

import gov.tak.api.video.ConnectionEntry;
import gov.tak.api.video.IVideoConnectionManager;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.media.IMedia;
import gov.takx.api.media.IMediaManager;
import gov.takx.api.media.IMediaManagerListener;
import gov.takx.api.media.IVideo;
import gov.takx.api.media.MediaManagerListenerAdapter;
import gov.takx.api.media.VideoBuilder;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import static gov.tak.api.video.cot.VideoDetailHandler.CONNECTION_ENTRY_ATTR;
import static gov.tak.api.video.cot.VideoDetailHandler.CONNECTION_ENTRY_UID_ATTR;
import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;

/**
 * Service to handle and track {@link ConnectionEntry Connection Entries}.
 * <p/>
 * <bold>Important:</bold> This service is currently focused on supporting ingest of {@link ConnectionEntry connection entries}
 * via CoT.  It should not otherwise be used directly to add/remove connection entries.  That will happen
 * automatically when {@link IVideo} media is added/removed using the {@link IMediaManager media manager}.  If a video
 * needs to be defined by an existing {@link ConnectionEntry} (e.g., from a downloaded
 * {@link gov.tak.api.comms.takserver.mission.data.Feed video feed}), then it should be added to the
 * {@link gov.takx.api.media.VideoBuilder video builder} via {@link gov.takx.api.media.VideoBuilder#setConnectionEntry(ConnectionEntry)}.
 *
 * @since 5.3
 */
@ApplicationScoped
public class VideoConnectionManager implements IVideoConnectionManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final IMediaManager mediaManager;

    private final Map<String, ConnectionEntry> connectionEntriesByUid = new ConcurrentHashMap<>();
    private final Map<String, ConnectionEntry> connectionEntriesByMediaKey = new ConcurrentHashMap<>();
    private final Map<String, IVideo> mediaByConnectionEntryUid = new ConcurrentHashMap<>();
    private final Set<ConnectionListener> listeners = new CopyOnWriteArraySet<>();
    private final IMediaManagerListener mediaManagerListener = new MediaManagerListener();

    @Inject
    VideoConnectionManager(IMediaManager mediaManager)
    {
        this.mediaManager = mediaManager;
    }

    // need to start before IMediaManager so we can receive listener callbacks
    private void startService(@Observes @Priority(START_GenericCoreService - 1) StartProjectServices event)
    {
        mediaManager.addMediaManagerListener(mediaManagerListener);
    }

    /**
     * Remove any connection entry associated with a MO being deleted.
     */
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        final var mapObject = event.getMapObject();
        if (mapObject.hasMetaValue(CONNECTION_ENTRY_ATTR))
        {
            final var ceAttrs = mapObject.getAttributeSet().getAttributeSetAttribute(CONNECTION_ENTRY_ATTR);
            final var ceUid = ceAttrs.getStringAttribute(CONNECTION_ENTRY_UID_ATTR, "");
            removeConnectionEntry(ceUid);
        }
    }

    @Nullable
    @Override
    public ConnectionEntry getConnectionEntry(@Nonnull String uid)
    {
        return connectionEntriesByUid.get(uid);
    }

    /**
     * @param media An {@link IMedia} related to a given {@link ConnectionEntry}
     * @return A {@link ConnectionEntry} related to the provided {@link IMedia}, if one exists and {@code null} if not
     */
    @Nullable
    public ConnectionEntry getConnectionEntry(@Nonnull IMedia media)
    {
        return connectionEntriesByMediaKey.get(media.getKey());
    }

    @Nonnull
    @Override
    public List<ConnectionEntry> getConnectionEntries(@Nullable Set<String> uids, boolean remoteOnly)
    {
        return connectionEntriesByUid.values().stream()
                .filter(entry -> (uids == null || uids.contains(entry.getUID())) && (!remoteOnly || entry.isRemote()))
                .collect(Collectors.toList());
    }

    /**
     * This is called from the kernel detail handler.
     */
    @Override
    public void addConnectionEntry(@Nonnull ConnectionEntry connectionEntry)
    {
        final var connectionEntryUid = connectionEntry.getUID();

        // remove any prior media we created with this entry's UID
        final var existingMedia = mediaByConnectionEntryUid.get(connectionEntryUid);
        if (existingMedia != null) mediaManager.removeMedia(existingMedia);

        // Only create new media if the connection address is not empty
        if (!StringUtils.isEmpty(connectionEntry.getAddress(), true))
        {
            final String url = ConnectionEntry.getURL(connectionEntry, false);
            final URI sourceUri;
            try
            {
                sourceUri = new URI(url);
            } catch (URISyntaxException e)
            {
                logger.warn("Invalid media URI: {}", url, e);
                return;
            }

            // We use the UID of the connection entry as the "key of source" to ensure uniqueness.
            final var media = new VideoBuilder(connectionEntry.getAlias(), connectionEntryUid, sourceUri)
                    .setConnectionEntry(connectionEntry)
                    .build();

            // Track locally created media
            mediaByConnectionEntryUid.put(connectionEntryUid, media);

            mediaManager.addMedia(media);
        }
    }

    @Override
    public void addConnectionEntries(@Nonnull List<ConnectionEntry> newEntries)
    {
        newEntries.forEach(this::addConnectionEntry);
    }

    private void addConnectionEntry(IVideo video)
    {
        // If the media has a connection entry already associated with it, then we want to use that instead of creating a new one
        var entry = video.getConnectionEntry();
        if (entry == null)
        {
            entry = new ConnectionEntry(video.getName(), video.getSourceUri().toString());
            entry.setUID(UUID.randomUUID().toString());
        } else
        {
            // This occurs when restoring a Media when re-opening a project, so record the linkage
            mediaByConnectionEntryUid.put(entry.getUID(), video);
        }

        connectionEntriesByUid.put(entry.getUID(), entry);
        connectionEntriesByMediaKey.put(video.getKey(), entry);
    }

    @Override
    public void removeConnectionEntry(@Nonnull String uid)
    {
        final var connectionEntry = connectionEntriesByUid.remove(uid);
        if (connectionEntry != null)
        {
            final IVideo removedVideo = mediaByConnectionEntryUid.remove(uid);
            if (removedVideo != null) mediaManager.removeMedia(removedVideo);

            ExceptionUtils.safeDoOperation(listeners, listener -> listener.onEntryRemoved(connectionEntry));
        }
    }

    @Override
    public void removeConnectionEntry(@Nonnull ConnectionEntry connectionEntry)
    {
        removeConnectionEntry(connectionEntry.getUID());
    }

    /**
     * Remove the {@link ConnectionEntry} associated with the provided {@link IVideo video}
     * @param video The {@link IVideo} to identify the correct {@link ConnectionEntry} to remove
     */
    private void removeConnectionEntry(@Nonnull IVideo video)
    {
        final var connectionEntry = video.getConnectionEntry();
        if (connectionEntry != null)
        {
            final String uid = connectionEntry.getUID();
            mediaByConnectionEntryUid.remove(uid);
            connectionEntriesByUid.remove(uid);
            connectionEntriesByMediaKey.remove(video.getKey());
        }
    }

    @Override
    public void removeConnectionEntries(@Nonnull Set<String> uids)
    {
        uids.forEach(this::removeConnectionEntry);
    }

    @Override
    public void addConnectionListener(@Nonnull ConnectionListener connectionListener)
    {
        listeners.add(connectionListener);
    }

    @Override
    public void removeConnectionListener(@Nonnull ConnectionListener connectionListener)
    {
        listeners.remove(connectionListener);
    }

    /**
     * We don't store our connection entries in the file system, so just return a File referring to the temp directory.
     */
    @Nonnull
    @Override
    public File getConnectionEntriesDirectory()
    {
        return new File(System.getProperty("java.io.tmpdir"));
    }

    /**
     * Listener to respond to media manager operations we care about.
     */
    private class MediaManagerListener extends MediaManagerListenerAdapter
    {
        @Override
        public void mediaAdded(IMedia media)
        {
            if (media instanceof IVideo video) addConnectionEntry(video);
        }

        @Override
        public void mediaRemoved(IMedia media)
        {
            if (media instanceof IVideo video) removeConnectionEntry(video);
        }
    }
}
