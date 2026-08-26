package gov.takx.api.breadcrumb.event;

import gov.takx.api.breadcrumb.IBreadcrumbManager;
import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.event.PlatformEvent;
import jakarta.annotation.Nonnull;

/**
 * Fired when a new {@link gov.takx.api.breadcrumb.ITrack} is created.
 *
 * @param track The track that was created
 * @see IBreadcrumbManager
 * @since 5.5 (moved from client)
 */
public record TrackCreated(@Nonnull ITrack track) implements PlatformEvent
{
}
