package gov.takx.core.view.gui.breadcrumb;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.Headless;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.breadcrumb.event.TrackCreated;
import gov.takx.api.breadcrumb.event.TrackDeleted;
import gov.takx.api.breadcrumb.event.TrackUpdated;
import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.takserver.TakServerConnectionClosed;
import gov.takx.api.event.takserver.TakServerConnectionOpened;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.core.view.gui.ATitledPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Manages the {@link TrackSearchPanel} and track queries.
 *
 * @since 5.4
 */
@ApplicationScoped
public class TrackSearchPanelController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IBreadcrumbManager breadcrumbManager;
    private final INotificationService notificationService;
    private final TrackSearchPanel trackSearchPanel = new TrackSearchPanel(this::search, this::cancel);

    @Nullable
    private CompletableFuture<List<ITrack>> queryFuture;

    @Inject
    TrackSearchPanelController(IBreadcrumbManager breadcrumbManager, INotificationService notificationService)
    {
        this.breadcrumbManager = breadcrumbManager;
        this.notificationService = notificationService;
    }

    @SuppressWarnings("unused")
    void onProjectClosing(@Observes ProjectClosing event)
    {
        cancel(); // stop query if one is running
    }

    @VisibleForTesting
    void takServerConnectionOpened(@Observes TakServerConnectionOpened event)
    {
        trackSearchPanel.addTakServer(event.getConnection());
    }

    @VisibleForTesting
    void takServerConnectionClosed(@Observes TakServerConnectionClosed event)
    {
        trackSearchPanel.removeTakServer(event.getConnection());
    }

    @SuppressWarnings("SameParameterValue")
    @VisibleForTesting
    void onMapObjectAdded(@Observes MapObjectAdded event, @Headless boolean headless)
    {
        // Markers that are "never_cot" can't produce tracks (AFAIK), so don't add them to the search panel
        if (!headless && event.getMapObject() instanceof Marker marker
                && !marker.getMetaBoolean(StandardAttributeNames.NEVER_COT, false))
        {
            trackSearchPanel.addMarker(marker);
            marker.addDeletionHandler(mapItem -> deleteMarker(marker));
        }
    }

    @SuppressWarnings("unused")
    private void onTrackCreated(@Observes TrackCreated ignored)
    {
        trackSearchPanel.refreshMarkers();
    }

    @SuppressWarnings("unused")
    private void onTrackUpdated(@Observes TrackUpdated event)
    {
        // Refresh the track if it's being displayed
        trackSearchPanel.getTrackListPanel().updateTrack(event.track());
    }

    @SuppressWarnings("unused")
    private void onTrackDeleted(@Observes TrackDeleted event)
    {
        // Refresh the marker combo box to update the track counts
        trackSearchPanel.refreshMarkers();

        // Remove it from the track list if it's showing
        trackSearchPanel.getTrackListPanel().removeTrack(event.track());
    }

    /**
     * @param marker The marker to remove from all search UIs
     */
    private void deleteMarker(@Nonnull Marker marker)
    {
        trackSearchPanel.removeMarker(marker);
    }

    /**
     * @return The {@link TrackSearchPanel} managed by this controller
     */
    public ATitledPanel getPanel()
    {
        return trackSearchPanel;
    }

    /**
     * Executes a search based on the {@link TrackSearchPanel} parameters.
     *
     * @throws IllegalStateException thrown if a server query is requested when a query is already in progress
     */
    private void search()
    {
        //noinspection DataFlowIssue
        final var markerUid = trackSearchPanel.getSelectedMarker().getUid();
        final var interval = trackSearchPanel.getInterval();
        final var server = trackSearchPanel.isSearchServer() ? trackSearchPanel.getSelectedServer() : null;

        // Search from the beginning of the selected day
        final var start = interval.start().truncatedTo(ChronoUnit.DAYS);
        // Search to the end of the selected day (next day, 12:00a)
        final var end = interval.end().plus(1, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);

        if (server == null)
        {
            final var trackList = breadcrumbManager.getTracks(markerUid, start, end);
            trackSearchPanel.onSearchComplete(sort(trackList));
        } else
        {
            if (queryFuture != null)
            {
                logger.error("Query requested while a query is already in progress. The UI should not allow this.");
                throw new IllegalStateException("Query requested while query already in progress");
            }

            queryFuture = breadcrumbManager.getTracks(markerUid, server, start, end);
            queryFuture.orTimeout(10, TimeUnit.SECONDS)
                    .thenApply(this::sort)
                    .thenAccept(trackList -> {
                        trackSearchPanel.onSearchComplete(trackList);
                        queryFuture = null;
                    })
                    .exceptionally(throwable -> {
                        logger.error("Error querying server for tracks", throwable);
                        trackSearchPanel.onSearchCancelled();
                        queryFuture = null;

                        final var details = throwable instanceof TimeoutException ? "Request timed out." : "See log for details.";
                        notificationService.show(
                                Notification.builder(
                                        "Error querying server for tracks. " + details, Notification.Type.Error)
                                        .build());

                        return null;
                    });
        }
    }

    /**
     * @param trackList The list to sort
     * @return The track list, sorted by start time from newest to oldest
     */
    private List<ITrack> sort(@Nonnull List<ITrack> trackList)
    {
        return trackList.stream().sorted(Comparator.comparing(ITrack::getStartTime).reversed()).toList();
    }

    /**
     * Cancels a running query.
     */
    private void cancel()
    {
        if (queryFuture != null)
        {
            queryFuture.cancel(true);
            queryFuture = null;
        }

        trackSearchPanel.onSearchCancelled();
    }
}
