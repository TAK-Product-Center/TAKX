package gov.takx.api.layer.pick;

import gov.tak.api.maps.coords.GeoPoint;
import jakarta.annotation.Nullable;

/**
 * Holds a pick result.
 *
 * @param location the location of the picked object
 * @param userObject the user object
 * @param terrain {@code true} if the pick object represents terrain, otherwise {@code false}
 * @since 5.2 (moved to platform in 5.8)
 */
public record PickedObject(@Nullable GeoPoint location, @Nullable Object userObject, boolean terrain)
{
    /**
     * Construct a {@link PickedObject} with specified parameters and set terrain flag to {@code false}.
     *
     * @param location the location of the picked object
     * @param userObject the user object
     */
    public PickedObject(@Nullable GeoPoint location, @Nullable Object userObject)
    {
        this(location, userObject, false);
    }
}
