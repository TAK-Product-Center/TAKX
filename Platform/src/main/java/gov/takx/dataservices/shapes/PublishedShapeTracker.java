package gov.takx.dataservices.shapes;

import gov.takx.api.event.mapobject.MapObjectAdded;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeMapObject;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeServiceListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static gov.takx.api.shapes.ShapeProperties.PUBLISHED;

/**
 * Service to track shapes that are "published" - also known as "geo-fences".
 *
 * @since 2.29
 */
@ApplicationScoped
public class PublishedShapeTracker implements PropertyChangeListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // All published shapes (regardless of who created them)
    protected Map<String, IShape> publishedShapes = new ConcurrentHashMap<>();

    protected final List<IShapeServiceListener> listeners = new CopyOnWriteArrayList<>();

    /**
     * @return All the known published shapes, never {@code null}, may be empty
     */
    @Nonnull
    public List<IShape> getShapes()
    {
        return new ArrayList<>(publishedShapes.values());
    }

    /**
     * Gets the shape associated with the given map object key
     *
     * @return The shape matching the key
     * @since 2.33
     */
    @Nullable
    public IShape getShape(String key)
    {
        return publishedShapes.get(key);
    }

    private void onProjectClosing(@Observes ProjectClosing event)
    {
        publishedShapes.values().forEach(shape -> shape.getShapeMapObject().removePropertyChangeListener(PUBLISHED, this));

        publishedShapes.clear();
    }

    /**
     * If the MapObject is a shape, then add the given shape to our registry, if it is published,
     * and add a listener to detect if it changes its published state.
     *
     * @since 3.3
     */
    private void onMapObjectAdded(@Observes MapObjectAdded event)
    {
        final IMapObject mapObject = event.getMapObject();

        if (mapObject instanceof IShapeMapObject<?> shapeMapObject)
        {
            shapeMapObject.addPropertyChangeListener(PUBLISHED, this);

            if (shapeMapObject.isPublished())
            {
                logger.debug("added: {}", shapeMapObject.getKey());

                final IShape shape = shapeMapObject.getAssociatedShape();
                publishedShapes.put(shapeMapObject.getKey(), shape);

                notifyListeners(l -> l.onShapeDefined(shape));
                notifyListeners(l -> l.onShapePublished(shape));
            }
        }
    }

    /**
     * Handle a property change on a shape we are monitoring.  Currently, we only care about the PUBLISHED property.
     * If PUBLISHED changes, we need to notify listeners accordingly.
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();

        if (!propertyName.equals(PUBLISHED) || !(evt.getSource() instanceof IShapeMapObject<?> shapeMapObject))
        {
            logger.error("Unexpected property change: {}", evt);
            return;
        }

        final String moKey = shapeMapObject.getKey();
        final boolean published = (Boolean) evt.getNewValue();

        if (published)
        {
            final IShape shape = shapeMapObject.getAssociatedShape();

            // Add it to the published collection and fire off a notification of a new published shape
            publishedShapes.put(moKey, shape);
            notifyListeners(l -> l.onShapePublished(shape));
        } else
        {
            // A shape is no longer published, remove it from our registry
            final IShape shape = publishedShapes.remove(moKey);
            notifyListeners(l -> l.onShapeUnpublished(shape));
        }
    }

    /**
     * Handle a Map Object being removed.  If it is a ShapeMapObject, then stop listening to PCes on it and
     * fire the appropriate event to our listeners.
     *
     * @since 3.0
     */
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        final IMapObject mapObject = event.getMapObject();

        if (mapObject instanceof IShapeMapObject)
        {
            mapObject.removePropertyChangeListener(PUBLISHED, this);

            final IShape removedShape = publishedShapes.remove(mapObject.getKey());
            if (removedShape != null)
            {
                logger.debug("deleted: {}", removedShape.getKey());
                notifyListeners(l -> l.onShapeDeletion(removedShape));
            }
        }
    }

    public void addShapeServiceListener(@Nonnull IShapeServiceListener shapeServiceListener)
    {
        listeners.add(shapeServiceListener);
    }

    public boolean removeShapeServiceListener(@Nonnull IShapeServiceListener shapeServiceListener)
    {
        return listeners.remove(shapeServiceListener);
    }

    /**
     * Helper method for notifying all listeners about a shape event.
     *
     * @param methodCaller Consumer to perform the method call on each listener
     * @since 3.3
     */
    private void notifyListeners(Consumer<IShapeServiceListener> methodCaller)
    {
        listeners.forEach(listener -> {
            try
            {
                methodCaller.accept(listener);
            } catch (Exception e)
            {
                logger.warn("Exception notifying listener", e);
            }
        });
    }

    PublishedShapeTracker()
    {
    }
}
