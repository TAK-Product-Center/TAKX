package gov.takx.core.view.gui.menus;

import gov.tak.api.location.ILocationTracker;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IPointMapObject;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.swing.ButtonAction;
import gov.takx.commons.swing.IPopupMenuProvider;
import gov.takx.core.globe.RangeAndBearingController;
import gov.takx.core.navigation.NavigationConstants;
import gov.takx.core.navigation.NavigationManager;
import gov.takx.core.toolbar.AToolbarComponent;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.navigation.NavigateToDialog;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import java.awt.event.ActionEvent;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;

/**
 * Wrapper for tab that contains means of drawing shapes for determining range and bearing.
 *
 * @since 5.0
 */
@PseudoBean
public class RangeAndBearingToolbarTabWrapper extends AToolbarTabWrapper
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String DYNAMIC_MEASURE_TOOLTIP = "Draw a dynamic measure arrow on the globe.";
    public static final String MEASURE_TOOLTIP = "Click two points to draw a measure arrow.";

    public static final String BULLSEYE_TOOLTIP = "Draw a Bullseye on the globe";
    public static final String RANGE_CIRCLE_TOOLTIP = "Draw a R&B Circle on the globe";
    public static final String DRIFTER_TOOLTIP = "Configure Drifter on a map item.";
    public static final String BLOODHOUND_TOOLTIP = "Configure Bloodhound on a map item.";

    private ToolbarButton dynamicMeasureButton;

    @Inject
    private RangeAndBearingController rangeAndBearingController;

    @PostConstruct
    private void postConstruct()
    {
        List<AToolbarComponent> components = new ArrayList<>();
        dynamicMeasureButton = createToggleButton(ButtonType.DYNAMIC_MEASURE, this::dynamicMeasureAction, DYNAMIC_MEASURE_TOOLTIP);
        ToolbarButton measureButton = createButton(ButtonType.MEASURE, this::measureAction, MEASURE_TOOLTIP);

        components.add(dynamicMeasureButton);
        components.add(measureButton);
        ToolbarButton bullseyeButton = createButton(ButtonType.BULLSEYE,
                e -> rangeAndBearingController.drawBullseye(), BULLSEYE_TOOLTIP);
        components.add(bullseyeButton);
        ToolbarButton circleButton = createButton(ButtonType.RB_CIRCLE,
                e -> rangeAndBearingController.drawCircle(), RANGE_CIRCLE_TOOLTIP);
        components.add(circleButton);
        components.add(createTogglePanelButton(CoreComponentType.DRIFTER_PANEL, DRIFTER_TOOLTIP));
        components.add(
                createButtonWithPopupMenu(
                        ButtonType.BLOODHOUND,
                        e -> uiSpaceManager.toggleComponent(CoreComponentType.BLOODHOUND_PANEL), new BloodhoundPopupMenuProvider(), BLOODHOUND_TOOLTIP)
                        .setDisplayText("Bloodhound"));

        components.add(new ToolbarSeparator(TabType.RANGE_AND_BEARING, components.size()));

        createTab(TabType.RANGE_AND_BEARING, components, true);
    }

    @Override
    protected void addComponents()
    {
        // No-op. Components are added in #postConstruct
    }

    /**
     * Button action that handles drawing a dynamic measure. When toggled on, lets the user draw a dynamic measure.
     * On toggle off, deletes the dynamic measure.
     *
     * @param e Action event.
     */
    private void dynamicMeasureAction(ActionEvent e)
    {
        var button = (JToggleButton) e.getSource();

        if (button.isSelected())
        {
            rangeAndBearingController.drawDynamicMeasure(error -> {
                toolbar.setToggleButtonState(dynamicMeasureButton.getId(), false);
                logger.warn("Shape creation failed", error);
            });
        } else
        {
            rangeAndBearingController.deleteDynamicMeasure();
        }
    }

    /**
     * Button action that handles drawing a measure arrow. When toggled on, lets the user pick two points for
     * the measurement arrow's anchors <i>(map-objects or locations)</i>.
     *
     * @param e Action event.
     * @since 5.5
     */
    private void measureAction(ActionEvent e)
    {
        rangeAndBearingController.drawMeasure();
    }

    /**
     * Deselect our toolbar button if the MO removed is the one we created.
     *
     * @since 5.2
     */
    @SuppressWarnings("unused")
    private void onMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        if (event.getMapObject().equals(rangeAndBearingController.getDrawnDynamicMeasureMapObject())) toolbar.setToggleButtonState(dynamicMeasureButton.getId(), false);
    }

    /**
     * @since 5.7
     */
    @PseudoBean
    private static class BloodhoundPopupMenuProvider implements IPopupMenuProvider
    {
        @Inject
        private ILocationTracker locationTracker;

        @Inject
        private NavigationManager navigationManager;

        private final JPopupMenu popupMenu = new JPopupMenu();

        public BloodhoundPopupMenuProvider()
        {
            popupMenu.add(new ButtonAction("Navigate To", NavigationConstants.NAVIGATE_TO_ICON, "Navigate to a map point",
                    e -> {
                        var dialog = new NavigateToDialog();
                        dialog.setFrom((IPointMapObject) locationTracker.getMarkerMapObject());
                        dialog.addCloseHandler(jDialog -> {
                            if (dialog.getNavigationConfiguration() != null)
                            {
                                navigationManager.navigate(dialog.getNavigationConfiguration());
                            }
                        });
                        dialog.setVisible(true);
                    }));
        }

        @Override
        public JPopupMenu getPopupMenu()
        {
            return popupMenu;
        }
    }
}
