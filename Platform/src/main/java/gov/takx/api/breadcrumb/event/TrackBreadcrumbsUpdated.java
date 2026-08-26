package gov.takx.api.breadcrumb.event;

import gov.takx.api.breadcrumb.IBreadcrumb;
import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.breadcrumb.ITrack;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Fired when {@link gov.takx.api.breadcrumb.ITrack} {@link IBreadcrumb breadcrumbs} have been updated. Updates include
 * newly added breadcrumbs and breadcrumb bearing changes.
 *
 * @param track The track with breadcrumbs that were updated
 * @param breadcrumbs The breadcrumbs that were updated
 * @see IBreadcrumbManager
 * @since 5.5 (moved from client)
 */
public record TrackBreadcrumbsUpdated(@Nonnull ITrack track, @Nonnull List<? extends IBreadcrumb> breadcrumbs)
{
}
