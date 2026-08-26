package gov.takx.core.globe.layers.undo;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.SurfaceImageLayerResizeEvent;
import gov.takx.core.globe.layers.SurfaceImageController;
import gov.takx.core.undo.UndoHandler;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

/**
 * Undo handler for surface images.  Watch resize events and produce undo operations.
 *
 * @see UndoSurfaceImageChange
 * @since 1.26
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SurfaceImageUndoHandler extends UndoHandler
{
    @Inject
    private EventBus eventBus;

    private final SurfaceImageController controller;
    private UndoSurfaceImageChange undoOperation;
    private boolean gotResize = false;

    /**
     * Construct on the given surface image controller.  The controller is used to apply the recorded corners, which
     * is how the undo is performed.
     *
     * @param controller Surface image controller to work with
     */
    public SurfaceImageUndoHandler(SurfaceImageController controller)
    {
        this.controller = controller;
    }

    @PostConstruct
    private void postConstruct()
    {
        eventBus.register(this); // Wire up event processing
    }

    /**
     * Shutdown our operation, unregister as an event subscriber.
     */
    @Override
    protected void onShutdown()
    {
        eventBus.unregister(this);  // Un-wire event processing
    }

    /**
     * Process a resize event from the event bus.  We will get one Armed event, followed by an arbitrary number of
     * Resize events (including 0), followed by a Disarmed event.  So, on the Arm, we create the undo operation.
     * If we get an intervening resize, upon disarm we add the undo operation.
     *
     * @param event The event to process
     */
    @SuppressWarnings("unused")
    @Subscribe
    public void onResizeEvent(SurfaceImageLayerResizeEvent event)
    {
        // Ensure that the event belongs to our controller
        if (event.getController() != controller) return;

        switch (event.getEventType())
        {
            case Armed:
                undoOperation = new UndoSurfaceImageChange(controller, event.getCorners());
                gotResize = false; // Until we do
                break;

            case Resized:
                gotResize = true; // Just track that we got at least one
                break;

            case Disarmed:
                // If we have an undo operation and we got a resize, then actually queue the undo

                if (undoOperation != null && gotResize)
                {
                    addUndoOperation(undoOperation);
                    undoOperation = null;
                }
                break;
        }
    }
}
