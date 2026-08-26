package gov.takx.api.breadcrumb.event;

import gov.takx.api.breadcrumb.IBreadcrumb;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Fired when {@link IBreadcrumb breadcrumbs} are removed from a trail.
 *
 * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
 * @param breadcrumbs The breadcrumbs that were removed
 * @since 5.5 (moved from client)
 */
public record TrailBreadcrumbsRemoved(@Nonnull String markerUid, @Nonnull List<IBreadcrumb> breadcrumbs)
{
}
