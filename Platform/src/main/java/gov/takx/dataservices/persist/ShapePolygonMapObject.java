package gov.takx.dataservices.persist;

import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.mapobject.IShapePolygonMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutablePolygon;
import gov.takx.api.shapes.IPolygon;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Map object representation of a polygon.
 */
@Entity
public class ShapePolygonMapObject extends ShapePathMapObject<IMutablePolygon> implements IShapePolygonMapObject
{
    protected ShapePolygonMapObject()
    {
    }

    @SuppressWarnings("unused")
    public ShapePolygonMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);
    }

    @Nonnull
    @Override
    public Set<IPolygon> getHoles()
    {
        return shape.getHoles();
    }

    @Override
    public void addHole(@Nonnull IMutablePolygon hole) throws IllegalArgumentException
    {
        final IMutablePolygon model = hole instanceof IShapePolygonMapObject polygonMO ? polygonMO.getAssociatedShape() : hole;
        shape.addHole(model);
    }

    @Override
    public void removeHole(@Nonnull IMutablePolygon hole)
    {
        final IMutablePolygon model = hole instanceof IShapePolygonMapObject polygonMO ? polygonMO.getAssociatedShape() : hole;
        shape.removeHole(model);
    }

    @Override
    public void restoreHoles()
    {
        final Set<IPolygon> holes = shape.getHoles();

        if (!holes.isEmpty())
        {
            // Find the Map Objects for each of our holes, and then reattach their shape models to our model

            final List<IMutablePolygon> holeModels = holes.stream()
                    .map(polygon -> findMapObjectByKey(polygon.getKey(), "hole", IShapePolygonMapObject.class))
                    .filter(Objects::nonNull)
                    .map(IShapeMapObject::getAssociatedShape)
                    .toList();

            shape.reattachHoles(holeModels);
        }
    }

    @Override
    public void reattachHoles(Collection<IMutablePolygon> restoredHoles)
    {
        throw new IllegalStateException("reattachHoles should never be called on a Map Object");
    }
}
