package gov.takx.dataservices.persist;

import gov.tak.api.cot.CotTypes;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeCircleMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.shapes.IMutableCircle;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nullable;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;

import java.beans.PropertyChangeEvent;

/**
 * Configuration of a circle shape.
 *
 */
@Entity
public class ShapeCircleMapObject extends AShapeRingMapObject<IMutableCircle> implements IShapeCircleMapObject
{
    // Supports linking the radius of this shape to a given map object
    @Transient
    protected IMapObject radiusAnchor;

    @Transient
    private LocationMonitor radiusAnchorLocationMonitor;

    protected Integer radiusAnchorId;

    public ShapeCircleMapObject()
    {
        super();
    }

    @SuppressWarnings("unused")
    public ShapeCircleMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        setCotType(CotTypes.SHAPE_CIRCLE);
    }

    @Override
    public void onAllLoaded()
    {
        super.onAllLoaded();

        relinkMapObject(radiusAnchorId, "radius anchor", this::setRadiusAnchor);
    }

    @Override
    public double getRadius()
    {
        return shape.getRadius();
    }

    @Override
    public void setRadius(double radius)
    {
        shape.setRadius(radius);
    }

    private void updateRadiusAnchorLocation()
    {
        final GeoPoint center = getLocation();
        final GeoPoint radiusAnchorLocation = radiusAnchor.getLocation();

        final double distance = center.distanceTo(radiusAnchorLocation);
        final double radius = shape.getRadius();

        if (Math.abs(distance - radius) > 0.1)
        {
            // Radius marker needs to be moved
            final double bearing = center.bearingTo(radiusAnchorLocation);
            final ILocation newPosition = ShapeUtils.greatCircleEndPosition(center, radius, bearing);
            radiusAnchor.getController().sendLocationChangeMessage(newPosition);
        }
    }

    @Override
    public void setRadiusAnchor(@Nullable IMapObject radiusAnchor)
    {
        final IMapObject oldAnchor = this.radiusAnchor;

        if (this.radiusAnchor != null)
        {
            radiusAnchorLocationMonitor.dispose();
            radiusAnchorLocationMonitor = null;
            radiusAnchorId = null;
        }

        this.radiusAnchor = radiusAnchor;

        if (radiusAnchor != null)
        {
            radiusAnchorId = radiusAnchor.getId();

            setRadius(getLocation().distanceTo(radiusAnchor.getLocation())); // Install radius from marker

            // Track its location so we can adjust our radius if/when it is moved
            radiusAnchorLocationMonitor = new LocationMonitor(radiusAnchor,
                    newLocation -> setRadius(getLocation().distanceTo(newLocation)));
        }

        if (oldAnchor != radiusAnchor)
        {
            propertyChangeSupport.firePropertyChange(ShapeProperties.RADIUS_ANCHOR,
                    oldAnchor == null ? null : oldAnchor.getKey(),
                    radiusAnchor == null ? null : radiusAnchor.getKey());
        }
    }

    @Nullable
    @Override
    public IMapObject getRadiusAnchor()
    {
        return radiusAnchor;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();

        if (ShapeProperties.RADIUS.equals(propertyName))
        {
            if (radiusAnchor != null) updateRadiusAnchorLocation(); // Update radius marker
        }

        super.propertyChange(evt);
    }

    @Override
    protected boolean internalSetLocation(ILocation location, long time, boolean manualLocation)
    {
        final GeoPoint oldCenter = getLocation();

        if (!super.internalSetLocation(location, time, manualLocation)) return false;

        // Update radius marker, if any
        if (radiusAnchor != null)
        {
            // Maintain original bearing from center to radius marker
            final double bearing = ShapeUtils.bearingTo(oldCenter, radiusAnchor.getLocation());
            final ILocation newPosition = ShapeUtils.greatCircleEndPosition(location, getRadius(), bearing);

            // TODO KMB: I think this is wrong. Instead of moving the radius marker, it should just increase the radius
            //  accordingly (which is what WinTAK does). However, there might be use cases for this behavior too, so it
            //  may need to be a configurable option. I've created TAKX-563 to investigate correct behavior and update
            //  if needed.
            radiusAnchor.getController().sendLocationChangeMessage(newPosition);
        }
        return true;
    }
}
