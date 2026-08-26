package gov.takx.core.globe.tak;

import com.atakmap.map.Globe;
import com.atakmap.map.LegacyAdapters;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.RenderContext;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSourceBuilder;
import com.atakmap.map.layer.control.ElevationSourceControl;
import com.atakmap.map.layer.control.RenderContextControl;
import com.atakmap.map.opengl.GLBaseMap;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.opengl.GLStarsRenderable;
import com.atakmap.map.projection.EquirectangularMapProjection;
import com.atakmap.util.ConfigOptions;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import gov.nasa.worldwind.WorldWindow;
import gov.tak.api.engine.map.IGlobe;
import gov.tak.api.engine.map.IMapRendererSpi;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.api.engine.map.RenderContextFactory;
import gov.tak.platform.commons.opengl.JOGLGLES;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.ConfigureGLMapView;
import gov.takx.api.event.project.ProjectOpening;
import gov.takx.api.event.tak.GLEventDisposed;
import gov.takx.api.event.tak.GLEventInitialized;
import gov.takx.api.event.tak.GLEventReshaped;
import gov.takx.api.event.tak.MapRendererInitialized;
import gov.takx.core.configuration.KernelConfigOptionConstants;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.JComponent;

/**
 * Create a TAK globe renderer using an underlying {@link GLMapView}.
 *
 * @since 3.4
 */
@PseudoBean
public final class JOGLMapRendererSpi implements IMapRendererSpi
{
    @Inject
    private Event<MapRendererInitialized> mapRendererInitializedEvent;

    @Inject
    private Event<ConfigureGLMapView> configureGLMapViewEvent;

    @Inject
    private Event<GLEventInitialized> glEventInitializedEvent;

    @Inject
    private Event<GLEventDisposed> glEventDisposedEvent;

    @Inject
    private Event<GLEventReshaped> glEventReshapedEvent;

    private static GLCapabilities glCapabilities;

    static
    {
        // Hack to make sure MapProjectionDisplayModel static initializers run and marshal handlers are registered
        gov.tak.api.engine.map.MapProjectionDisplayModel.isSupported(EquirectangularMapProjection.DISPLAY_MODEL.srid);
    }

    /**
     * Create our {@link GLCapabilities} early since it can take 1 - 4 seconds for this to complete.  I have no
     * idea why it is so variable, but the least amount of time I've seen it take is 1 second, and doing it early
     * allows it to run in parallel with the other background activities of opening a project.
     *
     * @since 5.2
     */
    @InvokeOnEDT
    private void onProjectOpening(@Observes ProjectOpening ignored, @PlatformUnderTest boolean underTest)
    {
        if (underTest) return;

        glCapabilities = new GLCapabilities(GLProfile.getMaxProgrammable(true));
        glCapabilities.setDepthBits(24);
        glCapabilities.setStencilBits(8);
    }

    private void setCurrentJOGLES(GLAutoDrawable drawable)
    {
        JOGLGLES.init(drawable);
    }

    @Override
    public MapRenderer<?> create(IGlobe globe, Object parent, Class<?> surfaceType)
    {
        if (!(globe instanceof Globe)) return null;

        if (surfaceType == null) surfaceType = JComponent.class;

        GlobeComponent component;
        if (surfaceType.isAssignableFrom(GlobePanel.class))
        {
            component = new GlobePanel(glCapabilities);
        } else if (surfaceType.isAssignableFrom(GlobeCanvas.class))
        {
            component = new GlobeCanvas(glCapabilities);
        } else
        {
            return null;
        }

        // create the Globe renderer and start it
        JOGLRenderContext renderContext = (JOGLRenderContext) RenderContextFactory.create(component);
        GLMapView glMapView = new GLMapView(
                renderContext,
                (Globe) globe,
                0,
                0,
                component.getWidth(),
                component.getHeight());
        configureGLMapView(glMapView);

        MapRenderer<?> mapRenderer = LegacyAdapters.adapt(glMapView);
        mapRendererInitializedEvent.fire(new MapRendererInitialized(glMapView, null));

        component.addGLEventListener(new GLEventListener()
        {
            private boolean glContextIsDestroyed;

            @Override
            public void init(GLAutoDrawable drawable)
            {
                setCurrentJOGLES(drawable);

                // disable vsync if not explicitly enabled
                boolean enableVsync = ConfigOptions.getOption(KernelConfigOptionConstants.ENABLE_VSYNC, 0) == 1;
                if (!enableVsync) drawable.getGL().setSwapInterval(0);

                if (glContextIsDestroyed)
                {
                    component.removeGLEventListener(renderContext);
                    JOGLRenderContext newRenderContext = (JOGLRenderContext) RenderContextFactory.create(component);
                    RenderContextControl renderContextControl = glMapView.getControl(RenderContextControl.class);
                    renderContextControl.setRenderContext(newRenderContext);
                    configureGLMapView(glMapView);
                }

                // Start the Globe renderer
                glMapView.start();

                // set the clear color (black)
                drawable.getGL().getGL().glClearColor(0f, 0f, 0f, 1f);

                // Fire GLEventInitialized event
                glEventInitializedEvent.fire(new GLEventInitialized(drawable));
            }

            @Override
            public void dispose(GLAutoDrawable drawable)
            {
                setCurrentJOGLES(drawable);

                // Fire GLEventDisposed event
                glEventDisposedEvent.fire(new GLEventDisposed(drawable));

                // signal the need to re-initialize JOGLES, since the JOGL is tearing down the GL instance
                glMapView.stop();
                glMapView.release();
                ((WorldWindow) component).getSceneController().reinitialize();
                glContextIsDestroyed = true;
            }

            @Override
            public void display(GLAutoDrawable drawable)
            {
                setCurrentJOGLES(drawable);

                GL gl = drawable.getGL();
                gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);

                glMapView.render();
            }

            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
            {
                // Fire GLEventReshaped event
                glEventReshapedEvent.fire(new GLEventReshaped(drawable, x, y, width, height));
            }
        });

        return mapRenderer;
    }

    /**
     * Configure a GLMapView. This needs to be called on initial creation and after calling {@link RenderContextControl#setRenderContext(RenderContext)}.
     * @param glMapView the glMapView
     * @since 5.6
     */
    private void configureGLMapView(GLMapView glMapView)
    {
        glMapView.setDisplayMode(MapRenderer3.DisplayMode.Globe);

        // Focus point can be off on initial calculation due to surface metrics
        glMapView.setFocusPointOffset(0, 0);

        glMapView.setBaseMap(new GLBaseMap());
        glMapView.setStarsLayer(new GLStarsRenderable());

        // We want to early-load a no-op ElevationSource. This prevents early terrain requests on the globe before we've
        // called setElevationSource() from using the default elevation source created in ElMgrTerrainRenderService.
        ElevationSourceControl control = glMapView.getControl(ElevationSourceControl.class);
        ElevationSource multiplexSource = ElevationSourceBuilder.multiplex("Null Elevation Source", new ElevationSource[0]);
        control.setElevationSource(multiplexSource);

        // Fire ConfigureGLMapView event
        configureGLMapViewEvent.fire(new ConfigureGLMapView(glMapView));
    }
}
