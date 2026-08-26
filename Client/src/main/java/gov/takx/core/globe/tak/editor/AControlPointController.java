package gov.takx.core.globe.tak.editor;

import com.atakmap.map.CameraController;
import gov.tak.api.engine.map.IMapRendererEnums;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.math.IGeometryModel;
import gov.tak.api.engine.math.PointD;
import gov.tak.platform.graphics.PointF;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.core.globe.tak.editor.IShapeEditorSource.ControlPointDefinition;
import gov.takx.core.globe.tak.editor.IShapeEditorSource.ShapeEditorSourceListener;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.Point;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;

/**
 * Base class for building an {@link IControlPointController} implementation. Provides:
 * <ul>
 *     <li>Source object change monitoring</li>
 *     <li>Creation of control points from source object</li>
 * </ul>
 *
 * @since 5.4
 */
public abstract class AControlPointController implements IControlPointController, PropertyChangeListener, ShapeEditorSourceListener
{
    private IShapeEditor shapeEditor;
    private final List<IControlPoint> controlPoints = new ArrayList<>();
    private final Set<ControlPointEventListener> listeners = new LinkedHashSet<>();
    private final PropertyChangeSupport pcs = new PropertyChangeSupport(this);

    @Override
    public void setEnabled(boolean enabled)
    {
        IShapeEditorSource<?> source = getShapeEditor().getShapeEditorSource();
        if (enabled)
        {
            List<ControlPointDefinition> controlPointDefinitions = source.createControlPointDefinitions();
            setControlPoints(controlPointDefinitions.stream().map(ControlPoint::new).toList());
            source.addShapeEditorSourceListener(this);
        } else
        {
            source.removeShapeEditorSourceListener(this);
        }
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

    @Override
    @Nonnull
    public List<IControlPoint> getControlPoints()
    {
        return Collections.unmodifiableList(controlPoints);
    }

    private void setControlPoints(List<ControlPoint> controlPoints)
    {
        Queue<IControlPoint> newControlPointQueue = new LinkedList<>(controlPoints);
        Queue<IControlPoint> oldControlPointQueue = new LinkedList<>(getControlPoints());
        while (!newControlPointQueue.isEmpty() && !oldControlPointQueue.isEmpty())
        {
            IControlPoint newControlPoint = newControlPointQueue.remove();
            IControlPoint oldControlPoint = oldControlPointQueue.remove();
            oldControlPoint.set(newControlPoint);
        }
        oldControlPointQueue.forEach(this::removeControlPoint);
        newControlPointQueue.forEach(this::addControlPoint);
    }

    private void addControlPoint(IControlPoint controlPoint)
    {
        controlPoints.add(controlPoint);
        controlPoint.addPropertyChangeListener(this);
    }

    private void removeControlPoint(IControlPoint controlPoint)
    {
        controlPoints.remove(controlPoint);
        controlPoint.removePropertyChangeListener(this);
    }

    @Override
    public void addControlPointEventListener(@Nonnull ControlPointEventListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeControlPointEventListener(@Nonnull ControlPointEventListener listener)
    {
        listeners.remove(listener);
    }

    /**
     * Send an {@link ControlPointEventListener#onControlPointAdded(ILocation, Map)} event to listeners.
     * @param location the location to add the control point
     */
    protected void onControlPointAdded(@Nonnull ILocation location)
    {
        onControlPointAdded(location, Collections.emptyMap());
    }

    /**
     * Send an {@link ControlPointEventListener#onControlPointAdded(ILocation, Map)} event to listeners.
     * @param location the location to add the control point
     * @param metadata metadata passed to the listener
     */
    protected void onControlPointAdded(@Nonnull ILocation location, @Nonnull Map<String, Object> metadata)
    {
        listeners.forEach(l -> l.onControlPointAdded(location, metadata));
    }

    /**
     * Send an {@link ControlPointEventListener#onControlPointRemoved(IControlPoint)} event to listeners.
     * @param controlPoint the control point to remove
     */
    protected void onControlPointRemoved(@Nonnull IControlPoint controlPoint)
    {
        listeners.forEach(l -> l.onControlPointRemoved(controlPoint));
    }

    /**
     * Send an {@link ControlPointEventListener#onControlPointDragged(String, ILocation)} event to listeners.
     * @param controlPointType the control point type that was dragged
     * @param location the location the control point was dragged to
     */
    protected void onControlPointDragged(@Nonnull String controlPointType, @Nonnull ILocation location)
    {
        listeners.forEach(l -> l.onControlPointDragged(controlPointType, location));
    }

    @Override
    public void addPropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        pcs.addPropertyChangeListener(listener);
    }

    @Override
    public void removePropertyChangeListener(@Nonnull PropertyChangeListener listener)
    {
        pcs.removePropertyChangeListener(listener);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        pcs.firePropertyChange(evt);
    }

    @Override
    public <T> void onSourceUpdated(T source)
    {
        List<ControlPointDefinition> controlPointDefinitions = getShapeEditor().getShapeEditorSource().createControlPointDefinitions();
        setControlPoints(controlPointDefinitions.stream().map(ControlPoint::new).toList());
    }

    /**
     * Convert {@link Point screen coordinates} to a {@link ILocation geographic point}.
     * @param point the screen coordinates
     * @param mapRenderer a map renderer
     * @return the location of the point
     */
    @Nullable
    protected static ILocation inverse(@Nonnull Point point, @Nonnull MapRenderer<?> mapRenderer)
    {
        var lla = GeoPoint.createMutable();
        IMapRendererEnums.InverseResult result = mapRenderer.inverse(new PointD(point.x, point.y, 0), lla, IMapRendererEnums.InverseMode.RayCast, 0, DisplayOrigin.UpperLeft);
        return result != IMapRendererEnums.InverseResult.None ? new gov.tak.api.maps.coords.GeoPoint(lla) : null;
    }

    /**
     * Convert {@link Point screen coordinates} to a {@link ILocation geographic point} using an existing {@link IControlPoint} as a reference point for altitude.
     * @param point the screen coordinates
     * @param mapRenderer a map renderer
     * @param controlPoint the control point to use as a reference point for altitude
     * @return the location of the point
     * @since 5.6
     */
    @Nullable
    protected static ILocation inverse(@Nonnull Point point, @Nonnull MapRenderer<?> mapRenderer, @Nonnull IControlPoint controlPoint)
    {
        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, DisplayOrigin.UpperLeft);
        com.atakmap.map.MapSceneModel mapSceneModelLegacy = MarshalManager.marshal(mapSceneModel, MapSceneModel.class, com.atakmap.map.MapSceneModel.class);
        com.atakmap.math.GeometryModel geometryModelLegacy = CameraController.Util.createFocusAltitudeModel(mapSceneModelLegacy, new com.atakmap.coremap.maps.coords.GeoPoint(controlPoint.getLocation()));
        IGeometryModel geometryModel = MarshalManager.marshal(geometryModelLegacy, com.atakmap.math.GeometryModel.class, IGeometryModel.class);
        var lla = GeoPoint.createMutable();
        boolean success = mapSceneModel.inverse(new PointF(point.x, point.y), lla, geometryModel);
        return success ? new gov.tak.api.maps.coords.GeoPoint(lla) : null;
    }
}
