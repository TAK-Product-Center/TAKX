package gov.takx.core.kml;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import de.micromata.opengis.kml.v_2_2_0.Link;
import de.micromata.opengis.kml.v_2_2_0.NetworkLink;
import de.micromata.opengis.kml.v_2_2_0.NetworkLinkControl;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.tasks.ITaskManager;
import gov.takx.api.tasks.ITaskStatusHandler;
import gov.takx.api.tasks.TaskGroup;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.kml.event.KmlModelAdded;
import gov.takx.core.kml.event.KmlModelRemoved;
import gov.takx.core.kml.event.NetworkLinkUpdated;
import gov.takx.core.tasks.AManagedTask;
import gov.takx.dataservices.utils.HashUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Service to manage refreshing {@link NetworkLink} contents on a timed basis according to their configured parameters.
 *
 * @see NetworkLink
 * @see NetworkLinkControl
 * @since 5.5
 */
@ApplicationScoped
public class NetworkLinkRefresher
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final File KML_DOWNLOAD_DIR = new File(System.getProperty("java.io.tmpdir"), "KML_NETWORK_LINKS");
    private static final List<String> HTTP_PREFIXES = List.of("http:", "https:");
    private static final Double MIN_REFRESH_INTERVAL_SECS = 10d;
    private static final NetworkLinkControl DEFAULT_NETWORK_LINK_CONTROL = new NetworkLinkControl()
            .withMinRefreshPeriod(0)
            .withMaxSessionLength(-1);

    private final ICoreExecutorService executorService;
    private final ITaskManager taskManager;
    private final KmlVisualManager kmlVisualManager;
    private final INotificationService notificationService;
    private final Event<NetworkLinkUpdated> networkLinkUpdatedEvent;

    private final Map<NetworkLink, RefreshState> refreshStates = Collections.synchronizedMap(new IdentityHashMap<>());

    @Inject
    NetworkLinkRefresher(ICoreExecutorService executorService, ITaskManager taskManager, KmlVisualManager kmlVisualManager,
                         INotificationService notificationService, Event<NetworkLinkUpdated> networkLinkUpdatedEvent)
    {
        this.executorService = executorService;
        this.taskManager = taskManager;
        this.kmlVisualManager = kmlVisualManager;
        this.notificationService = notificationService;
        this.networkLinkUpdatedEvent = networkLinkUpdatedEvent;
    }

    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices ignored)
    {
        synchronized (refreshStates)
        {
            refreshStates.values().forEach(this::cancelDownload);
            refreshStates.clear();
        }
    }

    /**
     * Process a new KML model being imported, initiate refresh on any contained network links.
     */
    private void onKmlModelAdded(@Observes KmlModelAdded event)
    {
        final var rootModel = event.kmlModel();
        rootModel.visitFeaturesOfType(NetworkLink.class, networkLink -> refresh(rootModel, networkLink, false));
    }

    /**
     * Process a KML model being removed, cancel all refresh futures, and cleanup all sub-models, removing visuals.
     */
    private void onKmlModelRemoved(@Observes KmlModelRemoved event)
    {
        event.kmlModel().visitFeaturesOfType(NetworkLink.class, networkLink -> stopRefreshAndRemoveVisuals(refreshStates.remove(networkLink)));
    }

    /**
     * Initiate a refresh on the given network link.  This will immediately try to download the referenced content,
     * and will then schedule any automatic updates as configured in the link.
     *
     * @param rootModel Top-level imported KML model
     * @param networkLink Network link to process
     * @param pause Pass {@code true} to pause auto-refresh updates after the content is refreshed
     */
    public void refresh(@Nonnull KmlModel rootModel, @Nonnull NetworkLink networkLink, boolean pause)
    {
        final var state = refreshStates.computeIfAbsent(networkLink, k -> new RefreshState(rootModel, networkLink));
        state.downloadPaused = pause;

        logger.debug("refresh state = {}", state);

        cancelDownload(state); // Cancel any pending download task since a new refresh has been requested
        initiateRefresh(state);
        scheduleNextDownload(state); // And reschedule downloads (since we canceled it above)
    }

    /**
     * Submit a {@link RefreshTask} to the task manager.
     * @since 5.6
     */
    private void initiateRefresh(@Nonnull RefreshState state)
    {
        Thread.currentThread().setName("Network Link Refresh: " + state.networkLink.getName());
        taskManager.execute(new RefreshTask(state));
    }

    /**
     * Pause any automatic refresh of the given {@code networkLink} and any nested network links.
     */
    public void pauseRefresh(@Nonnull NetworkLink networkLink)
    {
        pauseDownloads(networkLink);
    }

    /**
     * Cancel the download for the given link, and recursively do the same for any network links it contains.
     */
    private void pauseDownloads(@Nonnull NetworkLink networkLink)
    {
        final var state = refreshStates.get(networkLink);
        if (state != null)
        {
            cancelDownload(state);
            state.downloadPaused = true;
            state.model.visitFeaturesOfType(NetworkLink.class, this::pauseDownloads);
        }
    }

    /**
     * Download the contents of a network link and process it.
     */
    private void processDownload(@Nonnull RefreshState state)
    {
        Thread.currentThread().setName("Download NetworkLink: " + state.networkLink.getName());

        logger.debug("processDownload: {}", state);

        if (sessionLengthExceeded(state))
        {
            cancelDownload(state); // Don't keep trying
            return;
        }

        final File outputFile = download(state);
        if (outputFile == null) return;

        updateLinkContent(state, outputFile);
        scheduleNextDownload(state);
    }

    /**
     * @return {@code true} if we've exceeded the max session time in the link's {@link NetworkLinkControl}.
     */
    private static boolean sessionLengthExceeded(@Nonnull RefreshState state)
    {
        final long sessionLength = TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - state.sessionStart);
        final double maxSessionLength = state.control.getMaxSessionLength();
        if (maxSessionLength > 0 && sessionLength > maxSessionLength)
        {
            logger.debug("Max session length of {} exceeded on NetworkLink {}", maxSessionLength, state);
            return true;
        }
        return false;
    }

    /**
     * Download the link content.
     *
     * @param state Current refresh state for the network link being processed
     * @return The downloaded file (which may have been renamed) if the download succeeded, {@code null} otherwise
     */
    @Nullable
    private File download(@Nonnull RefreshState state)
    {
        final String linkUrl = makeLinkUrl(state);
        if (linkUrl == null) return null;

        logger.debug("Download {}", linkUrl);

        try
        {
            if (!state.downloadRoot.exists() && !state.downloadRoot.mkdirs()) logger.error("Failed to create {}", state.downloadRoot);

            // Generate the name of the file to download to
            final URL url = new URL(linkUrl);
            final String filename = FileUtils.sanitizeForFilename((url.getHost() + "-" + url.getPath()).toLowerCase());
            final File outputFile = new File(state.downloadRoot, filename);

            try (var inputChannel = Channels.newChannel(url.openStream());
                 var outputStream = new FileOutputStream(outputFile);
                 var outputChannel = outputStream.getChannel())
            {
                outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
            }

            // If the downloaded file doesn't have a proper suffix, fix it
            if (!filename.endsWith(".kml") && !filename.endsWith(".kmz"))
            {
                final String suffix = FileSystemUtils.isZip(outputFile) ? ".kmz" : ".kml";
                final File newOutputFile = new File(state.downloadRoot, filename + suffix);
                Files.move(outputFile.toPath(), newOutputFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

                newOutputFile.deleteOnExit(); // All downloaded files are transient
                return newOutputFile;
            } else
            {
                outputFile.deleteOnExit(); // All downloaded files are transient
                return outputFile;
            }
        } catch (MalformedURLException e)
        {
            logger.warn("Invalid NetworkLink URL: {}", linkUrl, e);
        } catch (IOException e)
        {
            final String message = "Failed to download NetworkLink from URL: %s".formatted(linkUrl);
            logger.warn(message, e);
            notificationService.show(Notification.builder(message, Notification.Type.Error).build());
        }

        return null;
    }

    @Nullable
    private String makeLinkUrl(@Nonnull RefreshState state)
    {
        final String href = state.downloadLink != null ? state.downloadLink.getHref() : null;
        if (StringUtils.isBlank(href)) return null;

        if (!StringUtils.startsWithAny(href.toLowerCase(), HTTP_PREFIXES))
        {
            logger.warn("Unsupported NetworkLink URL: {}", href);
            return null;
        }

        final String query = state.downloadLink.getHttpQuery();
        final String url = StringUtils.isBlank(query) ? href : href + "?" + query;

        final String cookie = state.control.getCookie();
        return StringUtils.isBlank(cookie) ? url : url + cookie;
    }

    /**
     * When a network link's content is updated, its structure may have completely changed, and any nested network
     * links that have pending refreshes scheduled need to be canceled.
     * 
     * @param state Current refresh state
     * @param source Source to be parsed as the new link's content
     */
    private void updateLinkContent(@Nonnull RefreshState state, @Nonnull File source)
    {
        try
        {
            stopRefreshAndRemoveVisuals(state);

            final var model = KmlModel.parseModel(source);
            state.model = model;

            // Create visuals from the new model and start refresh on any child network links
            kmlVisualManager.createVisuals(model, state.taskStatusHandler);
            model.visitFeaturesOfType(NetworkLink.class, networkLink -> refresh(state.rootModel, networkLink, state.downloadPaused));

            // Update our NetworkLinkControl if one is present in the downloaded model.
            // This might affect our refresh rate for future downloads.
            final var control = model.kml().getNetworkLinkControl();
            if (control != null) state.control = control;

            networkLinkUpdatedEvent.fire(new NetworkLinkUpdated(model, state.networkLink));
        } catch (IOException e)
        {
            final String message = "Failed to process network link content: %s".formatted(state.networkLink.getName());
            logger.warn(message, e);
            notificationService.show(Notification.builder(message, Notification.Type.Error).build());
        }
    }

    /**
     * Schedule the next download attempt for a network link.
     *
     * @param state Current refresh state
     */
    private void scheduleNextDownload(@Nonnull RefreshState state)
    {
        if (state.downloadPaused || state.downloadLink == null) return;

        Double refreshInterval = state.downloadLink.getRefreshInterval();

        if (refreshInterval != null)
        {
            refreshInterval = Math.max(refreshInterval, MIN_REFRESH_INTERVAL_SECS);

            final double minRefreshPeriod = state.control.getMinRefreshPeriod();
            if (minRefreshPeriod > 0 && refreshInterval < minRefreshPeriod) refreshInterval = minRefreshPeriod;

            state.downloadFuture = executorService.schedule(() -> initiateRefresh(state), refreshInterval.longValue(), TimeUnit.SECONDS);

            logger.debug("schedule download in {} for {}", refreshInterval, state);
        } else
        {
            logger.debug("No refresh interval for {}", state);
        }
    }

    /**
     * Cancel any pending download, and remove all visuals associated with the links model.
     * This operation is performed recursively.
     *
     * @param state Refresh state for a network link's content, if {@code null} nothing is done
     */
    private void stopRefreshAndRemoveVisuals(@Nullable RefreshState state)
    {
        if (state == null) return;

        cancelDownload(state);

        if (state.model != null)
        {
            kmlVisualManager.removeVisuals(state.model);

            state.model.visitFeaturesOfType(NetworkLink.class,
                    networkLink -> stopRefreshAndRemoveVisuals(refreshStates.remove(networkLink)));
        }
    }

    private void cancelDownload(@Nonnull RefreshState state)
    {
        final var future = state.downloadFuture;
        if (future != null && !future.isDone()) future.cancel(false);
    }

    /**
     * Managed task to handle the download and processing of the network link contents.
     * @since 5.6
     */
    private class RefreshTask extends AManagedTask
    {
        private final RefreshState state;

        RefreshTask(@Nonnull RefreshState state)
        {
            super(TaskGroup.IMPORT_EXPORT, state.networkLink.getName());
            this.state = state;
            state.taskStatusHandler = getTaskStatusHandler();
        }

        @Override
        public void execute()
        {
            processDownload(state);
        }
    }

    /**
     * State holder for the elements related to handling a given {@link NetworkLink}.
     */
    private static class RefreshState
    {
        private final NetworkLink networkLink;
        private final Link downloadLink;
        private final long sessionStart = System.currentTimeMillis(); // First download determines session start
        private final KmlModel rootModel; // Top-level imported KML model that contains this Network Link
        private final File downloadRoot; // Where to download our link contents

        private NetworkLinkControl control = DEFAULT_NETWORK_LINK_CONTROL;
        private KmlModel model; // Model, valid after downloaded link content is parsed
        private Future<?> downloadFuture; // Scheduled downloads
        private boolean downloadPaused;
        private ITaskStatusHandler taskStatusHandler; // For reporting progress and checking for cancellation

        private RefreshState(@Nonnull KmlModel rootModel, @Nonnull NetworkLink networkLink)
        {
            this.networkLink = networkLink;
            this.rootModel = rootModel;
            downloadLink = getLinkOrUrl(networkLink);

            // Use a simple hash as the directory name, calculated from the source file path
            final String hash = HashUtils.getHash(rootModel.sourceFile().getAbsolutePath().getBytes(StandardCharsets.UTF_8));
            downloadRoot = new File(KML_DOWNLOAD_DIR, hash);
        }

        @Nullable
        private Link getLinkOrUrl(@Nonnull NetworkLink networkLink)
        {
            final var link = networkLink.getLink(); // Prefer 'Link'
            final var linkOrUrl = link != null ? link : networkLink.getUrl();// Fallback to Url

            if (linkOrUrl == null) logger.debug("No Link/Url found on NetworkLink: {}", networkLink.getName());
            return linkOrUrl;
        }

        @Override
        public String toString()
        {
            return "RefreshState{" +
                    "networkLink=" + networkLink.getName() +
                    ", model=" + (model != null ? model.sourceFile() : "null") +
                    ", paused=" + downloadPaused +
                    '}';
        }
    }
}
