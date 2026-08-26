package gov.takx.core.globe;

import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.event.project.ProjectOpening;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.api.globe.IGlobeMapObject;
import gov.takx.api.globe.IWorldWindowGL;
import gov.takx.api.mapobject.*;
import gov.takx.api.pointdropper.PointDropperConfiguration;
import gov.takx.api.shapes.IArrow;
import gov.takx.api.shapes.IShapeDrawingService;
import gov.takx.api.shapes.ShapeType;
import gov.takx.platform.KeyParts;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.utils.WWUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Controller for Range and Bearing tools.
 *
 * @since 5.5
 */
@ApplicationScoped
public class RangeAndBearingController
{
    private static final String DRAW_DYNAMIC_MEASURE_MESSAGE = "Click and drag to draw a dynamic measure arrow.";
    private static final String MEASURE_MESSAGE_1 = "Click a starting location, or map item.";
    private static final String MEASURE_MESSAGE_2 = "Click an ending location, or map item.";

    private static final String DRAW_BULLSEYE_MESSAGE = "Click the center, and then at the desired radius";
    private static final String DRAW_CIRCLE_MESSAGE = "Click and drag to the desired radius";

    private static final PointDropperConfiguration.CountingNameGenerator RB_CIRCLE_NAME_GENERATOR = new PointDropperConfiguration.CountingNameGenerator("R&B Circle", 1, 1);
    private static final PointDropperConfiguration.CountingNameGenerator RB_ARROW_NAME_GENERATOR = new PointDropperConfiguration.CountingNameGenerator("R&B Arrow", 1, 1);

    private final IExtendedGlobeManager globeManager;
    private final IMapObjectManager mapObjectManager;
    private final IShapeDrawingService shapeDrawingService;

    private IShapeArrowMapObject drawnDynamicMeasureMapObject;
    private MeasureAnchorListener anchorListener;

    @Inject
    RangeAndBearingController(@Nonnull IExtendedGlobeManager globeManager,
                              @Nonnull IMapObjectManager mapObjectManager,
                              @Nonnull IShapeDrawingService shapeDrawingService)
    {
        this.globeManager = globeManager;
        this.mapObjectManager = mapObjectManager;
        this.shapeDrawingService = shapeDrawingService;
    }

    /**
     * @since 5.5
     */
    @SuppressWarnings("unused")
    private void onProjectOpening(@Observes ProjectOpening ignored)
    {
        RB_ARROW_NAME_GENERATOR.reset();
        RB_CIRCLE_NAME_GENERATOR.reset();
    }

    /**
     * Deletes drawn dynamic measure on project closing.
     *
     * @param ignored {@link ProjectClosing} event
     */
    private void onProjectClosing(@Observes ProjectClosing ignored)
    {
        if (drawnDynamicMeasureMapObject != null) deleteDynamicMeasure();
    }

    /**
     * Lets the user draw a dynamic measure arrow on the globe.
     *
     * @param errorHandler Optional error handler.
     */
    public void drawDynamicMeasure(@Nullable Consumer<Throwable> errorHandler)
    {
        IApplicationGlobe lastFocusedGlobe = globeManager.getLastFocusedGlobe();
        shapeDrawingService.requestDraw(lastFocusedGlobe, ShapeType.ARROW, DRAW_DYNAMIC_MEASURE_MESSAGE).whenComplete((shape, t) -> {
            if (t == null)
            {
                ((IArrow) shape).setDynamic(true);
                KeyParts keyParts = new KeyParts(shape.getKey());
                MapObjectSpec spec = MapObjectSpec.builder(keyParts.getFamily(), keyParts.getType())
                        .location(shape.getLocation())
                        .name(RB_ARROW_NAME_GENERATOR.generateName())
                        .initializer(mo -> {
                            final var arrow = (IShapeArrowMapObject) mo;
                            arrow.setAssociatedShape(shape);
                            arrow.setLineColor(Color.RED);
                            arrow.setFillColor(Color.RED);
                            arrow.setMetaBoolean(StandardAttributeNames.RANGE_BEARING, true);
                            shape.setName(arrow.getName());
                        })
                        .build();

                shapeDrawingService.removeDrawnShape(shape);
                drawnDynamicMeasureMapObject = (IShapeArrowMapObject) mapObjectManager.getOrCreateMapObject(spec);
            } else if (errorHandler != null)
            {
                // Unwrap the completion failure
                Throwable cause = t.getCause();
                if (!(cause instanceof CancellationException)) errorHandler.accept(cause);
            }
        });
    }

