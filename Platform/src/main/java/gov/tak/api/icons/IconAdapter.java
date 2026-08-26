package gov.tak.api.icons;

import gov.tak.api.maps.Marker;
import jakarta.annotation.Nonnull;

/**
 * Contract for providing the capability to "adapt" a Marker and supply an appropriate icon for use based on the
 * current state of the marker.
 *
 * @since 3.0
 */
@SuppressWarnings("NewClassNamingConvention")
public interface IconAdapter
{
    /**
     * Accept the given marker and adapt its icon, if the implementor can properly adapt it.
     *
     * @param marker the marker to adapt
     * @return {@code true} if the marker was processed and an icon was supplied, {@code false} if not
     */
    boolean adapt(@Nonnull Marker marker);

    /**
     * Dispose/release any resources in use by this adapter.
     */
    void dispose();
}
