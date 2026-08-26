package gov.takx.core.globe.layers.spiderfy;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.util.BasicDragger;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeLifeCycleListener;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.globe.spiderfy.IIconStack;
import gov.takx.api.globe.spiderfy.IIconStackController;
import gov.takx.api.globe.spiderfy.IIconStackExpansionListener;
import gov.takx.api.globe.spiderfy.IconStackExpansionShape;
import gov.takx.api.globe.spiderfy.IconStackPropertyConstants;
import gov.takx.api.layer.IRaptorLayer;
import gov.takx.api.layer.IRaptorLayerManager;
import gov.takx.api.layer.LayerGroup;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.core.globe.PrimaryGlobeCreatedEvent;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Manages the expansion and collapsing of {@code IIconStack}.
 *
 * @since 2.26
 */
@ApplicationScoped
public class IconStackController implements IIconStackController, SelectListener, IZoomListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Controls the delay between queueing up icon expansion steps
     */
    private static final int ANIMATION_DELAY_MS = 8;
    private static final int ANIMATION_COMPLETE = 100;

    /**
     * There are two mouse events triggered when clicking an icon on the globe. To help distinguish between the two
     * this flag mimics the consume behavior of an event so that the mouse event is not processed twice.
     */
    private boolean ignoreMouseEvent;

    private volatile int animationPercent = ANIMATION_COMPLETE;

    /**
     * Track the last selection event where icons were expanded so icons can be repositioned during other events.
     */
    private SelectEvent lastSelectEvent;
    private WorldWindow primaryWorldWindow;

    private IIconStack tooltipIconStack;
    private IIconStack lastExpandedIconStack;

    private final IconStackLayer iconLayer = new IconStackLayer();
    private final IconStackPathLayer lineLayer = new IconStackPathLayer();
    private final IconStackPositionCalculator positionCalculator = new IconStackPositionCalculator();
    private final IconStackPropertyChangeListener iconStackPropertyChangeListener = new IconStackPropertyChangeListener();

    private final Collection<IIconStack> queuedIcons = new ArrayList<>();
    private final Collection<IIconStackExpansionListener> expansionListeners = new CopyOnWriteArrayList<>();
    private final Map<IIconStack, Collection<Renderable>> iconStackRenderableMap = new HashMap<>();

    private Position lastIconPosition;

    private final MouseAdapter mouseAdapter;

    private BasicDragger basicDragger;

    private final ICoreExecutorService executorService;
    private final IExtendedGlobeManager globeManager;
    private final IRaptorLayerManager raptorLayerManager;

    @Inject
    IconStackController(ICoreExecutorService executorService, IExtendedGlobeManager globeManager, IRaptorLayerManager raptorLayerManager)
    {
        this.executorService = executorService;
        this.globeManager = globeManager;
        this.raptorLayerManager = raptorLayerManager;

        mouseAdapter = new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent event)
            {
                if (!ignoreMouseEvent && animationPercent >= ANIMATION_COMPLETE)
                {
                    lastSelectEvent = null;
                    clearSpiderfy();
                }

                ignoreMouseEvent = false;
            }

            @Override
            public void mouseReleased(MouseEvent event)
            {
                if (lastIconPosition != null)
                {
                    dragSubIcons(lastExpandedIconStack, lastIconPosition, getActiveWorldWindow(event), true);
                    lastIconPosition = null;
                }
            }
        };
    }

    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        queuedIcons.clear();
        expansionListeners.clear();
        iconStackRenderableMap.clear();
        iconLayer.removeAllIcons();
        lineLayer.clearPolylines();
    }

    @SuppressWarnings("unused")
    private void primaryGlobeInitialized(@Observes PrimaryGlobeCreatedEvent ignored)
    {
        primaryWorldWindow = globeManager.getPrimaryGlobeDelegate().getWorldWindow();
        basicDragger = new BasicDragger(primaryWorldWindow);

        globeManager.getAllGlobeDelegates().forEach(globeDelegate -> addGlobeEventHandlers(globeDelegate.getWorldWindow()));
        globeManager.addGlobeLifeCycleListener(new IGlobeLifeCycleListener()
        {
            @Override
            public void globeCreated(IGlobeDelegate globeDelegate)
            {
                addGlobeEventHandlers(globeDelegate.getWorldWindow());
            }
        });

        IRaptorLayer spiderfyIconRaptorLayer = raptorLayerManager.addLayer(iconLayer, LayerGroup.INTERNAL_TOOLS);

        iconLayer.setPickEnabled(true);
        raptorLayerManager.addLayer(lineLayer, LayerGroup.INTERNAL_TOOLS);

        spiderfyIconRaptorLayer.setPickable(true);
        queuedIcons.forEach(this::addIcons);
        queuedIcons.clear();
    }

    /**
     * Visually disperses selected icons that are stacked. Only the icons' visual representation is changed during
     * this process; the icons' positions are never altered.
     *
     * @param selectEvent Icon selection trigger.
     */
    @Override
    public void selected(SelectEvent selectEvent)
    {
        if (selectEvent != null && selectEvent.getObjects() != null)
        {
            IIconStack selectedIconStack = getIconStack(selectEvent);

            if (tooltipIconStack != null && !tooltipIconStack.equals(selectedIconStack))
            {
                tooltipIconStack.setShowToolTip(false);
            }

            if (animationPercent >= ANIMATION_COMPLETE && selectedIconStack != null)
            {
                lastSelectEvent = selectEvent;
                ignoreMouseEvent = true;

                if (selectEvent.isLeftClick() || (selectEvent.isDrag() && lastIconPosition == null))
                {
                    clearSpiderfy();
                }
                if (selectEvent.isDrag())
                {
                    basicDragger.selected(selectEvent);
                    ignoreMouseEvent = false;
                }

                if (selectEvent.isHover())
                {
                    tooltipIconStack = selectedIconStack;
                    tooltipIconStack.setShowToolTip(true);
                } else if (selectEvent.isLeftClick() || selectEvent.isDrag())
                {
                    WorldWindow worldWindow = getActiveWorldWindow(selectEvent.getMouseEvent());

                    lastExpandedIconStack = selectedIconStack;

                    // Leaf nested icons can't be expanded so we need to expand the parent IconStack instead
                    if (lastExpandedIconStack.getParentIconStack() != null && !hasChildStacks(lastExpandedIconStack))
                    {
                        lastExpandedIconStack = lastExpandedIconStack.getParentIconStack();
                        lastIconPosition = null;
                    }

                    if (selectEvent.isDrag())
                    {
                        dragSubIcons(lastExpandedIconStack, lastIconPosition, worldWindow, false);
                        lastIconPosition = lastExpandedIconStack.getPosition();
                    }

                    boolean enableAnimation = !selectEvent.isDrag() && !selectEvent.isDragEnd();
                    expandIconStack(lastExpandedIconStack, worldWindow, enableAnimation);
                }
            }
        }
    }

    /**
     * @return {@code true} if the given stack's child icon stacks is not empty
     * @since 3.4
     * @see IIconStack#getChildIconStacks()
     */
    private boolean hasChildStacks(IIconStack iconStack)
    {
        return iconStack.getChildIconStacks().findAny().isPresent();
    }

    @Override
    public void zoomChange(WorldWindow worldWindow)
    {
        if (lastSelectEvent != null)
        {
            IIconStack selectedIconStack = getIconStack(lastSelectEvent);
            if (selectedIconStack != null)
            {
                lastExpandedIconStack = selectedIconStack;
                expandIconStack(lastExpandedIconStack, worldWindow, false);
            }
        }
    }

    @Override
    public void expandIconStack(@Nonnull IIconStack iconStack, boolean enableAnimation)
    {
        expandIconStack(iconStack, primaryWorldWindow, enableAnimation);
    }

    @Override
    public void expandIconStack(@Nonnull IIconStack iconStack, @Nonnull WorldWindow worldWindow, boolean enableAnimation)
    {
        IIconStack parentIconStack = iconStack.getParentIconStack();

        // If we are dragging a nested icon that has no children then we need to expand from the parent
        // to ensure that the ground lines will be updated as well
        if (parentIconStack != null && parentIconStack.isVisible() && !hasChildStacks(iconStack))
        {
            iconStack = parentIconStack;
            lastExpandedIconStack = parentIconStack;
        }

        Collection<IIconStack> childIconStacks = iconStack.getChildIconStacks().collect(Collectors.toList());

        childIconStacks.add(iconStack);
        double totalRadius = positionCalculator.getRadius(childIconStacks);

        hideAncestors(iconStack);

        Point2D.Double center = positionCalculator.getScreenPoint(worldWindow, iconStack.getPosition());

        queueAnimations(enableAnimation, iconStack, center, totalRadius, worldWindow);
    }

    @Override
    public void collapseIconStack(@Nonnull IIconStack iconStack)
    {
        hideChildren(iconStack);
        removePolylines(iconStack);

        if (hasChildStacks(iconStack))
        {
            while (iconStack.getParentIconStack() != null)
            {
                iconStack.setVisible(false);
                iconStack = iconStack.getParentIconStack();
            }
        }

        iconStack.setVisible(true);

        IIconStack finalIconStack = iconStack;
        expansionListeners.forEach(expansionListener -> expansionListener.iconStackCollapsed(finalIconStack));

        redrawAllGlobes();
    }

    @Override
    public void addIconStackExpansionListener(@Nonnull IIconStackExpansionListener expansionListener)
    {
        expansionListeners.add(expansionListener);
    }

    @Override
    public void removeIconStackExpansionListener(@Nonnull IIconStackExpansionListener expansionListener)
    {
        expansionListeners.remove(expansionListener);
    }

    @Override
    public IIconStack createIconStack(@Nonnull Object imageSource, @Nonnull Position position)
    {
        IIconStack iconStack = new IconStack(imageSource, position);

        iconStack.addPropertyChangeListener(iconStackPropertyChangeListener);

        return iconStack;
    }

    @Override
    public void addIconStackToGlobe(@Nonnull IIconStack iconStack)
    {
        if (primaryWorldWindow == null)
        {
            queuedIcons.add(iconStack);
        } else
        {
            addIcons(iconStack);
            iconStack.setVisible(true);

            redrawAllGlobes();
        }
    }

    @Override
    public void removeIconStackFromGlobe(@Nonnull IIconStack stack)
    {
        if (primaryWorldWindow == null)
        {
            queuedIcons.remove(stack);
        } else
        {
            removePolylines(stack);
            iconLayer.getIconRenderer().setIconPosition(stack, null);
            stack.getChildIconStacks().forEach(this::removeIconStackFromGlobe);
            iconLayer.removeIcon(stack);

            redrawAllGlobes();
        }
    }

    /**
     * Queues up icon expansion events on the EDT to create an animation effect.
     *
     * @param enableAnimation True if icons should be animated to their final location; otherwise false.
     * @param iconStack       An icon that can be expanded to reveal additional icons.
     * @param origin          The point from which all the icons will animate away from.
     * @param totalRadius     The distance between the center and the outside of the icon's expansion shape.
     * @param worldWindow     The globe model where the zoom event occurred.
     */
    private void queueAnimations(boolean enableAnimation, IIconStack iconStack, Point2D.Double origin,
                                 double totalRadius, WorldWindow worldWindow)
    {
        executorService.submit(() -> {
            Thread.currentThread().setName("Queue icon stack animations");
            int animationPercent = ANIMATION_COMPLETE;

            if (enableAnimation)
            {
                animationPercent = 1;
            }

            for (; animationPercent <= ANIMATION_COMPLETE; animationPercent++)
            {

                try
                {
                    spiderfyIcons(iconStack, origin, worldWindow, totalRadius, animationPercent);
                    Thread.sleep(ANIMATION_DELAY_MS);
                } catch (InterruptedException | InvocationTargetException e)
                {
                    logger.warn("Failed to draw animation frame", e);
                }
            }
        });
    }

    /**
     * Hides all of the parents of the {@code expandedStackIcon}.
     *
     * @param expandedStackIcon The icon whose parents should be hidden.
     */
    private void hideAncestors(IIconStack expandedStackIcon)
    {
        IIconStack parentStack = expandedStackIcon.getParentIconStack();

        lastExpandedIconStack = expandedStackIcon;

        while (parentStack != null)
        {
            parentStack.setVisible(false);
            parentStack = parentStack.getParentIconStack();
        }

        //The expanded icon should be visible at the center of its children
        expandedStackIcon.setVisible(true);
    }

    /**
     * Sets the visibility of all nested children of {@code iconStack} to false.
     *
     * @param iconStack The icon stack to hide the children of.
     */
    private void hideChildren(IIconStack iconStack)
    {
        iconStack.getChildIconStacks().forEach(childIcon -> {
            childIcon.setVisible(false);
            hideChildren(childIcon);
        });
    }

    /**
     * Adds the {@code stack} and all nested icons below it to the icon layer.
     *
     * @param iconStack The root icon containing all icons that should be added to the icon layer.
     */
    private void addIcons(IIconStack iconStack)
    {
        if (iconStack != null)
        {
            boolean existingStack = StreamSupport.stream(iconLayer.getIcons().spliterator(), false).anyMatch(iconStack::equals);

            if (!existingStack)
            {
                iconLayer.addIcon(iconStack);
            }
        }
    }

    /**
     * @param iconStack         The icon to expand to reveal a subset of icons.
     * @param screenCenterPoint The screen coordinates of the {@code IconStack} from which all nested icons will expand
     *                          outward from.
     * @param worldWindow       The globe model defining the view used to identify eye position and screen
     *                          coordinates of icons.
     * @param radius            The distance between the center and the outside of the shape.
     * @param animationPercent  Scalar between 0 and 1 used to transition the icons between their initial positions
     *                          and their final position.
     */
    private void spiderfyIcons(IIconStack iconStack, Point2D.Double screenCenterPoint, WorldWindow worldWindow,
                               double radius, int animationPercent)
            throws InvocationTargetException, InterruptedException
    {
        SwingUtilities.invokeAndWait(() -> {
            Collection<IIconStack> iconStacks = iconStack.getChildIconStacks().collect(Collectors.toList());
            Map<IIconStack, Position> iconSpiderfyPositions = positionCalculator
                    .getExpandedIconPositions(worldWindow, iconStacks, iconStack.getExpansionShape(), screenCenterPoint, radius,
                            animationPercent / (double) ANIMATION_COMPLETE);

            Collection<Renderable> iconStackGroundLines = new ArrayList<>();

            removePolylines(iconStack);
            iconStackRenderableMap.put(iconStack, iconStackGroundLines);

            iconLayer.getIconRenderer().setIconPosition(iconStack, null);

            for (IIconStack icon : iconStacks)
            {
                Position spiderfyPosition = iconSpiderfyPositions.getOrDefault(icon, icon.getPosition());

                if (spiderfyPosition != null)
                {
                    Path path = lineLayer.addPath(iconStack.getPosition(), spiderfyPosition);

                    iconStackGroundLines.add(path);
                    iconLayer.getIconRenderer().setIconPosition(icon, spiderfyPosition);
                }

                icon.setVisible(spiderfyPosition != null);
            }

            redrawAllGlobes();

            this.animationPercent = animationPercent;

            if (animationPercent >= ANIMATION_COMPLETE)
            {
                expansionListeners.forEach(expansionListener -> expansionListener.iconStackExpanded(iconStack));
            }
        });
    }

    /**
     * Removes all ground lines that originate from the {@code stack}.
     *
     * @param stack The origin of ground lines that should be removed.
     */
    private void removePolylines(IIconStack stack)
    {
        if (iconStackRenderableMap.containsKey(stack))
        {
            iconStackRenderableMap.remove(stack).forEach(lineLayer::removeRenderable);
        }
    }

    /**
     * Updates the positions of nested icons when an {@code IconStack} is dragged.
     *
     * @param draggedIcon            The original {@code IconStack} that the user manually moved.
     * @param draggedIconOldPosition The previous position of the {@code IconStack} that the user manually moved.
     * @param worldWindow            The globe model from which the icon was dragged.
     * @param updateAllChildren      True if all nested icons should be updated; otherwise, only the direct descendants
     *                               of the current icon will be dragged. Only enable this when dragging is completed
     *                               to avoid computations on icons that are not directly visible.
     */
    private void dragSubIcons(IIconStack draggedIcon, Position draggedIconOldPosition, WorldWindow worldWindow, boolean updateAllChildren)
    {
        if (draggedIconOldPosition != null)
        {
            Point2D.Double newPoint = positionCalculator.getScreenPoint(worldWindow, draggedIcon.getPosition());
            Point2D.Double oldPoint = positionCalculator.getScreenPoint(worldWindow, draggedIconOldPosition);

            double xOffset = newPoint.getX() - oldPoint.getX();
            double yOffset = newPoint.getY() - oldPoint.getY();

            draggedIcon.getChildIconStacks().forEach(childIconStack -> {
                Point2D.Double subIconPoint = positionCalculator.getScreenPoint(worldWindow, childIconStack.getPosition());
                double movedSubIconX = subIconPoint.getX() + xOffset;
                double movedSubIconY = subIconPoint.getY() + yOffset;

                Position childIconStackOldPosition = childIconStack.getPosition();
                Position childIconStackNewPosition = worldWindow.getView().computePositionFromScreenPoint(movedSubIconX, movedSubIconY);

                if (childIconStackNewPosition != null)
                {
                    childIconStack.setPosition(childIconStackNewPosition);

                    if (updateAllChildren)
                    {
                        dragSubIcons(childIconStack, childIconStackOldPosition, worldWindow, true);
                    }
                }
            });
        }
    }

    /**
     * Collapses the last expanded icon stack and removes any ground poly lines that were connecting nested icons.
     */
    private void clearSpiderfy()
    {
        lineLayer.clearPolylines();

        iconLayer.getIconRenderer().resetIconPositions();

        if (lastExpandedIconStack != null)
        {
            if (!hasChildStacks(lastExpandedIconStack) && lastExpandedIconStack.getParentIconStack() != null)
            {
                collapseIconStack(lastExpandedIconStack.getParentIconStack());
            } else
            {
                collapseIconStack(lastExpandedIconStack);
            }
        }

        redrawAllGlobes();
    }

    /**
     * Extracts an IIconStack from a select event if one is exists; otherwise, {@code null}.
     *
     * @param selectEvent The event containing map elements that have been selected.
     * @return The selected IIconStack.
     */
    private IIconStack getIconStack(SelectEvent selectEvent)
    {
        return ((IIconStack) selectEvent.getObjects().stream()
                .map(PickedObject::getObject)
                .filter(pickedObject -> pickedObject instanceof IIconStack)
                .findFirst().orElse(null));
    }

    /**
     * Registers mouse, drag, and zoom listeners that update IconStacks and Ground lines.
     *
     * @param worldWindow The globe model to register listeners to.
     */
    private void addGlobeEventHandlers(IWorldWindowGL worldWindow)
    {
        ZoomEventManager zoomEventManager = new ZoomEventManager(worldWindow);
        zoomEventManager.addZoomListener(this);
        worldWindow.addSelectListener(this);
        worldWindow.addMouseListener(mouseAdapter);
    }

    /**
     * Refreshes all globes after changes to the icons or ground lines have been made.
     */
    private void redrawAllGlobes()
    {
        globeManager.getAllGlobeDelegates().forEach(IGlobeDelegate::redraw);
    }

    /**
     * Attempts to get the WorldWindow from which an event spawned. If a WorldWindow can't be determined
     * from the event, then the primary globe's WorldWindow will be returned.
     *
     * @param mouseEvent An event to identify the source of.
     * @return The active WorldWindow which spawned the event.
     */
    private WorldWindow getActiveWorldWindow(MouseEvent mouseEvent)
    {
        if (mouseEvent == null || !(mouseEvent.getSource() instanceof WorldWindow))
        {
            return primaryWorldWindow;
        }
        return (WorldWindow) mouseEvent.getSource();
    }

    private class IconStackPropertyChangeListener implements PropertyChangeListener
    {
        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent)
        {
            if (propertyChangeEvent.getSource() instanceof IIconStack iconStack)
            {
                switch (propertyChangeEvent.getPropertyName())
                {
                    case IconStackPropertyConstants.EXPANSION_SHAPE:
                        if (iconStack.isExpanded() && propertyChangeEvent.getNewValue() instanceof IconStackExpansionShape)
                        {
                            expandIconStack(iconStack, true);
                        }
                        break;
                    case IconStackPropertyConstants.CHILD_ADDED:
                        if (propertyChangeEvent.getNewValue() instanceof IIconStack childIconStack)
                        {
                            addIcons(childIconStack);

                            childIconStack.setVisible(false);

                            //If the parent icon stack was previously expanded, re-expand it so that the new child icon stack is shown and
                            //a path is drawn from the parent to the child.
                            if (iconStack.isExpanded())
                            {
                                expandIconStack(iconStack, false);
                            }
                            redrawAllGlobes();
                        }
                        break;
                    case IconStackPropertyConstants.CHILD_REMOVED:
                        if (propertyChangeEvent.getOldValue() instanceof IIconStack)
                        {
                            removeIconStackFromGlobe((IIconStack) propertyChangeEvent.getOldValue());
                        }
                        break;
                    case IconStackPropertyConstants.ALL_POSITIONS:
                        if (propertyChangeEvent.getOldValue() instanceof Position oldPosition)
                        {
                            dragSubIcons(iconStack, oldPosition, primaryWorldWindow, true);
                        }
                        break;
                }
            }
        }
    }
}
