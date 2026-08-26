package gov.takx.core.globe.tak;

import android.net.Uri;
import com.atakmap.android.maps.graphics.GLBitmapLoader;
import com.atakmap.android.maps.graphics.GLBitmapLoader.QueueType;
import com.atakmap.android.maps.graphics.GLImage;
import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.MapRenderer2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.opengl.GLES20FixedPipeline;
import com.atakmap.opengl.GLNinePatch;
import com.jogamp.opengl.util.texture.Texture;
import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.cache.GpuResourceCache;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.BasicWWTexture;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.ScreenAnnotation;
import gov.tak.api.commons.graphics.Font;
import gov.tak.api.commons.graphics.TextFormat;
import gov.tak.api.engine.map.MapSceneModel;
import gov.tak.api.widgets.opengl.IGLWidget;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.widgets.TextWidget;
import gov.tak.platform.widgets.opengl.GLTextWidget;
import gov.tak.platform.widgets.opengl.GLWidget;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeLifeCycleListener;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * A service that manages a set of {@link ScreenAnnotation} for rendering.
 *
 * @since 3.5
 */
@ApplicationScoped
public class ScreenAnnotationManager implements IGlobeLifeCycleListener
{
    private final IExtendedGlobeManager globeManager;

    // A per-globe collection of registrations
    private final IdentityHashMap<IApplicationGlobe, RegistrationCollection> registrationCollections = new IdentityHashMap<>();
    // A per-globe set of screen annotations for the next frame
    private final IdentityHashMap<IApplicationGlobe, Set<ScreenAnnotation>> queuedScreenAnnotations = new IdentityHashMap<>();

    @Inject
    ScreenAnnotationManager(IExtendedGlobeManager globeManager)
    {
        this.globeManager = globeManager;
        this.globeManager.addGlobeLifeCycleListener(this);
        // Install our custom URL handler
        GLBitmapLoader.registerBitmapLoader("ww", new WWResourceBitmapLoaderSpi(), QueueType.ICON);
    }

    /**
     * Offer a {@link Renderable} to be rendered using TAK features. If the renderable is accepted this method will return true
     * and render its representation during the next render cycle. If the renderable is not offered during the next render cycle
     * then the renderable representation will no longer be drawn.
     * @param renderable the renderable to be offered
     * @param globe the globe
     * @return true if the renderable is accepted
     * @since 3.5
     */
    public boolean offerRenderable(Renderable renderable, @Nonnull IApplicationGlobe globe)
    {
        if (renderable instanceof ScreenAnnotation screenAnnotation)
        {
            addScreenAnnotation(screenAnnotation, globe);
            return true;
        }
        return false;
    }

    /**
     * Add a {@link ScreenAnnotation} to be rendered.
     *
     * @param screenAnnotation the screen annotation
     * @param globe the globe
     */
    private void addScreenAnnotation(ScreenAnnotation screenAnnotation, IApplicationGlobe globe)
    {
        synchronized (queuedScreenAnnotations)
        {
            Set<ScreenAnnotation> references = queuedScreenAnnotations.computeIfAbsent(globe, g -> Collections.newSetFromMap(new IdentityHashMap<>()));
            references.add(screenAnnotation);
        }
    }

    /**
     * Render queued screen annotations.
     *
     * @param glMapView the map view
     * @param dc        the draw context
     */
    public void render(GLMapView glMapView, DrawContext dc)
    {
        IApplicationGlobe globe = (IApplicationGlobe) dc.getValue(ApplicationSceneController.GLOBE_DELEGATE);
        if (globe == null) return;
        RegistrationCollection registrationCollection = registrationCollections.computeIfAbsent(globe, r -> new RegistrationCollection(new IdentityHashMap<>()));
        synchronized (queuedScreenAnnotations)
        {
            var registrations = registrationCollection.registrations;
            // Unregister any of the registered screen annotations that have changed
            List<ScreenAnnotation> changedScreenAnnotations = registrations.keySet().stream()
                    .filter(si -> registrations.get(si).siHashCode != hashScreenAnnotation(si))
                    .toList();
            changedScreenAnnotations.forEach(si -> unregisterScreenAnnotation(si, registrationCollection, dc));

            // Register queued screen annotations
            Set<ScreenAnnotation> globeScreenAnnotation = queuedScreenAnnotations.computeIfAbsent(globe, g -> Collections.newSetFromMap(new IdentityHashMap<>()));
            globeScreenAnnotation.forEach(si -> registerScreenAnnotation(si, registrationCollection, glMapView, dc));

            // Unregister any removed screen annotations
            List<ScreenAnnotation> removedScreenAnnotations = registrations.keySet().stream()
                    .filter(si -> !globeScreenAnnotation.contains(si))
                    .toList();
            removedScreenAnnotations.forEach(si -> unregisterScreenAnnotation(si, registrationCollection, dc));
            globeScreenAnnotation.clear();
        }

        renderRegisteredScreenAnnotations(glMapView, dc, registrationCollection.registrations, 0);
    }

