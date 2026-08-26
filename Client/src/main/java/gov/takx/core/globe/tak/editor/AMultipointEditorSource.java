package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for building an {@link IShapeEditorSource} implementation for a multipoint source object. Provides:
 * <ul>
 *     <li>Creation of control point definitions from source object</li>
 *     <li>Handling of control point adding/removing/dragging</li>
 * </ul>
 *
 * @since 5.4
 */
public abstract class AMultipointEditorSource<T> extends AShapeEditorSource<T>
{
    private final boolean isClosed;

    /**
     * @param source the source object being edited
     * @param isClosed {@code true} if the locations written to the source need to be closed, {@code false} if not
     */
    protected AMultipointEditorSource(T source, boolean isClosed)
    {
        super(source);
        this.isClosed = isClosed;
    }

    /**
     * Get the locations of the multipoint shape.
     * @param source the source object
     * @return a list of locations for the source
     */
    protected abstract List<ILocation> getLocations(T source);

    /**
     * Set the locations of the multipoint shape.
     * @param source the source object
     * @param locations a list of locations to set on the source
     */
    protected abstract void setLocations(T source, List<ILocation> locations);

    /**
     * Get the altitude mode for the multipoint shape vertices.
     *
     * @return the altitude mode for the source
     * @since 5.6
     */
    protected abstract AltitudeMode getAltitudeMode(T source);

    /**
     * Get whether the shape is editable.
     * @return {@code true} if editable, {@code false} if not
     */
    protected abstract boolean isEditable();

    @Override
    public List<ControlPointDefinition> createControlPointDefinitions()
    {
        List<ILocation> locations = getUnclosedLocations();
        AltitudeMode altitudeMode = getAltitudeMode(getSource());
        AtomicInteger index = new AtomicInteger(0);
        return locations.stream()
                .map(l -> new ControlPointDefinition(l, Integer.toString(index.getAndIncrement()), isEditable(), altitudeMode))
                .toList();
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
    {
        List<ILocation> locations = getUnclosedLocations();
        int index = Integer.parseInt(controlPointType);
        locations.set(index, location);
        setClosedLocations(locations);
        onSourceUpdated(getSource());
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        if (!isEditable()) return;
        List<ILocation> locations = getUnclosedLocations();
        if (metadata.containsKey(IControlPoint.MODIFIER_CTRL))
        {
            // Use original source locations so that if this is a closed shape, we will use closed positions here
            Integer index = insertIndexForPositionInPlace(getLocations(getSource()), location);
            if (index != null)
            {
                locations.add(index, location);
            }
        } else
        {
            locations.add(location);
        }
        setClosedLocations(locations);
        onSourceUpdated(getSource());
        super.onControlPointAdded(location, metadata);
    }

    @Override
    public void onControlPointRemoved(@Nonnull IControlPoint controlPoint)
    {
        if (!isEditable()) return;
        List<ILocation> locations = getUnclosedLocations();
        int index = Integer.parseInt(controlPoint.getControlPointType());
        locations.remove(index);
        setClosedLocations(locations);
        onSourceUpdated(getSource());
        super.onControlPointRemoved(controlPoint);
    }

    /**
     * Get the current locations of the multipoint shape as an unclosed list.
     * @return the current locations as an unclosed list
     */
    protected final List<ILocation> getUnclosedLocations()
    {
        List<ILocation> locations = getLocations(getSource());
        if (isClosed) locations = ShapeUtils.uncloseLocations(locations);
        return locations;
    }

    /**
     * Set the current locations of the multipoint shape as a closed list.
     * @param locations  the locations to set
     */
    protected final void setClosedLocations(List<ILocation> locations)
    {
        if (isClosed) locations = ShapeUtils.closeLocations(locations);
        setLocations(getSource(), locations);
    }
}
