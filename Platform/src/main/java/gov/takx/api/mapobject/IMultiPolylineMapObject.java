package gov.takx.api.mapobject;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Contract for a map object defining multiple polylines (e.g. a telestration).
 *
 * @since 3.0
 */
public interface IMultiPolylineMapObject extends ICompoundMapObject
{
    /**
     * Map object type for multi-polylines.
     */
    String MULTI_POLYLINE_TYPE = "Multi Polyline";

    // Multi-polyline properties
    String HEIGHT = "HEIGHT";
    String POLYLINE_ADDED = "POLYLINE_ADDED";
    String POLYLINE_REMOVED = "POLYLINE_REMOVED";

    /**
     * @return The list of polyline map objects, or an empty list if none present.
     */
    @Nonnull
    List<IShapePathMapObject<?>> getPolylines();

    /**
     * Gets the polyline map object at the specified index.
     *
     * @param index The index of the desired polyline.
     * @return The polyline map object.
     */
    @Nonnull
    IShapePathMapObject<?> getPolyline(int index);

    /**
     * Adds a polyline to the multi-polyline map object.
     *
     * @param polylineMapObject The polyline map object to add
     */
    void addPolyline(@Nonnull IShapePathMapObject<?> polylineMapObject);

    /**
     * Removes the polyline from the multi-polyline map object.
     *
     * @param polylineMapObject The polyline map object to remove
     * @return Boolean indicating if a polyline was removed.
     */
    boolean removePolyline(@Nonnull IShapePathMapObject<?> polylineMapObject);

    /**
     * @return True if a valid height has been set.
     */
    boolean hasHeight();

    /**
     * @return The height for the multi-polyline in meters.
     */
    double getHeightMeters();

    /**
     * Sets the height for the multi-polyline.
     *
     * @param heightMeters The height in meters
     */
    void setHeightMeters(double heightMeters);
}
