package gov.takx.api.ui.location;

import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;

/**
 * Interface for a panel that accepts location input from the user.
 *
 * @since 3.0
 */
public interface ILocationInputPanel
{
    /**
     * Add a listener for notification when a location is selected.
     */
    void addListener(@Nonnull ILocationInputListener listener);

    /**
     * Remove a listener from panel notifications.
     */
    void removeListener(@Nonnull ILocationInputListener listener);

    /**
     * Set the location currently displayed by the panel
     *
     * @param location         The location to display
     * @param coordinateSystem The coordinate system to use to display the location
     */
    void setLocation(@Nonnull ILocation location, CoordinateSystem coordinateSystem);

    /**
     * Indicates if the small marker icon selection buttons should be displayed in the lower-left of the panel. Default
     * is false as TAK only displays these buttons for the go to dialog.
     *
     * @param showMarkerIcons True if marker icon buttons should be displayed
     */
    void setShowMarkerIcons(boolean showMarkerIcons);
}
