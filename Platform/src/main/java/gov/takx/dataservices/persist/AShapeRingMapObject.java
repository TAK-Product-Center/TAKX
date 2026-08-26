package gov.takx.dataservices.persist;

import gov.takx.api.mapobject.IShapeRingMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableRingedShape;
import jakarta.persistence.Entity;

/**
 * Base class for map objects that support concentric rings around the main shape (i.e. range rings).
 *
 * @since 3.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Entity
public abstract class AShapeRingMapObject<T extends IMutableRingedShape> extends ARegularShapeMapObject<T> implements IShapeRingMapObject<T>
{
    protected AShapeRingMapObject()
    {
    }

    protected AShapeRingMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);
    }

    @Override
    public int getRingCount()
    {
        return shape.getRingCount();
    }

    @Override
    public void setRingCount(int ringCount)
    {
        shape.setRingCount(ringCount);
    }
}
