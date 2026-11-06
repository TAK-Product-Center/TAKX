package gov.takx.core.globe.tak.editor;

import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.core.globe.tak.editor.IShapeEditorSource.ControlPointDefinition;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base class for building an {@link IControlPointProvider} implementation for a multipoint source object. Provides:
 * <ul>
 *     <li>Creation of control point definitions from source object</li>
 *     <li>Handling of control point adding/removing/dragging</li>
 * </ul>
 *
 * @since 5.8.1 (backported from 5.9) (renamed from AMultipointEditorSource)
 */
public abstract class AMultipointControlPointProvider<T extends IGlobeShapeMapObject> extends AControlPointProvider<T>
{
    private final boolean isClosed;

    /**
     * @param source the source object being edited
     * @param isClosed {@code true} if the locations written to the source need to be closed, {@code false} if not
     */
    protected AMultipointControlPointProvider(@Nonnull T source, boolean isClosed)
    {
        super(source);
        this.isClosed = isClosed;
    }

    /**
     * Get the locations of the multipoint shape.
     * @param source the source object
     * @return a list of locations for the source
     */
    @Nonnull
    protected abstract List<ILocation> getLocations(@Nonnull T source);

    /**
     * Set the locations of the multipoint shape.
     * @param source the source object
     * @param locations a list of locations to set on the source
     */
    protected abstract void setLocations(@Nonnull T source, @Nonnull List<ILocation> locations);

    /**
     * Get the altitude mode for the multipoint shape vertices.
     *
     * @return the altitude mode for the source
     * @since 5.6
     */
    protected abstract AltitudeMode getAltitudeMode(@Nonnull T source);

    /**
     * Get whether the shape is editable.
     * @return {@code true} if editable, {@code false} if not
     */
    protected abstract boolean isEditable(@Nonnull T source);

    @Override
    @Nonnull
    public List<ControlPointDefinition> createControlPointDefinitions()
    {
        List<ILocation> locations = getUnclosedLocations();
        AltitudeMode altitudeMode = getAltitudeMode(getSource());
        AtomicInteger index = new AtomicInteger(0);
        return locations.stream()
                .map(l -> new ControlPointDefinition(l, IControlPoint.toVertexControlPointType(index.getAndIncrement()), isEditable(getSource()), altitudeMode))
                .toList();
    }

    @Override
    public void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata, @Nonnull Runnable onSourceUpdated)
    {
        if (!isEditable(getSource())) return;
        List<ILocation> locations = getUnclosedLocations();
        if (metadata.containsKey(IControlPoint.MODIFIER_CTRL))
        {
            // Use original source locations so that if this is a closed shape, we will use closed positions here
            Integer index = AShapeEditorSource.insertIndexForPositionInPlace(getLocations(getSource()), location);
            if (index != null)
            {
                locations.add(index, location);
            }
        } else
        {
            locations.add(location);
        }
        setClosedLocations(locations);
        onSourceUpdated.run();
    }

    @Override
    public void onControlPointRemoved(@Nonnull IControlPoint controlPoint, @Nonnull Runnable onSourceUpdated)
    {
        if (!isEditable(getSource())) return;
        List<ILocation> locations = getUnclosedLocations();
        Integer index = IControlPoint.getVertexIndex(controlPoint.getControlPointType());
        if (index == null) return;
        locations.remove(index.intValue());
        setClosedLocations(locations);
        onSourceUpdated.run();
    }

    @Override
    public void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location, @Nonnull Runnable onSourceUpdated)
    {
        List<ILocation> locations = getUnclosedLocations();
        Integer index = IControlPoint.getVertexIndex(controlPointType);
        if (index == null) return;
        locations.set(index, location);
        setClosedLocations(locations);
        onSourceUpdated.run();
    }

    /**
     * Get the current locations of the multipoint shape as an unclosed list.
     * @return the current locations as an unclosed list
     */
    @Nonnull
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
    protected final void setClosedLocations(@Nonnull List<ILocation> locations)
    {
        if (isClosed) locations = ShapeUtils.closeLocations(locations);
        setLocations(getSource(), locations);
    }
}
