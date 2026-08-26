package gov.takx.dataservices.persist;

import gov.tak.api.cot.CotTypes;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableIrregularShape;
import jakarta.persistence.Entity;

/**
 * Map object representation of a polyline (path).
 *
 */
@Entity
public class ShapePathMapObject<T extends IMutableIrregularShape> extends AIrregularShapeMapObject<T>
{
    protected ShapePathMapObject()
    {
    }

    public ShapePathMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        setCotType(CotTypes.SHAPE_POLYSHAPE);
    }

    @Override
    public boolean canUseVerticalHeight()
    {
        return shape.canUseVerticalHeight();
    }

    @Override
    public void setVerticalHeight(double verticalHeight)
    {
        shape.setVerticalHeight(verticalHeight);
    }

    @Override
    public double getVerticalHeight()
    {
        return shape.getVerticalHeight();
    }

    @Override
    public void setVerticalHeightEnabled(boolean verticalHeightEnabled)
    {
        shape.setVerticalHeightEnabled(verticalHeightEnabled);
    }

    @Override
    public boolean isVerticalHeightEnabled()
    {
        return shape.isVerticalHeightEnabled();
    }
}
