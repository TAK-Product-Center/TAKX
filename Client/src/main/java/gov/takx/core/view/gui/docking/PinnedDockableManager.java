package gov.takx.core.view.gui.docking;

import bibliothek.gui.DockStation;
import bibliothek.gui.Dockable;
import bibliothek.gui.dock.FlapDockStation;
import bibliothek.gui.dock.FlapDockStation.Direction;
import bibliothek.gui.dock.action.ActionGuard;
import bibliothek.gui.dock.action.DefaultDockActionSource;
import bibliothek.gui.dock.action.DockAction;
import bibliothek.gui.dock.action.DockActionSource;
import bibliothek.gui.dock.action.LocationHint;
import bibliothek.gui.dock.action.actions.SimpleButtonAction;
import bibliothek.gui.dock.common.CControl;
import bibliothek.gui.dock.common.CGridArea;
import bibliothek.gui.dock.common.CLocation;
import bibliothek.gui.dock.common.CStation;
import bibliothek.gui.dock.common.event.CFocusListener;
import bibliothek.gui.dock.common.intern.AbstractCDockable;
import bibliothek.gui.dock.common.intern.CDockable;
import bibliothek.gui.dock.common.intern.CommonDockable;
import bibliothek.gui.dock.common.intern.station.CSplitDockStation;
import bibliothek.gui.dock.common.location.CContentAreaCenterLocation;
import bibliothek.gui.dock.common.location.CExternalizedLocation;
import bibliothek.gui.dock.common.location.CFlapIndexLocation;
import bibliothek.gui.dock.common.location.CMinimizeAreaLocation;
import bibliothek.gui.dock.common.location.CStackLocation;
import bibliothek.gui.dock.common.location.TreeLocationRoot;
import bibliothek.gui.dock.station.flap.button.ButtonContentAction;
import gov.takx.api.annotation.Headless;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.UserPreferenceType;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.ComponentUtils;
import gov.takx.core.globe.GlobeDockable;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.ComponentDescriptor;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.lang.invoke.MethodHandles;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import static bibliothek.gui.dock.FlapDockStation.Direction.EAST;
import static bibliothek.gui.dock.FlapDockStation.Direction.NORTH;
import static bibliothek.gui.dock.FlapDockStation.Direction.SOUTH;
import static bibliothek.gui.dock.FlapDockStation.Direction.WEST;
import static bibliothek.gui.dock.common.mode.ExtendedMode.MINIMIZED;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * This class handles the pinned/unpinned and auto-pin state of dockables.
 *
 * @since 2.25
 */
