package gov.takx.api.breadcrumb.event;

import gov.takx.api.breadcrumb.IBreadcrumb;
import jakarta.annotation.Nonnull;

/**
 * Fired when a {@link IBreadcrumb} is added to a trail.
 *
 * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
 * @param breadcrumb The breadcrumb that was added
 * @param trailColor The trail's color. Internally, this is only used when the first breadcrumb is added to initialize
 *                   the rendered trail's color. Thereafter, {@link TrailColorUpdated} is used notify listeners of trail
 *                   color changes.
 * @since 5.5 (moved from client)
 */
public record TrailBreadcrumbAdded(@Nonnull String markerUid, @Nonnull IBreadcrumb breadcrumb, int trailColor)
{
}
