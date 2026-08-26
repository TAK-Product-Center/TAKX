package gov.takx.core.globe.tak;

import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.map.elevation.ElevationManager;
import com.atakmap.map.layer.feature.Feature.AltitudeMode;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.opengl.GLLabelManager;
import com.atakmap.map.opengl.GLLabelManager.HorizontalAlignment;
import com.atakmap.map.opengl.GLLabelManager.VerticalAlignment;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.render.Annotation;
import gov.tak.api.engine.map.IMapRendererEnums.DisplayOrigin;
import gov.tak.api.engine.map.IMapRendererEnums.InverseMode;
import gov.tak.api.engine.map.IMapRendererEnums.InverseResult;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.math.PointD;
import gov.tak.core.map.layer.IPickContext;
import gov.tak.core.map.layer.IPickedObjectProvider;
import gov.tak.core.map.layer.PickedObject;
import gov.tak.core.map.layer.PickedObjectList;
import gov.tak.platform.marshal.MarshalManager;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IGlobeAnnotationMapObject;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeFlatAnnotationMapObject;
import gov.takx.api.globe.IGlobeLifeCycleListener;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IGlobeMapObjectChangeListener;
import gov.takx.api.globe.IGlobeMapObjectManager;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.api.mapobject.IFlatAnnotationMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IShape;
import gov.takx.core.globe.ApplicationGlobe;
import gov.takx.core.globe.ApplicationGlobeManager;
import gov.takx.core.globe.tak.TakLabelManager.LabelRegistration;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.awt.Font;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A service that manages the {@link GLLabelManager} for rendering {@link IGlobeAnnotationMapObject} and {@link IGlobeFlatAnnotationMapObject}  objects as TAK labels.
 *
 * @since 5.0
 */
@ApplicationScoped
public class GlobeAnnotationMapObjectManager implements IGlobeLifeCycleListener, IGlobeMapObjectChangeListener, PropertyChangeListener, IPickedObjectProvider
{
    /* Copied from GlobeAnnotationMapObject */
    private static final List<String> ANNOTATION_PROPERTIES = List.of(
            IMapObject.LOCATION,
            IAnnotationMapObject.VISIBILITY,
            IAnnotationMapObject.ANNOTATION_TEXT, IAnnotationMapObject.ANNOTATION_TEXT_COLOR,
            IAnnotationMapObject.ANNOTATION_TEXT_ALIGN, IAnnotationMapObject.ANNOTATION_FONT,
            IAnnotationMapObject.ANNOTATION_BACKGROUND_COLOR, IAnnotationMapObject.ANNOTATION_OPACITY,
            IAnnotationMapObject.ANNOTATION_MIN_ACTIVE_ALTITUDE, IAnnotationMapObject.ANNOTATION_MAX_ACTIVE_ALTITUDE,
            IAnnotationMapObject.ANNOTATION_BORDER_COLOR, IAnnotationMapObject.ANNOTATION_BORDER_LEAD_ENABLE,
            IAnnotationMapObject.ANNOTATION_BORDER_SHAPE, IAnnotationMapObject.ANNOTATION_BORDER_RADIUS,
            IAnnotationMapObject.ANNOTATION_BORDER_THICKNESS, IAnnotationMapObject.ANNOTATION_ENABLE_FIXED_HEIGHT,
            IAnnotationMapObject.ANNOTATION_FIXED_HEIGHT, IAnnotationMapObject.ANNOTATION_DRAW_OFFSET_X,
            IAnnotationMapObject.ANNOTATION_DRAW_OFFSET_Y);
    /* Copied from IFlatAnnotationMapObject */
    private static final List<String> FLAT_ANNOTATION_PROPERTIES = List.of(
            IMapObject.LOCATION,
            IFlatAnnotationMapObject.VISIBILITY,
            IFlatAnnotationMapObject.FLAT_ANNOTATION_TEXT, IFlatAnnotationMapObject.FLAT_ANNOTATION_FONT, IFlatAnnotationMapObject.FLAT_ANNOTATION_FONT_NAME,
            IFlatAnnotationMapObject.FLAT_ANNOTATION_FONT_STYLE, IFlatAnnotationMapObject.FLAT_ANNOTATION_TEXT_COLOR, IFlatAnnotationMapObject.FLAT_ANNOTATION_BG_COLOR,
            IFlatAnnotationMapObject.FLAT_ANNOTATION_BG_FILL_ENABLED, IFlatAnnotationMapObject.FLAT_ANNOTATION_BORDER_COLOR, IFlatAnnotationMapObject.FLAT_ANNOTATION_ENABLE_BORDER,
            IFlatAnnotationMapObject.FLAT_ANNOTATION_OPACITY, IFlatAnnotationMapObject.FLAT_ANNOTATION_SIZE, IFlatAnnotationMapObject.FLAT_ANNOTATION_BORDER_THICKNESS,
            IFlatAnnotationMapObject.FLAT_ANNOTATION_ORIENTATION, IFlatAnnotationMapObject.FLAT_ANNOTATION_TEXT_ALIGNMENT);

