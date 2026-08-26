package gov.takx.api.globe;

import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nullable;

/**
 * Contract for a renderable on the globe that wants to override the location reported by the Position Overlay Layer.
 * This allows the implementing type to control the location displayed while the mouse is over its renderable.
 *
 * @since 3.0
 */
public interface IPositionOverlayOverride
{
    /**
     * @return The location to report, {@code null} means to use the current mouse position
     */
    @Nullable
    ILocation getLocation();

    /**
     * @return The object name at the override location
     * @since 5.4
     */
    String getName();
}
