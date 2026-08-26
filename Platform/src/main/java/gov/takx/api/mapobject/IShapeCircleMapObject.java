package gov.takx.api.mapobject;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.shapes.IMutableCircle;
import jakarta.annotation.Nullable;

/**
 * Interface defining the Circle shape map object.
 *
 */
@NoPluginImplementations
public interface IShapeCircleMapObject extends IShapeRingMapObject<IMutableCircle>, IMutableCircle
{
    /**
     * Set a radius anchor, referencing another map object.  If that object is moved, then the radius of this
     * circle will be updated accordingly.  If the radius of this circle is changed directly, then the location
     * of the given {@code radiusAnchor} will be updated accordingly.
     *
     * @param radiusAnchor Map Object to use as a radius anchor, pass {@code null} to remove an existing anchor
     * @since 3.0
     */
    void setRadiusAnchor(@Nullable IMapObject radiusAnchor);

    /**
     * @return Current radius anchor, if any
     * @since 3.0
     */
    @Nullable
    IMapObject getRadiusAnchor();
}