@ApplicationScoped
public class PinnedDockableManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ImageIcon PIN_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "pin.png");
    private static final ImageIcon UNPIN_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "unpin.png");
    private static final EnumMap<ComponentPosition, Direction> POSITION_TO_DIRECTION_MAP = new EnumMap<>(ComponentPosition.class);
    private static final Map<String, Direction> NSEW_TO_DIRECTION_MAP = new HashMap<>();
    static
    {
        // Construct the position to direction map
        POSITION_TO_DIRECTION_MAP.put(ComponentPosition.LEFT, WEST);
        POSITION_TO_DIRECTION_MAP.put(ComponentPosition.RIGHT, EAST);
        POSITION_TO_DIRECTION_MAP.put(ComponentPosition.GLOBE, SOUTH);

        // And the direction name to reference map
        NSEW_TO_DIRECTION_MAP.put("north", NORTH);
        NSEW_TO_DIRECTION_MAP.put("south", SOUTH);
        NSEW_TO_DIRECTION_MAP.put("east", EAST);
        NSEW_TO_DIRECTION_MAP.put("west", WEST);
    }

    @Inject
    @Headless
    private boolean headless;

    @Inject
    private DockingManager dockingManager;

    private final Map<CDockable, Component> dockablePinnedComponentsMap = new HashMap<>();
    private final Map<CDockable, Direction> dockableDirectionMap = new HashMap<>();
    private final Map<String, Boolean> dockableNameHidePinMap = new HashMap<>();
    private final Set<CDockable> pendingPins = new HashSet<>();
    private final WeakHashMap<CDockable, DockableFocusListener> dockableFocusListenerMap = new WeakHashMap<>();

    // This should remain null until the dock layout settings have been initialized
    private Boolean autoPin;

    /**
     * Configures whether a panel's pin button should be displayed. Panels can still be pinned programmatically even if
     * the button is hidden, but users will not be able to manually change the pin state of a panel. The pin button is
     * shown to users by default.
     *
     * @param dockableName The title of the panel to configure the visibility of the pin control on
     * @param hidden       true if a panel's pin button should not be shown to users
     * @since 3.0
     */
    public void setPinHidden(String dockableName, boolean hidden)
    {
        dockableNameHidePinMap.put(dockableName, hidden);
    }

    boolean isAutoPin()
    {
        return autoPin != null && autoPin;
    }

    void setAutoPin(boolean autoPin)
    {
        this.autoPin = autoPin;
    }

    /**
     * Process a new dockable. Track its direction and add a focus listener to handle when it is restored.
     *
     * @param dockableAndProps Dockable config to process
     */
    void handleNewDockable(@Nonnull DockableAndProps dockableAndProps)
    {
        final ComponentDescriptor descriptor = dockableAndProps.getDescriptor();
        final Direction direction = getDirection(descriptor.getComponentPosition());
        final ComponentDockable dockable = dockableAndProps.requireDockable();

        if (shouldPin(descriptor))
        {
            dockableDirectionMap.put(dockable, direction);
            dockablePinnedComponentsMap.put(dockable, dockableAndProps.getComponent());
        }

        dockable.addFocusListener(dockableFocusListenerMap.computeIfAbsent(dockable, c -> new DockableFocusListener()));
    }

    /**
     * Clears tracking information for the given dockable once it has been removed.
     *
     * @param dockable Removed dockable.
     * @since 5.7
     * @see DockingManager#removeDockable(DockableAndProps) Call origin
     */
    void onRemoveDockable(@Nonnull CDockable dockable)
    {
        // The dockable can house a reference to the component it displays,
        // so we do not want to keep these around in a map after they're no longer needed.
        dockablePinnedComponentsMap.remove(dockable);
        dockableDirectionMap.remove(dockable);
        DockableFocusListener dockableFocusListener = dockableFocusListenerMap.remove(dockable);
        if (dockableFocusListener != null) dockable.removeFocusListener(dockableFocusListener);
    }

    /**
     * Docks the panel next to the globe and keeps it open when another dockable panel gains focus.
     *
     * @param cDockable A component wrapped in a dockable.
     * @param component The component shown inside the dockable container.
     * @param direction The location on the screen where the panel should be shown.
     * @since 2.23
     */
    private void pinComponent(@Nonnull CDockable cDockable, @Nonnull Component component, Direction direction)
    {
        dockablePinnedComponentsMap.put(cDockable, component);
        dockableDirectionMap.put(cDockable, direction);

        // This must be queued up on the EDT to avoid triggering a bug in the docking framework that will infinitely loop
        SwingUtilities.invokeLater(() -> {
            // Get the dimensions and location of the root docking space.
            JComponent dockingRootComponent = ((CGridArea) dockingManager.getDockControl().getStation("center")).getComponent();
            if (!dockingRootComponent.isShowing()) return; // can happen when opening a new project

            Dimension dockableSpaceDimensions = dockingRootComponent.getSize();
            Point dockableSpaceOffset = dockingRootComponent.getLocationOnScreen();

            // Compute the center point of the unpinned dockable's display.
            Point unpinnedCenterScreenPoint = null;
            if (cDockable instanceof ComponentDockable componentDockable)
            {
                final Component comp = componentDockable.getComponent();
                unpinnedCenterScreenPoint = comp.isShowing() ? ComponentUtils.getCenterPoint(comp, true) : null;
            }

            // Skip if we cannot figure out where we are.
            CDockable closestOpenDockable = null;
            if (unpinnedCenterScreenPoint != null)
            {
                double closestDockableDistance = Double.MAX_VALUE;

                // Determine which direction the incoming unpinned dockable comes from.
                // Currently, these will always originate from the flap dock stations on the S/E/W directions.
                Direction unpinnedDirection = getUnpinnedDirection(cDockable);

                // Check and see if there is an open dockable near where our unpinned dockable is.
                CControl dockControl = dockingManager.getDockControl();
                for (int i = 0; i < dockControl.getCDockableCount(); i++)
                {
                    // Skip dockables that are ourselves or not open.
                    CDockable existingDockable = dockControl.getCDockable(i);
                    if (existingDockable == cDockable || !existingDockable.isShowing()) continue;

                    // For existing dockables that are open, find the one that is closest to where we are currently with our unpinned dockable.
                    //
                    // Let's say you have a single dockable on the 'west' side, and you attempt to pin an unpinned dockable also on the west.
                    //  - The unpinned dockable will be floating on top of the existing pinned dockable.
                    //  - Because our to-be-pinned dockable is on top of the existing pinned dockable its center point will be very close to the pinned dockable's center point.
                    //  - We pick the existing pinned dockable as our target dockable and will stack up with it later.
                    //
                    // Let's say you have the same case as before but there are two dockables on the 'west' side. They are split vertically.
                    //  - The unpinned dockable will occupy the full height of the main display space.
                    //  - The unpinned dockable's center point will be closer to the larger of the existing pinned dockable's center points.
                    //  - This is because the larger dockable will naturally have its center point closer to the middle of the main display space's height.
                    //  - We pick the larger pinned dockable as our target dockable and will stack up with it later.
                    //
                    // Let's say you have an unpinned dockable on the 'west' and there is NOTHING else on the west side.
                    //  - We will not consider stacking up with pinned dockables that have a center point too far away.
                    //  - The maximum distance is half of the *current* TAKX window size.
                    //    This ensures the max distance is always relative, and we will never try and stack with something on the opposite side of the application UI.
                    if (existingDockable instanceof ComponentDockable componentDockable)
                    {
                        // Get the center point of the existing dockable's display.
                        Component existingComponent = componentDockable.getComponent();
                        Point existingCenterScreenPoint = ComponentUtils.getCenterPoint(existingComponent, true);
                        Point existingCenterLocalizedPoint = new Point(existingCenterScreenPoint.x - dockableSpaceOffset.x, existingCenterScreenPoint.y - dockableSpaceOffset.y);

                        // Skip this dockable as a candidate for stacking if its direction does not match the unpinned dockable's direction.
                        Direction existingDirection = computeClosestSide(dockableSpaceDimensions, existingCenterLocalizedPoint);
                        if (unpinnedDirection == null || existingDirection != unpinnedDirection) continue;

                        // Update which existing dockable's display is the closest to our unpinned dockable's display.
                        double maxDistance = SwingUtilities.getWindowAncestor(existingComponent).getWidth() / 2.0;
                        double distance = existingCenterScreenPoint.distance(unpinnedCenterScreenPoint);
                        if (distance < closestDockableDistance && distance < maxDistance)
                        {
                            closestDockableDistance = distance;
                            closestOpenDockable = existingDockable;
                        }
                    }
                }
            }

            try
            {
                // Track dockables that are having their locations updated as part of our pinning process.
                // This is mainly to address a possible infinite loop with the dockable focus listener that also triggers pinning.
                pendingPins.add(cDockable);
                if (closestOpenDockable != null)
                {
                    // If we found an existing pinned dockable close to where our unpinned dockable is, we will place it in the same space as it gets pinned.
                    cDockable.setLocation(closestOpenDockable.getBaseLocation());
                } else
                {
                    // If nothing is nearby, then we will open it in a new space in the requested direction.
                    TreeLocationRoot pinnedLocationRoot = getPinnedLocationRoot(component.getPreferredSize(), component.getSize(), direction);
                    cDockable.setLocation(pinnedLocationRoot);
                }
            } finally
            {
                pendingPins.remove(cDockable);
            }
        });
    }

    /**
     * @param dockable An unpinned dockable.
     * @return The direction the dockable originates from <i>(Unpinned items reside in flaps on the S/E/W sides)</i>.
     * Can be {@code null} if for some reason a pin request comes from a dockable not already in one of these side-flaps.
     * @since 5.6
     */
    @Nullable
    private static Direction getUnpinnedDirection(@Nonnull CDockable dockable)
    {
        // The flap contains an index "which item in the series are we" and its parent has its unique id
        // set to the name of a cardinal direction "north", "south", "east", "west".
        // We just want to look up that ID value and map it to a direction.
        if (dockable.getBaseLocation() instanceof CFlapIndexLocation flapIndexedLocation
                && flapIndexedLocation.getParent() instanceof CMinimizeAreaLocation minimizedAreaLocation)
        {
            String directionName = minimizedAreaLocation.getArea().getUniqueId();
            return NSEW_TO_DIRECTION_MAP.getOrDefault(directionName, null);
        }

        // In any other case, there isn't a great way to determine where a dockable originates from.
        return null;
    }

    /**
     * Determine the location at which to pin the given component.
     *
     * @param preferredSize The component's desired size. This will only be used if the
     *                      {@code actualSize} of the panel is 0 width and 0 height.
     * @param actualSize    The component's current size.
     * @param direction     The direction to pin the component at.
     * @return location root
     */
    private TreeLocationRoot getPinnedLocationRoot(@Nonnull Dimension preferredSize, @Nonnull Dimension actualSize, Direction direction)
    {
        final double ratio = getPinnedSizeRatio(preferredSize, actualSize, direction);

        final CContentAreaCenterLocation normal = CLocation.base().normal();

        return switch (direction)
        {
            case EAST -> normal.east(ratio);
            case WEST -> normal.west(ratio);
            case NORTH -> normal.north(ratio); // TAKX currently doesn't have any, but just in case
            case SOUTH -> normal.south(ratio);
        };
    }

    /**
     * Determine the size ratio to use for pinning a component using the given direction.
     *
     * @param preferredSize The component's desired size. This will only be used if the
     *                      {@code actualSize} of the panel is 0 width and 0 height.
     * @param actualSize    The component's current size.
     * @param direction     Direction at which to place the component
     * @return Computed size ratio
     */
    private double getPinnedSizeRatio(@Nonnull Dimension preferredSize, @Nonnull Dimension actualSize, Direction direction)
    {
        Dimension centerAreaDimension = dockingManager.getCenterAreaDimension();
        double ratio = 0.25;
        double panelSize;
        double centerSize;

        if (direction == WEST || direction == EAST)
        {
            final int componentWidth = actualSize.width;

            // Handle case where the panel has not been opened previously
            panelSize = componentWidth == 0 ? preferredSize.getWidth() : componentWidth;
            centerSize = centerAreaDimension.getWidth();

            if (panelSize == 0) panelSize = preferredSize.getWidth();
        } else
        {
            // TODO: remove hard code accounting for tab height
            final int tabHeight = 36;
            final int componentHeight = actualSize.height;

            // Handle case where the panel has not been opened previously
            panelSize = (componentHeight == 0 ? preferredSize.getHeight() : componentHeight) + tabHeight;
            centerSize = centerAreaDimension.getHeight();
        }

        // Sanity check
        if (panelSize > 0 && panelSize < centerSize)
        {
            ratio = panelSize / centerSize;
        }
        return ratio;
    }

    /**
     * Makes the panel float above all other dockable panels and makes the panel collapse when another
     * dockable panel gains focus.
     *
     * @param dockable A component wrapped in a dockable.
     * @since 2.23
     */
    private void unpinComponent(@Nonnull AbstractCDockable dockable)
    {
        Component component = dockablePinnedComponentsMap.remove(dockable);
        Dimension currentSize = component != null ? component.getSize() : null;

        dockable.setExtendedMode(MINIMIZED);
        dockable.toFront();
        setUnpinnedDockableSize(dockable, currentSize);
    }

    /**
     * Marks a dockable as pinned. Use this in place of {@link #pinComponent(CDockable, Component, Direction)}
     * when the panel should be pinned but not automatically opened.
     *
     * @param dockableAndProps Dockable to pin.
     * @since 2.24
     */
    void autoPin(@Nonnull DockableAndProps dockableAndProps)
    {
        dockablePinnedComponentsMap.put(dockableAndProps.getDockable(), dockableAndProps.getComponent());
    }

    /**
     * Determines if a dockable is pinned.
     *
     * @param cDockable The component to check the pin state of.
     * @return True if the dockable is pinned.
     * @since 2.24
     */
    boolean isPinned(@Nonnull CDockable cDockable)
    {
        return dockablePinnedComponentsMap.containsKey(cDockable) || isShownPinned(cDockable);
    }

    /**
     * Changes the displayed size of a dockable. The width or height may solely be used from the preferred size
     * depending on if the panel is externalized or depending on which side the dockable is stationed on.
     *
     * @param dockableAndProps Wrapper object containing the component to resize
     * @param preferredSize    The desired dimensions of the component. Note that these dimensions
     *                         are only a recommendation and may only be partially respected.
     */
    @InvokeOnEDT
    void setDockableSize(@Nonnull DockableAndProps dockableAndProps, @Nonnull Dimension preferredSize)
    {
        if (!dockableAndProps.hasDockable())
        {
            logger.warn("No dockable found; size will not be updated. {}", dockableAndProps);
            return;
        }

        ComponentDockable dockable = dockableAndProps.requireDockable();
        Direction direction = getDirection(dockable);

        if (isPinned(dockable))
        {
            TreeLocationRoot treeLocationRoot = getPinnedLocationRoot(preferredSize, preferredSize, direction);

            List<ComponentDockable> siblingDockables =
                    dockingManager.findDockableAndPropsWithDockable()
                            .map(DockableAndProps::requireDockable)
                            .filter(candidate -> isShownPinned(candidate) && getDirection(candidate) == direction)
                            .toList();

            // We need to minimize all dockables that will be stacked together before pinning in order for the
            // new sizes to be respected. However, we don't want to make currently minimized panels shift around
            // so we filter those out first before applying our desired minimized state + location.
            dockable.setExtendedMode(MINIMIZED);
            siblingDockables.forEach(siblingDockable -> siblingDockable.setExtendedMode(MINIMIZED));
            siblingDockables.forEach(siblingDockable -> siblingDockable.setLocation(treeLocationRoot));

            dockable.setLocation(treeLocationRoot);
        } else
        {
            setUnpinnedDockableSize(dockable, preferredSize);
        }
    }

    /**
     * Sets the size of an unpinned dockable. The width or height may solely be used from the preferred size
     * depending on if the panel is externalized or depending on which side the dockable is stationed on.
     *
     * @param cDockable     A component wrapped in a dockable.
     * @param preferredSize The desired dimensions of the component. Note that these dimensions
     *                      are only a recommendation and may only be partially respected.
     */
    private void setUnpinnedDockableSize(@Nonnull AbstractCDockable cDockable, @Nullable Dimension preferredSize)
    {
        // Update the unpinned containers size to match the size of the pinned container. The two sizes may
        // vary if the user pins a component, then re-sizes the panel, then unpins the panel. Note that when
        // specifying the new dimensions the height is required as the first parameter and the width is the second.
        if (preferredSize != null)
        {
            //noinspection SuspiciousNameCombination
            cDockable.setResizeRequest(new Dimension(preferredSize.height, preferredSize.width), true);
        }
    }

    /**
     * Determine if the component descriptor indicates that it should be pinned, or if we are in "auto pin" mode and
     * the descriptor does not indicate a pinned state.
     *
     * @param descriptor Component descriptor to test
     * @return {@code true} if this component's dockable should be pinned
     */
    private boolean shouldPin(@Nonnull ComponentDescriptor descriptor)
    {
        final Boolean pinned = descriptor.isPinned();
        return pinned == null ? isAutoPin() : pinned;
    }

    /**
     * If the {@link UserPreferenceEntry#AUTO_PIN} preference changes, then update accordingly.
     *
     * @param change Preference entry change event
     */
    @SuppressWarnings("unused")
    private void handleNotificationChange(@Observes @UserPreferenceType(UserPreferenceEntry.AUTO_PIN) PreferenceEntryChange change)
    {
        if (headless) return; // Nothing to do when headless

        boolean newValue = change.getBooleanValue();

        if (newValue != autoPin)
        {
            autoPin = newValue;

            SwingUtilities.invokeLater(() -> {
                if (autoPin)
                {
                    int dialogResult = AlwaysOnTopJOptionPane.showConfirmDialog(JFrame.getFrames()[0],
                            "Would you like to pin all panels now?",
                            "Update Current Settings", JOptionPane.YES_NO_OPTION);

                    if (dialogResult == JOptionPane.YES_OPTION)
                    {
                        dockingManager.findDockableAndPropsWithDockable().forEach(this::autoPin);
                    }
                } else
                {
                    int dialogResult = AlwaysOnTopJOptionPane.showConfirmDialog(JFrame.getFrames()[0],
                            "Would you like to unpin all panels now?",
                            "Update Current Settings", JOptionPane.YES_NO_OPTION);

                    if (dialogResult == JOptionPane.YES_OPTION)
                    {
                        dockingManager.findDockableAndPropsWithDockable().forEach(dockableAndProps -> {
                            // Need to check if minimized here to avoid popping up all panels
                            final ComponentDockable dockable = dockableAndProps.requireDockable();

                            if (dockable.getExtendedMode().equals(MINIMIZED))
                            {
                                dockablePinnedComponentsMap.remove(dockable);
                            } else
                            {
                                unpinComponent(dockable);
                            }
                        });
                    }
                }
            });
        }
    }

    /**
     * Create our custom "sticky" button to move the location of a flap panel to the CENTER region
     *
     * @param control The CControl
     * @since 2.16
     */
    void installPinButtons(CControl control)
    {
        // Install a new "ActionGuard", which will inject an action where the default sticky action normally shows up
        control.getController().addActionGuard(new ActionGuard()
        {
            @Override
            public boolean react(Dockable dockable)
            {
                if (dockable instanceof CommonDockable)
                {
                    final CDockable cDockable = ((CommonDockable) dockable).getDockable();
                    final CLocation baseLocation = cDockable.getBaseLocation();
                    final boolean isFloating = baseLocation == null || baseLocation instanceof CExternalizedLocation;

                    return cDockable instanceof AbstractCDockable && !(cDockable instanceof GlobeDockable) && !isFloating;
                }

                return false;
            }

            @Override
            public DockActionSource getSource(Dockable dockable)
            {
                CDockable cDockable = ((CommonDockable) dockable).getDockable();

                if (cDockable.getParentStation() instanceof FlapDockStation)
                {
                    dockableDirectionMap.put(cDockable, ((FlapDockStation) cDockable.getParentStation()).getDirection());
                }

                boolean pinned = dockable.getDockParent() instanceof CSplitDockStation || cDockable.getBaseLocation() instanceof CStackLocation;
                boolean hidePin = dockableNameHidePinMap.getOrDefault(dockable.getTitleText(), false);

                if (hidePin) return null;

                return createCustomButtonSource(pinned);
            }
        });
    }

    /**
     * Create an action source for the custom pin button
     *
     * @param pinned True if the panel should be kept open.
     * @since 2.16
     */
    private DockActionSource createCustomButtonSource(boolean pinned)
    {
        DockAction action = new PinButton(pinned);

        final LocationHint locationHint = new LocationHint(LocationHint.DIRECT_ACTION, LocationHint.VERY_RIGHT);
        final DefaultDockActionSource source = new DefaultDockActionSource(locationHint);

        source.add(action);

        return source;
    }

    /**
     * Maps a component's position to a side to anchor the dockable to.
     *
     * @param componentPosition The position to map to an anchor point.
     * @return The side which the dockable will anchor to.
     */
    private Direction getDirection(ComponentPosition componentPosition)
    {
        return POSITION_TO_DIRECTION_MAP.getOrDefault(componentPosition, NORTH);
    }

    /**
     * Gets the side of the frame which the dockable is currently or will be anchored to.
     *
     * @param cDockable The component to find the anchor point of.
     * @return The side which the {@code cDockable} will be anchored to.
     */
    private Direction getDirection(@Nonnull CDockable cDockable)
    {
        CLocation baseLocation = cDockable.getBaseLocation();
        String rootName = baseLocation != null ? baseLocation.findRoot().toUpperCase() : "";
        Direction direction;

        try
        {
            direction = Direction.valueOf(rootName);
        } catch (IllegalArgumentException exception)
        {
            // The component's base location is in the center and can't be directly determined.
            direction = dockableDirectionMap.computeIfAbsent(cDockable, key -> {
                if (cDockable instanceof ComponentDockable componentDockable)
                {
                    final var dockableAndProps = dockingManager.findDockableAndProps(componentDockable);
                    if (dockableAndProps != null) return getDirection(dockableAndProps.getComponentPosition());
                }

                return null;
            });
        }

        return direction;
    }

    /**
     * @param dimensions Rectangle dimensions.
     * @param point Some point normalized to the dimensions.
     *              This means if a dimension represents a component that is 50px east of the origin,
     *              a point of {@code 0,0} would be the top left pixel of that component rather than the root container of the application.
     * @return The closest direction of the point in a rectangle of the given dimensions.
     * @since 5.6
     */
    @Nonnull
    private static Direction computeClosestSide(@Nonnull Dimension dimensions, @Nonnull Point point)
    {
        double w = dimensions.getWidth();
        double h = dimensions.getHeight();
        double x = point.getX();
        double y = point.getY();

        // Sanity check: Ensure the passed dimensions are valid
        if (w > 0 || h > 0)
        {
            // Based on a simple 4 quadrant division of the rectangle (divided across the diagonals) find the closest side.
            double yMain = (h * x) / w;
            double yOther = h - yMain;
            if (y < yMain && y < yOther)
                return NORTH;
            if (y > yMain && y > yOther)
                return SOUTH;
            if (y > yMain && y < yOther)
                return WEST;
            if (y < yMain && y > yOther)
                return EAST;
        }

        // If the point lies perfectly on one of the diagonals fall back to using a basic distance check.
        double distTop = Math.abs(y);
        double distLeft = Math.abs(x);
        double distBottom = Math.abs(h - y);
        double distRight = Math.abs(w - x);
        double min = distTop;
        Direction side = NORTH;
        if (distBottom < min)
        {
            min = distBottom;
            side = SOUTH;
        }
        if (distLeft < min)
        {
            min = distLeft;
            side = WEST;
        }
        if (distRight < min)
        {
            side = EAST;
        }
        return side;
    }

    /**
     * Determines if a dockable is currently being shown as pinned. A dockable can be pinned internally while not
     * being shown as pinned if a user pins a panel, then minimizes the panel, then restores the panel since the
     * framework does not maintain pinned state information.
     *
     * @param cDockable The component to check the pin state of
     * @return True if the dockable is currently being displayed as pinned.
     */
    private boolean isShownPinned(@Nonnull CDockable cDockable)
    {
        final CStation<?> parentStation = cDockable.getParentStation();
        return parentStation instanceof CSplitDockStation
                || parentStation instanceof CGridArea
                || cDockable.getBaseLocation() instanceof CStackLocation;
    }

    /**
     * Custom pin button that relocates a panel from the flap dock to the CENTER region, mainly
     * so it doesn't obscure the globe
     *
     * @since 2.16
     */
    @ButtonContentAction
    private class PinButton extends SimpleButtonAction
    {
        PinButton(boolean pinned)
        {
            setText(pinned ? "Unpins this panel" : "Pins this panel to keep it open");
            setIcon(pinned ? UNPIN_ICON : PIN_ICON);
        }

        @Override
        public void action(Dockable dockable)
        {
            final DockStation parent = dockable.getDockParent();
            final CDockable innerDockable = ((CommonDockable) dockable).getDockable();

            boolean stacked = innerDockable.getBaseLocation() instanceof CStackLocation;

            if (parent instanceof CSplitDockStation || stacked)
            {
                unpinComponent((AbstractCDockable) innerDockable);
            } else
            {
                pinComponent(innerDockable, dockable.getComponent(), getDirection(innerDockable));
            }
        }
    }

    private final class DockableFocusListener implements CFocusListener
    {
        @Override
        public void focusGained(@Nonnull CDockable dockable)
        {
            if (dockablePinnedComponentsMap.containsKey(dockable) && !pendingPins.contains(dockable))
            {
                Component pinnedComponent = dockablePinnedComponentsMap.get(dockable);

                // Only process locations that map to a Direction
                if (!isShownPinned(dockable))
                {
                    pinComponent(dockable, pinnedComponent, getDirection(dockable));
                }
            }
        }

        @Override
        public void focusLost(CDockable dockable)
        {
        }
    }
}
