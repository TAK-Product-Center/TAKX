package gov.takx.api.breadcrumb;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Manages {@link gov.tak.api.maps.Marker} {@link IBreadcrumb breacrumbs} and their associated trails and
 * {@link ITrack tracks}.
 * 
 * @since 5.3
 */
@InjectableService
@NoPluginImplementations
@IncubatingApi(since = "5.3")
public interface IBreadcrumbManager
{
    /**
     * Starts recording {@link IBreadcrumb breadcrumbs} for the specified {@link gov.tak.api.maps.Marker}. If the marker 
     * does not currently have an active {@link ITrack track}, a new {@link ITrack track} will be started with the 
     * default track name.
     * 
     * @param markerUid The {@link gov.tak.api.maps.Marker Marker's} UID
     * @param callsign The callsign of the user associated with the {@link gov.tak.api.maps.Marker}, if applicable
     * @throws IllegalArgumentException if no {@link gov.tak.api.maps.Marker} is associated with the {@code markerUid}
     */
    void startBreadcrumbs(@Nonnull String markerUid, @Nullable String callsign) throws IllegalArgumentException;

    /**
     * Stops recording {@link IBreadcrumb breadcrumbs} for the specified {@link gov.tak.api.maps.Marker}. If breadcrumbs 
     * are not currently being recorded, no action is taken.
     * 
     * @param markerUid The marker UID
     */
    void stopBreadcrumbs(@Nonnull String markerUid);

    /**
     * @param markerUid The {@link gov.tak.api.maps.Marker} UID
     * @return {@code true} if breadcrumbs are enabled and being recorded for the {@link gov.tak.api.maps.Marker}, 
     * {@code false} otherwise
     */
    boolean areBreadcrumbsEnabled(@Nonnull String markerUid);

    /**
     * @param markerUid the {@link gov.tak.api.maps.Marker marker's} UID
     * @return The {@link gov.tak.api.maps.Marker marker's} trail {@link IBreadcrumb breadcrumbs}. An empty list will be
     * returned if the marker cannot be found.
     */
    @Nonnull
    List<IBreadcrumb> getTrailBreadcrumbs(@Nonnull String markerUid);

    /**
     * @param track The {@link ITrack}
     * @return The {@link ITrack track's} breadcrumbs. An empty list will be returned if the marker cannot be found.
     */
    @Nonnull
    List<IBreadcrumb> getTrackBreadcrumbs(@Nonnull ITrack track);

    /**
     * Starts a new track for specified {@link gov.tak.api.maps.Marker} and begins recording {@link IBreadcrumb IBreadcrumbs}.
     * 
     * @param markerUid The {@link gov.tak.api.maps.Marker Marker's} UID
     * @param callsign The callsign associated with the {@link gov.tak.api.maps.Marker} or {@code null} if not applicable
     * @param trackName The new {@link ITrack ITrack's} name or {@code null} if the default name should be used
     * @throws IllegalArgumentException if no {@link gov.tak.api.maps.Marker} is associated with the {@code markerUid}
     */
    void startNewTrack(@Nonnull String markerUid, @Nullable String callsign, @Nullable String trackName) throws IllegalArgumentException;

    /**
     * @param track The track to delete
     */
    void deleteTrack(@Nonnull ITrack track);

    /**
     * @param markerUid The {@link gov.tak.api.maps.Marker Marker's} UID
     * @return The {@link gov.tak.api.maps.Marker marker's} current {@link ITrack} or {@code null} if no track is
     * currently active
     */
    @Nullable
    ITrack getCurrentTrack(@Nonnull String markerUid);

    /**
     * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
     * @return The {@link gov.tak.api.maps.Marker marker's} {@link ITrack tracks}
     * @since 5.4
     */
    @Nonnull
    List<ITrack> getTracks(@Nonnull String markerUid);

    /**
     * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
     * @param minTime Time that limits the results to {@link ITrack tracks} that have a start time greater than the
     *                specified min; pass {@code null} if the end time should not be restricted // todo: use start or end time? TAKX-3317
     * @param maxTime Time that limits the results to {@link ITrack tracks} that have a start time less than the
     *                specified max; pass {@code null} if the end time should not be restricted // todo: use start or end time? TAKX-3317
     * @return The {@link gov.tak.api.maps.Marker Marker's} {@link ITrack ITracks}
     * @since 5.4
     */
    @Nonnull
    List<ITrack> getTracks(@Nonnull String markerUid, @Nullable Instant minTime, @Nullable Instant maxTime);

    /**
     * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
     * @param serverConnection The connection to the server to query in addition to the local database
     * @param minTime Time that limits the results to {@link ITrack tracks} that have a start time greater than the
     *                specified min; pass {@code null} if the end time should not be restricted 
     * @param maxTime Time that limits the results to {@link ITrack tracks} that have a start time less than the
     *                specified max; pass {@code null} if the end time should not be restricted 
     * @return A {@link CompletableFuture} that will return the {@link gov.tak.api.maps.Marker Marker's} {@link ITrack ITracks}
     * @since 5.4
     */
    CompletableFuture<List<ITrack>> getTracks(@Nonnull String markerUid, @Nonnull ITakServerConnection serverConnection,
                                              @Nullable Instant minTime, @Nullable Instant maxTime);

    /**
     * @param markerUid The {@link gov.tak.api.maps.Marker Marker's} UID
     * @return The number of locally available tracks
     */
    long getTrackCount(@Nonnull String markerUid);

    /**
     * Sets all tracks to invisible.
     *
     * @return The number of tracks there were hidden
     * @since 5.5
     */
    int hideAllTracks();
}
