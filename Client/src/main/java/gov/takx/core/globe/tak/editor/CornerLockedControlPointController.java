package gov.takx.core.globe.tak.editor;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.Vector3D;
import com.atakmap.map.projection.ECEFProjection;
import com.atakmap.math.PointD;
import gov.nasa.worldwind.event.SelectListener;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import org.jetbrains.annotations.NotNull;

/**
 * An extension of {@link MultipointControlPointController} for multipoint source objects that prevents corners from crossing over.
 *
 * @since 5.6
 */
@PseudoBean
public class CornerLockedControlPointController extends MultipointControlPointController implements SelectListener
{
    @Override
    protected void onControlPointDragged(@NotNull String controlPointType, @NotNull ILocation location)
    {
        PointD locPoint = ECEFProjection.INSTANCE.forward(new com.atakmap.coremap.maps.coords.GeoPoint(location), null);
        Vector3D loc = new Vector3D((float) locPoint.x, (float) locPoint.y, (float) locPoint.z);
        Vector3D v1 = null; // The dragged control point
        Vector3D v2 = null; // Other points go CW
        Vector3D v3 = null;
        Vector3D v4 = null;
        Vector3D sw = toVec(IControlPoint.CONTROL_POINT_SOUTHWEST);
        Vector3D se = toVec(IControlPoint.CONTROL_POINT_SOUTHEAST);
        Vector3D ne = toVec(IControlPoint.CONTROL_POINT_NORTHEAST);
        Vector3D nw = toVec(IControlPoint.CONTROL_POINT_NORTHWEST);

        switch (controlPointType)
        {
            case IControlPoint.CONTROL_POINT_NORTHWEST ->
            {
                v1 = nw;
                v2 = ne;
                v3 = se;
                v4 = sw;
            }
            case IControlPoint.CONTROL_POINT_SOUTHWEST ->
            {
                v1 = sw;
                v2 = nw;
                v3 = ne;
                v4 = se;
            }
            case IControlPoint.CONTROL_POINT_NORTHEAST ->
            {
                v1 = ne;
                v2 = se;
                v3 = sw;
                v4 = nw;
            }
            case IControlPoint.CONTROL_POINT_SOUTHEAST ->
            {
                v1 = se;
                v2 = sw;
                v3 = nw;
                v4 = ne;
            }
        }

        // Check to see if the newly dragged control point location crosses the opposite diagonal line and if so, adjust so it doesn't cross
        if (v1 != null && v2 != null && v4 != null && crossesLine(loc, v2, v4)) location = toGeoPoint(Vector3D.nearestPointOnSegment(loc, v2, v4));
        // Check to see if the newly dragged control point location crosses either of the opposite edge lines and if so, cancel the drag
        if (v1 != null && v2 != null && v3 != null && crossesLine(loc, v2, v3)) return;
        if (v1 != null && v3 != null && v4 != null && crossesLine(loc, v3, v4)) return;

        super.onControlPointDragged(controlPointType, location);
    }

    private Vector3D toVec(String controlPoint)
    {
        IControlPoint cp = IControlPoint.getControlPointByType(getControlPoints(), controlPoint);
        PointD p = cp != null ? ECEFProjection.INSTANCE.forward(new com.atakmap.coremap.maps.coords.GeoPoint(cp.getLocation()), null) : null;
        return p != null ? new Vector3D((float) p.x, (float) p.y, (float) p.z) : null;
    }

    private ILocation toGeoPoint(Vector3D vec)
    {
        GeoPoint geoPoint = ECEFProjection.INSTANCE.inverse(new PointD(vec.x, vec.y, vec.z), null);
        return new gov.tak.api.maps.coords.GeoPoint(geoPoint.getLatitude(), geoPoint.getLongitude(), geoPoint.getAltitude());
    }

    private boolean crossesLine(Vector3D pt, Vector3D line1, Vector3D line2)
    {
        double d = (pt.x - line1.x) * (line2.y - line1.y) - (pt.y - line1.y) * (line2.x - line1.x);
        return Math.signum(d) != 1.0;
    }
}
