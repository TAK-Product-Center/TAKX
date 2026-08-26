package gov.takx.api.breadcrumb.event;

import gov.takx.api.event.PlatformEvent;
import jakarta.annotation.Nonnull;

/**
 * Fired when a {@link gov.tak.api.maps.Marker marker's} trail visibility changes.
 *
 * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
 * @param visible {@code true} if the trail should be visible, {@code false} if it should be hidden
 * @since 5.5 (moved from client)
 */
public record TrailVisibilityChanged(@Nonnull String markerUid, boolean visible) implements PlatformEvent
{
}
