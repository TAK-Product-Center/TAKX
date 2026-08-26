package gov.takx.api.event.location;

import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.event.PlatformEvent;
import jakarta.annotation.Nullable;

/**
 * Event fired by a {@link gov.tak.api.location.ILocationProvider Location Provider} when the current
 * determined location changes.
 *
 * @param oldLocation   Previous location
 * @param newLocation   New location being reported
 * @param userGenerated Pass {@code true} if location change came from user-initiated operation
 * @since 3.3 (convert to record, signature changed)
 */
public record LocationChanged(@Nullable GeoPointMetaData oldLocation, @Nullable GeoPointMetaData newLocation, boolean userGenerated) implements PlatformEvent
{
}
