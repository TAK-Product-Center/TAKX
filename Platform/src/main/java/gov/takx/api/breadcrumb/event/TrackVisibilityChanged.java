package gov.takx.api.breadcrumb.event;

import gov.takx.api.breadcrumb.ITrack;
import gov.takx.api.event.PlatformEvent;
import jakarta.annotation.Nonnull;

/**
 * Fired when a {@link ITrack track's} visibility changes.
 *
 * @param track The track that changed
 * @since 5.5 (moved from client)
 */
public record TrackVisibilityChanged(@Nonnull ITrack track) implements PlatformEvent
{
}
