package gov.takx.api.layer.pick;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.globe.IGlobeDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Rectangle;

/**
 * Implementation of {@link IPickContext}.
 *
 * @param globe the globe object
 * @param rectangle the pick rectangle in screen coordinates
 * @param location the globe location
 * @since 5.2 (moved to platform in 5.8)
 */
public record PickContext(@Nonnull IGlobeDelegate globe, @Nonnull Rectangle rectangle, @Nullable GeoPoint location) implements IPickContext
{
}