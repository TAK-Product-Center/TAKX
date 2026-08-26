package gov.takx.core.globe;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.globe.IMultiSelector;
import gov.takx.api.globe.MultiSelectorConfiguration;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.shapes.IPolygon;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShape;
import gov.takx.api.shapes.IShapeDrawingService;
import gov.takx.api.shapes.ShapeType;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.UISpaceVisibilityEvent;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.core.view.gui.multiselector.MultiSelectorPanel;
import gov.takx.platform.ui.ComponentDescriptor;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Service to manage the multi-selection tool
 *
 * @since 3.5
 */
@ApplicationScoped
public class MultiSelectorService implements IMultiSelector
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final IGlobeManager globeManager;
    private final IShapeDrawingService shapeDrawingService;
    private final IMapObjectManager mapObjectManager;
    private final MultiSelectorPanel multiSelectorPanel = new MultiSelectorPanel();
    private final IExtendedUISpaceManager uiSpaceManager;

    private final ComponentDescriptor panelDescriptor;
    private final Set<IMapObject> surroundedObjects = new HashSet<>();

    private ShapeType selectorShapeType;
    private boolean armed;
    private CompletableFuture<Collection<IMapObject>> future;
    private IShape lastDrawnShape;
    private volatile boolean hiding;

    @Inject
    public MultiSelectorService(IGlobeManager globeManager, IMapObjectManager mapObjectManager,
                                IShapeDrawingService shapeDrawingService, IExtendedUISpaceManager uiSpaceManager)
    {
        this.globeManager = globeManager;
        this.shapeDrawingService = shapeDrawingService;
        this.mapObjectManager = mapObjectManager;
        this.uiSpaceManager = uiSpaceManager;

        panelDescriptor = new ComponentDescriptorBuilder(MultiSelectorPanel.class, MultiSelectorPanel.TITLE)
                .setComponentPosition(ComponentPosition.LEFT)
                .setIcon(UIResources.getUrl(PLATFORM_ICONS, "multiselector.png"))
                .setToolbarButtonVisible(false)
                .setPinned(true)
                .setHidePin(false)
                .build();
    }

    /**
     * Start this service.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored)
    {
        uiSpaceManager.addComponent(panelDescriptor, multiSelectorPanel);
        uiSpaceManager.hideComponent(multiSelectorPanel);
    }

    /**
     * Stop this service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        uiSpaceManager.removeComponent(multiSelectorPanel);
        disarmAndClear();
    }

    /**
     * If the user closes the panel, disarm and clear the selection.
     *
     * @since 5.4
     */
    @InvokeOnEDT
    public void visibilityChanged(@Observes UISpaceVisibilityEvent event)
    {
        if (event.getComponent() == multiSelectorPanel && !event.isVisible() && !hiding)
        {
            disarmAndClear();
            uiSpaceManager.removeComponent(multiSelectorPanel);
        }
    }

    @Override
    public boolean isArmed()
    {
        return armed;
    }

    @Nonnull
    @Override
    public List<IMapObject> getSurroundedObjects()
    {
        return new ArrayList<>(surroundedObjects);
    }

    @Nonnull
    @Override
    public List<IMapObject> getSelectedObjects()
    {
        return multiSelectorPanel.getSelectedObjects();
    }

    @Nonnull
    @Override
    public CompletableFuture<Collection<IMapObject>> arm(@Nonnull MultiSelectorConfiguration configuration)
    {
        if (armed) disarmAndClear();

        final var shapeType = configuration.shapeType();
        final var instructions = switch (shapeType)
        {
            case RECTANGLE -> "Click and Drag to select";
            case POLYGON -> "Click to place points, double click to complete selection";
            case FREEHAND_POLYGON -> "Click and drag to draw, release mouse to complete selection";
            default -> throw new IllegalArgumentException("Unsupported selector shape: " + shapeType.name());
        };

        future = new CompletableFuture<>();
        selectorShapeType = shapeType;

        if (!configuration.append()) clearSelection();

        removeLastShape();

        armed = true;
        hiding = true; // Prevent clearing/canceling our operation when the panel is hidden
        uiSpaceManager.hideComponent(multiSelectorPanel);

        //noinspection DataFlowIssue - getLastFocusedGlobe can't return null
        shapeDrawingService.requestDraw(globeManager.getLastFocusedGlobe(), shapeType, instructions).whenComplete((shape, t) -> {

            if (t == null)
            {
                shape.setOpacity(25); // Make it more visually distinct
                lastDrawnShape = shape;

                final var newlySelectedObjects = findSelection(shape);
                surroundedObjects.addAll(newlySelectedObjects);

                future.complete(new ArrayList<>(surroundedObjects));

                final var cotConsumer = configuration.cotConsumer();
                if (cotConsumer != null) cotConsumer.accept(new ArrayList<>(surroundedObjects));

                if (configuration.customSelectionHandling())
                {
                    disarmAndClear(); // The cotConsumer or Future handler should have processed the results
                } else
                {
                    processSelection(newlySelectedObjects, configuration.append());
                }
            } else
            {
                logger.debug("Shape creation failed: {}", t.getCause().getMessage());
                future.completeExceptionally(t);
                disarmAndClear();
                uiSpaceManager.removeComponent(multiSelectorPanel);
            }

            hiding = false;
            future = null; // Either way, we're no longer executing
        });

        return future;
    }

    /**
     * Process the user's selection.
     */
    private void processSelection(@Nonnull Set<IMapObject> newlySelectedObjects, boolean appending)
    {
        multiSelectorPanel.addSelectedObjects(newlySelectedObjects);
        uiSpaceManager.showComponent(multiSelectorPanel);
        disarm(false);

        if (surroundedObjects.isEmpty() && !appending) disarmAndClear();
    }

    private void disarm(boolean clearTable)
    {
        if (clearTable) clearSelection();

        selectorShapeType = null;
        armed = false;

        shapeDrawingService.cancelDraw();
    }

    /**
     * Remove the last shape drawn, if any.
     */
    private void removeLastShape()
    {
        if (lastDrawnShape != null)
        {
            shapeDrawingService.removeDrawnShape(lastDrawnShape);
            lastDrawnShape = null;
        }
    }

    /**
     * Find all the selected objects by testing containment against the given shape.
     */
    private Set<IMapObject> findSelection(IShape shape)
    {
        final List<ILocation> vertices;

        switch (selectorShapeType)
        {
            case RECTANGLE ->
            {
                vertices = ((IRectangle) shape).getVertices();
                vertices.add(vertices.get(0)); // Make a closed polygon
            }
            case POLYGON, FREEHAND_POLYGON -> vertices = ((IPolygon) shape).getVertices();
            default ->
            {
                logger.error("Unsupported shape type: {}", selectorShapeType.name());
                return Collections.emptySet();
            }
        }

        return mapObjectManager.getMapObjects().stream()
                .filter(mapObject -> mapObject.isSelectable() && ShapeUtils.polygonContainsLocation(vertices, mapObject.getLocation()))
                .filter(mapObject -> !surroundedObjects.contains(mapObject))
                .collect(Collectors.toSet());
    }

    @Override
    public void disarmAndClear()
    {
        disarm(true);
        removeLastShape();

        if (future != null)
        {
            if (!future.isDone()) future.cancel(true);
            future = null;
        }
    }

    /**
     * Clears current map object selection.
     */
    private void clearSelection()
    {
        multiSelectorPanel.clearTable();
        surroundedObjects.clear();
    }
}
