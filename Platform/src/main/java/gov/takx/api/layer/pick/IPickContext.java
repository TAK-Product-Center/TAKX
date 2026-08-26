package gov.takx.api.layer.pick;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.globe.IGlobeDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Rectangle;

/**
 * Context object for pick operations.
 *
 * @since 5.2 (moved to platform in 5.8)
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
    IGlobeDelegate globe();
}