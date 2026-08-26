package gov.takx.api.ui.location;

import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.net.URI;

/**
 * Interface for listening for a location to be input to an {@link ILocationInputPanel}.
 *
 * @since 3.0
 */
public interface ILocationInputListener
{
    /**
     * Called when the user selects a location.
     *
     * @param location The selected location
     * @param coordinateSystem The coordinate system the user used for the location
     * @param markerIconUri The URI of the selected marker icon, if marker icons were displayed; will be {@code null}
     *                      if marker icons were not displayed or the "no marker" option was selected
     * @param cotType The CoT type to use for the created map object (or {@code null} if no map object is to be created)
     */
    void onLocationSelected(@Nonnull ILocation location, @Nonnull CoordinateSystem coordinateSystem, @Nullable URI markerIconUri,
                            @Nullable String cotType);

    /**
     * Called if the user cancels location selection.
     */
    void onCancel();
}
