package gov.takx.api.breadcrumb;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.time.Instant;

/**
 * Contract for recording a {@link gov.tak.api.maps.Marker} location along with additional metadata. For use with
 * {@link ITrack tracks} that are composed of a set of breadcrumbs.
 *
 * @since 5.3
 * @see ITrack
 */
@NoPluginImplementations
@IncubatingApi(since = "5.3")
public interface IBreadcrumb
{
    /**
     * @return This breadcrumb's UID.
     */
    @Nonnull
    String getUid();

    /**
     * @return The UID of that track that this breadcrumb is associated with
     */
    @Nonnull
    String getTrackUid();

    /**
     * @return The marker's location and associated metadata
     */
    @Nonnull
    GeoPointMetaData getGeoPointMetaData();

    /**
     * @return The time when the location was recorded
     */
    @Nonnull
    Instant getTime();

    /**
     * @return The marker's bearing when the location was recorded. May be {@code null} if no bearing is associated with
     * the breadcrumb
     */
    @Nullable
    Double getBearing();

    /**
     * @return {@code true} if the breadcrumb has a bearing value
     */
    boolean hasBearing();

    /**
     * @return The marker's speed, in m/s, when the location was recorded. May be {@code null} if no speed is associated
     * with the breadcrumb.
     */
    @Nullable
    Double getSpeedInMps();

    /**
     * @return {@code true} if the breadcrumb has a speed value
     */
    boolean hasSpeed();
}
