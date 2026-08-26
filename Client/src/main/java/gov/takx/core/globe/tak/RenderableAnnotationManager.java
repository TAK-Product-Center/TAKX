package gov.takx.core.globe.tak;

import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.Point;
import com.atakmap.map.opengl.GLLabelManager;
import com.atakmap.map.opengl.GLLabelManager.HorizontalAlignment;
import com.atakmap.map.opengl.GLLabelManager.VerticalAlignment;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.render.Annotation;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.GlobeAnnotation;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ScreenAnnotation;
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
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeLifeCycleListener;
import gov.takx.api.mapobject.IAnnotationMapObject;
import gov.takx.core.globe.RotatableGlobeAnnotation;
import gov.takx.core.globe.tak.layer.RenderableAnnotationManagerLayer;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.platform.globe.GeometryAnnotation;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.awt.Font;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * A service that manages the {@link GLLabelManager} for rendering {@link Annotation}s as TAK labels. It is designed to collect annotations during the WW draw cycle via
 * {@link #offerRenderable(Renderable, IApplicationGlobe)}, and then service them as a collective set during the call to {@link #render(IApplicationGlobe)}.
 *
 * @since 5.1
 */
@ApplicationScoped
public class RenderableAnnotationManager implements IGlobeLifeCycleListener, IPickedObjectProvider
{
    private final RenderableFeatureManager renderableFeatureManager;
    private final RenderableAnnotationManager renderableAnnotationManager;
    private final boolean headless;
    private final IExtendedGlobeManager globeManager;
    private final ConcurrentHashMap<Annotation, Registration> registrations = new ConcurrentHashMap<>();

    private final ArrayList<Annotation> queuedAnnotations = new ArrayList<>();
    private RenderableAnnotationManagerLayer layer;

    @Inject
    RenderableAnnotationManager(IExtendedGlobeManager globeManager, RenderableFeatureManager renderableFeatureManager,
                                RenderableAnnotationManager renderableAnnotationManager, @Headless boolean headless)
    {
        this.renderableFeatureManager = renderableFeatureManager;
        this.renderableAnnotationManager = renderableAnnotationManager;
        this.headless = headless;
        this.globeManager = globeManager;
    }

    private void startService(@Observes @Priority(ServiceOrdering.START_CreateTAKLayers) StartProjectServices event)
    {
        globeManager.addGlobeLifeCycleListener(this);
        layer = new RenderableAnnotationManagerLayer(renderableAnnotationManager);
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_TAKLayers) StopProjectServices event)
    {
        globeManager.removeGlobeLifeCycleListener(this);
        queuedAnnotations.clear();
        registrations.clear();
    }

    /**
     * Offer a {@link Renderable} to be rendered using TAK GLLabelManager. If the renderable is accepted this method will return true
     * and render its representation during the next render cycle. If the renderable is not offered during the next render cycle
     * then the renderable representation will no longer be drawn.
     *
     * @param renderable the renderable to be offered
     * @param globe the globe
     * @return true if the renderable is accepted
     */
    public boolean offerRenderable(Renderable renderable, @Nonnull IApplicationGlobe globe)
    {
        // We work in tandem with the AnnotationFeatureProvider which provides all non-text rendering
        if (renderable instanceof Annotation annotation && renderableFeatureManager.offerRenderable(renderable, globe))
        {
            queuedAnnotations.add(annotation);
            return true;
        }
        return false;
    }

    public void render(@Nonnull IApplicationGlobe globe)
    {
        Set<Annotation> supportedAnnotations = new HashSet<>(queuedAnnotations);
        List<Annotation> removedAnnotations;
        synchronized (registrations)
        {
            removedAnnotations = registrations.keySet().stream()
                    .filter(l -> !supportedAnnotations.contains(l))
                    .toList();
        }
        queuedAnnotations.clear();

        register(supportedAnnotations, globe);
        unregister(removedAnnotations, globe);
    }

    private void register(Iterable<Annotation> annotations, IApplicationGlobe globe)
    {
        if (headless) return;
        synchronized (registrations)
        {
            annotations.forEach(annotation -> {
                Registration registration = registrations.computeIfAbsent(annotation, r -> new Registration(new HashMap<>(), new HashSet<>(), null));
                registration.references.add(globe);
                AnnotationState state = getAnnotationState(annotation);
                visitLabelManager(globe, (mapRenderer, labelManager) -> {
                    int labelId = registration.labelIdPerGlobe.computeIfAbsent(mapRenderer, mr -> labelManager.addLabel());
                    if (!state.equals(registration.annotationState))
                    {
                        registrations.put(annotation, new Registration(registration.labelIdPerGlobe, registration.references, state));
                        applyToLabel(labelManager, labelId, state);
                    }
                });
            });
        }
    }

    private void unregister(Iterable<Annotation> annotations, IApplicationGlobe globe)
    {
        synchronized (registrations)
        {
            annotations.forEach(annotation -> {
                Registration registration = registrations.get(annotation);
                if (registration != null)
                {
                    boolean removed = registration.references.remove(globe);
                    if (removed)
                    {
                        visitLabelManager(globe, (mapRenderer, labelManager) -> {
                            Integer labelId = registration.labelIdPerGlobe.remove(mapRenderer);
                            if (labelId != null) labelManager.removeLabel(labelId);
                        });
                    }
                    if (registration.references.isEmpty())
                    {
                        registrations.remove(annotation);
                    }
                }
            });
        }
    }

    private void applyToLabel(GLLabelManager labelManager, int labelId, AnnotationState state)
    {
        double minActiveAltitude = Math.max(state.minActiveAltitude, IAnnotationMapObject.DEFAULT_MIN_ACTIVE_ALTITUDE);
        double maxActiveAltitude = Math.min(state.maxActiveAltitude, IAnnotationMapObject.DEFAULT_MAX_ACTIVE_ALTITUDE);
        FeatureUtility.applyAnnotationAltitudeAsLod(minActiveAltitude, maxActiveAltitude, (minLod, maxLod) -> labelManager.setLevelOfDetail(labelId, minLod, maxLod));
        Geometry geometry = null;
        if (state.geometry != null)
        {
            geometry = state.geometry;
        } else if (state.position != null)
        {
            geometry = new Point(state.position.longitude.degrees, state.position.latitude.degrees, state.position.elevation);
        }

        labelManager.setText(labelId, state.text);
        if (geometry != null) labelManager.setGeometry(labelId, geometry);
        labelManager.setTextFormat(labelId, state.fontName, state.textSize, state.isBold, state.isItalic, false, false);
        labelManager.setColor(labelId, state.textColor);
        labelManager.setBackgroundColor(labelId, state.backgroundColor);
        labelManager.setFill(labelId, state.isFillEnabled);
        labelManager.setAltitudeMode(labelId, state.altitudeMode);
        labelManager.setRotation(labelId, state.rotation, state.absolute);
        labelManager.setHitTestable(labelId, true);
        labelManager.setHorizontalAlignment(labelId, state.horizontalAlignment);
        labelManager.setVerticalAlignment(labelId, state.verticalAlignment);
        labelManager.setDesiredOffset(labelId, state.offsetX, state.offsetY, 0);
    }

    @Nonnull
    @Override
    public List<PickedObject> pick(@Nonnull IPickContext context)
    {
        PickedObjectList pickedObjectList = new PickedObjectList();
        MapRenderer<?> mapRenderer = context.globe().getPortableMapRenderer();
        GLLabelManager labelManager = context.globe().getGLMapView().getLabelManager();
        float size = Math.max(context.rectangle().width, context.rectangle().height);
        ArrayList<Integer> labelIdResults = new ArrayList<>();
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
        synchronized (registrations)
        {
            var annotation = registrations.entrySet().stream()
                    .filter(e -> Objects.equals(e.getValue().labelIdPerGlobe.get(mapRenderer), labelId))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(null);
            boolean isPickDisabled = (annotation != null && !annotation.isPickEnabled()) ||
                    annotation instanceof AVList avList && avList.getValue(AVKey.LAYER) instanceof Layer annotationLayer && !annotationLayer.isPickEnabled();
            return annotation != null && !isPickDisabled ? annotation : null;
        }
    }

    private VerticalAlignment getVerticalAlignment(Annotation annotation)
    {
        // For Arrows, we want the label to be directly on the line instead of above it. We can detect this since Arrows use a special RotatableGlobeAnnotation.
        return annotation instanceof RotatableGlobeAnnotation ? VerticalAlignment.Middle : VerticalAlignment.Top;
    }

    @Override
    public void globeRemoved(IGlobeDelegate globe)
    {
        synchronized (registrations)
        {
            unregister(registrations.keySet(), (IApplicationGlobe) globe);
        }
    }

    public RenderableAnnotationManagerLayer getLayer()
    {
        return layer;
    }

    private void visitLabelManager(IApplicationGlobe globe, BiConsumer<MapRenderer<?>, GLLabelManager> labelManagerConsumer)
    {
        GLLabelManager labelManager = globe.getGLMapView().getLabelManager();
        labelManagerConsumer.accept(globe.getPortableMapRenderer(), labelManager);
    }

    private static boolean hasFrame(AnnotationAttributes attributes)
    {
        return attributes.getFrameShape() != null && !attributes.getFrameShape().equals(AVKey.SHAPE_NONE);
    }

    /**
     * Gets a {@link Position} for the provided {@link Annotation} if it is supported by the {@link RenderableAnnotationManager}.
     * @param annotation the annotation
     * @return the position if supported, or null
     */
    @Nullable
    public static Position getPosition(@Nonnull Annotation annotation)
    {
        Position position = null;
        if (annotation instanceof ScreenAnnotation screenAnnotation)
        {
            position = screenAnnotation.getPosition();
            if (position == null) position = (Position) screenAnnotation.getValue(AVKey.POSITION);
        } else if (annotation instanceof GlobeAnnotation globeAnnotation)
        {
            position = globeAnnotation.getPosition();
        } else if (annotation instanceof GeometryAnnotation geometryAnnotation)
        {
            position = geometryAnnotation.getCenterPosition();
        }
        return position;
    }

    /**
     * Gets a {@link Geometry} for the provided {@link Annotation} if it is supported by the {@link RenderableAnnotationManager}.
     * @param annotation the annotation
     * @return the geometry if supported, or null
     */
    @Nullable
    private Geometry getGeometry(@Nonnull Annotation annotation)
    {
        return annotation instanceof GeometryAnnotation geometryAnnotation ? geometryAnnotation.getGeometry() : null;
    }

    private AnnotationState getAnnotationState(Annotation annotation)
    {
        Geometry geometry = getGeometry(annotation);
        Position position = getPosition(annotation);

        final AnnotationAttributes attributes = annotation.getAttributes();
        Font font = attributes.getFont();
        int width = attributes.getSize().width;
        int height = attributes.getSize().height;
        int offsetX = attributes.getDrawOffset().x;
        int offsetY = attributes.getDrawOffset().y;
        boolean hasFrame = hasFrame(attributes);
        boolean hasPredeterminedSize = width > 0 && height > 0;
        String text = annotation.getText();
        String fontName = font.getName();
        int textOpacity = (int) ((attributes.getTextColor().getRGB() >>> 24) * attributes.getOpacity());
        int textColor = (0x00FFFFFF & attributes.getTextColor().getRGB()) | (textOpacity << 24);
        int backgroundOpacity = (int) ((attributes.getBackgroundColor().getRGB() >>> 24) * attributes.getOpacity());
        int backgroundColor = (0x00FFFFFF & attributes.getBackgroundColor().getRGB()) | (backgroundOpacity << 24);
        boolean isFillEnabled = hasFrame && !hasPredeterminedSize;
        boolean highlighted = attributes.isHighlighted();
        float labelScale = (float) (highlighted ? attributes.getHighlightScale() : attributes.getScale());
        float textSize = labelScale * font.getSize();

        float rotation = annotation instanceof RotatableGlobeAnnotation rotatableAnnotation
                ? (float) rotatableAnnotation.getTextRotationAngle()
                : 0;
        boolean absolute = annotation instanceof RotatableGlobeAnnotation;

        double elevation = 0;
        if (geometry != null)
        {
            Envelope envelope = geometry.getEnvelope();
            elevation = envelope.maxZ;
        } else if (position != null)
        {
            elevation = position.elevation;
        }
        Feature.AltitudeMode altitudeMode = elevation == 0 ? Feature.AltitudeMode.ClampToGround : Feature.AltitudeMode.Absolute;
        double minActiveAltitude = annotation.getMinActiveAltitude();
        double maxActiveAltitude = annotation.getMaxActiveAltitude();
        HorizontalAlignment horizontalAlignment = HorizontalAlignment.Center;
        VerticalAlignment verticalAlignment = getVerticalAlignment(annotation);

        return new AnnotationState(geometry, position, text, fontName, textSize, font.isBold(), font.isItalic(), textColor, backgroundColor,
                isFillEnabled, altitudeMode, rotation, absolute, horizontalAlignment, verticalAlignment, offsetX, offsetY, minActiveAltitude, maxActiveAltitude);
    }

    private record AnnotationState(@Nullable Geometry geometry, @Nullable Position position, String text,
            String fontName, float textSize, boolean isBold, boolean isItalic,
            int textColor, int backgroundColor, boolean isFillEnabled, Feature.AltitudeMode altitudeMode, float rotation, boolean absolute,
            HorizontalAlignment horizontalAlignment, VerticalAlignment verticalAlignment, double offsetX, double offsetY,
            double minActiveAltitude, double maxActiveAltitude)
    {
    }

    private record Registration(HashMap<MapRenderer<?>, Integer> labelIdPerGlobe, HashSet<IApplicationGlobe> references, AnnotationState annotationState)
    {
    }
}