    @SuppressWarnings("deprecation")
    private void renderRegisteredScreenAnnotations(GLMapView glMapView, DrawContext dc, Map<ScreenAnnotation, Registration> registrations, int pickColor)
    {
        GLES20FixedPipeline.glMatrixMode(GLES20FixedPipeline.GL_PROJECTION);
        GLES20FixedPipeline.glPushMatrix();
        GLES20FixedPipeline.glOrthof(glMapView.currentPass.left, glMapView.currentPass.right, glMapView.currentPass.bottom, glMapView.currentPass.top, (float) glMapView.currentPass.scene.camera.near, (float) glMapView.currentPass.scene.camera.far);
        GLES20FixedPipeline.glMatrixMode(GLES20FixedPipeline.GL_MODELVIEW);
        GLES20FixedPipeline.glPushMatrix();
        try
        {
            boolean isPicking = dc.isPickingMode();
            GpuResourceCache textureCache = dc.getTextureCache();
            registrations.forEach((annotation, registration) -> {
                GLES20FixedPipeline.glLoadIdentity();
                GLES20FixedPipeline.glTranslatef((float) annotation.getScreenPoint().getX(), (float) annotation.getScreenPoint().getY(), 0);

                AnnotationAttributes attributes = annotation.getAttributes();
                int offsetx = attributes.getDrawOffset().x;
                int offsety = attributes.getDrawOffset().y;
                double scale = attributes.getScale() * (attributes.isHighlighted() ? attributes.getHighlightScale() : 1);
                GLES20FixedPipeline.glScalef((float) scale, (float) scale, 1f);
                GLES20FixedPipeline.glTranslatef(offsetx, offsety, 0f);
                Dimension preferredSize = annotation.getPreferredSize(dc);
                GLES20FixedPipeline.glTranslatef((-preferredSize.width / 2f), 0f, 0f);

                String text = annotation.getText();
                boolean hasFrame = attributes.getFrameShape() != null && !attributes.getFrameShape().equals(AVKey.SHAPE_NONE);
                int textOpacity = (int) ((attributes.getTextColor().getRGB() >>> 24) * attributes.getOpacity());
                int textColorArgb = (0x00FFFFFF & attributes.getTextColor().getRGB()) | (textOpacity << 24);
                int textColor = isPicking ? pickColor : textColorArgb;
                int backgroundOpacity = (int) ((attributes.getBackgroundColor().getRGB() >>> 24) * attributes.getOpacity());
                int backgroundColorArgb = (0x00FFFFFF & attributes.getBackgroundColor().getRGB()) | (backgroundOpacity << 24);
                int backgroundColor = isPicking ? pickColor : (hasFrame ? backgroundColorArgb : 0);
                if (registration.imageSource != null)
                {
                    if (isPicking)
                    {
                        GLES20FixedPipeline.glColor4f(
                                gov.tak.platform.graphics.Color.red(backgroundColor) / 255f,
                                gov.tak.platform.graphics.Color.green(backgroundColor) / 255f,
                                gov.tak.platform.graphics.Color.blue(backgroundColor) / 255f,
                                gov.tak.platform.graphics.Color.alpha(backgroundColor) / 255f);
                        registration.glNinePatch.draw(registration.glImage.getWidth(), registration.glImage.getHeight());
                    } else
                    {
                        double imageOpacity = attributes.getImageOpacity();
                        Texture texture = textureCache.getTexture(registration.imageSource);
                        if (texture != null)
                        {
                            int texId = texture.getTextureObject();
                            registration.glImage.setTexId(texId);
                            registration.glImage.draw(1, 1, 1, (float) imageOpacity);
                        }
                    }
                }
                if (!StringUtils.isEmpty(text))
                {
                    TextWidget textWidget = (TextWidget) registration.glTextWidget.getSubject();
                    if (textWidget.getColor() != textColor)
                    {
                        textWidget.setColor(textColor);
                        registration.glTextWidget.onTextWidgetColorChanged(textWidget);
                    }
                    if (textWidget.getBackground() != backgroundColor)
                    {
                        textWidget.setBackground(backgroundColor);
                        registration.glTextWidget.onTextWidgetHasBackgroundChanged(textWidget);
                    }
                    registration.glTextWidget.drawWidgetContent(drawStateFromFixedPipeline(glMapView));
                }
            });
        } finally
        {
            GLES20FixedPipeline.glMatrixMode(GLES20FixedPipeline.GL_PROJECTION);
            GLES20FixedPipeline.glPopMatrix();
            GLES20FixedPipeline.glMatrixMode(GLES20FixedPipeline.GL_MODELVIEW);
            GLES20FixedPipeline.glPopMatrix();

            // Clear out any shader in use by TAK so WW can use fixed-function pipeline
            dc.getGL().getGL2().glUseProgram(0);
        }
    }

