package gov.tak.core.map.layer;

import gov.tak.api.maps.coords.GeoPoint;

/**
 * Holds a pick result.
 *
 * @param location the location of the picked object
 * @param userObject the user object
 * @param terrain {@code true} if the pick object represents terrain, otherwise {@code false}
 * @since 5.2
 */
public record PickedObject(GeoPoint location, Object userObject, boolean terrain)
{
    /**
     * Construct a {@link PickedObject} with specified parameters and set terrain flag to {@code false}.
     *
     * @param location the location of the picked object
     * @param userObject the user object
     */
    public PickedObject(GeoPoint location, Object userObject)
    {
        this(location, userObject, false);
    }
}
