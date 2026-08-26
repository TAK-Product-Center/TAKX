package gov.takx.dataservices.persist;

import gov.tak.api.cot.CotTypes;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableIrregularShape;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;

import java.util.List;

/**
 * Map object representation of an irregular shape, defined by a series of vertices.
 *
 * @since 3.3
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@Entity
public abstract class AIrregularShapeMapObject<T extends IMutableIrregularShape> extends AShapeMapObject<T> implements IShapePathMapObject<T>
{
    protected AIrregularShapeMapObject()
    {
    }

    protected AIrregularShapeMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        setCotType(CotTypes.SHAPE_POLYSHAPE);
    }

    @Nonnull
    @Override
    public List<ILocation> getVertices()
    {
        return shape.getVertices();
    }

    @Override
    public void setVertices(@Nonnull List<? extends ILocation> points)
    {
        shape.setVertices(points);
    }
}
