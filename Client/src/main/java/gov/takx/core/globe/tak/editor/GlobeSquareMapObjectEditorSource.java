package gov.takx.core.globe.tak.editor;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.dataservices.shapes.Square;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * An implementation of {@link IShapeEditorSource} for {@link IGlobeShapeMapObject} that represents a {@link Square} object.
 *
 * @since 5.7
 */
public class GlobeSquareMapObjectEditorSource extends GlobeRectangleMapObjectEditorSource
{
    public GlobeSquareMapObjectEditorSource(IGlobeShapeMapObject source)
    {
        super(source);
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
    {
        Square square = getSource().requireShapeModel();
        if (IControlPoint.CONTROL_POINT_NORTHEAST.equals(controlPointType) || IControlPoint.CONTROL_POINT_NORTHWEST.equals(controlPointType)
                || IControlPoint.CONTROL_POINT_SOUTHEAST.equals(controlPointType) || IControlPoint.CONTROL_POINT_SOUTHWEST.equals(controlPointType))
        {
            Collection<IControlPoint> controlPoints = getShapeEditor().getControlPointController().getControlPoints();
            ILocation oppositeLocation = getOppositeLocation(controlPoints, controlPointType);
            if (oppositeLocation == null) return;
            double bearing = ShapeUtils.rhumbAzimuth(oppositeLocation, location) - square.getOrientation();
            double distance = GeoCalculations.distance(oppositeLocation, location);

            // Compute shape's the width and height in meters from the diagonal between the shape's new center
            // position and its new corner position.
            double widthMeters = distance * Math.abs(Math.sin(Math.toRadians(bearing)));
            double heightMeters = distance * Math.abs(Math.cos(Math.toRadians(bearing)));

            double sideLength = Math.max(widthMeters, heightMeters);
            square.setSideLength(sideLength);
        } else
            super.onControlPointDragged(controlPointType, location);
    }
}
