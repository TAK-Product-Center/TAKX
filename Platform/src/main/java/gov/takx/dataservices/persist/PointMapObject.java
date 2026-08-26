package gov.takx.dataservices.persist;

import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.takx.api.mapobject.IPointMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import jakarta.annotation.Nonnull;
import jakarta.persistence.Entity;

/**
 * Concrete implementation of a "point" Map object.
 *
 * @since 3.0
 */
@Entity
public abstract class PointMapObject extends MapObject implements IPointMapObject
{
    protected PointMapObject()
    {
    }

    protected PointMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);
    }

    @Override
    public void setPoint(@Nonnull GeoPointMetaData geoPointMetaData)
    {
        setGeoPointMetaData(geoPointMetaData);
    }

    @Override
    public GeoPoint getPoint()
    {
        return geoPoint;
    }

    @Override
    public void setPoint(@Nonnull GeoPoint geoPoint)
    {
        setGeoPointMetaData(new GeoPointMetaData(geoPoint));
    }
}
