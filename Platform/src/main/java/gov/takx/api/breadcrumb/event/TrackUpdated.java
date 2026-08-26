package gov.takx.api.breadcrumb.event;

import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.breadcrumb.ITrack;
import jakarta.annotation.Nonnull;

/**
 * Fired when a {@link gov.takx.api.breadcrumb.ITrack}'s properties are updated.
 *
 * @param track The track that was updated
 * @see IBreadcrumbManager
 * @since 5.5 (moved from client)
 */
public record TrackUpdated(@Nonnull ITrack track)
{
}