    /**
     * Lets the user draw a measure arrow on the globe.
     */
    public void drawMeasure()
    {
        cancelMeasureSelection();
        anchorListener = new MeasureAnchorListener((first, last) -> {
            final MapObjectSpec spec = MapObjectSpec.builder(KeyUtils.TOOL_FAMILY, ShapeType.ARROW.getShapeType())
                    .name(RB_ARROW_NAME_GENERATOR.generateName())
                    .initializer(mo -> {
                        final var arrow = (IShapeArrowMapObject) mo;
                        arrow.setAllowDeviceToBeContainer(false);
                        arrow.setAllowDrag(false);
                        arrow.setEditable(false);
                        arrow.setLineColor(Color.RED);
                        arrow.setFillColor(Color.RED);
                        arrow.setMetaBoolean(StandardAttributeNames.RANGE_BEARING, true);
                        arrow.getAssociatedShape().setName(arrow.getName());

                        // Arrow start anchor (location or map object)
                        if (first instanceof MeasureAnchorListener.Anchor.LocationAnchor locationAnchor)
                        {
                            arrow.setStartingPoint(locationAnchor.location());
                        } else if (first instanceof MeasureAnchorListener.Anchor.MapObjectAnchor objectAnchor)
                        {
                            arrow.setStartAnchor(objectAnchor.mapObject());
                        } else
                        {
                            throw new IllegalStateException("Invalid anchor: " + first);
                        }

                        // Arrow end anchor (location or map object)
                        if (last instanceof MeasureAnchorListener.Anchor.LocationAnchor locationAnchor)
                        {
                            arrow.setVertices(List.of(arrow.getLocation(), locationAnchor.location()));
                        } else if (last instanceof MeasureAnchorListener.Anchor.MapObjectAnchor objectAnchor)
                        {
                            arrow.setRangeAnchor(objectAnchor.mapObject());
                        } else
                        {
                            throw new IllegalStateException("Invalid anchor: " + first);
                        }
                    }).build();
            mapObjectManager.getOrCreateMapObject(spec);
        });
        anchorListener.begin();
    }

    /**
     * @return Current drawn dynamic measure map object, if present.
     */
    @Nullable
    public IShapeArrowMapObject getDrawnDynamicMeasureMapObject()
    {
        return drawnDynamicMeasureMapObject;
    }

    /**
     * If a dynamic measure is drawn on the map, delete it.
     */
    public void deleteDynamicMeasure()
    {
        if (drawnDynamicMeasureMapObject != null)
        {
            mapObjectManager.deleteMapObject(drawnDynamicMeasureMapObject);
        } else
        {
            shapeDrawingService.cancelDraw();
        }
        drawnDynamicMeasureMapObject = null;
    }

    /**
     * If an in-progress measure tool selection process exists, cancel it.
     */
    private void cancelMeasureSelection()
    {
        if (anchorListener != null)
        {
            anchorListener.clean();
            anchorListener = null;
        }
    }

    /**
     * Lets the user draw a {@link gov.takx.dataservices.shapes.Bullseye} on the globe.
     */
    public void drawBullseye()
    {
        IApplicationGlobe lastFocusedGlobe = globeManager.getLastFocusedGlobe();
        shapeDrawingService.requestDraw(lastFocusedGlobe, ShapeType.BULLSEYE, DRAW_BULLSEYE_MESSAGE).whenComplete((shape, t) -> {
            if (t == null)
            {
                KeyParts keyParts = new KeyParts(shape.getKey());
                MapObjectSpec spec = MapObjectSpec.builder(keyParts.getFamily(), keyParts.getType())
                        .location(shape.getLocation())
                        .initializer(mo -> {
                            ((IShapeBullseyeMapObject) mo).setAssociatedShape(shape);
                        })
                        .build();

                shapeDrawingService.removeDrawnShape(shape);
                mapObjectManager.getOrCreateMapObject(spec);
            }
        });
    }

    /**
     * Lets the user draw a {@link gov.takx.dataservices.shapes.Circle} on the globe, specifically designated as a Range&Bearing circle.
     */
    public void drawCircle()
    {
        IApplicationGlobe lastFocusedGlobe = globeManager.getLastFocusedGlobe();
        shapeDrawingService.requestDraw(lastFocusedGlobe, ShapeType.CIRCLE, DRAW_CIRCLE_MESSAGE).whenComplete((shape, t) -> {
            if (t == null)
            {
                KeyParts keyParts = new KeyParts(shape.getKey());
                MapObjectSpec spec = MapObjectSpec.builder(keyParts.getFamily(), keyParts.getType())
                        .location(shape.getLocation())
                        .name(RB_CIRCLE_NAME_GENERATOR.generateName())
                        .initializer(mo -> {
                            final var circle = (IShapeCircleMapObject) mo;
                            circle.setAssociatedShape(shape);
                            circle.setOpacity(0);
                            circle.setLineColor(Color.RED);
                            circle.setMetaBoolean(StandardAttributeNames.RANGE_BEARING, true);
                            shape.setName(circle.getName());
                        })
                        .build();

                shapeDrawingService.removeDrawnShape(shape);
                mapObjectManager.getOrCreateMapObject(spec);
            }
        });
    }

