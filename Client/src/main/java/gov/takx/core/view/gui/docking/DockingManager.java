package gov.takx.core.view.gui.docking;

import bibliothek.extension.gui.dock.theme.EclipseTheme;
import bibliothek.extension.gui.dock.theme.eclipse.EclipseButtonBorder;
import bibliothek.gui.DockController;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.ScreenDockStation;
import bibliothek.gui.dock.action.DefaultActionOffer;
import bibliothek.gui.dock.action.DockAction;
import bibliothek.gui.dock.action.DockActionSource;
import bibliothek.gui.dock.action.FilteredDockActionSource;
import bibliothek.gui.dock.common.CContentArea;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CExternalizeArea;
import bibliothek.gui.dock.common.CGridArea;
import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.CMinimizeArea;
import bibliothek.gui.dock.common.MultipleCDockable;
import bibliothek.gui.dock.common.event.CDockableStateListener;
import bibliothek.gui.dock.common.event.CVetoClosingEvent;
import bibliothek.gui.dock.common.event.CVetoClosingListener;
import bibliothek.gui.dock.common.group.CGroupBehavior;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.intern.DefaultCDockable;
import bibliothek.gui.dock.common.intern.DefaultCommonDockable;
import bibliothek.gui.dock.common.location.CBaseLocation;
import bibliothek.gui.dock.common.location.CGridAreaLocation;
import bibliothek.gui.dock.common.mode.ExtendedMode;
import bibliothek.gui.dock.common.theme.ThemeMap;
import bibliothek.gui.dock.control.focus.DefaultFocusStrategy;
import bibliothek.gui.dock.control.focus.FocusStrategyRequest;
import bibliothek.gui.dock.facile.mode.NormalMode;
import bibliothek.gui.dock.layout.DockableProperty;
import bibliothek.gui.dock.station.flap.FlapDockHoldToggle;
import bibliothek.gui.dock.station.flap.button.ButtonContent;
import bibliothek.gui.dock.station.flap.button.ButtonContentCondition;
import bibliothek.gui.dock.station.screen.window.DefaultScreenDockWindowFactory;
import bibliothek.gui.dock.station.screen.window.WindowConfiguration;
import bibliothek.gui.dock.themes.ThemeManager;
import bibliothek.gui.dock.title.DockTitle;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeAndWaitOnEDT;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.project.ProjectClosed;
import gov.takx.api.event.project.ProjectOpened;
import gov.takx.api.event.project.ProjectSaving;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.docking.DockingArea;
import gov.takx.api.ui.docking.DockingPortNotFoundException;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.core.globe.GlobeDockable;
import gov.takx.core.globe.GlobeDockableFactory;
import gov.takx.core.launcher.ApplicationLauncher;
import gov.takx.core.view.gui.RectPainter;
import gov.takx.platform.ui.ComponentDescriptor;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static bibliothek.gui.dock.FlapDockStation.BUTTON_CONTENT;
import static bibliothek.gui.dock.FlapDockStation.BUTTON_CONTENT_FILTER;
import static bibliothek.gui.dock.FlapDockStation.Direction.EAST;
import static bibliothek.gui.dock.FlapDockStation.Direction.NORTH;
import static bibliothek.gui.dock.FlapDockStation.Direction.WEST;
import static gov.takx.core.project.ServiceOrdering.START_DockingSystem;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;
import static gov.takx.platform.preferences.PreferenceEntry.DOCKING_FRAMES_DRAG_AND_DROP_TO_EXTERNALIZE_ENABLED;
import static gov.takx.platform.preferences.PreferenceEntry.DOCKING_FRAMES_FRAME_MAXIMIZATION_ENABLED;
import static gov.takx.platform.preferences.PreferenceEntry.DOCKING_FRAMES_MAXIMIZE_ON_DOUBLE_CLICK_ENABLED;
import static gov.takx.platform.preferences.PreferenceEntry.DOCKING_FRAMES_MINIMIZE_AREAS_ENABLED;

/**
 * This class handles all the docking setup and configuration on the main app frame.
 *
 * @since 2.25
 */
