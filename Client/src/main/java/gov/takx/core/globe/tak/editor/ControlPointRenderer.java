package gov.takx.core.globe.tak.editor;

import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.GeometryCollection;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.IconPointStyle;
import com.atakmap.map.layer.feature.style.Style;
import com.atakmap.map.opengl.GLLabelManager;
import com.atakmap.map.opengl.GLLabelManager.VerticalAlignment;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.VFSManagerScope;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.filesystem.IVirtualFileSystem;
import gov.takx.api.filesystem.IVirtualFileSystemManager;
import gov.takx.api.filesystem.VFSScope;
import gov.takx.api.layer.pick.IPickContext;
import gov.takx.api.layer.pick.IPickedFeatureProvider;
import gov.takx.api.layer.pick.IPickedObjectDeconflictor;
import gov.takx.api.layer.pick.IPickedObjectManager;
import gov.takx.api.layer.pick.PickedObject;
import gov.takx.api.layer.pick.PickedObjectList;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.shapes.IShape.AltitudeMode;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.core.globe.tak.FeatureRendererManager;
import gov.takx.core.globe.tak.FeatureUtility;
import gov.takx.core.globe.tak.TakLabelManager;
import gov.takx.core.globe.tak.TakLabelManager.LabelRegistration;
import gov.takx.core.globe.tak.editor.IShapeEditorSource.ShapeEditorSourceListener;
import gov.takx.core.globe.tak.features.FrameFactory;
import gov.takx.platform.globe.GlobeTextUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * An implementation of {@link IShapeEditorRenderer} that renders control points using the TAK feature renderer.
 *
 * @since 5.4
 */
@PseudoBean
public class ControlPointRenderer extends AFeatureRenderer implements IPickedFeatureProvider, ShapeEditorSourceListener, PropertyChangeListener, IPickedObjectDeconflictor
{
    private static final int PROPERTY_MASK = FeatureDataStore2.PROPERTY_FEATURE_GEOMETRY | FeatureDataStore2.PROPERTY_FEATURE_STYLE;

    @Inject
    FeatureRendererManager featureRendererManager;

    @Inject
    @VFSManagerScope(VFSScope.Project)
    private IVirtualFileSystemManager projectVFSManager;

    @Inject
    private FrameFactory frameFactory;

    @Inject
    private IPickedObjectManager pickedObjectManager;

    @Inject
    TakLabelManager labelManager;

    private Long fsid;
    private final Map<IControlPoint, Registration> controlPointMap = new LinkedHashMap<>();

    @Override
    public void setEnabled(boolean enabled)
    {
        IShapeEditor shapeEditor = getShapeEditor();
        if (enabled)
        {
            fsid = insertFeatureSet(createFeatureSet());
            shapeEditor.getControlPointController().addPropertyChangeListener(this);
            shapeEditor.getShapeEditorSource().addShapeEditorSourceListener(this);
            featureRendererManager.addPickedFeatureProvider(this);
            pickedObjectManager.addPickedObjectDeconflictor(this);
            shapeEditor.getControlPointController().getControlPoints().forEach(this::onControlPointAdded);
        } else
        {
            new ArrayList<>(controlPointMap.keySet()).forEach(this::onControlPointRemoved);
            deleteFeatureSet(fsid);
            fsid = null;
            shapeEditor.getControlPointController().removePropertyChangeListener(this);
            shapeEditor.getShapeEditorSource().removeShapeEditorSourceListener(this);
            featureRendererManager.removePickedFeatureProvider(this);
            pickedObjectManager.removePickedObjectDeconflictor(this);
        }
    }

    @Override
    public void setArmed(boolean armed)
    {
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        onControlPointUpdated((IControlPoint) evt.getSource(), evt);
    }

