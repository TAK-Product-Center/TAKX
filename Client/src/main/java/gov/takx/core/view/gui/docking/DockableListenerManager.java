package gov.takx.core.view.gui.docking;

import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.event.CDockableAdapter;
import bibliothek.gui.dock.common.event.CDockableLocationEvent;
import bibliothek.gui.dock.common.event.CDockableLocationListener;
import bibliothek.gui.dock.common.event.CFocusListener;
import bibliothek.gui.dock.common.event.CVetoClosingEvent;
import bibliothek.gui.dock.common.event.CVetoClosingListener;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.location.CExternalizedLocation;
import bibliothek.gui.dock.common.location.CFlapIndexLocation;
import bibliothek.gui.dock.common.location.CMaximalExternalizedLocation;
import bibliothek.gui.dock.common.location.CMaximizedLocation;
import bibliothek.gui.dock.common.location.CStackLocation;
import bibliothek.gui.dock.common.location.TreeLocationLeaf;
import gov.takx.api.ui.UISpaceEvent;
import gov.takx.api.ui.UISpaceFocusEvent;
import gov.takx.api.ui.UISpaceResizeEvent;
import gov.takx.api.ui.UISpaceStateChangeEvent;
import gov.takx.api.ui.UISpaceStateChangeEvent.DockingState;
import gov.takx.api.ui.UISpaceVisibilityEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.lang.invoke.MethodHandles;
import java.util.WeakHashMap;

/**
 * Service to add listeners to dockables to broadcast events as changes occur, like position, state, size, etc.
 *
 * @since 2.25
 */
@ApplicationScoped
public class DockableListenerManager
{
    private final WeakHashMap<ComponentDockable, DockableClosingListener> dockableClosingListenerMap = new WeakHashMap<>();
    private final WeakHashMap<ComponentDockable, DockableLocationListener> dockableLocationListenerMap = new WeakHashMap<>();
    private final WeakHashMap<ComponentDockable, DockablePropertyListener> dockablePropertyListenerMap = new WeakHashMap<>();
    private final WeakHashMap<ComponentDockable, DockableFocusListener> dockableFocusListenerMap = new WeakHashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final Event<UISpaceEvent> uiSpaceEvent;

    private boolean suppressEvents = true;

    /**
     * @since 3.4
     */
    @Inject
    DockableListenerManager(Event<UISpaceEvent> uiSpaceEvent)
    {
        this.uiSpaceEvent = uiSpaceEvent;
    }

    /**
     * Add listeners to the given dockable.
     *
     * @param dockable The dockable to which to add listeners
     * @param dockableAndProps Properties of dockable to process
     */
    public void addDockableListeners(@Nonnull ComponentDockable dockable, @Nonnull DockableAndProps dockableAndProps)
    {
        dockable.addVetoClosingListener(dockableClosingListenerMap.computeIfAbsent(dockable, c -> new DockableClosingListener(dockableAndProps)));
        dockable.addCDockableLocationListener(dockableLocationListenerMap.computeIfAbsent(dockable, c -> new DockableLocationListener(dockableAndProps)));
        dockable.addCDockablePropertyListener(dockablePropertyListenerMap.computeIfAbsent(dockable, c -> new DockablePropertyListener(dockableAndProps)));
        dockable.addFocusListener(dockableFocusListenerMap.computeIfAbsent(dockable, c -> new DockableFocusListener(dockableAndProps)));
    }

    public void removeDockableListeners(@Nonnull ComponentDockable dockable)
    {
        DockableClosingListener dockableClosingListener = dockableClosingListenerMap.remove(dockable);
        if (dockableClosingListener != null) dockable.removeVetoClosingListener(dockableClosingListener);
        DockableLocationListener dockableLocationListener = dockableLocationListenerMap.remove(dockable);
        if (dockableLocationListener != null) dockable.removeCDockableLocationListener(dockableLocationListener);
        DockablePropertyListener dockablePropertyListener = dockablePropertyListenerMap.remove(dockable);
        if (dockablePropertyListener != null) dockable.removeCDockablePropertyListener(dockablePropertyListener);
        DockableFocusListener dockableFocusListener = dockableFocusListenerMap.remove(dockable);
        if (dockableFocusListener != null) dockable.removeFocusListener(dockableFocusListener);
    }

    /**
     * Suppress or allow events to be fired.  This is only used to suppress events while a layout is being loaded.
     *
     * @param suppressEvents Pass {@code true} to suppress events, {@code false} to allow events
     */
    public void setSuppressEvents(boolean suppressEvents)
    {
        this.suppressEvents = suppressEvents;
    }

