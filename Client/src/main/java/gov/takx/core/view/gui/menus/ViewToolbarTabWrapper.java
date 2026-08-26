package gov.takx.core.view.gui.menus;

import gov.nasa.worldwind.geom.Sector;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.shapes.IMutableShape;
import gov.takx.api.shapes.IRectangle;
import gov.takx.api.shapes.IShapeDrawingService;
import gov.takx.api.shapes.ShapeType;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.view.gui.mapobjectmanager.KeepInViewDialog;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 */
@PseudoBean
public class ViewToolbarTabWrapper extends AToolbarTabWrapper
{
    private static final String GO_TO_AREA_TOOLTIP = "Draw a rectangular and zoom to the extent on the globe.";
    private static final String GLOBE_VIEW_TOOLTIP = "Zoom out so the entire globe is in view.";
    private static final String DEFAULT_HEADING_TOOLTIP = "Set your view of the globe to due North.";
    private static final String DEFAULT_TILT_TOOLTIP = "Set your view of the globe to top-down.";
    private static final String DEFAULT_HEADING_AND_TILT_TOOLTIP = "Set your view of the globe to due North and top-down.";
    private static final String KEEP_IN_VIEW_TOOLTIP = "Manage which devices are kept in view at all times.";
    private static final String GOTO_AREA_INSTRUCTIONS = "Click and drag to indicate the area to zoom to.";

    private final SectorZoomHandler sectorZoomHandler = new SectorZoomHandler();
    private final UUID goToAreaButtonId = ButtonType.GO_TO_AREA.requireSingularId();

    @Inject
    private IShapeDrawingService shapeDrawingService;

    @PostConstruct
    private void postConstruct()
    {
        createTab(TabType.VIEW, true);
    }

    @Override
    protected void addComponents()
    {
        tab.addComponent(createToggleButton(ButtonType.GO_TO_AREA, sectorZoomHandler, GO_TO_AREA_TOOLTIP));
        tab.addComponent(createButton(ButtonType.GLOBE_VIEW, e -> getLastFocusedGlobe().resetWorldView(), GLOBE_VIEW_TOOLTIP));
        tab.addComponent(createButton(ButtonType.DEFAULT_HEADING, e -> getLastFocusedGlobe().resetHeading(), DEFAULT_HEADING_TOOLTIP));
        tab.addComponent(createButton(ButtonType.DEFAULT_TILT, e -> getLastFocusedGlobe().resetTilt(), DEFAULT_TILT_TOOLTIP));
        tab.addComponent(createButton(ButtonType.DEFAULT_HEADING_AND_TILT, e -> getLastFocusedGlobe().resetHeadingAndTilt(), DEFAULT_HEADING_AND_TILT_TOOLTIP));
        tab.addComponent(createButton(ButtonType.KEEP_IN_VIEW, e -> new KeepInViewDialog().setVisible(true), KEEP_IN_VIEW_TOOLTIP));
        tab.addComponent(new ToolbarSeparator(TabType.VIEW, tab.getComponents().size()));
    }

    /**
     * Class to handle interactions with the shape drawing service and zoom to the extents of a user drawn rectangle.
     *
     * @since 2.37
     */
    private final class SectorZoomHandler implements ActionListener
    {
        private CompletableFuture<IMutableShape> drawFuture;

        /**
         * Start the shape drawing operation.
         */
        private void startDrawing()
        {
            drawFuture = shapeDrawingService.requestDraw(getLastFocusedGlobe(), ShapeType.RECTANGLE, GOTO_AREA_INSTRUCTIONS)
                    .whenComplete((shape, ex) -> {
                        if (shape instanceof IRectangle rectangle)
                        {
                            final var minCorner = rectangle.getMinCorner();
                            final var maxCorner = rectangle.getMaxCorner();
                            final var sector = Sector.fromDegrees(minCorner.getLatitude(), maxCorner.getLatitude(), minCorner.getLongitude(), maxCorner.getLongitude());
                            getLastFocusedGlobe().goTo(sector);
                            shapeDrawingService.removeDrawnShape(shape);
                        }
                        drawFuture = null;
                        toolbar.setToggleButtonState(goToAreaButtonId, false);
                    });
        }

        @Override
        public void actionPerformed(ActionEvent e)
        {
            if (drawFuture == null)
            {
                startDrawing();
            } else
            {
                // Cancel any draw operation we might have started and deselect our toolbar button.
                shapeDrawingService.cancelDraw();
                toolbar.setToggleButtonState(goToAreaButtonId, false);
            }
        }
    }
}