    @Override
    public <T> void onSourceUpdated(T source)
    {
        Queue<IControlPoint> newControlPointQueue = new LinkedList<>(getShapeEditor().getControlPointController().getControlPoints());
        Queue<IControlPoint> oldControlPointQueue = new LinkedList<>(this.controlPointMap.keySet());
        while (!newControlPointQueue.isEmpty() && !oldControlPointQueue.isEmpty())
        {
            IControlPoint newControlPoint = newControlPointQueue.remove();
            IControlPoint oldControlPoint = oldControlPointQueue.remove();
            oldControlPoint.set(newControlPoint);
        }
        oldControlPointQueue.forEach(this::onControlPointRemoved);
        newControlPointQueue.forEach(this::onControlPointAdded);
    }

    private void onControlPointAdded(IControlPoint controlPoint)
    {
        long fid = insertFeature(createControlPointFeature(fsid, controlPoint));
        LabelRegistration labelRegistration = labelManager.createLabel((glLabelManager, labelId) -> applyToLabel(controlPoint, glLabelManager, labelId));
        updateLabel(labelRegistration);
        controlPointMap.put(controlPoint, new Registration(fid, labelRegistration));
    }

    private void onControlPointRemoved(IControlPoint controlPoint)
    {
        Registration registration = controlPointMap.remove(controlPoint);
        if (registration != null)
        {
            deleteFeature(registration.fid);
            labelManager.deleteLabel(registration.labelRegistration);
        }
    }

    private void onControlPointUpdated(IControlPoint controlPoint, @Nullable PropertyChangeEvent evt)
    {
        Registration registration = controlPointMap.get(controlPoint);
        if (registration != null)
        {
            Feature controlPointFeature = createControlPointFeature(fsid, controlPoint);
            updateFeature(registration.fid, controlPointFeature, PROPERTY_MASK);
            updateLabel(registration.labelRegistration);
        }
    }

    private Feature createControlPointFeature(long fsid, IControlPoint controlPoint)
    {
        Geometry geometry = createGeometry(controlPoint);
        Style style = createStyle(controlPoint);
        Feature.Traits traits = new Feature.Traits();
        traits.altitudeMode = MarshalManager.marshal(controlPoint.getAltitudeMode(), AltitudeMode.class, Feature.AltitudeMode.class);
        return new Feature(fsid, FeatureDataStore2.FEATURE_ID_NONE, null, geometry, style, null, traits, FeatureDataStore2.TIMESTAMP_NONE, FeatureDataStore2.FEATURE_VERSION_NONE);
    }

