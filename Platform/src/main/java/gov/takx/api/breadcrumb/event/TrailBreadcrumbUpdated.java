package gov.takx.api.breadcrumb.event;

import gov.takx.api.breadcrumb.IBreadcrumb;
import jakarta.annotation.Nonnull;

/**
 * Fired when a single trail {@link IBreadcrumb} bearing is updated.
 *
 * @param markerUid The marker's UID
 * @param breadcrumb The breadcrumb that had its bearing updated
 * @since 5.5 (moved from client)
 */
public record TrailBreadcrumbUpdated(@Nonnull String markerUid, @Nonnull IBreadcrumb breadcrumb)
{
}
