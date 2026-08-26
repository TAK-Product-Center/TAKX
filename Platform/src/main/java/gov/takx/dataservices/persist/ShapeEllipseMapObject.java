package gov.takx.dataservices.persist;

import gov.tak.api.cot.CotTypes;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IShapeEllipseMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableEllipse;
import jakarta.persistence.Entity;

import java.util.List;

/**
 * Store configuration for an ellipse.
 *
 */
@Entity
public class ShapeEllipseMapObject extends AShapeRingMapObject<IMutableEllipse> implements IShapeEllipseMapObject
{
    protected ShapeEllipseMapObject()
    {
    }

    public ShapeEllipseMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        setCotType(CotTypes.SHAPE_ELLIPSE);
    }

    @Override
    public double getSemiMajorAxis()
    {
        return shape.getSemiMajorAxis();
    }

    @Override
    public void setSemiMajorAxis(double semiMajorAxis)
    {
        shape.setSemiMajorAxis(semiMajorAxis);
    }

    @Override
    public double getSemiMinorAxis()
    {
        return shape.getSemiMinorAxis();
    }

    @Override
    public void setSemiMinorAxis(double semiMinorAxis)
    {
        shape.setSemiMinorAxis(semiMinorAxis);
    }

    @Override
    public void setAxes(double semiMajorAxis, double semiMinorAxis)
    {
        shape.setAxes(semiMajorAxis, semiMinorAxis);
    }

    @Override
    public List<ILocation> getVertices()
    {
        return shape.getVertices();
    }
}