    /**
     * Create a {@link Geometry} for the provided {@link IControlPoint}.
     * @param controlPoint the control point
     * @return a geometry
     */
    protected Geometry createGeometry(IControlPoint controlPoint)
    {
        GeometryCollection geometries = new GeometryCollection(3);
        ILocation cpLocation = controlPoint.getLocation();
        geometries.addGeometry(new Point(cpLocation.getLongitude(), cpLocation.getLatitude(), cpLocation.getAltitude()));
        if (IControlPoint.CONTROL_POINT_ORIENTATION.equals(controlPoint.getControlPointType()))
        {
            List<IControlPoint> controlPoints = getShapeEditor().getControlPointController().getControlPoints();
            IControlPoint northControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_NORTH);
            IControlPoint northEastControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_NORTHEAST);
            IControlPoint northWestControlPoint = IControlPoint.getControlPointByType(controlPoints, IControlPoint.CONTROL_POINT_NORTHWEST);
            LineString line;
            if (northControlPoint != null)
            {
                line = new LineString(3);
                line.addPoint(northControlPoint.getLocation().getLongitude(), northControlPoint.getLocation().getLatitude(), northControlPoint.getLocation().getAltitude());
                line.addPoint(cpLocation.getLongitude(), cpLocation.getLatitude(), cpLocation.getAltitude());
                geometries.addGeometry(line);
            }
            if (northEastControlPoint != null && northWestControlPoint != null)
            {
                IGeoPoint midpoint = GeoCalculations.midpoint(northWestControlPoint.getLocation(), northEastControlPoint.getLocation());
                double altitude = (northWestControlPoint.getLocation().getAltitude() + northEastControlPoint.getLocation().getAltitude()) / 2;
                line = new LineString(3);
                line.addPoint(AShapeEditorSource.wrapIdl(midpoint.getLongitude()), midpoint.getLatitude(), altitude);
                line.addPoint(cpLocation.getLongitude(), cpLocation.getLatitude(), cpLocation.getAltitude());
                geometries.addGeometry(line);
            }
        }

        return geometries;
    }

    /**
     * Create a {@link Style} for the provided {@link IControlPoint}.
     * @param controlPoint the control point
     * @return a style
     */
    protected Style createStyle(IControlPoint controlPoint)
    {
        ArrayList<Style> styles = new ArrayList<>();
        int size = controlPoint.getPointSize() / 4;
        String uri = getGraphicShapeUrl(FrameFactory.ShapeType.Ellipse, size, size);
        int color = controlPoint.isSelected() ? controlPoint.getHighlightColor() : controlPoint.getColor();
        IconPointStyle iconPointStyle = new IconPointStyle(color, uri, size, size, 0, 0, 0, 0, 0, true);
        styles.add(iconPointStyle);
        if (IControlPoint.CONTROL_POINT_ORIENTATION.equals(controlPoint.getControlPointType()))
        {
            styles.add(new BasicStrokeStyle(0xAAFFFFFF, 6));
        }
        return new CompositeStyle(styles.toArray(Style[]::new));
    }

    @Nullable
    @Override
    public Object getPickObject(@Nonnull IPickContext context, long fid)
    {
        for (Map.Entry<IControlPoint, Registration> entry : controlPointMap.entrySet())
        {
            if (entry.getValue().fid == fid) return entry.getKey();
        }
        return null;
    }

    @Override
    public void deconflictPick(@Nonnull PickedObjectList pickedObjectList, @Nonnull IPickContext context)
    {
        if (getShapeEditor().isEnabled())
        {
            ArrayList<PickedObject> pickedControlPoints = new ArrayList<>();
            Iterator<PickedObject> it = pickedObjectList.iterator();
            while (it.hasNext())
            {
                PickedObject pickedObject = it.next();
                if (pickedObject.userObject() instanceof IControlPoint)
                {
                    pickedControlPoints.add(pickedObject);
                    it.remove();
                }
            }
            pickedObjectList.addAll(0, pickedControlPoints);
        }
    }

    private void updateLabel(LabelRegistration labelRegistration)
    {
        labelManager.updateLabel(labelRegistration);
    }

    protected void applyToLabel(IControlPoint controlPoint, GLLabelManager labelManager, int labelId)
    {
        String controlPointText = getShapeEditor().getShapeEditorSource() instanceof IControlPointTextProvider controlPointTextProvider ? controlPointTextProvider.getControlPointText(controlPoint) : "";
        labelManager.setText(labelId, controlPointText);
        labelManager.setGeometry(labelId, new Point(controlPoint.getLocation().getLongitude(), controlPoint.getLocation().getLatitude(), controlPoint.getLocation().getAltitude()));
        labelManager.setAltitudeMode(labelId, MarshalManager.marshal(controlPoint.getAltitudeMode(), AltitudeMode.class, Feature.AltitudeMode.class));
        labelManager.setVerticalAlignment(labelId, VerticalAlignment.Top);
        labelManager.setTextFormat(labelId, GlobeTextUtils.DEFAULT_GLOBE_FONT.getFontName(), 18, false, false, false, false);
        labelManager.setPriority(labelId, GLLabelManager.Priority.Always);
        labelManager.setRotation(labelId, 0, false);
    }

    protected String getGraphicShapeUrl(FrameFactory.ShapeType shapeType, int width, int height)
    {
        BufferedImage image = frameFactory.createShapeFill(shapeType, width, height, 0, -1);
        return FeatureUtility.sanitizeUri(getBufferedImageUrl(image));
    }

    protected String getBufferedImageUrl(BufferedImage image)
    {
        // Get the image persisted in VFS
        final IVirtualFileSystem vfs = projectVFSManager.getVirtualFileSystem(IMapObject.MAP_OBJECT_ICONS_DIR);
        String uri = ImageUtils.writeImageToCanonicalFile(vfs.getRootDir(), image).toString();
        return FeatureUtility.sanitizeUri(uri);
    }

    private record Registration(long fid, LabelRegistration labelRegistration)
    {
    }
}
