package gov.takx.dataservices.persist;

import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableRegularShape;
import jakarta.persistence.Entity;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Entity
public abstract class ARegularShapeMapObject<T extends IMutableRegularShape> extends AShapeMapObject<T> implements IMutableRegularShape
{
    protected ARegularShapeMapObject()
    {
    }

    protected ARegularShapeMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);
    }

    @Override
    public double getOrientation()
    {
        return shape.getOrientation();
    }

    @Override
    public void setOrientation(double orientationDegrees)
    {
        shape.setOrientation(orientationDegrees);
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
