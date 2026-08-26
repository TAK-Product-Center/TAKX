package gov.takx.api.breadcrumb;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.persist.IPersistable;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;

/**
 * Contract for {@link gov.takx.api.playback.IMarker marker} tracks. A track is a segment of a marker's location history,
 * composed of multiple positions recorded as {@link IBreadcrumb breadcrumbs}.
 * 
 * @since 5.3
 */
@NoPluginImplementations
@IncubatingApi(since = "5.3")
public interface ITrack extends IPersistable
{
    /**
     * The track's rendering style
     */
    enum Style
    {
        Arrow,
        Line,
        Dash
    }

    /**
     * @return This track's UID
     */
    @Nonnull
    String getUid();

    /**
     * @return The UID of the {@link gov.tak.api.maps.Marker} that owns this track
     */
    @Nonnull
    String getMarkerUid();

    /**
     * @return the {@link ITrack track's} name
     */
    @Nonnull
    String getName();

    /**
     * @return the color to use when rendering the track's {@link IBreadcrumb breadcrumbs}
     */
    int getColor();

    /**
     * @return the style to use when rendering the track's {@link IBreadcrumb breadcrumbs}
     */
    Style getStyle();

    /**
     * @return The callsign of the user represented by the {@link gov.tak.api.maps.Marker} that owns this track. May be 
     * {@code null}
     */
    @Nullable
    String getCallsign();

    /**
     * @return The time the track was started
     */
    @Nonnull
    Instant getStartTime();

    /**
     * @return The last time this track was updated
     */
    @Nonnull
    Instant getEndTime();

    /**
     * @return The location where this track started. Will be {@code null} if unset.
     */
    @Nullable
    GeoPoint getStartPoint();

    /**
     * @return The last location added to this track. Will be {@code null} if unset.
     */
    @Nullable
    GeoPoint getEndPoint();

    /**
     * @return The location of the {@link IBreadcrumb} in this track with the minimum elevation. May be {@code null} if
     * no elevation values have been recorded.
     */
    @Nullable
    GeoPoint getMinElevationPoint();

    /**
     * @return The location of the {@link IBreadcrumb} in this track with the maximum elevation. May be {@code null} if
     * no elevation values have been recorded.
     */
    @Nullable
    GeoPoint getMaxElevationPoint();

    /**
     * @return The location of the {@link IBreadcrumb} in this track with the minimum speed. May be {@code null} if no
     * speed values have been recorded.
     */
    @Nullable
    GeoPoint getMaxSpeedPoint();

    /**
     * @return The marker's average speed along this track. Only positions that included a speed value are included in
     * the average. May be 0 if no speed values have been recorded.
     */
    double getAverageSpeed();

    /**
     * @return The marker's max speed along this track. Will be 0 if no speed values have been recorded.
     */
    double getMaxSpeed();

    /**
     * @return The marker's total elevation gain along this track. Will be 0 if no elevation values have been recorded.
     */
    double getElevationGain();

    /**
     * @return The marker's total elevation loss along this track. Will be 0 if no elevation values have been recorded.
     */
    double getElevationLoss();

    /**
     * @return The total distance the marker traveled along this track. The distance between each position is calculated 
     * using the slant distance.
     * @see gov.tak.api.engine.map.coords.GeoCalculations#slantDistance(IGeoPoint, IGeoPoint) (IGeoPoint, IGeoPoint)
     */
    double getTotalDistance();

    /**
     * @return The total number of {@link IBreadcrumb breadcrumbs} in this track
     */
    int getBreadcrumbCount();

    /**
     * @return {@code true} if this track is visible on the map
     */
    boolean isVisible();
}
