package gov.tak.api.location;

import gov.tak.api.maps.PointMapItem;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nullable;

import java.awt.image.BufferedImage;

/**
 * Service interface for a Location Tracker that exposes the ability to customize how the tracker displays itself on the globe.
 *
 * @since 3.0
 */
@InjectableService
@NoPluginImplementations
public interface ILocationTracker
{
    /**
     * Sets the icon that will be displayed on the map if the Location Tracker is configured to display the current location.
     *
     * @param iconImage BufferedImage to be displayed for this location tracker, pass {@code null} to reset to the default
     */
    void setLocationMarkerIcon(@Nullable BufferedImage iconImage);

    /**
     * Sets the text displayed as a tooltip when the mouse cursor hovers over the location tracker icon.
     *
     * @param tooltipText A string to be displayed as a tooltip, or {@code null} to remove the tooltip text.
     */
    void setLocationMarkerTooltipText(@Nullable String tooltipText);

    /**
     * @return The {@link PointMapItem Map Object} used to show the current location.  May be {@code null} if this service
     * has not been started.
     * @since 5.0
     */
    @Nullable
    PointMapItem getMarkerMapObject();
}
