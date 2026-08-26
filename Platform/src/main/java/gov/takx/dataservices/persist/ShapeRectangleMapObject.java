package gov.takx.dataservices.persist;

import gov.tak.api.cot.CotTypes;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IShapeRectangleMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableRectangle;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;

import java.util.List;

/**
 * Configuration for a rectangle.
 *
 */
@Entity
public class ShapeRectangleMapObject extends ARegularShapeMapObject<IMutableRectangle> implements IShapeRectangleMapObject
{
    protected ShapeRectangleMapObject()
    {
    }

    public ShapeRectangleMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        setCotType(CotTypes.SHAPE_RECTANGLE);
    }

    @Override
    public double getHeight()
    {
        return shape.getHeight();
    }

    @Override
    public double getWidth()
    {
        return shape.getWidth();
    }

    @Override
    public void setWidth(double width)
    {
        shape.setWidth(width);
    }

    @Override
    public void setHeight(double height)
    {
        shape.setHeight(height);
    }

    @Override
    public void setSize(double width, double height)
    {
        shape.setSize(width, height);
    }

    @Override
    public ILocation getMinCorner()
    {
        return shape.getMinCorner();
    }

    @Override
    public ILocation getMaxCorner()
    {
        return shape.getMaxCorner();
    }

    @Override
    public List<ILocation> getVertices()
    {
        return shape.getVertices();
    }

    @Override
    public void setVertices(@Nonnull List<ILocation> vertices)
    {
        shape.setVertices(vertices);
    }
}