    /**
     * @return {@code true} if events should be fired
     */
    private boolean fireEvents()
    {
        return !suppressEvents;
    }

    /**
     * Construct a UISpaceStateChangeEvent object from our dockable and the given state.
     *
     * @param dockableAndProps Dockable properties for which to generate the event
     * @param state State to include in event
     * @return new event
     */
    private UISpaceStateChangeEvent makeStateChangeEvent(@Nonnull DockableAndProps dockableAndProps, DockingState state)
    {
        return new UISpaceStateChangeEvent(dockableAndProps.getDescriptor(), dockableAndProps.getComponent(), state);
    }

    /**
     * Construct a UISpaceResizeEvent object from our dockable and the given size.
     *
     * @param dockableAndProps Dockable properties for which to generate the event
     * @param rectangle size to include in event
     * @return new event
     */
    private UISpaceResizeEvent makeResizeEvent(DockableAndProps dockableAndProps, Rectangle rectangle)
    {
        return new UISpaceResizeEvent(dockableAndProps.getDescriptor(), dockableAndProps.getComponent(), rectangle);
    }

    /**
     * Construct a UISpaceFocusEvent object from our dockable and the given state.
     *
     * @param dockableAndProps Dockable properties for which to generate the event
     * @param focus Focus state to include in event
     * @return new event
     */
    private UISpaceFocusEvent makeFocusEvent(DockableAndProps dockableAndProps, boolean focus)
    {
        return new UISpaceFocusEvent(dockableAndProps.getDescriptor(), dockableAndProps.getComponent(), focus);
    }

    /**
     * Construct a UISpaceVisibilityEvent object from our dockable and the given state.
     *
     * @param dockableAndProps Dockable properties for which to generate the event
     * @param visible Visibility state to include in event
     * @return new event
     */
    private UISpaceVisibilityEvent makeVisibilityEvent(DockableAndProps dockableAndProps, boolean visible)
    {
        return new UISpaceVisibilityEvent(dockableAndProps.getDescriptor(), dockableAndProps.getComponent(), visible);
    }

    /**
     * Class to monitor a dockable close operations.
     */
    private final class DockableClosingListener implements CVetoClosingListener
    {
        private final DockableAndProps dockableAndProps;

        private DockableClosingListener(@Nonnull DockableAndProps dockableAndProps)
        {
            this.dockableAndProps = dockableAndProps;
        }

        @Override
        public void closing(CVetoClosingEvent event)
        {
            // Previously, the dockable was made null here, to keep track of panels that were closed during
            // a layout load. After some digging, however, I found a couple of scenarios where a dockable
            // would enter this method but not get to the closed method below, or even get to being removed by
            // the dock control. This resulted in null dockables that were still docked in the UI, and when
            // the layout is reloaded (see DockingManager#loadLayout), there were two of each (RAPTORX-386).
        }

        @Override
        public void closed(CVetoClosingEvent event)
        {
            // Wait for confirmation of a dock being closed before marking it null
            dockableAndProps.setDockable(null);

            if (fireEvents())
            {
                uiSpaceEvent.fire(makeStateChangeEvent(dockableAndProps, DockingState.Closed));
                if (logger.isDebugEnabled()) logger.debug("closed:{}", dockableAndProps.getName());
            }
        }
    }

    /**
     * Class to monitor for location changes.
     */
    private final class DockableLocationListener implements CDockableLocationListener
    {
        private final DockableAndProps dockableAndProps;

        private DockableLocationListener(@Nonnull DockableAndProps dockableAndProps)
        {
            this.dockableAndProps = dockableAndProps;
        }

        @Override
        public void changed(@Nonnull CDockableLocationEvent locationEvent)
        {
            final CLocation newLocation = locationEvent.getNewLocation();
            if (locationEvent.getNewShowing() && newLocation instanceof CExternalizedLocation externalizedLocation)
            {
                dockableAndProps.setExternalizedLocation(externalizedLocation);
            }

            if (fireEvents())
            {
                CLocation oldLocation = locationEvent.getOldLocation();

                if (logger.isDebugEnabled())
                {
                    logger.debug("Location Changed: {}, new=:{}/{}, old={}/{}", dockableAndProps.getName(), newLocation,
                            locationEvent.getNewShowing(), oldLocation, locationEvent.getOldShowing());
                }

                if (newLocation != null)
                {
                    if (oldLocation == null || !oldLocation.getClass().equals(newLocation.getClass()))
                    {
                        uiSpaceEvent.fire(makeStateChangeEvent(dockableAndProps, getState(newLocation)));
                    }

                    if (locationEvent.getNewShowing() != locationEvent.getOldShowing())
                    {
                        uiSpaceEvent.fire(makeVisibilityEvent(dockableAndProps, locationEvent.getNewShowing()));
                    }

                    Rectangle newRect = getRect(newLocation);
                    Rectangle oldRect = getRect(oldLocation);

                    if (newRect != null && !newRect.equals(oldRect)) uiSpaceEvent.fire(makeResizeEvent(dockableAndProps, newRect));
                } else
                {
                    // If the new location is null, the panel was closed
                    uiSpaceEvent.fire(makeStateChangeEvent(dockableAndProps, DockingState.Closed));
                    uiSpaceEvent.fire(makeVisibilityEvent(dockableAndProps, locationEvent.getNewShowing()));
                }
            }
        }

