package gov.takx.core.globe.tak.editor;

import com.atakmap.coremap.maps.coords.Vector2D;
import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.core.globe.GlobeShapeMapObject;
import gov.takx.core.globe.tak.editor.IControlPointController.ControlPointEventListener;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Base class for building an {@link IShapeEditorSource} implementation.
 *
 * @param <T> the type of source object being edited
 * @since 5.4
 */
public abstract class AShapeEditorSource<T> implements IShapeEditorSource<T>, ControlPointEventListener, PropertyChangeListener
{
    /**
     * A collection of common properties that need to be monitored for {@link gov.takx.api.shapes.IShape} objects
     * or {@link GlobeShapeMapObject} objects that house IShape.
     */
    protected static final List<String> SHAPE_PROPERTIES = List.of(
            IMapObject.NAME, ShapeProperties.ANGLE, ShapeProperties.BEARING, ShapeProperties.EDITABLE,
            ShapeProperties.FILL_COLOR, ShapeProperties.GROUND_REFERENCE, ShapeProperties.HEIGHT, ShapeProperties.HOLE_ADDED,
            ShapeProperties.HOLE_REMOVED, ShapeProperties.LINE_COLOR, ShapeProperties.LINE_WIDTH, ShapeProperties.LOCATION, ShapeProperties.LOCATIONS,
            ShapeProperties.SEMI_MAJOR_AXIS, ShapeProperties.SEMI_MINOR_AXIS, ShapeProperties.ORIENTABLE, ShapeProperties.ORIENTATION,
            ShapeProperties.PUBLISHED, ShapeProperties.RADIUS, ShapeProperties.RANGE, ShapeProperties.RING_COUNT,
            ShapeProperties.SWEEP, ShapeProperties.VERTICAL_HEIGHT, ShapeProperties.VERTICAL_HEIGHT_ENABLED, ShapeProperties.VISIBILITY,
            ShapeProperties.WIDTH);

    private IShapeEditor shapeEditor;
    private final Set<ShapeEditorSourceListener> listeners = new LinkedHashSet<>();
    private final T source;
    private boolean controlPointAdded; // True if any control point has been added

    /**
     * @param source the source object being edited
     */
    protected AShapeEditorSource(@Nonnull T source)
    {
        this.source = source;
    }

    /**
     * Add the supplied {@link PropertyChangeListener} to the source object.
     * @param listener the property change listener
     */
    protected abstract void addSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener);

    /**
     * Remove the supplied {@link PropertyChangeListener} from the source object.
     * @param listener the property change listener
     */
    protected abstract void removeSourcePropertyChangeListener(@Nonnull PropertyChangeListener listener);

    @Override
    @Nonnull
    public T getSource()
    {
        return source;
    }

    @Override
    public void setEnabled(boolean enabled)
    {
        if (enabled)
        {
            getShapeEditor().getControlPointController().addControlPointEventListener(this);
            addSourcePropertyChangeListener(this);
        } else
        {
            getShapeEditor().getControlPointController().removeControlPointEventListener(this);
            removeSourcePropertyChangeListener(this);
        }
    }

    @Override
    public void setArmed(boolean armed)
    {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        onSourceUpdated(getSource());
    }

    @Override
    public void addShapeEditorSourceListener(ShapeEditorSourceListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeShapeEditorSourceListener(ShapeEditorSourceListener listener)
    {
        listeners.remove(listener);
    }

    protected void onSourceUpdated(T source)
    {
        listeners.forEach(l -> l.onSourceUpdated(source));
    }

    @Override
    @OverridingMethodsMustInvokeSuper
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        controlPointAdded = true;
    }

    @Override
    public boolean hasControlPoints()
    {
        return controlPointAdded;
    }

    @Override
    public void onControlPointRemoved(@Nonnull IControlPoint controlPoint)
    {
        //
    }

    @Override
    @Nonnull
    public IShapeEditor getShapeEditor()
    {
        return Objects.requireNonNull(shapeEditor);
    }

    @Override
    public void setShapeEditor(@Nonnull IShapeEditor shapeEditor)
    {
        this.shapeEditor = shapeEditor;
    }

    /**
     * Calculate the insertion index for a location within a list of existing connected locations.
     * @param locations the existing locations
     * @param location the location to insert
     * @return the index where to insert the new location, or {@code null} if there is no appropriate index to insert
     * @since 5.5
     */
    protected static Integer insertIndexForPositionInPlace(List<ILocation> locations, ILocation location)
    {
        Integer index = null;
        double minDistance = Double.MAX_VALUE;
        Vector2D locationVec = new Vector2D(location.getLongitude(), location.getLatitude());
        ILocation p1 = locations.get(0);
        for (int a = 1; a < locations.size(); a++)
        {
            ILocation p2 = locations.get(a);
            Vector2D nearestPoint = Vector2D.nearestPointOnSegment(locationVec,
                    new Vector2D(p1.getLongitude(), p1.getLatitude()),
                    new Vector2D(p2.getLongitude(), p2.getLatitude()));
            double distance = nearestPoint.distance(locationVec);
            if (index == null || distance < minDistance)
            {
                index = a;
                minDistance = distance;
            }
            p1 = p2;
        }
        return index;
    }
}
