package gov.takx.api.breadcrumb.event;

import gov.takx.api.event.PlatformEvent;
import jakarta.annotation.Nonnull;

/**
 * Fired when a {@link gov.tak.api.maps.Marker marker's} {@link gov.takx.api.breadcrumb.IBreadcrumb breadcrumb} trail is
 * deleted.
 *
 * @param markerUid The UID of the {@link gov.tak.api.maps.Marker} that owns that trail
 * @since 5.5 (moved from client)
 */
public record TrailDeleted(@Nonnull String markerUid) implements PlatformEvent
{
}