        /**
         * Convert a CLocation to a docking state.
         *
         * @param loc Dockable's location
         * @return docking state for that location
         */
        @Nullable
        private DockingState getState(CLocation loc)
        {
            DockingState state = null;
            if (loc instanceof CFlapIndexLocation)
            {
                state = DockingState.FlapButton;
            } else if (loc instanceof TreeLocationLeaf)
            {
                state = DockingState.Docked;
            } else if (loc instanceof CMaximalExternalizedLocation)
            {
                state = DockingState.MaximalExternalized;
            } else if (loc instanceof CExternalizedLocation)
            {
                state = DockingState.Externalized;
            } else if (loc instanceof CStackLocation)
            {
                state = DockingState.StackTab;
            } else if (loc instanceof CMaximizedLocation)
            {
                state = DockingState.MaximalDocked;
            }
            return state;
        }

        /**
         * Convert the CLocation into a meaningful Rectangle (if possible)
         *
         * @param loc the CLocation
         * @return the Rectangle
         */
        private Rectangle getRect(CLocation loc)
        {
            final Rectangle rect;

            if (loc instanceof TreeLocationLeaf)
            {
                // Not sure what these should be set to, but we want the event to be fired
                rect = new Rectangle();
                rect.x = -1;
                rect.y = -1;
                rect.width = -1;
                rect.height = -1;
            } else if (loc instanceof final CExternalizedLocation externalizedLocation)
            {
                rect = new Rectangle();
                rect.x = externalizedLocation.getX();
                rect.y = externalizedLocation.getY();
                rect.width = externalizedLocation.getWidth();
                rect.height = externalizedLocation.getHeight();
            } else
            {
                rect = null;
            }

            return rect;
        }
    }

    /**
     * Class to watch for minimize size changes.
     */
    private final class DockablePropertyListener extends CDockableAdapter
    {
        private final DockableAndProps dockableAndProps;

        private DockablePropertyListener(DockableAndProps dockableAndProps)
        {
            this.dockableAndProps = dockableAndProps;
        }

        @Override
        public void minimizeSizeChanged(@Nonnull CDockable cDockable)
        {
            final Dimension minimizedSize = cDockable.getMinimizedSize();

            logger.debug("Minimize Size Changed: {}, {}", dockableAndProps, minimizedSize);

            dockableAndProps.setMinimizedSize(minimizedSize);

            uiSpaceEvent.fire(makeResizeEvent(dockableAndProps, new Rectangle(-1, -1, minimizedSize.width, minimizedSize.height)));
        }
    }

    /**
     * Class to watch for focus changes.
     */
    private final class DockableFocusListener implements CFocusListener
    {
        private final DockableAndProps dockableAndProps;

        private DockableFocusListener(@Nonnull DockableAndProps dockableAndProps)
        {
            this.dockableAndProps = dockableAndProps;
        }

        @Override
        public void focusGained(@Nonnull CDockable dockable)
        {
            final String name = dockableAndProps.getName();

            uiSpaceEvent.fire(makeFocusEvent(dockableAndProps, true));
            if (logger.isDebugEnabled())
            {
                CLocation baseLoc = dockable.getBaseLocation();
                String baseLocName = baseLoc == null ? null : baseLoc.getClass().getSimpleName();
                if (logger.isDebugEnabled()) logger.debug("Focus Gained: {}, {}", name, baseLocName);
            }

            // Request focus for the component if it is focusable
            final Component component = dockableAndProps.getComponent();
            if (component.isFocusable()) component.requestFocus();
        }

        @Override
        public void focusLost(CDockable dockable)
        {
            uiSpaceEvent.fire(makeFocusEvent(dockableAndProps, false));
            if (logger.isDebugEnabled()) logger.debug("Focus Lost: {}", dockableAndProps.getName());
        }
    }
}