    public void pick(DrawContext dc, GLMapView glMapView, Point pickPoint)
    {
        if (pickPoint == null) return;

        IApplicationGlobe globe = (IApplicationGlobe) dc.getValue(ApplicationSceneController.GLOBE_DELEGATE);
        if (globe == null) return;
        RegistrationCollection registrationCollection = registrationCollections.computeIfAbsent(globe, r -> new RegistrationCollection(new IdentityHashMap<>()));

        PickSupport pickSupport = new PickSupport();
        try
        {
            for (Map.Entry<ScreenAnnotation, Registration> entry : registrationCollection.registrations.entrySet())
            {
                ScreenAnnotation annotation = entry.getKey();
                Registration registration = entry.getValue();
                Layer layer = (Layer) annotation.getValue(AVKey.LAYER);
                boolean isLayerNotPickable = layer != null && !layer.isPickEnabled();
                if (isLayerNotPickable || !annotation.isPickEnabled()) continue;

                pickSupport.clearPickList();
                pickSupport.beginPicking(dc);
                Color pickColor = dc.getUniquePickColor();
                try
                {
                    pickSupport.addPickableObject(PickUtils.createPickedObject(dc, annotation, pickColor));
                    renderRegisteredScreenAnnotations(glMapView, dc, Collections.singletonMap(annotation, registration), 0xFF000000 | pickColor.getRGB());
                } finally
                {
                    Point point = PickUtils.getTranslatedPoint(dc, glMapView, pickPoint);
                    pickSupport.endPicking(dc);
                    pickSupport.resolvePick(dc, point, layer);
                }
            }
        } finally
        {
            // Clear out any shader in use by TAK so WW can use fixed-function pipeline
            dc.getGL().getGL2().glUseProgram(0);
        }
    }

    private void registerScreenAnnotation(ScreenAnnotation screenAnnotation, RegistrationCollection registrationCollection, GLMapView glMapView, DrawContext dc)
    {
        registrationCollection.registrations.computeIfAbsent(screenAnnotation, si -> create(si, glMapView, dc));
    }

    private Registration create(ScreenAnnotation annotation, GLMapView glMapView, DrawContext dc)
    {
        String text = annotation.getText();
        AnnotationAttributes attributes = annotation.getAttributes();
        int textOpacity = (int) ((attributes.getTextColor().getRGB() >>> 24) * attributes.getOpacity());
        int textColor = (0x00FFFFFF & attributes.getTextColor().getRGB()) | (textOpacity << 24);
        boolean hasFrame = attributes.getFrameShape() != null && !attributes.getFrameShape().equals(AVKey.SHAPE_NONE);
        int backgroundOpacity = (int) ((attributes.getBackgroundColor().getRGB() >>> 24) * attributes.getOpacity());
        int backgroundColor = hasFrame ? (0x00FFFFFF & attributes.getBackgroundColor().getRGB()) | (backgroundOpacity << 24) : 0;
        int fontSize = attributes.getFont().getSize();
        String fontFamily = attributes.getFont().getFamily();
        int fontStyle = attributes.getFont().getStyle();
        Object imageSource = attributes.getImageSource();

        TextWidget textWidget = new TextWidget(text);
        textWidget.setWidgetTextFormat(new TextFormat(new Font(fontFamily, fromAwtStyle(fontStyle), fontSize), 0));
        textWidget.setBackground(backgroundColor);
        textWidget.setColor(textColor);
        GLTextWidget glTextWidget = new GLTextWidget(textWidget, LegacyAdapters.adapt(glMapView));

        GLImage image = null;
        GLNinePatch glNinePatch = null;
        if (imageSource != null)
        {
            final BasicWWTexture wwTexture = new BasicWWTexture(imageSource);

            if (wwTexture.bind(dc))
            {
                Texture texture = dc.getTextureCache().getTexture(imageSource);
                if (texture != null)
                {
                    final int width = texture.getWidth();
                    final int height = texture.getHeight();

                    // Apply a y-axis flip
                    image = new GLImage(
                            glMapView.getRenderContext(),
                            texture.getTextureObject(),
                            width, height, 0, height,
                            width - 1.0f, -(height - 1.0f),
                            0, 0, width, height);
                    glNinePatch = new GLNinePatch(width, height);
                }
            }
        }

        return new Registration(glTextWidget, image, imageSource, glNinePatch, hashScreenAnnotation(annotation));
    }

