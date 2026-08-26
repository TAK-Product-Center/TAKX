package gov.tak.api.comms.takserver.mission.data;

import gov.tak.api.maps.coords.GeoPoint;
import jakarta.annotation.Nonnull;

/**
 * Simple location model.
 *
 * @param lat Latitude degrees.
 * @param lon Longitude degrees.
 *
 * @since 3.5
 */
public record Location(double lat, double lon)
{
    /**
     * @return Converted position representation.
     */
    @Nonnull
    public GeoPoint toPosition()
    {
        return new GeoPoint(lat, lon);
    }
}
