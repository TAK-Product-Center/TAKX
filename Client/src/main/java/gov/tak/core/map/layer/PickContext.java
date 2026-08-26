package gov.tak.core.map.layer;

import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.globe.IApplicationGlobe;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Rectangle;

/**
 * Implementation of {@link IPickContext}.
 *
 * @param globe the globe object
 * @param rectangle the pick rectangle in screen coordinates
 * @param location the globe location
 * @since 5.2
 */
public record PickContext(@Nonnull IApplicationGlobe globe, @Nonnull Rectangle rectangle, @Nullable GeoPoint location) implements IPickContext
{
}