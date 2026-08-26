package gov.takx.api.geofence;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.api.maps.PointMapItem;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.IShape;
import jakarta.annotation.Nonnull;

/**
 * Describes an alert generated from a {@link GeoFence} boundary crossing.
 *
 * @param shape The shape defining the GeoFence boundary
 * @param geoFence The GeoFence for which this alert was generated
 * @param mapItem The {@link PointMapItem} that crossed the boundary
 * @param crossingType The type of crossing (entry/exit)
 * @param time The time of the crossing
 *
 * @since 5.3
 */
public record GeoFenceAlert(@Nonnull IShape shape, @Nonnull GeoFence geoFence, @Nonnull PointMapItem mapItem,
        @Nonnull Trigger crossingType, @Nonnull CoordinatedTime time)
{
    /**
     * @return The UID of the map object associated with our shape
     */
    @Nonnull
    public String shapeUid()
    {
        return shapeMapObject().getUid();
    }

    @Nonnull
    public IShapeMapObject<?> shapeMapObject()
    {
        return shape().getShapeMapObject();
    }

    /**
     * @return Simple message describing the alert, of the form {@code 'Item 1' Entered 'Circle 1' @ [date/time]}
     */
    public String message()
    {
        return String.format("'%s' %s '%s' @ %s", mapItem.getName(), crossingVerb(), shape.getName(), time);
    }

    /**
     * @return The past tense verb for the crossing type: {@code Entered} or {@code Exited}
     */
    public String crossingVerb()
    {
        return crossingType == Trigger.Entry ? "Entered" : "Exited";
    }
}