    private IGlobeMapObjectManager globeMapObjectManager;
    private final boolean headless;
    private final ApplicationGlobeManager globeManager;
    private final TakLabelManager labelManager;
    private final ConcurrentHashMap<IGlobeMapObject, LabelRegistration> registrations = new ConcurrentHashMap<>();

    private final IPreferenceMonitor<Boolean> showShapeLabelsMonitor = UserPreferenceEntry.SHOW_SHAPE_LABELS.newBooleanMonitor(v -> forceRender(), false);
    private final IPreferenceMonitor<Boolean> showRangeLabelsMonitor = UserPreferenceEntry.SHOW_RANGE_BEARING_LABELS.newBooleanMonitor(v -> forceRender(), false);
    private final IPreferenceMonitor<NorthReference> northReferenceMonitor = UserPreferenceEntry.DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, v -> forceRender(), false);
    private final IPreferenceMonitor<BearingUnits> bearingUnitsMonitor = UserPreferenceEntry.DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, v -> forceRender(), false);
    private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, v -> forceRender(), false);
    private final IPreferenceMonitor<RangeUnits> rangeUnitsMonitor = UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, v -> forceRender(), false);
    private final IPreferenceMonitor<SpeedUnits> speedUnitsMonitor = UserPreferenceEntry.DISPLAY_SPEED_UNITS.newMonitor(SpeedUnits.class, v -> forceRender(), false);

    @Inject
    GlobeAnnotationMapObjectManager(ApplicationGlobeManager globeManager, TakLabelManager labelManager, @Headless boolean headless)
    {
        this.headless = headless;
        this.globeManager = globeManager;
        this.labelManager = labelManager;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_CreateTAKLayers) StartProjectServices event)
    {
        if (headless) return;

        ApplicationGlobe primaryGlobeDelegate = globeManager.getPrimaryGlobeDelegate();
        globeMapObjectManager = primaryGlobeDelegate.getGlobeMapObjectManager();
        primaryGlobeDelegate.addGlobeMapObjectChangeListener(this);
        globeManager.addGlobeLifeCycleListener(this);

        showShapeLabelsMonitor.start();
        northReferenceMonitor.start();
        bearingUnitsMonitor.start();
        altitudeUnitsMonitor.start();
        rangeUnitsMonitor.start();
        speedUnitsMonitor.start();
        showRangeLabelsMonitor.start();
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_TAKLayers) StopProjectServices event)
    {
        if (headless) return;

        final ApplicationGlobe globeDelegate = globeManager.getPrimaryGlobeDelegate();
        globeDelegate.removeGlobeMapObjectChangeListener(this);
        globeManager.removeGlobeLifeCycleListener(this);
        showShapeLabelsMonitor.stop();
        northReferenceMonitor.stop();
        bearingUnitsMonitor.stop();
        altitudeUnitsMonitor.stop();
        rangeUnitsMonitor.stop();
        speedUnitsMonitor.stop();
        showRangeLabelsMonitor.stop();

        registrations.clear();
    }

    @InvokeOnEDT
    private void register(IGlobeMapObject globeMapObject)
    {
        if (headless) return;
        if (!(globeMapObject instanceof IGlobeAnnotationMapObject || globeMapObject instanceof IGlobeFlatAnnotationMapObject)) return;

        if (!registrations.containsKey(globeMapObject))
        {
            // We start monitoring for property changes before we add the label to the label manager so that we capture any in-flight changes.
            // As long as calls to register() are synchronized, any property changes will be processed in order.
            startMonitoring(globeMapObject);
        }
        LabelRegistration registration = registrations.get(globeMapObject);
        if (registration == null)
        {
            registration = labelManager.createLabel((glLabelManager, labelId) -> {
                if (globeMapObject instanceof IGlobeAnnotationMapObject globeAnnotationMapObject)
                {
                    applyToLabel(glLabelManager, labelId, globeAnnotationMapObject);
                } else if (globeMapObject instanceof IGlobeFlatAnnotationMapObject globeFlatAnnotationMapObject)
                {
                    applyToLabel(glLabelManager, labelId, globeFlatAnnotationMapObject);
                }
            });
            registrations.put(globeMapObject, registration);
        } else
        {
            labelManager.updateLabel(registration);
        }
    }

    @InvokeOnEDT
    private void unregister(IGlobeMapObject globeMapObject)
    {
        LabelRegistration registration = registrations.remove(globeMapObject);
        if (registration != null)
        {
            stopMonitoring(globeMapObject);
            labelManager.deleteLabel(registration);
        }
    }

    private void applyToLabel(GLLabelManager labelManager, int labelId, IGlobeAnnotationMapObject globeMapObject)
    {
        var mapObject = (IAnnotationMapObject) globeMapObject.getMapObject();
        ILocation location = mapObject.getLocation();
        double hae = location.getElevation() + ElevationManager.getGeoidHeight(location.getLatitude(), location.getLongitude());
        Font font = mapObject.getFont();
        String text = mapObject.getText();
        String fontName = font.getName();
        int textOpacity = (int) ((mapObject.getTextColor().getRGB() >>> 24) * mapObject.getOpacity());
        int textColor = (0x00FFFFFF & mapObject.getTextColor().getRGB()) | (textOpacity << 24);
        int backgroundOpacity = (int) ((mapObject.getBackgroundColor().getRGB() >>> 24) * mapObject.getOpacity());
        int backgroundColor = (0x00FFFFFF & mapObject.getBackgroundColor().getRGB()) | (backgroundOpacity << 24);
        int size = font.getSize();
        float rotation = mapObject.hasHeading() ? (float) mapObject.getHeading() : 0;
        boolean absolute = mapObject.hasHeading();
        AltitudeMode altitudeMode = mapObject.getLocation().getElevation() == 0 ? AltitudeMode.ClampToGround : AltitudeMode.Absolute;

        double minActiveAltitude = Math.max(mapObject.getMinActiveAltitude(), IAnnotationMapObject.DEFAULT_MIN_ACTIVE_ALTITUDE);
        double maxActiveAltitude = Math.min(mapObject.getMaxActiveAltitude(), IAnnotationMapObject.DEFAULT_MAX_ACTIVE_ALTITUDE);
        FeatureUtility.applyAnnotationAltitudeAsLod(minActiveAltitude, maxActiveAltitude, (minLod, maxLod) -> labelManager.setLevelOfDetail(labelId, minLod, maxLod));

        labelManager.setText(labelId, text);
        labelManager.setGeometry(labelId, new Point(location.getLongitude(), location.getLatitude(), hae));
        labelManager.setTextFormat(labelId, fontName, size, font.isBold(), font.isItalic(), false, false);
        labelManager.setColor(labelId, textColor);
        labelManager.setBackgroundColor(labelId, backgroundColor);
        labelManager.setFill(labelId, backgroundOpacity > 0);  //TODO - tie to border shape
        labelManager.setAltitudeMode(labelId, altitudeMode);
        labelManager.setRotation(labelId, rotation, absolute);
        labelManager.setHitTestable(labelId, true);
        labelManager.setHorizontalAlignment(labelId, getHorizontalAlignment(mapObject));
        labelManager.setVerticalAlignment(labelId, VerticalAlignment.Top);
        labelManager.setVisible(labelId, mapObject.isVisible());
        labelManager.setPriority(labelId, GLLabelManager.Priority.High);
    }

    private void applyToLabel(GLLabelManager labelManager, int labelId, IGlobeFlatAnnotationMapObject globeMapObject)
    {
        var mapObject = (IFlatAnnotationMapObject) globeMapObject.getMapObject();
        Font font = mapObject.getFont();
        String fontName = font.getName();

        labelManager.setText(labelId, mapObject.getText());
        labelManager.setGeometry(labelId, new Point(mapObject.getLocation().getLongitude(), mapObject.getLocation().getLatitude()));
        labelManager.setTextFormat(labelId, fontName, 14, font.isBold(), font.isItalic(), false, false);
        labelManager.setColor(labelId, mapObject.getTextColor().getRGB());
        int bgFillColor = (0x00FFFFFF & mapObject.getBackgroundColor().getRGB()) | ((int) (mapObject.getOpacity() * 255) << 24);
        labelManager.setBackgroundColor(labelId, bgFillColor);
        labelManager.setFill(labelId, mapObject.isBackgroundFillEnabled());
        labelManager.setHints(labelId, labelManager.getHints(labelId) | GLLabelManager.HINT_SURFACE);
        labelManager.setHeightInMeters(labelId, mapObject.getSize());
        labelManager.setAltitudeMode(labelId, AltitudeMode.ClampToGround);
        labelManager.setRotation(labelId, (float) mapObject.getOrientation(), true);
        labelManager.setHitTestable(labelId, true);
        labelManager.setHorizontalAlignment(labelId, getHorizontalAlignment(mapObject));
        labelManager.setVerticalAlignment(labelId, VerticalAlignment.Middle);
        labelManager.setVisible(labelId, mapObject.isVisible());
    }

    private void startMonitoring(IGlobeMapObject globeMapObject)
    {
        if (globeMapObject instanceof IGlobeAnnotationMapObject)
        {
            globeMapObject.getMapObject().addPropertyChangeListener(ANNOTATION_PROPERTIES, this);
        } else if (globeMapObject instanceof IGlobeFlatAnnotationMapObject)
        {
            globeMapObject.getMapObject().addPropertyChangeListener(FLAT_ANNOTATION_PROPERTIES, this);
        }
    }

    private void stopMonitoring(IGlobeMapObject globeMapObject)
    {
        if (globeMapObject instanceof IGlobeAnnotationMapObject)
        {
            globeMapObject.getMapObject().removePropertyChangeListener(ANNOTATION_PROPERTIES, this);
        } else if (globeMapObject instanceof IGlobeFlatAnnotationMapObject)
        {
            globeMapObject.getMapObject().removePropertyChangeListener(FLAT_ANNOTATION_PROPERTIES, this);
        }
    }

    @Nonnull
    @Override
    public List<PickedObject> pick(@Nonnull IPickContext context)
    {
        PickedObjectList pickedObjectList = new PickedObjectList();
        MapRenderer<?> mapRenderer = context.globe().getPortableMapRenderer();
        GLLabelManager labelManager = context.globe().getGLMapView().getLabelManager();
        float size = Math.max(context.rectangle().width, context.rectangle().height);
        List<Integer> labelIdResults = new ArrayList<>();
        GeoPoint inverse = GeoPoint.createMutable();
        InverseResult inverseResult = mapRenderer.inverse(new PointD(context.rectangle().x, context.rectangle().y, 0), inverse, InverseMode.RayCast, 0, DisplayOrigin.UpperLeft);

        MapSceneModel mapSceneModel = mapRenderer.getMapSceneModel(false, DisplayOrigin.UpperLeft);
        // GLLabelManager requires flip to lower-left
        int y = mapSceneModel.height - context.rectangle().y;
        if (inverseResult != InverseResult.None) labelManager.hitTest(context.rectangle().x, y, size, MarshalManager.marshal(inverse, GeoPoint.class, com.atakmap.coremap.maps.coords.GeoPoint.class), 128, labelIdResults);
        for (int labelId : labelIdResults)
        {
            Object pickObject = getPickObject(labelId, mapRenderer);
            if (pickObject != null)
            {
                pickedObjectList.add(new PickedObject(context.location(), pickObject));
            }
        }
        return pickedObjectList;
    }

    private Object getPickObject(Integer labelId, MapRenderer<?> mapRenderer)
    {
        IGlobeMapObject globeMapObject = registrations.entrySet().stream()
                .filter(e -> Objects.equals(e.getValue().labelIdPerGlobe().get(mapRenderer), labelId))
                .map(Map.Entry::getKey)
                .findFirst().orElse(null);
        boolean isPickDisabled = globeMapObject instanceof Annotation annotation && !annotation.isPickEnabled();
        return globeMapObject != null && !isPickDisabled ? globeMapObject : null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        IGlobeMapObject globeMapObject = null;
        if (evt.getSource() instanceof IGlobeMapObject aGlobeMapObject)
        {
            globeMapObject = aGlobeMapObject;
        } else if (evt.getSource() instanceof IMapObject mapObject)
        {
            globeMapObject = globeMapObjectManager.getGlobeMapObject(mapObject.getKey());
        } else if (evt.getSource() instanceof IShape shape)
        {
            globeMapObject = globeMapObjectManager.getGlobeMapObject(shape.getKey());
        }
        if (globeMapObject != null)
        {
            register(globeMapObject);
        }
    }

    /**
     * A global setting has changed that affects feature rendering, so re-render all registered objects.
     */
    @InvokeOnEDT
    private void forceRender()
    {
        registrations.keySet().forEach(this::register);
    }

    @Override
    public void globeCreated(IGlobeDelegate globe)
    {
        forceRender();
    }

    @Override
    public void globeMapObjectChanged(IGlobeMapObject globeMapObject, MapObjectChangeEvent mapObjectChangeEvent)
    {
        if (headless || !(globeMapObject instanceof IGlobeAnnotationMapObject || globeMapObject instanceof IGlobeFlatAnnotationMapObject)) return;

        if (mapObjectChangeEvent == MapObjectChangeEvent.ADDED) register(globeMapObject);
        if (mapObjectChangeEvent == MapObjectChangeEvent.REMOVED) unregister(globeMapObject);
    }

    private HorizontalAlignment getHorizontalAlignment(IAnnotationMapObject mapObject)
    {
        return switch (mapObject.getTextAlign())
        {
            case 0 -> HorizontalAlignment.Left;
            case 2 -> HorizontalAlignment.Right;
            default -> HorizontalAlignment.Center;
        };
    }

    private HorizontalAlignment getHorizontalAlignment(IFlatAnnotationMapObject mapObject)
    {
        return switch (mapObject.getTextAlignment())
        {
            case AVKey.LEFT -> HorizontalAlignment.Left;
            case AVKey.RIGHT -> HorizontalAlignment.Right;
            default -> HorizontalAlignment.Center;
        };
    }
}