    /**
     * Listener that allows users to pick any combination of two locations/map-objects.
     */
    private class MeasureAnchorListener
    {
        private final Map<IApplicationGlobe, AnchorSelectionListener> globeListeners = new IdentityHashMap<>();
        private final List<Anchor> anchors = new ArrayList<>(2);
        private final BiConsumer<Anchor, Anchor> onComplete;

        /**
         * @param onComplete Completion listener taking in the two anchors selected.
         */
        private MeasureAnchorListener(@Nonnull BiConsumer<Anchor, Anchor> onComplete)
        {
            this.onComplete = onComplete;
        }

        /**
         * Initialize the listeners on all globes.
         */
        private void begin()
        {
            globeManager.showGlobeMessage(MEASURE_MESSAGE_1);

            // Add mouse/key listener to each globe to operate on terrain/object clicks
            for (IApplicationGlobe globe : globeManager.getAllGlobeDelegates())
            {
                AnchorSelectionListener anchorSelectionListener = new AnchorSelectionListener(globe);
                globe.setSelectListenerActive(false);
                globe.getWorldWindow().addMouseListener(anchorSelectionListener);
                globe.getWorldWindow().addKeyListener(anchorSelectionListener);
                globeListeners.put(globe, anchorSelectionListener);
            }
        }

        /**
         * Cleanup listeners and call the completion handler.
         */
        private void complete()
        {
            clean();

            onComplete.accept(anchors.get(0), anchors.get(1));
        }

        /**
         * Cleanup listeners.
         */
        private void clean()
        {
            globeListeners.forEach((globe, anchorSelectionListener) -> {
                globe.getWorldWindow().removeMouseListener(anchorSelectionListener);
                globe.getWorldWindow().removeKeyListener(anchorSelectionListener);
                globe.setSelectListenerActive(true);
            });
            globeListeners.clear();
            globeManager.hideGlobeMessage();
        }

        /**
         * Adds an anchor from the user selection.
         *
         * @param anchor Anchor to add.
         */
        private void add(@Nonnull Anchor anchor)
        {
            if (!anchors.contains(anchor))
            {
                globeManager.showGlobeMessage(MEASURE_MESSAGE_2);
                anchors.add(anchor);
                if (anchors.size() == 2) complete();
            }
        }

        /**
         * Mouse listener that listens to clicks, and interprets as a terrain/map-object click, calling {@link #add(Anchor)} when observed.
         * <br>
         * Key listener behavior just cancels when {@link KeyEvent#VK_ESCAPE} is observed.
         */
        private class AnchorSelectionListener extends MouseAdapter implements KeyListener
        {
            private final IApplicationGlobe globe;

            /**
             * @param globe Target globe to operate on.
             */
            private AnchorSelectionListener(@Nonnull IApplicationGlobe globe)
            {
                this.globe = globe;
            }

            @Override
            public void mouseClicked(MouseEvent e)
            {
                IWorldWindowGL wwd = globe.getWorldWindow();
                if (WWUtils.clickedOnTerrain(wwd))
                {
                    ILocation location = WWUtils.positionToRaptorLocation(wwd.getCurrentPosition());
                    add(new Anchor.LocationAnchor(location));
                } else
                {
                    IGlobeMapObject picked = WWUtils.getTopObjectOfType(wwd, IGlobeMapObject.class);
                    if (picked != null) add(new Anchor.MapObjectAnchor(picked.getMapObject()));
                }
            }

            @Override
            public void keyPressed(KeyEvent e)
            {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) clean();
            }

            @Override
            public void keyReleased(KeyEvent e)
            {
                // no-op
            }

            @Override
            public void keyTyped(KeyEvent e)
            {
                // no-op
            }
        }

        /**
         * Anchor modeling either a terrain selected location, or a map object.
         */
        private sealed interface Anchor
        {
            record LocationAnchor(@Nonnull ILocation location) implements Anchor
            {
            }

            record MapObjectAnchor(@Nonnull IMapObject mapObject) implements Anchor
            {
            }
        }
    }
}
