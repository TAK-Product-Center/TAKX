package gov.takx.dataservices.persist;

import gov.tak.api.cot.CotTypes;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMultiPolylineMapObject;
import gov.takx.api.mapobject.IShapePathMapObject;
import gov.takx.api.mapobject.MapObjectSpec;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Transient;
import org.hibernate.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A map object to represent a set of associated polylines. It is made up of {@link IShapePathMapObject}s that represent
 * each polyline.
 *
 * @since 3.0
 */
@PseudoBean
@Entity
public class MultiPolylineMapObject extends MapObject implements IMultiPolylineMapObject
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Transient
    private LocationMonitor polylineLocationMonitor;

    @Column(length = Length.LONG32)
    private volatile String polylineMapObjectIds; // CSV string of the polyline MO ids

    @Transient
    private final List<IShapePathMapObject<?>> polylineMapObjects = new CopyOnWriteArrayList<>();

    private double heightMeters = Double.NaN;

    @Transient
    @Inject
    private ICoreExecutorService executorService;

    @Transient
    @Inject
    private IMapObjectPersist mapObjectPersist;

    protected MultiPolylineMapObject()
    {
    }

    public MultiPolylineMapObject(MapObjectSpec mapObjectSpec, IPluginDefinition<? extends IRaptorPlugin> pluginDefinition)
    {
        super(mapObjectSpec, pluginDefinition);

        setCotType(CotTypes.SHAPE_MULTI_POLYLINE);
    }

    /**
     * Reconnect the polyline map objects that make up this multi-polyline MO.
     */
    @Override
    public void onAllLoaded()
    {
        super.onAllLoaded();

        this.<IShapePathMapObject<?>>findMapObjectsByCsvIds(polylineMapObjectIds).forEach(this::addPolyline);
    }

    @Nonnull
    @Override
    public List<IShapePathMapObject<?>> getPolylines()
    {
        return new ArrayList<>(polylineMapObjects);
    }

    @Nonnull
    @Override
    public IShapePathMapObject<?> getPolyline(int index)
    {
        return polylineMapObjects.get(index);
    }

    @Override
    public void addPolyline(@Nonnull IShapePathMapObject<?> polylineMapObject)
    {
        if (polylineMapObjects.contains(polylineMapObject))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Polyline {} is already part of multi-polyline {}; skipping add.", polylineMapObject.getKey(), getKey());
            }
            return;
        }

        polylineMapObject.setOwner(this); // Take ownership of this MO
        mapObjectPersist.persistMapObject(polylineMapObject); // Ensure new ownership is persisted

        // We need to track the location of one of the polylines for the overall multi-polyline, so we'll just use the first one
        if (polylineLocationMonitor == null)
        {
            setLocation(polylineMapObject.getLocation());
            polylineLocationMonitor = new LocationMonitor(polylineMapObject, this::setLocation);
        }

        polylineMapObjects.add(polylineMapObject);
        updatePolylineMapObjectIds();
        propertyChangeSupport.firePropertyChange(POLYLINE_ADDED, null, polylineMapObject);
    }

    @Override
    public boolean removePolyline(@Nonnull IShapePathMapObject<?> polylineMapObject)
    {
        if (!polylineMapObjects.contains(polylineMapObject))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Polyline {} is not part of multi-polyline {}; skipping remove.", polylineMapObject.getKey(), getKey());
            }

            return false;
        }

        final boolean removed = polylineMapObjects.remove(polylineMapObject);
        updatePolylineMapObjectIds();

        polylineMapObject.setOwner(null); // Give up ownership of this MO

        // If this was the source of the location updates for the overall multi-polyline, we need to pick a new one
        if (removed && polylineLocationMonitor.source.equals(polylineMapObject))
        {
            polylineLocationMonitor.dispose();
            polylineLocationMonitor = null;

            if (!polylineMapObjects.isEmpty())
            {
                final IShapePathMapObject<?> firstPolyline = polylineMapObjects.get(0);
                setLocation(firstPolyline.getLocation());
                polylineLocationMonitor = new LocationMonitor(firstPolyline, this::setLocation);
            }
        }

        // It may seem a little weird to have the removed value as the "newValue", but it's justifiable since this is the
        // MO that was "newly" removed, and it helps the code to be common when handling the events.
        propertyChangeSupport.firePropertyChange(POLYLINE_REMOVED, null, polylineMapObject);

        return removed;
    }

    @Override
    public double getHeightMeters()
    {
        return heightMeters;
    }

    @Override
    public boolean hasHeight()
    {
        return !Double.isNaN(heightMeters);
    }

    @Override
    public void setHeightMeters(double heightMeters)
    {
        // TODO KMB: it's not really clear to me yet what we should do with the height... I would assume we should set
        //  it on each polyline, but there's no easy way to do that right now and WinTAK doesn't seem to do that. So
        //  just hang onto the value for now until we figure it out.

        propertyChangeSupport.updateAndFire(HEIGHT, this.heightMeters, heightMeters,
                () -> this.heightMeters = heightMeters);
    }

    /**
     * Updates the CSV string of polyline MO ids according to the current contents of {@code polylineMapObjects}.
     */
    private void updatePolylineMapObjectIds()
    {
        polylineMapObjectIds = encodeMapObjectsAsCsvIds(polylineMapObjects);
    }

    /**
     * Called when a map object is deleted.
     *
     * @param event The event describing the removed map object
     */
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        final IMapObject mapObject = event.getMapObject();
        if (mapObject instanceof IShapePathMapObject)
        {
            final boolean removed = removePolyline((IShapePathMapObject<?>) mapObject);

            // To match WinTAK, if the last polyline is removed, we should delete this MO.
            if (removed && polylineMapObjects.isEmpty())
            {
                executorService.execute(() -> {
                    Thread.currentThread().setName("Delete multi-polyline");
                    mapObjectManager.deleteMapObject(this);
                });
            }
        }
    }

    @Override
    public void setVisible(boolean visible)
    {
        super.setVisible(visible);

        // Apply the new visibility to all our component MOs
        polylineMapObjects.forEach(p -> p.setVisible(visible));
    }

    @RemoveObserver
    @Override
    public void shutdown()
    {
        if (polylineLocationMonitor != null)
        {
            polylineLocationMonitor.dispose();
            polylineLocationMonitor = null;
        }

        super.shutdown();
    }
}
