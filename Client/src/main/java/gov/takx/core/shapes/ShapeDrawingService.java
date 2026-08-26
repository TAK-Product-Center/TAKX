package gov.takx.core.shapes;

import gov.takx.api.annotation.UiEventBus;
import gov.takx.api.event.EditorFailedEvent;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.UserShapeCreatedEvent;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeMapObjectManager;
import gov.takx.api.globe.IGlobeShapeMapObject;
import gov.takx.api.mapobject.CreationFailedException;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.MapToolType;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeDrawingService;
import gov.takx.api.shapes.ShapeDrawingConfiguration;
import gov.takx.api.ui.event.MapObjectSelectedEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTEvent;
import java.awt.Cursor;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.lang.invoke.MethodHandles;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Concrete implementation of {@link IShapeDrawingService}.
 *
 * @since 3.5
 */
@ApplicationScoped
public class ShapeDrawingService implements IShapeDrawingService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IExtendedGlobeManager globeManager;
    private final IMapObjectManager mapObjectManager;
    private final EventBus uiEventBus;

    private final Map<IShape, IApplicationGlobe> drawnShapes = new ConcurrentHashMap<>(); // Track all shapes drawn
    private DrawOperation activeDrawOperation;

    @Inject
    ShapeDrawingService(IExtendedGlobeManager globeManager, IMapObjectManager mapObjectManager, @UiEventBus EventBus uiEventBus)
    {
        this.globeManager = globeManager;
        this.mapObjectManager = mapObjectManager;
        this.uiEventBus = uiEventBus;
    }

    @Nonnull
    @Override
    public synchronized CompletableFuture<IMutableShape> requestDraw(@Nonnull ShapeDrawingConfiguration configuration) throws IllegalStateException
    {
        if (activeDrawOperation != null) throw new IllegalStateException("Already drawing a " + activeDrawOperation.mapToolType);

        final IApplicationGlobe applicationGlobe = globeManager.getGlobeDelegate(configuration.globe().getGlobeId());

        // Force the termination of any active drawing tool
        uiEventBus.post(new MapObjectSelectedEvent(null));

        //noinspection DataFlowIssue - applicationGlobe can't be null in real operation
        activeDrawOperation = new DrawOperation(configuration, applicationGlobe);
        activeDrawOperation.beginDraw();

        return activeDrawOperation.future
                .whenComplete((shape, t) -> drawingComplete(shape, applicationGlobe));
    }

    @Override
    public synchronized void cancelDraw()
    {
        if (activeDrawOperation != null)
        {
            activeDrawOperation.cancel();
            activeDrawOperation = null;
        }
    }

    @Override
    public void removeDrawnShape(@Nonnull IShape shape) throws IllegalArgumentException
    {
        final IApplicationGlobe globe = drawnShapes.remove(shape);
        if (globe == null) throw new IllegalArgumentException("Not one of our drawn shapes: " + shape);

        globe.getGlobeMapObjectManager().removeGlobeMapObjectForShape(shape);
    }

    /**
     * Drawing is complete, record the shape and null out the active draw operation.
     *
     * @param shape The drawn shape, or {@code null} if the draw operation failed or was canceled
     * @param applicationGlobe Globe on which the shape was drawn
     */
    private synchronized void drawingComplete(@Nullable IMutableShape shape, IApplicationGlobe applicationGlobe)
    {
        if (shape != null) drawnShapes.put(shape, applicationGlobe);
        activeDrawOperation = null;
    }

    /**
     * Stop this service, cancel any active drawing operation.
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event)
    {
        cancelDraw();
        drawnShapes.clear();
    }

    /**
     * Manages the state of a draw operation.
     */
    public class DrawOperation
    {
        private static final Cursor CROSSHAIR_CURSOR = Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR);

        private final MapToolType mapToolType;
        private final IApplicationGlobe globe;
        private final ShapeDrawingConfiguration configuration;
        private final IGlobeMapObjectManager globeMapObjectManager;
        private final String instructions;

        private IGlobeShapeMapObject currentGlobeShapeMapObject;
        private final CompletableFuture<IMutableShape> future = new CompletableFuture<>();
        private final DrawToolListener drawToolListener = new DrawToolListener(this::stop);

        /**
         * @param configuration draw configuration
         * @param globe globe on which to draw
         */
        private DrawOperation(@Nonnull ShapeDrawingConfiguration configuration, @Nonnull IApplicationGlobe globe)
        {
            this.configuration = configuration;
            this.mapToolType = MapToolType.valueOfShapeType(configuration.shapeType());
            this.instructions = configuration.instructions();
            this.globe = globe;
            globeMapObjectManager = globe.getGlobeMapObjectManager();
        }

        private void beginDraw()
        {
            logger.debug("Start draw: tool {} on globe {}", mapToolType, globe.getGlobeId());

            mapObjectManager.setSelected(null);
            globeManager.setSelectListenersActive(false);
            uiEventBus.register(this);
            globe.setCursor(CROSSHAIR_CURSOR);
            if (instructions != null) globeManager.showGlobeMessage(instructions);
            // We only support drawing MapToolTypes that produce IGlobeShapeMapObjects (no Annotations)
            currentGlobeShapeMapObject = (IGlobeShapeMapObject) globeMapObjectManager.setMapTool(mapToolType, true);
            drawToolListener.attach();
        }

        /**
         * @param removeShape Pass {@code true} to remove the globe shape map object created in {@link #beginDraw()}
         * @param canceling Pass {@code true} if we're canceling a draw operation
         */
        private void endDraw(boolean removeShape, boolean canceling)
        {
            logger.debug("end draw: tool {}, remove {}, canceling {}", mapToolType, removeShape, canceling);

            drawToolListener.detach();
            if (instructions != null) globeManager.hideGlobeMessage();

            // When canceling a draw operation, we need to unsubscribe from the shape events BEFORE
            // setting the map tool to NONE.  Otherwise, we will potentially see it as a normal completion.
            if (canceling && uiEventBus.isRegistered(this)) uiEventBus.unregister(this);

            if (canceling)
            {
                globeMapObjectManager.cancelMapTool();
            } else
            {
                globeMapObjectManager.setMapTool(MapToolType.NONE); // Also resets the cursor
            }

            globeManager.setSelectListenersActive(true);

            if (!canceling && uiEventBus.isRegistered(this)) uiEventBus.unregister(this);

            if (removeShape && currentGlobeShapeMapObject != null)
            {
                // The shape model can be null if the user never started drawing the shape.
                final IMutableShape shape = currentGlobeShapeMapObject.getShapeModel();
                if (shape != null) globeMapObjectManager.removeGlobeMapObjectForShape(shape);
            }
            currentGlobeShapeMapObject = null;
        }

        /**
         * Cancel the operation.  This will end the drawing operation and cancel the future.
         */
        private void cancel()
        {
            endDraw(true, true);
            future.cancel(true);
        }

        /**
         * Stop the active drawing operation.  Handle according to our configuration.
         *
         * @see ShapeDrawingConfiguration#escapeAction()
         * @since 5.2
         */
        private void stop()
        {
            logger.debug("stopped, escapeAction: {}", configuration.escapeAction());

            if (configuration.escapeAction() == ShapeDrawingConfiguration.EscapeAction.complete)
            {
                endDraw(false, false);
            } else
            {
                cancel();
            }
        }

        @Subscribe
        public void creationFailed(EditorFailedEvent ignored)
        {
            currentGlobeShapeMapObject = null; // Since creation failed, no GlobeShapeMapObject was created
            endDraw(true, true);
            future.completeExceptionally(new CreationFailedException("Invalid shape", null));
        }

        @Subscribe
        public void creationComplete(UserShapeCreatedEvent event)
        {
            final IMutableShape shape = event.shape();
            shape.setName("new " + mapToolType);

            logger.debug("draw complete: shape {}", shape);

            endDraw(false, true);
            future.complete(shape);
        }
    }

    /**
     * Listener to invoke a specified action if the user presses the Escape key.
     *
     * @since 3.5, moved here from {@link gov.takx.core.view.gui.menus.DrawToolbarTabWrapper}
     */
    public static class DrawToolListener implements AWTEventListener
    {
        private final Runnable stopHandler;

        private boolean listenerAttached = false;

        /**
         * @param stopHandler action to invoke when the user clicks outside allowable areas or presses ESC
         */
        public DrawToolListener(Runnable stopHandler)
        {
            this.stopHandler = stopHandler;
        }

        /**
         * Add ourselves as an AWT event listener.
         */
        public void attach()
        {
            if (!listenerAttached)
            {
                Toolkit.getDefaultToolkit().addAWTEventListener(this, AWTEvent.KEY_EVENT_MASK);
                listenerAttached = true;
            }
        }

        /**
         * Remove ourselves as an AWT event listener.
         */
        public void detach()
        {
            if (listenerAttached)
            {
                Toolkit.getDefaultToolkit().removeAWTEventListener(this);
                listenerAttached = false;
            }
        }

        /**
         * If we have a {@link KeyEvent#KEY_PRESSED KEY_PRESSED} event, and it was the Escape key, disable the drawing tool.
         *
         * @param event a {@link KeyEvent}
         */
        @Override
        public void eventDispatched(AWTEvent event)
        {
            if (event.getID() == KeyEvent.KEY_PRESSED && ((KeyEvent) event).getKeyCode() == KeyEvent.VK_ESCAPE) stopHandler.run();
        }
    }
}
