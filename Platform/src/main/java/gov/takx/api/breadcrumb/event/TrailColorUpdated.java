package gov.takx.api.breadcrumb.event;

import jakarta.annotation.Nonnull;

/**
 * Fired when a {@link gov.tak.api.maps.Marker marker's} trail's color is updated.
 *
 * @param markerUid The {@link gov.tak.api.maps.Marker marker's} UID
 * @param color The trail color
 * @since 5.5 (moved from client)
 */
public record TrailColorUpdated(@Nonnull String markerUid, int color)
{
}