    /**
     * Custom hash function for a {@link ScreenAnnotation}.
     *
     * @param annotation The annotation to hash
     * @return the hash
     * @since 3.5.0
     */
    private int hashScreenAnnotation(ScreenAnnotation annotation)
    {
        AnnotationAttributes attributes = annotation.getAttributes();
        return Objects.hash(annotation.getPosition(), annotation.getScreenPoint(),
                annotation.getText(), annotation.isAlwaysOnTop(),
                attributes.getFrameShape(), attributes.getSize(), attributes.getScale(), attributes.getOpacity(), attributes.getLeader(),
                attributes.getLeaderGapWidth(), attributes.getCornerRadius(), attributes.getAdjustWidthToText(), attributes.getDrawOffset(),
                attributes.getHighlightScale(), attributes.getInsets(), attributes.getFont(), attributes.getTextAlign(), attributes.getTextColor(),
                attributes.getBackgroundColor(), attributes.getBorderColor(), attributes.getBorderWidth(), attributes.getBorderStippleFactor(),
                attributes.getBorderStipplePattern(), attributes.getImageScale(), attributes.getImageOffset(), attributes.getImageOpacity(),
                attributes.getImageRepeat(), attributes.getDistanceMinScale(), attributes.getDistanceMaxScale(), attributes.getDistanceMinOpacity(),
                attributes.getEffect());
    }

    private void destroy(Registration registration)
    {
        registration.glTextWidget.releaseWidget();
    }

    private void unregisterScreenAnnotation(ScreenAnnotation screenAnnotation, RegistrationCollection registrationCollection, DrawContext dc)
    {
        Registration registration = registrationCollection.registrations.remove(screenAnnotation);
        if (registration != null)
        {
            dc.getTextureCache().remove(registration.imageSource);
            destroy(registration);
        }
    }

    @Override
    public void globeRemoved(IGlobeDelegate globe)
    {
        synchronized (queuedScreenAnnotations)
        {
            queuedScreenAnnotations.remove((IApplicationGlobe) globe);
        }
        RegistrationCollection registrationCollection = registrationCollections.remove((IApplicationGlobe) globe);
        // Ideally destroy() would be called from the GL thread
        if (registrationCollection != null) registrationCollection.registrations.values().forEach(this::destroy);
    }

    private record RegistrationCollection(IdentityHashMap<ScreenAnnotation, Registration> registrations)
    {
    }

    private record Registration(GLTextWidget glTextWidget, GLImage glImage, Object imageSource, GLNinePatch glNinePatch, long siHashCode)
    {
    }

    private static final class WWResourceBitmapLoaderSpi extends GLBitmapLoader.AbstractInputStreamLoaderSpi
    {
        private WWResourceBitmapLoaderSpi()
        {
            super(0);
        }

        @Override
        protected InputStream openStream(String uriString) throws Exception
        {
            Uri uri = Uri.parse(uriString);
            URL resource = WorldWind.class.getResource(uri.getPath());
            return resource != null ? resource.openStream() : null;
        }
    }

    private static Font.Style fromAwtStyle(int style)
    {
        return switch (style)
        {
            case java.awt.Font.BOLD -> Font.Style.Bold;
            case java.awt.Font.ITALIC -> Font.Style.Italic;
            case java.awt.Font.BOLD | java.awt.Font.ITALIC -> Font.Style.BoldItalic;
            default -> Font.Style.Normal;
        };
    }

    @SuppressWarnings("deprecation")
    private static IGLWidget.DrawState drawStateFromFixedPipeline(MapRenderer2 mapSceneRenderer)
    {
        MapSceneModel sceneModel = MarshalManager.marshal(mapSceneRenderer.getMapSceneModel(true, mapSceneRenderer.getDisplayOrigin()),
                com.atakmap.map.MapSceneModel.class, MapSceneModel.class);
        GLWidget.DrawState drawState = new GLWidget.DrawState(sceneModel);
        drawState.projectionMatrix = new float[16];
        drawState.modelMatrix = new float[16];
        GLES20FixedPipeline.glGetFloatv(GLES20FixedPipeline.GL_PROJECTION, drawState.projectionMatrix, 0);
        GLES20FixedPipeline.glGetFloatv(GLES20FixedPipeline.GL_MODELVIEW, drawState.modelMatrix, 0);
        return drawState;
    }
}
