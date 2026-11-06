package gov.takx.core.globe.tak.editor;

import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IShape;
import gov.takx.core.globe.tak.editor.IShapeEditorSource.ControlPointDefinition;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * An implementation of {@link IControlPointProvider} for {@link IGlobeShapeMapObject} that provide a 2525 control point.
 *
 * @since 5.8.1 (backported from 5.9)
 */
public class MilStdControlPointProvider<T extends IGlobeShapeMapObject> extends AControlPointProvider<T>
{
    /**
     * @param source the source object being edited
     */
    protected MilStdControlPointProvider(@Nonnull T source)
    {
        super(source);
    }

    @Override
    @Nonnull
    public List<ControlPointDefinition> createControlPointDefinitions()
    {
        IMapObject mapObject = getSource().getMapObject();
        if (mapObject == null) return Collections.emptyList();
        AttributeSet attributeSet = mapObject.getAttributeSet();
        if (!attributeSet.containsAttribute(MilSymStandardAttributes.MILSYM_CP_LAT) || !attributeSet.containsAttribute(MilSymStandardAttributes.MILSYM_CP_LON)) return Collections.emptyList();
        IMutableShape shape = getSource().requireShapeModel();
        boolean isEditable = shape.isEditable();
        IShape.AltitudeMode altitudeMode = shape.getAltitudeMode();
        double lat = attributeSet.getDoubleAttribute(MilSymStandardAttributes.MILSYM_CP_LAT);
        double lon = attributeSet.getDoubleAttribute(MilSymStandardAttributes.MILSYM_CP_LON);
        double alt = attributeSet.getDoubleAttribute(MilSymStandardAttributes.MILSYM_CP_ALT, 0);
        return List.of(createControlPointDefinition(new GeoPoint(lat, lon, alt), IControlPoint.CONTROL_POINT_2525, isEditable, altitudeMode));
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata, @Nonnull Runnable onSourceUpdated)
    {
    }

    @Override
    public void onControlPointRemoved(@Nonnull IControlPoint controlPoint, @Nonnull Runnable onSourceUpdated)
    {
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location, @Nonnull Runnable onSourceUpdated)
    {
        IMapObject mapObject = getSource().getMapObject();
        if (mapObject == null) return;
        if (controlPointType.equals(IControlPoint.CONTROL_POINT_2525))
        {
            var attributeSet = mapObject.getAttributeSet();
            attributeSet.setAttribute(MilSymStandardAttributes.MILSYM_CP_LAT, location.getLatitude());
            attributeSet.setAttribute(MilSymStandardAttributes.MILSYM_CP_LON, location.getLongitude());
            attributeSet.setAttribute(MilSymStandardAttributes.MILSYM_CP_ALT, location.getAltitude());
            onSourceUpdated.run();
        }
    }

    @Override
    public String getControlPointText(@Nonnull IControlPoint controlPoint)
    {
        return null;
    }
}