package gov.takx.api.breadcrumb.event;

import jakarta.annotation.Nonnull;

/**
 * Fired after a set of trail {@link gov.takx.api.breadcrumb.IBreadcrumb breadcrumbs} have been modified. This allows
 * listeners to ignore intermediate {@link TrailBreadcrumbAdded} and {@link TrailBreadcrumbsRemoved} events and avoid
 * performing updates that require the complete set of breadcrumbs multiple times (e.g. trail opacity updates).
 *
 * @param markerUid The UID of the {@link gov.tak.api.maps.Marker} that owns that trail
 * @since 5.5 (moved from client)
 */
public record TrailBreadcrumbsUpdated(@Nonnull String markerUid)
{
}
