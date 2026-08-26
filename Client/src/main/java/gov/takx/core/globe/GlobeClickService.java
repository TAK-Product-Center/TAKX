package gov.takx.core.globe;

import com.google.common.util.concurrent.Uninterruptibles;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IGlobeClickService;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.pointdropper.PointDropperConfiguration.IDeactivator;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.AWTEvent;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Implementation of the globe click service.
 *
 * @since 5.4
 */
@ApplicationScoped
public class GlobeClickService implements IGlobeClickService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IGlobeManager globeManager;

    private final GlobeClickMouseListener mouseListener = new GlobeClickMouseListener(this::handleClick);
    private final DeactivationListener deactivationListener = new DeactivationListener();

    private boolean active;
    private IGlobeDelegate globe;
    private Consumer<ILocation> clickHandler;
    private Runnable deactivationHandler;
    private IDeactivator deactivator;

    @Inject
    GlobeClickService(IGlobeManager globeManager)
    {
        this.globeManager = globeManager;
    }

    @Override
    public void activate(@Nullable IGlobeDelegate globe, @Nullable String message, @Nonnull Consumer<ILocation> clickHandler,
                         @Nonnull IDeactivator deactivator, @Nullable Runnable deactivationHandler)
            throws IllegalStateException
    {
        if (active) throw new IllegalStateException("A click operation is already active");

        this.globe = globe;
        this.clickHandler = clickHandler;
        this.deactivationHandler = deactivationHandler;
        this.deactivator = deactivator;

        active = true;

        if (!StringUtils.isEmpty(message)) globeManager.showGlobeMessage(message, globe);

        applyToGlobes(applicationGlobe -> {
            applicationGlobe.addMouseListener(mouseListener);
            applicationGlobe.addMouseMotionListener(mouseListener);
        });

        mouseListener.setArmed(true);

        // There are several issues affecting something you would think is simple, catching an ESC key event.
        //
        // 1. This is subtle/wierd, but the docking library creates some very strange component hierarchies for
        //    flap dockables that are not pinned.  It manages to embed another Window within the overall hierarchy,
        //    so typical action map handling for WHEN_IN_FOCUSED_WINDOW doesn't work properly if that panel has
        //    the focus.
        //
        // 2. If something that is not focusable is clicked to bring a window back into focus, it causes the Docking Frames
        //    library to go a little crazy, and it tries 10+ times to give that component the focus.  Each time it tries
        //    it clears the global focus, making it null.  Without an actual focused component, KeyEvents are not
        //    generated, meaning that AWTEventListeners will never see the key press.
        //
        // So, the end result is that we need to use an AWTEventListener instead of KeyActionHandler, and we need to
        // install a "watchdog" to give the focus to the last focused globe if after 1 second nothing has the focus.
        //
        // It should not be this hard.

        Toolkit.getDefaultToolkit().addAWTEventListener(deactivationListener, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);

        //noinspection DataFlowIssue
        final Component lastFocusedGlobeComponent = globeManager.getLastFocusedGlobe().getWorldWindow().getComponent();
        if (lastFocusedGlobeComponent != null) // Can be null in tests
        {
            ForkJoinPool.commonPool().execute(() -> {
                Uninterruptibles.sleepUninterruptibly(1, TimeUnit.SECONDS);
                lastFocusedGlobeComponent.requestFocus();
            });
        }

    }

    @Override
    public void deactivate()
    {
        if (!active) return;

        active = false;
        globeManager.hideGlobeMessage(globe);

        applyToGlobes(applicationGlobe -> {
            applicationGlobe.removeMouseListener(mouseListener);
            applicationGlobe.removeMouseMotionListener(mouseListener);
        });

        mouseListener.setArmed(false);
        Toolkit.getDefaultToolkit().removeAWTEventListener(deactivationListener);

        try
        {
            if (deactivationHandler != null) deactivationHandler.run();
        } catch (Exception e)
        {
            logger.error("Error in click deactivation handler", e);
        }

        globe = null;
        clickHandler = null;
        deactivationHandler = null;
        deactivator = null;
    }

    /**
     * Apply an operation to all affected globes.
     */
    private void applyToGlobes(Consumer<IApplicationGlobe> handler)
    {
        final var globeDelegates = globe != null ? List.of(globe) : globeManager.getAllGlobeDelegates();

        globeDelegates.forEach(globeDelegate -> handler.accept((IApplicationGlobe) globeDelegate));
    }

    /**
     * Invoke the configured handler with the clicked location.
     */
    private void handleClick(ILocation location)
    {
        try
        {
            clickHandler.accept(location);
        } catch (Exception e)
        {
            logger.error("Error in click handler", e);
        }
    }

    /**
     * Listener to deactivate according to the given deactivator or when the ESC key is pressed.
     *
     * @since 5.2
     */
    private class DeactivationListener implements AWTEventListener
    {
        @Override
        public void eventDispatched(AWTEvent event)
        {
            final int id = event.getID();
            if (id == MouseEvent.MOUSE_PRESSED && deactivator.shouldDeactivate((MouseEvent) event)) deactivate();
            if (id == KeyEvent.KEY_PRESSED && ((KeyEvent) event).getKeyCode() == KeyEvent.VK_ESCAPE) deactivate();
        }
    }
}
