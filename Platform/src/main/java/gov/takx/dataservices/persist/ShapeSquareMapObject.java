package gov.takx.dataservices.persist;

import gov.tak.api.cot.CotTypes;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IShapeSquareMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableSquare;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;

import java.util.List;

/**
 * Configuration for a square.
 *
 */
@Entity
public class ShapeSquareMapObject extends ARegularShapeMapObject<IMutableSquare> implements IShapeSquareMapObject
{
    protected ShapeSquareMapObject()
    {
    }

    @SuppressWarnings("unused")
    public ShapeSquareMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        setCotType(CotTypes.SHAPE_RECTANGLE);
    }

    @Override
    public double getWidth()
    {
        return shape.getWidth();
    }

    @Override
    public void setWidth(double width)
    {
        shape.setSideLength(width);
    }

    @Override
    public double getSideLength()
    {
        return shape.getSideLength();
    }

    @Override
    public void setSideLength(double sideLength)
    {
        shape.setSideLength(sideLength);
    }

    @Override
    public double getHeight()
    {
        return shape.getHeight();
    }

    @Override
    public void setHeight(double height)
    {
        shape.setSideLength(height);
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

    @Override
    public void setSize(double width, double height)
    {
        throw new UnsupportedOperationException("Can't set independent side lengths on a Square");
    }
}
