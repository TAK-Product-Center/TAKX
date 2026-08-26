package gov.takx.core.globe.tak;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.control.SurfaceRendererControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.feature.geometry.opengl.GLBatchPolygon;
import com.atakmap.map.layer.feature.style.BasicFillStyle;
import com.atakmap.map.layer.raster.DatasetProjection2;
import com.atakmap.map.layer.raster.DefaultDatasetProjection2;
import com.atakmap.map.layer.raster.tilereader.opengl.GLTileMesh;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.projection.EquirectangularMapProjection;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureIO;
import com.jogamp.opengl.util.texture.awt.AWTTextureIO;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.SurfaceImage;
import gov.tak.api.engine.map.RenderContext;
import gov.takx.api.event.tak.GLEventDisposed;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeLifeCycleListener;
import gov.takx.core.globe.tak.ww.ApplicationSceneController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A service that manages a set of SurfaceImages for rendering.
 *
 * @since 3.4
 */
@ApplicationScoped
public class SurfaceImageManager implements IGlobeLifeCycleListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IExtendedGlobeManager globeManager;

    // A per-globe collection of registrations
    private final IdentityHashMap<IApplicationGlobe, RegistrationCollection> registrationCollections = new IdentityHashMap<>();
    // A per-globe set of surface images for the next frame
    private final IdentityHashMap<IApplicationGlobe, Set<SurfaceImage>> queuedSurfaceImages = new IdentityHashMap<>();

    @Inject
    SurfaceImageManager(IExtendedGlobeManager globeManager)
    {
        this.globeManager = globeManager;
        this.globeManager.addGlobeLifeCycleListener(this);
    }

    /**
     * Offer a {@link Renderable} to be rendered using TAK features. If the renderable is accepted this method will return {@code true}
     * and render its representation during the next render cycle. If the renderable is not offered during the next render cycle
     * then the renderable representation will no longer be drawn.
     * @param renderable the renderable to be offered
     * @param globe the globe
     * @return {@code true} if the renderable is accepted
     * @since 5.1
     */
    public boolean offerRenderable(Renderable renderable, @Nonnull IApplicationGlobe globe)
    {
        if (renderable instanceof SurfaceImage surfaceImage)
        {
            synchronized (queuedSurfaceImages)
            {
                Set<SurfaceImage> references = queuedSurfaceImages.computeIfAbsent(globe, g -> Collections.newSetFromMap(new IdentityHashMap<>()));
                references.add(surfaceImage);
            }
            return true;
        }
        return false;
    }

    private void onGLContextDisposed(@Observes GLEventDisposed event)
    {
        if (event.drawable() instanceof WorldWindow ww)
        {
            IApplicationGlobe globe = globeManager.getGlobeForComponent(ww);
            if (globe != null)
            {
                RegistrationCollection registrationCollection = registrationCollections.remove(globe);
                if (registrationCollection != null) registrationCollection.registrations().values().forEach(r -> destroy(r, globe.getPortableMapRenderer().getRenderContext()));
            }
        }
    }

    /**
     * Render queued surface images.
     *
     * @param glMapView the map view
     * @param globe     the globe
     */
    public void render(GLMapView glMapView, IApplicationGlobe globe)
    {
        RegistrationCollection registrationCollection = registrationCollections.computeIfAbsent(globe, r -> new RegistrationCollection(new IdentityHashMap<>(), 0));
        var registrations = registrationCollection.registrations;
        // If the current glMapView's render pump has changed, flush queued surface images to be registered surface images
        if (registrationCollection.renderPump != glMapView.currentPass.renderPump)
        {
            synchronized (queuedSurfaceImages)
            {
                // Unregister any of the registered surface images that have changed
                List<SurfaceImage> changedSurfaceImages = registrations.keySet().stream()
                        .filter(si -> registrations.get(si).siHashCode != hashSurfaceImage(si))
                        .toList();
                changedSurfaceImages.forEach(si -> unregisterSurfaceImage(si, registrationCollection, globe));

                // Register queued surface images
                Set<SurfaceImage> globeSurfaceImages = queuedSurfaceImages.computeIfAbsent(globe, g -> Collections.newSetFromMap(new IdentityHashMap<>()));
                globeSurfaceImages.forEach(si -> registerSurfaceImage(si, registrationCollection, glMapView));

                // Unregister any removed surface images
                List<SurfaceImage> removedSurfaceImages = registrations.keySet().stream()
                        .filter(si -> !globeSurfaceImages.contains(si))
                        .toList();
                removedSurfaceImages.forEach(si -> unregisterSurfaceImage(si, registrationCollection, globe));
                globeSurfaceImages.clear();
            }
            registrationCollections.put(globe, new RegistrationCollection(registrations, glMapView.currentPass.renderPump));
        }
        try
        {
            registrations.forEach((surfaceImage, registration) -> {
                double layerOpacity = surfaceImage.getValue(AVKey.LAYER) instanceof Layer layer ? layer.getOpacity() : 1.0;
                registration.mesh.drawMesh(glMapView, registration.texture.getTextureObject(), 1, 1, 1, (float) (surfaceImage.getOpacity() * layerOpacity));
            });
        } finally
        {
            // Clear out any shader in use by TAK so WW can use fixed-function pipeline
            GLContext.getCurrentGL().getGL2().glUseProgram(0);
        }
    }

    public void pick(DrawContext dc, GLMapView glMapView, Point pickPoint)
    {
        IApplicationGlobe globe = (IApplicationGlobe) dc.getValue(ApplicationSceneController.GLOBE_DELEGATE);
        if (globe == null) return;
        RegistrationCollection registrationCollection = registrationCollections.computeIfAbsent(globe, r -> new RegistrationCollection(new IdentityHashMap<>(), 0));

        PickSupport pickSupport = new PickSupport();
        GL2 gl = dc.getGL().getGL2();
        try
        {
            for (Map.Entry<SurfaceImage, Registration> entry : registrationCollection.registrations.entrySet())
            {
                SurfaceImage surfaceImage = entry.getKey();
                Registration registration = entry.getValue();
                Layer layer = (Layer) surfaceImage.getValue(AVKey.LAYER);
                boolean isLayerNotPickable = layer != null && !layer.isPickEnabled();
                if (isLayerNotPickable || !surfaceImage.isPickEnabled()) continue;

                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
                pickSupport.clearPickList();
                pickSupport.beginPicking(dc);
                try
                {
                    pickSupport.addPickableObject(PickUtils.createPickedObject(dc, surfaceImage, PickUtils.PICK_COLOR));
                    registration.polygon.setStyle(new BasicFillStyle(PickUtils.PICK_COLOR.getRGB()));
                    registration.polygon.draw(glMapView);
                } finally
                {
                    Point point = PickUtils.getTranslatedPoint(dc, glMapView, pickPoint);
                    pickSupport.endPicking(dc);
                    pickSupport.resolvePick(dc, point, null);
                }
            }
        } finally
        {
            gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            // Clear out any shader in use by TAK so WW can use fixed-function pipeline
            dc.getGL().getGL2().glUseProgram(0);
        }
    }

    private void registerSurfaceImage(SurfaceImage surfaceImage, RegistrationCollection registrationCollection, GLMapView glMapView)
    {
        if (!registrationCollection.registrations.containsKey(surfaceImage))
        {
            Registration registration = registrationCollection.registrations.computeIfAbsent(surfaceImage, si -> create(si, glMapView));
            if (registration != null)
            {
                // Encourage TAK kernel to redraw invalidated surface tiles over entire surface image area for all globes
                globeManager.getAllGlobeDelegates().forEach(g -> {
                    g.getPortableMapRenderer().visitControl(null, control -> control.markDirty(registration.envelope, true), SurfaceRendererControl.class);
                });
            }
        }
    }

    private Registration create(SurfaceImage surfaceImage, GLMapView glMapView)
    {
        List<GeoPoint> corners = surfaceImage.getCorners().stream().map(ll -> new GeoPoint(ll.getLatitude().degrees, ll.getLongitude().degrees)).toList();
        GeoPoint ul = corners.get(0);
        GeoPoint ur = corners.get(1);
        GeoPoint lr = corners.get(2);
        GeoPoint ll = corners.get(3);
        Envelope.Builder builder = new Envelope.Builder();
        corners.forEach(g -> builder.add(g.getLongitude(), g.getLatitude()));
        Envelope envelope = builder.build();

        Texture texture = getTexture(GLContext.getCurrentGL(), surfaceImage.getImageSource());
        if (texture != null)
        {
            final int width = texture.getWidth();
            final int height = texture.getHeight();

            DatasetProjection2 img2lla = new DefaultDatasetProjection2(
                    EquirectangularMapProjection.INSTANCE.getSpatialReferenceID(),
                    width, height, ul, ur, lr, ll);
            GLTileMesh mesh = new GLTileMesh(0, 0, width, height, 0, 1, 1, 0, img2lla);

            var glPolygon = new GLBatchPolygon(glMapView);
            LineString ls = new LineString(2);
            ls.addPoint(ul.getLongitude(), ul.getLatitude());
            ls.addPoint(ur.getLongitude(), ur.getLatitude());
            ls.addPoint(lr.getLongitude(), lr.getLatitude());
            ls.addPoint(ll.getLongitude(), ll.getLatitude());
            ls.addPoint(ul.getLongitude(), ul.getLatitude());
            Polygon polygon = new Polygon(ls);
            glPolygon.setGeometry(polygon);

            return new Registration(mesh, surfaceImage.getImageSource(), texture, envelope, glPolygon, hashSurfaceImage(surfaceImage));
        }
        return null;
    }

    /**
     * Custom hash function to take into consideration the corners of the SurfaceImage, not just the sector as SurfaceImage does by default.
     * <p/>
     *
     * Sums the hashes for each LatLon corner of the SurfaceImage with the hash of the SurfaceImage. If an error occurs hashing the corners,
     * it will default to the SurfaceImage hash.
     *
     * @param surfaceImage The SurfaceImage to hash
     * @return a hash containing the default SurfaceImage hash as well as a hash of each LatLon corner
     * @since 3.5.0
     */
    private int hashSurfaceImage(SurfaceImage surfaceImage)
    {
        return (surfaceImage.getCorners().hashCode() * 31) + surfaceImage.hashCode();
    }

    private void destroy(Registration registration, @Nullable RenderContext<?> renderContext)
    {
        registration.mesh.release();
        registration.polygon.release();
        if (renderContext != null)
        {
            if (renderContext.isRenderThread() && renderContext.isAttached())
            {
                registration.texture.destroy(GLContext.getCurrentGL());
            } else
            {
                renderContext.queueEvent(() -> registration.texture.destroy(GLContext.getCurrentGL()));
            }
        }
    }

    private void unregisterSurfaceImage(SurfaceImage surfaceImage, RegistrationCollection registrationCollection, IApplicationGlobe globe)
    {
        Registration registration = registrationCollection.registrations.remove(surfaceImage);
        if (registration != null)
        {
            destroy(registration, globe.getPortableMapRenderer().getRenderContext());
        }
    }

    @Override
    public void globeRemoved(IGlobeDelegate globe)
    {
        synchronized (queuedSurfaceImages)
        {
            queuedSurfaceImages.remove((IApplicationGlobe) globe);
        }
        RegistrationCollection registrationCollection = registrationCollections.remove((IApplicationGlobe) globe);
        if (registrationCollection != null) registrationCollection.registrations.values().forEach(r -> destroy(r, globe.getPortableMapRenderer().getRenderContext()));
    }

    private static Texture getTexture(GL gl, Object imageSource)
    {
        if (imageSource instanceof String path)
        {
            try
            {
                return TextureIO.newTexture(new File(path), false);
            } catch (Exception e)
            {
                logger.error("Exception during texture initialization for image file at path {}.", path, e);
            }
        } else if (imageSource instanceof BufferedImage bufferedImage)
        {
            try
            {
                return AWTTextureIO.newTexture(gl.getGLProfile(), bufferedImage, false);
            } catch (Exception e)
            {
                logger.error("Exception during texture initialization.", e);
            }
        } else if (imageSource instanceof URL url)
        {
            try
            {
                return TextureIO.newTexture(url, false, null);
            } catch (Exception e)
            {
                logger.error("Exception during texture initialization for image file at URL {}.", url, e);
            }
        } else
        {
            logger.warn("Unrecognized image source type {}.", imageSource.getClass().getName());
        }
        return null;
    }

    private record RegistrationCollection(IdentityHashMap<SurfaceImage, Registration> registrations, int renderPump)
    {
    }

    private record Registration(GLTileMesh mesh, Object imageSource, Texture texture, Envelope envelope, GLBatchPolygon polygon, long siHashCode)
    {
    }
}