@ApplicationScoped
public class DockingManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ApplicationLauncher applicationLauncher;
    private final boolean headless;
    private final DockableListenerManager dockableListenerManager;
    private final PinnedDockableManager pinnedDockableManager;
    private final IExtendedGlobeManager globeManager;
    private final PlaybackBorderModifierBridge playbackBorderModifierBridge;
    private final DockingLayoutHandler layoutHandler;
    private final ComponentDockableFactory componentDockableFactory;
    private final GlobeDockableFactory globeDockableFactory;
    private final DockingStateNotifier dockingStateNotifier;

    private CControl dockControl;
    private CGridArea centerGridArea;
    private DockingColorScheme dockingColorScheme;
    private JPanel dockingContentPanel;
    private GlobeDockable primaryGlobeDockable;

    private CMinimizeArea eastArea;
    private CMinimizeArea westArea;
    private CMinimizeArea southArea;

    private boolean layoutLoadSuccessful;
    private boolean initialVisibility;

    private final Map<String, CContentArea> dockingAreas = new HashMap<>();
    private final Set<DockableAndProps> componentDockables = new HashSet<>();

    // Track all GlobeDockables by id.
    private final Map<Integer, GlobeDockable> globeDockablesById = new HashMap<>();

    private final GlobeClosingListener globeClosingListener = new GlobeClosingListener();

    @Inject
    public DockingManager(ApplicationLauncher applicationLauncher, @Headless boolean headless, DockableListenerManager dockableListenerManager,
                          PinnedDockableManager pinnedDockableManager, IExtendedGlobeManager globeManager,
                          PlaybackBorderModifierBridge playbackBorderModifierBridge, DockingLayoutHandler layoutHandler,
                          ComponentDockableFactory componentDockableFactory, GlobeDockableFactory globeDockableFactory,
                          DockingStateNotifier dockingStateNotifier)
    {
        this.applicationLauncher = applicationLauncher;
        this.headless = headless;
        this.dockableListenerManager = dockableListenerManager;
        this.pinnedDockableManager = pinnedDockableManager;
        this.globeManager = globeManager;
        this.playbackBorderModifierBridge = playbackBorderModifierBridge;
        this.layoutHandler = layoutHandler;
        this.componentDockableFactory = componentDockableFactory;
        this.globeDockableFactory = globeDockableFactory;
        this.dockingStateNotifier = dockingStateNotifier;
    }

    /**
     * @return The panel that holds our docking content
     */
    public JPanel getDockingContentPanel()
    {
        return dockingContentPanel;
    }

    /**
     * @return The dimensions of the center dockable grid area
     */
    public Dimension getCenterAreaDimension()
    {
        return centerGridArea.getComponent().getSize();
    }

    /**
     * Find a dockable where the DockableAndProps entry matches the given filter.
     *
     * @param filter the Predicate for the filter
     * @return the DockableAndProps or {@code null}
     */
    @Nullable
    public DockableAndProps findDockableAndProps(Predicate<DockableAndProps> filter)
    {
        return componentDockables.stream().filter(filter).findFirst().orElse(null);
    }

    /**
     * Find a dockable where the DockableAndProps {@code dockable} is equal to the given dockable.
     *
     * @param dockable Dockable to match
     * @return the DockableAndProps or {@code null}
     */
    @Nullable
    public DockableAndProps findDockableAndProps(ComponentDockable dockable)
    {
        return findDockableAndProps(dap -> dap.hasDockable() && dap.requireDockable().equals(dockable));
    }

    /**
     * Find a DockableAndProps where the dockableProps component equals the given component.
     *
     * @param component Component to search against
     * @return DockableAndProps with that component or {@code null}
     */
    @Nullable
    public DockableAndProps findDockableAndProps(Component component)
    {
        return findDockableAndProps(dap -> dap.getComponent().equals(component));
    }

    /**
     * Find a DockableAndProps where the descriptor has the given id.
     *
     * @param id The descriptor id to search against
     * @return DockableAndProps with that descriptor id or {@code null}
     */
    @Nullable
    public DockableAndProps findDockableAndPropsById(@Nonnull String id)
    {
        return findDockableAndProps(dap -> id.equals(dap.getId()));
    }

    /**
     * Find a DockableAndProps where the descriptor has the given name.
     *
     * @param name The name to search against
     * @return DockableAndProps with that descriptor name or {@code null}
     */
    @Nullable
    public DockableAndProps findDockableAndPropsByName(@Nonnull String name)
    {
        return findDockableAndProps(dap -> name.equals(dap.getName()));
    }

    /**
     * Find all DockableAndProps entries where the dockable is non-null.
     *
     * @return Stream of DockableAndProps that have a non-null dockable.
     * @since 3.4
     */
    @Nonnull
    public Stream<DockableAndProps> findDockableAndPropsWithDockable()
    {
        return matchDockableAndProps(DockableAndProps::hasDockable);
    }

    /**
     * Find all DockableAndProps entries that match the given filter.
     *
     * @param filter the Predicate for the filter
     * @return Stream of matching DockableAndProps
     */
    @Nonnull
    public Stream<DockableAndProps> matchDockableAndProps(Predicate<DockableAndProps> filter)
    {
        return componentDockables.stream().filter(filter);
    }

    /**
     * Search the componentDockables to find one that matches the given filter and then return the dockable from
     * that tuple, which may be {@code null}.
     *
     * @param filter The predicate to match against DockableAndProps instances
     * @return The dockable from the matching DockableAndProps or {@code null}
     */
    @Nullable
    public ComponentDockable findComponentDockable(Predicate<DockableAndProps> filter)
    {
        return matchDockableAndProps(filter).findFirst().map(DockableAndProps::getDockable).orElse(null);
    }

    /**
     * Scan all our dockables, and if it is externalized, then re-dock it.
     *
     * @since 5.8
     */
    public void reDockAllExternalizedComponents()
    {
        // All side panels
        componentDockables.forEach(dap -> {
            final var dockable = dap.getDockable();

            if (dockable != null && dockable.getExtendedMode() == ExtendedMode.EXTERNALIZED)
            {
                relocateWindowIfNeeded(dap);
                dockable.setExtendedMode(getRestoreLocation(dockable)); // Normalize or minimize the component
            }
        });

        // Now all globes, since they are tracked separately
        globeDockablesById.values().forEach(dockable -> {
            if (dockable.getExtendedMode() == ExtendedMode.EXTERNALIZED) dockable.setExtendedMode(ExtendedMode.NORMALIZED);
        });
    }

    /**
     * If the window containing the given component is not located on any screen device, then force it back onto the
     * screen that is showing TAKX.
     * 
     * @since 5.8
     */
    private static void relocateWindowIfNeeded(DockableAndProps dap)
    {
        final Component component = dap.getComponent();

        if (ComponentUtils.isComponentWindowOutsideAllDisplays(component))
        {
            final Rectangle componentBounds = SwingUtilities.windowForComponent(component).getBounds();

            final var windowBounds = ComponentUtils.getActiveMonitorScreenBounds();
            componentBounds.x = windowBounds.x;
            componentBounds.y = windowBounds.y;

            dap.setExternalizedLocation(componentBounds);
            dap.requireDockable().setLocation(dap.getExternalizedLocation());
        }
    }

    /**
     * Determine where to re-dock a component.  If it has a "normal" mode entry in the history, we will restore it
     * as {@link ExtendedMode#NORMALIZED}, else {@link ExtendedMode#MINIMIZED}.
     *
     * @since 5.8
     */
    private static ExtendedMode getRestoreLocation(ComponentDockable dockable)
    {
        final var history = dockable.getControl().getLocationManager().getPropertyHistory(dockable.intern());
        final boolean hasNormalizedLocation = history.stream().anyMatch(location -> NormalMode.IDENTIFIER.equals(location.getMode()));
        return hasNormalizedLocation ? ExtendedMode.NORMALIZED : ExtendedMode.MINIMIZED;
    }

    /**
     * Create the content area for the given unique identifier.
     *
     * @param id Unique identifier of content area
     * @return New content area
     * @throws IllegalArgumentException if id is not unique
     */
    @Nonnull
    public CContentArea createContentArea(@Nonnull String id)
    {
        final CContentArea contentArea = dockControl.createContentArea(id);
        dockingAreas.put(id, contentArea);
        return contentArea;
    }

    /**
     * Create the dockable for the primary globe and add it to the center grid.
     */
    public void addPrimaryGlobe()
    {
        primaryGlobeDockable = createAndAddGlobeDockable(globeManager.getPrimaryGlobeDelegate(), "Primary Globe");
    }

    /**
     * Show the primary globe dockable.
     */
    public void showPrimaryGlobe()
    {
        primaryGlobeDockable.toFront();
        primaryGlobeDockable.getGlobe().getWorldWindow().getComponent().requestFocusInWindow();
    }

    /**
     * Create and add a globe dockable to the center stack dock station.
     *
     * @param globe Globe for which to create dockable
     */
    @Nonnull
    public GlobeDockable createAndAddGlobeDockable(@Nonnull IApplicationGlobe globe, @Nullable String title)
    {
        final GlobeDockable globeDockable = createGlobeDockable(globe);

        if (title != null) globeDockable.setTitleText(title);

        dockControl.addDockable(globeDockable);

        // If this is the primary globe (first dockable), then use the base location.  Otherwise, put the
        // globe in the right side of the dock area.
        if (dockControl.getCDockableCount() > 0)
        {
            globeDockable.setLocation(new CBaseLocation().normalEast(.5));
        } else
        {
            globeDockable.setLocation(CLocation.base());
        }

        // Show it and give it the focus
        globeDockable.setVisible(initialVisibility);
        globeDockable.getGlobe().getWorldWindow().getComponent().requestFocusInWindow();

        return globeDockable;
    }

    @Nonnull
    private GlobeDockable createGlobeDockable(@Nonnull IApplicationGlobe globe)
    {
        final GlobeDockable globeDockable = new GlobeDockable(globeDockableFactory);
        globeDockable.addGlobe(globe);
        addGlobeDockable(globeDockable);
        return globeDockable;
    }

    /**
     * Process a new globe dockable created directly or when restoring the layout.
     *
     * @param globeDockable dockable to process
     * @since 5.1
     */
    public void addGlobeDockable(GlobeDockable globeDockable)
    {
        globeDockablesById.put(globeDockable.getGlobe().getGlobeId(), globeDockable);
        globeDockable.addVetoClosingListener(globeClosingListener);
    }

    /**
     * Remove a globe dockable component from the center panel.
     *
     * @param globeId The Id of the globe dockable component to remove.
     */
    public void removeGlobeDockableById(int globeId)
    {
        // The dock may have been destroyed already. If so, no need to remove anything, so just ignore the request.
        if (dockControl == null) return;

        final GlobeDockable globeDockable = globeDockablesById.get(globeId);
        if (globeDockable != null)
        {
            globeDockable.removeVetoClosingListener(globeClosingListener);
            dockControl.removeDockable(globeDockable);
        }
    }

    /**
     * Create the dockable and properties for the given component. Register it with the controller.
     *
     * @param component Component for which to get/create a dockable
     * @param descriptor descriptor of the dockable
     * @return newly created DockableAndProps, or originalDockableAndProps if it was passed as non-null
     */
    @Nonnull
    public DockableAndProps createDockable(@Nonnull final Component component, @Nonnull ComponentDescriptor descriptor)
    {
        final ComponentDockable dockable = createComponentDockable(component, descriptor);

        final DockableAndProps originalDockableAndProps = findDockableAndPropsById(descriptor.getId());

        final DockableAndProps dockableAndProps;
        if (originalDockableAndProps == null)
        {
            dockableAndProps = new DockableAndProps(component, descriptor, dockable);
            componentDockables.add(dockableAndProps);
        } else
        {
            dockableAndProps = originalDockableAndProps;
            dockableAndProps.setDockable(dockable);
        }

        dockable.setMinimizedSize(dockableAndProps.getMinimizedSize());

        dockableListenerManager.addDockableListeners(dockable, dockableAndProps);

        return dockableAndProps;
    }

    /**
     * Create a dockable to display the provided component in the UI.
     *
     * @param component Component for which to create a dockable
     * @param descriptor The descriptor of the component
     * @return 3.4
     */
    @Nonnull
    private ComponentDockable createComponentDockable(@Nonnull final Component component, @Nonnull ComponentDescriptor descriptor)
    {
        final String name = descriptor.getName();
        pinnedDockableManager.setPinHidden(name, descriptor.isHidePin());
        Component componentToDock = component;
        if (descriptor.isParentScrollingEnabled())
        {
            final JScrollPane scrollPane = new JScrollPane(component);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getHorizontalScrollBar().setUnitIncrement(10);
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getVerticalScrollBar().setUnitIncrement(10);
            componentToDock = scrollPane;
        }

        final ComponentDockable dockable = descriptor.isPlaybackAware()
                ? new PlaybackAwareComponentDockable(componentToDock)
                : new ComponentDockable(componentDockableFactory, componentToDock);

        // Try to give the dockable a reasonable title
        if (!name.isEmpty()) dockable.setTitleText(name);
        dockable.setCloseable(descriptor.isCloseable());
        dockable.setExternalizable(descriptor.isExternalizable());

        logger.debug("Creating dockable: {}", dockable);

        return dockable;
    }

    /**
     * Create (if necessary) a dockable from the given component and place it in the given docking area if it is newly
     * created. Mark the dockable as visible, closeable, and minimizable.
     *
     * @param component Component from which to create the dockable
     * @param descriptor descriptor for the dockable
     */
    public void dockComponent(@Nonnull Component component, @Nonnull ComponentDescriptor descriptor)
    {
        // Retrieve the dockable using the id, which will be the same, since there's a chance this is a new instance
        // of the component.
        DockableAndProps dockableAndProps = findDockableAndPropsById(descriptor.getId());
        if (null != dockableAndProps && dockableAndProps.getComponent() != component)
        {
            // This is a stale version of the component, so replace it. This can happen when a new project is opened
            // while the application is running since MainAppPanel adds the panels on project opened and most panels are
            // recreated for the new project.
            dockableAndProps.setComponent(component);
            if (dockableAndProps.hasDockable())
            {
                dockControl.removeDockable(dockableAndProps.getDockable());
                dockableAndProps.setDockable(null);
            }
        }

        if (dockableAndProps == null || !dockableAndProps.hasDockable())
        {
            if (dockableAndProps == null) dockableAndProps = new DockableAndProps(component, descriptor, null);
            createAndPositionDockable(descriptor.getComponentPosition(), dockableAndProps);
        }
    }

    /**
     * Creates a dockable (if necessary) for a component that was previously added to the docking framework.
     *
     * @param dockableAndProps The properties of the dockable that needs to be set up
     * @since 3.4
     */
    public void restoreComponent(@Nonnull DockableAndProps dockableAndProps)
    {
        if (!dockableAndProps.hasDockable())
        {
            createAndPositionDockable(dockableAndProps.getComponentPosition(), dockableAndProps);
        }
    }

    /**
     * Sets up a dockable in the specified position. If the globe has not been initialized at this point,
     * the dockable will be placed in a queue to be processed later. This is the ideal method to call for
     * any delayed processing of a dockable.
     *
     * @param componentPosition The position for the dockable
     * @param dockableAndProps The properties of the dockable that needs to be set up
     */
    private void createAndPositionDockable(@Nonnull ComponentPosition componentPosition, @Nonnull DockableAndProps dockableAndProps)
    {
        switch (componentPosition)
        {
            case CENTER, GLOBE -> dockableAndProps = addComponentToCenterDock(dockableAndProps);
            case BOTTOM, RIGHT, LEFT -> dockableAndProps = addComponentToFlapDock(dockableAndProps);
            default -> throw new IllegalStateException("Unknown componentPosition: " + componentPosition);
        }

        final ComponentDockable dockable = dockableAndProps.requireDockable();
        pinnedDockableManager.handleNewDockable(dockableAndProps);
        dockingColorScheme.setupDockableColors(dockable);
    }

    /**
     * Docks the specified {@code Component} relative to an existing {@code parent} component.
     * <p>
     * The parent must be currently docked; otherwise, this method will throw an {@code IllegalArgumentException}.
     * The specified {@code ratio} sets the proportional size of the child relative to the parent.
     *
     * @param descriptor The descriptor to uniquely identify and specify initial properties of the component
     * @param component  The {@code Component} to be docked
     * @param parent     The {@code Component} used as a reference point for docking
     * @param position   The position of the child {@code Component} relative to the parent
     * @param ratio      The proportional size of the child relative to the parent in the range (0, 1)
     * @throws IllegalArgumentException if {@code parent} is not already docked
     */
    public void dockChildComponent(@Nonnull ComponentDescriptor descriptor, @Nonnull Component component,
                                   @Nonnull Component parent, ComponentPosition position, float ratio)
    {
        final MultipleCDockable parentDockable = findComponentDockable(dap -> dap.getComponent().equals(parent));

        if (parentDockable == null)
        {
            final String msg = "The second component argument to dockComponents must already be docked.";

            logger.error(msg);
            throw new IllegalArgumentException(msg);
        }

        final DockableAndProps dockableAndProps = getOrCreateDockable(descriptor, component);
        final ComponentDockable dockable = dockableAndProps.requireDockable();

        final CGridAreaLocation gridAreaLocation = CLocation.normalized((CGridArea) dockControl.getStation(DockingArea.CENTER.getDockingAreaId()));
        final DockableProperty location = switch (position)
        {
            case BOTTOM -> gridAreaLocation.south(ratio).findProperty();
            case RIGHT -> gridAreaLocation.east(ratio).findProperty();
            case LEFT -> gridAreaLocation.west(ratio).findProperty();
            case CENTER, GLOBE -> parentDockable.getBaseLocation().findProperty();
        };

        parentDockable.getParentStation().getStation().asDockStation().drop(dockable.intern(), location);
        dockingColorScheme.setupDockableColors(dockable);
        dockable.setVisible(initialVisibility);
    }

    /**
     * Docks the specified {@code Component} to the docking port identified by {@code portIdentifier}.
     *
     * @param portIdentifier Identifier of the docking port
     * @param descriptor     The descriptor to uniquely identify and specify initial properties of the component
     * @param component      The {@code Component} to be added
     * @throws DockingPortNotFoundException if no docking port with the {@code portIdentifier} exists
     */
    public void addComponentToDockingPort(@Nonnull String portIdentifier, @Nonnull ComponentDescriptor descriptor,
                                          @Nonnull Component component)
            throws DockingPortNotFoundException
    {
        final CContentArea area = dockingAreas.get(portIdentifier); // retrieve the panel

        if (area == null)
        {
            throw new DockingPortNotFoundException("The docking port with identifier " + portIdentifier + " was not found.");
        }

        final DockableAndProps dockableAndProps = getOrCreateDockable(descriptor, component);
        area.getCenter().drop(dockableAndProps.requireDockable().intern());
    }

    /**
     * Changes the displayed size of a dockable. The width or height may solely be used from the preferred size
     * depending on if the panel is externalized or depending on which side the dockable is stationed on.
     * @param component The component to update
     * @param preferredSize The desired width and height of the component.
     */
    public void setDockableSize(@Nonnull Component component, @Nonnull Dimension preferredSize)
    {
        DockableAndProps dockableAndProps = findDockableAndProps(component);
        if (dockableAndProps == null)
        {
            logger.warn("No dockable found for component {}; size will not be updated", component);
            return;
        }

        pinnedDockableManager.setDockableSize(dockableAndProps, preferredSize);
    }

    /**
     * Get or create the dockable for the given component. Register it with the controller.
     *
     * @param cd The descriptor for the component
     * @param component Component for which to get/create a dockable
     * @return Dockable for component
     */
    @Nonnull
    private DockableAndProps getOrCreateDockable(@Nonnull final ComponentDescriptor cd, @Nonnull final Component component)
    {
        // Retrieve the dockable using the id, which will be the same, since there's a chance this is a new instance
        // of the component.
        DockableAndProps dockableAndProps = findDockableAndPropsById(cd.getId());

        if (dockableAndProps == null || !dockableAndProps.hasDockable())
        {
            dockableAndProps = createAndAddDockable(component, cd);
        }

        return dockableAndProps;
    }

    /**
     * Add a given component to the center stack dock station (same as primary globe).  If for some reason the
     * dock control (which doesn't get initialized until loadState) is not initialized yet, add the component to a
     * queue so that it can be added to the center dock on initialization
     *
     * @param dockableAndProps Component to add to stack
     */
    @Nonnull
    private DockableAndProps addComponentToCenterDock(@Nonnull DockableAndProps dockableAndProps)
    {
        dockableAndProps = createInitializeAndAddDockable(dockableAndProps);

        ComponentDockable dockable = dockableAndProps.requireDockable();

        if (dockableAndProps.getComponentPosition() == ComponentPosition.GLOBE)
        {
            dockable.setLocation(primaryGlobeDockable.getBaseLocation());
        } else
        {
            dockable.setLocation(new CBaseLocation().normalEast(.5));
        }

        dockable.setRemoveOnClose(true);
        dockable.setVisible(initialVisibility);

        return dockableAndProps;
    }

    /**
     * @return Backing dock control.
     * @since 5.6
     */
    @Nonnull
    public CControl getDockControl()
    {
        return dockControl;
    }

    @InvokeOnEDT
    public void loadLayout()
    {
        dockableListenerManager.setSuppressEvents(true);
        layoutHandler.loadLayout(dockControl);
        dockableListenerManager.setSuppressEvents(false);

        // TODO: fix DockingFrames layout persistence bug to avoid the need to reset these
        // Unfortunately, setting auto to false in the <window auto="false" direction=.../> element
        // seems to have no effect.
        setDirections();

        layoutLoadSuccessful = true;
    }

    @Nonnull
    private DockableAndProps addComponentToFlapDock(@Nonnull DockableAndProps dockableAndProps)
    {
        dockableAndProps = createInitializeAndAddDockable(dockableAndProps);
        ComponentPosition panelPos = dockableAndProps.getComponentPosition();
        CMinimizeArea min = (CMinimizeArea) dockControl.getStation(panelPos.getComponentPositionId());
        final ComponentDockable dockable = dockableAndProps.requireDockable();

        dockable.setLocation(CLocation.minimized(min));
        dockable.setVisible(initialVisibility);
        return dockableAndProps;
    }

    /**
     * Create the DAP, the dockable, and add the dockable based on the input DAP.
     * <p>
     * Note: the returned DAP is a new instance, and the input DAP is not updated.  So, if you need access
     * to the new dockable (for instance), you'll need to inspect the returned DAP.
     *
     * @param dockableAndProps DAP describing the dockable to create
     * @return DAP of instantiated dockable
     */
    @Nonnull
    private DockableAndProps createInitializeAndAddDockable(@Nonnull DockableAndProps dockableAndProps)
    {
        final ComponentDescriptor descriptor = dockableAndProps.getDescriptor();
        dockableAndProps = createAndAddDockable(dockableAndProps.getComponent(), descriptor);

        final ComponentDockable dockable = dockableAndProps.requireDockable();

        dockable.setCloseable(descriptor.isCloseable());
        dockable.setMinimizable(true);

        final URL url = descriptor.getIconUrl();
        dockable.setTitleIcon(UIResources.getStandardSizedIcon(url));

        return dockableAndProps;
    }

    /**
     * Create the dockable and add it to the dockControl.
     *
     * @param component  the component to put in the dockable
     * @param descriptor the descriptor of the dockable
     * @return newly created DockableAndProps, or originalDockableAndProps if it was passed as non-null
     */
    @Nonnull
    private DockableAndProps createAndAddDockable(@Nonnull final Component component, @Nonnull ComponentDescriptor descriptor)
    {
        DockableAndProps dockableAndProps = createDockable(component, descriptor);
        dockControl.addDockable(dockableAndProps.requireDockable());
        return dockableAndProps;
    }

    /**
     * Initialize the docking system.
     */
    private void startService(@Observes @Priority(START_DockingSystem) StartProjectServices ignored)
    {
        if (!headless) initDockStation();
    }

    /**
     * Once a project is opened at least once we will make newly docked items visible by default.
     * This addresses the <i>"UI reload flash"</i> by only making docked components visible afterward.
     */
    private void onProjectOpened(@Observes ProjectOpened ignored)
    {
        initialVisibility = true;
    }

    /**
     * When a project is closed we reset {@link #initialVisibility} to reduce cross-project load flashing.
     */
    private void onProjectClosed(@Observes ProjectClosed ignored)
    {
        initialVisibility = false;
    }

    /**
     * Remove the given dockable.
     *
     * @param dap the DockableAndProps object
     */
    public void removeDockable(@Nullable DockableAndProps dap)
    {
        if (dap != null)
        {
            if (dap.hasDockable() && dockControl != null)
            {
                final ComponentDockable dockable = dap.requireDockable();
                if (logger.isDebugEnabled()) logger.debug("Removing dockable: {}", dockable.getName());

                dockControl.removeDockable(dockable);
                dockingStateNotifier.removeDockable(dockable);
                pinnedDockableManager.onRemoveDockable(dockable);
                dockableListenerManager.removeDockableListeners(dockable);
                dap.setDockable(null);
            }
        }
    }

    /**
     * Initialize the central docking station components.
     */
    @InvokeOnEDT
    private void initDockStation()
    {
        if (dockControl != null) return;

        final Boolean frameMaximizationEnabled = DOCKING_FRAMES_FRAME_MAXIMIZATION_ENABLED.getBooleanValue();
        final boolean dragAndDropToExternalizeEnabled = DOCKING_FRAMES_DRAG_AND_DROP_TO_EXTERNALIZE_ENABLED.getBooleanValue();

        // I don't like the deprecated method here either, but there does not appear to be an alternative
        //noinspection deprecation
        DockController.disableCoreWarning();
        dockControl = new CControl(applicationLauncher.getAppFrame())
        {
            @Override
            public void replace(MultipleCDockable oldDockable, MultipleCDockable newDockable)
            {
                // Weird docking state where we are trying to replace one component with another
                // and both already exist and are laid out in the UI. We can't remove a dockable
                // because then its inaccessible later. We can't let the replacement go through because
                // then the underlying docking library throws an exception. So far it looks like
                // just bailing in this circumstance is fine. The layout loads back to its expected state.
                // Nothing appears to be missing. Nothing extra exists either. More context at: TAKX-4123
                if (newDockable.getControl() == this) return;

                // The base implementation doesn't have this redundancy check which causes pointless extra work.
                if (oldDockable != newDockable) super.replace(oldDockable, newDockable);
            }
        };

        DockController dockController = dockControl.getController();

        // Addresses a bug caused by multiple attempts to open and close a panel in quick succession - RAPTORX-580
        dockController.getFocusController().setStrategy(new DockFocusStrategy(dockController));

        dockingColorScheme = new DockingColorScheme(dockControl);

        // Install our dockable factories
        dockControl.addMultipleDockableFactory("globes", globeDockableFactory);
        dockControl.addMultipleDockableFactory("dockables", componentDockableFactory);

        // Set externalized windows to use JFrames rather than JDialogs; this allows them to be treated as real windows
        // by the OS's window manager, e.g., so you can minimize to the task bar, alt-tab between windows, etc.
        DefaultScreenDockWindowFactory windowFactory = new DefaultScreenDockWindowFactory();
        windowFactory.setKind(DefaultScreenDockWindowFactory.Kind.DIALOG);
        //windowFactory.setUndecorated(false);
        dockControl.putProperty(ScreenDockStation.WINDOW_FACTORY, windowFactory);

        // Initially center externalized windows
        dockControl.getLocationManager().getExternalizedMode().setBehavior(new ExternalizedModeLocationBehavior());

        dockControl.putProperty(ScreenDockStation.WINDOW_CONFIGURATION, (screenDockStation, dockable) -> {
            WindowConfiguration config = new WindowConfiguration();
            config.setMoveOnTitleGrab(true);
            config.setMoveOnBorder(dragAndDropToExternalizeEnabled);
            config.setAllowDragAndDropOnTitle(true);
            config.setResetOnDropable(false);

            // If drag and drop is disabled, there is no reason to draw the border buttons for moving and resizing
            if (!dragAndDropToExternalizeEnabled)
            {
                config.setBorderFactory(EmptyScreenDockWindowBorder::new);
            }

            return config;
        });

        if (!frameMaximizationEnabled)
        {
            // Only support maximizing dockables that have been externalized
            dockControl.addStateListener(new CDockableStateListener()
            {
                @Override
                public void visibilityChanged(CDockable dockable)
                {
                    // Ignore
                }

                @Override
                public void extendedModeChanged(CDockable dockable, ExtendedMode mode)
                {
                    if (dockable instanceof DefaultCDockable cDockable)
                    {
                        cDockable.setMaximizable(cDockable.getParentStation() instanceof CExternalizeArea);
                    }
                }
            });
        }

        if (!dragAndDropToExternalizeEnabled)
        {
            // Prevent drag and drop from working between the main application and the screen dock station
            dockController.getRelocator().addVetoableDockRelocatorListener(new NoExternalizeDragAndDropRelocatorAdapter());
        }

        if (!frameMaximizationEnabled || !DOCKING_FRAMES_MAXIMIZE_ON_DOUBLE_CLICK_ENABLED.getBooleanValue())
        {
            // Prevent double click from maximizing a panel by returning true to indicate the event is used up and should
            // not be processed further
            dockControl.addDoubleClickListener((source, event) -> true);
        }

        dockingContentPanel = new JPanel(new BorderLayout());

        // Add a little space above the center area, where the north minimize area use to be
        dockingContentPanel.setBorder(new EmptyBorder(4, 0, 0, 0));
        centerGridArea = dockControl.createGridArea(DockingArea.CENTER.getDockingAreaId());
        dockingContentPanel.add(centerGridArea.getComponent(), BorderLayout.CENTER);

        southArea = dockControl.createMinimizeArea(DockingArea.SOUTH.getDockingAreaId());
        eastArea = dockControl.createMinimizeArea(DockingArea.EAST.getDockingAreaId());
        westArea = dockControl.createMinimizeArea(DockingArea.WEST.getDockingAreaId());
        setDirections();

        dockingContentPanel.add(southArea, BorderLayout.SOUTH);
        dockingContentPanel.add(eastArea, BorderLayout.EAST);
        dockingContentPanel.add(westArea, BorderLayout.WEST);

        // If preference is disabled, set "minimize areas" to zero width, effectively hiding them
        if (!DOCKING_FRAMES_MINIMIZE_AREAS_ENABLED.getBooleanValue())
        {
            westArea.setPreferredSize(new Dimension(0, westArea.getPreferredSize().height));
            eastArea.setPreferredSize(new Dimension(0, eastArea.getPreferredSize().height));
            southArea.setPreferredSize(new Dimension(southArea.getPreferredSize().width, 0));
        }

        // Add a custom action filter which will only show the hold toggle (pin) action
        dockControl.putProperty(BUTTON_CONTENT_FILTER, dockAction -> dockAction instanceof FlapDockHoldToggle);

        // Specify which content to display on the button flaps (we want to hide the knob and filter the actions)
        dockControl.putProperty(BUTTON_CONTENT, createCustomDockableFlapContent());

        // Hide the standard sticky button
        dockController.setDefaultActionOffer(new DefaultActionOffer()
        {
            @Override
            public DockActionSource getSource(Dockable dockable, DockActionSource source, DockActionSource[] guards, DockActionSource parent, DockActionSource[] parents)
            {
                DockActionSource all = super.getSource(dockable, source, guards, parent, parents);
                return new FilteredDockActionSource(all)
                {
                    @Override
                    protected boolean include(DockAction action)
                    {
                        return !(action instanceof FlapDockHoldToggle);
                    }
                };
            }
        });

        // Add our custom sticky button
        pinnedDockableManager.installPinButtons(dockControl);

        dockControl.setTheme(ThemeMap.KEY_ECLIPSE_THEME); //ThemeMap.KEY_FLAT_THEME); // TODO KMB: Swap for TAKX-1008

        dockControl.putProperty(EclipseTheme.TAB_PAINTER, RectPainter.FACTORY); // TODO KMB: Remove for TAKX-1008

        // Custom eclipse theme connector, which simply allows us to hide the extraneous tab when a dockable is externalized
        dockControl.putProperty(EclipseTheme.THEME_CONNECTOR, new ExternalizedTitleBarThemeConnector(dockControl)); // TODO KMB: Remove for TAKX-1008

        // Update the border on the minimized dockable flap to be square instead of rounded at the bottom
        ThemeManager themeManager = dockController.getThemeManager();
        themeManager.setBorderModifier(ThemeManager.BORDER_MODIFIER + ".title.eclipse.button.flat", border -> { // TODO KMB: Remove for TAKX-1008
            if (border instanceof CompoundBorder compoundBorder)
            {
                if (compoundBorder.getOutsideBorder() instanceof EclipseButtonBorder eclipseButtonBorder)
                {
                    eclipseButtonBorder.setRoundEdges(0);
                }
            }
            return border;
        });

        // Install the UI bridge to handle border changes when entering/leaving playback mode
        playbackBorderModifierBridge.registerBridge(themeManager);

        // Don't move entire stacks, just the topmost dockable
        dockControl.setGroupBehavior(CGroupBehavior.TOPMOST);

        dockControl.putProperty(DockTitle.ORIENTATION_STRATEGY, new RotationStrategy());
    }

    /**
     * Ensure the flap dock panels always open to the interior of the frame
     */
    private void setDirections()
    {
        southArea.setDirection(NORTH);
        eastArea.setDirection(WEST);
        westArea.setDirection(EAST);
    }

    /**
     * Creates a custom set of content for the minimized dockable flaps (the buttons that represent the minimized
     * dockable).  The custom content hides the knob and allows the actions to be filtered.
     *
     * @return The custom content for a dockable flap
     */
    @Nonnull
    private ButtonContent createCustomDockableFlapContent()
    {
        ButtonContentCondition knob = ButtonContent.FALSE;
        ButtonContentCondition icon = ButtonContent.IF_DOCKABLE;
        ButtonContentCondition text = ButtonContent.IF_DOCKABLE;
        ButtonContentCondition children = ButtonContent.IF_STATION;
        ButtonContentCondition actions = ButtonContent.TRUE;
        ButtonContentCondition filterActions = ButtonContent.TRUE;

        return new ButtonContent(knob, icon, text, children, actions, filterActions);
    }

    /**
     * Save the layout when the project is being saved.
     */
    @InvokeAndWaitOnEDT
    private void saveLayout(@Observes(notifyObserver = Reception.IF_EXISTS) ProjectSaving ignored)
    {
        if (headless) return; // Nothing to do when headless
        // Only save layout if project was started successfully (i.e. the last layout was successfully loaded),
        // since we might be in danger of saving a broken Docking.xml
        if (layoutLoadSuccessful) layoutHandler.saveLayout(dockControl);
    }

    /**
     * Clear our state when generic core services are stopping.
     */
    @InvokeAndWaitOnEDT
    private void stopService(@Observes(notifyObserver = Reception.IF_EXISTS) @Priority(STOP_GenericCoreService) StopProjectServices ignored)
    {
        if (headless) return; // Nothing to do when headless

        componentDockables.forEach(this::removeDockable);
        componentDockables.clear();

        globeDockablesById.clear();

        primaryGlobeDockable = null;

        if (dockControl != null)
        {
            dockControl.destroy();
            dockControl = null;
        }

        dockingAreas.clear();
    }

    /**
     * Extension of ComponentDockable that produces {@code PlaybackAwareDockable} instances.
     */
    private final class PlaybackAwareComponentDockable extends ComponentDockable
    {
        public PlaybackAwareComponentDockable(Component component)
        {
            super(componentDockableFactory, component);
        }

        /**
         * @return New PlaybackAwareComponentDockable
         */
        @Nonnull
        @Override
        protected DefaultCommonDockable createCommonDockable()
        {
            return new PlaybackAwareDockable(this, getClose());
        }
    }

    /**
     * Listener to handle globe closing events from the UI.  Prevent primary globe from being closed.
     */
    private final class GlobeClosingListener implements CVetoClosingListener
    {
        @Override
        public void closing(@Nonnull CVetoClosingEvent event)
        {
            if (event.getDockableCount() <= 0) return;

            // Should always be 1, but do a sanity check.
            final GlobeDockable globeDockable = (GlobeDockable) event.getDockable(0);
            final IApplicationGlobe globe = globeDockable.getGlobe();

            if (globe.isPrimaryGlobe())
            {
                event.cancel(); // Can't close the primary globe
            } else
            {
                int globeId = globe.getGlobeId();

                // The dockable has already been removed via the UI.
                globeDockablesById.remove(globeId);

                // Delete the globe.
                globeManager.deleteGlobe(globeId);
            }
        }

        @Override
        public void closed(CVetoClosingEvent event)
        {
        }
    }

    /**
     * Custom Dock Focus Strategy that prevents the Docking framework from caching the last focused dockable.
     * This addresses a bug resulting from multiple focus requests in quick succession from preventing a tab from being
     * opened until the tab loses focus. For additional details see RAPTORX-580.
     *
         * @since 2.31
     */
    private static final class DockFocusStrategy extends DefaultFocusStrategy
    {
        DockFocusStrategy(DockController dockController)
        {
            super(dockController);
        }

        @Override
        public Component getFocusComponent(FocusStrategyRequest request)
        {
            return null;
        }
    }
}
