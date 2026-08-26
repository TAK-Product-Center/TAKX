package gov.tak.core.map.layer;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.globe.IApplicationGlobe;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Rectangle;

/**
 * Context object for pick operations.
 *
 * @since 5.2
 */
public interface IPickContext
{
    /**
     * @return the globe location that was picked
     */
    @Nullable
    GeoPoint location();

    /**
     * @return the rectangle in screen coordinates for the area that was picked
     */
    @Nonnull
    Rectangle rectangle();

    /**
     * @return the globe object where the pick originates from
     */
    @Nonnull
    IApplicationGlobe globe();
}