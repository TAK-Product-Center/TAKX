package gov.takx.api.layer.pick;

import gov.tak.api.maps.coords.GeoPoint;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;

/**
 * A list of {@link PickedObject}.
 *
 * @since 5.2 (moved to platform in 5.8)
 */
public class PickedObjectList extends ArrayList<PickedObject>
{
    public PickedObjectList()
    {
    }

    /**
     * @since 5.8
     */
    public PickedObjectList(@Nonnull PickedObjectList pickedObjects)
    {
        super(pickedObjects);
    }

    /**
     * @return the first {@link PickedObject} where the terrain property is {@code true}.
     */
    @Nullable
    public PickedObject getTerrainObject()
    {
        return stream().filter(PickedObject::terrain).findFirst().orElse(null);
    }

    /**
     * @return the location property of the result of {@link #getTerrainObject()}
     */
    @Nullable
    public GeoPoint getTerrainLocation()
    {
        PickedObject terrain = getTerrainObject();
        return terrain != null ? terrain.location() : null;
    }
}
