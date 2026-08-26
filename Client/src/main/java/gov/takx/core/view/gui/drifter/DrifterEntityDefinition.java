package gov.takx.core.view.gui.drifter;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.commons.swing.actionlist.ActionList;
import gov.takx.commons.swing.actionlist.ActionListAction;
import gov.takx.commons.swing.entity.EntityColumn;
import gov.takx.commons.swing.entity.mapobject.MapObjectEntityDefinition;
import gov.takx.core.drifter.DrifterController;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.UIManager;

import java.awt.Color;
import java.awt.Component;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;
import static gov.takx.platform.ui.UIResources.ACTION_BUTTON_IMAGE_SIZE;
import static gov.takx.platform.ui.UIResources.DELETE_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.EDIT_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.PAUSE_ICON_FILE;
import static gov.takx.platform.ui.UIResources.PLAY_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.REFRESH_ACTION_BUTTON_ICON;

/**
 * Entity definition for Map Objects that contain {@link gov.takx.api.drifter.DrifterConfiguration} information.
 *
 * @since 5.7
 */
@PseudoBean
public class DrifterEntityDefinition extends MapObjectEntityDefinition<IMapObject>
{
    public static final String DELAY_ICON_NAME = "hourglass_top.svg";

    private static final Icon DELAY_ICON = UIResources.getSizedIcon(PLATFORM_ICONS, DELAY_ICON_NAME, ACTION_BUTTON_IMAGE_SIZE, true);
    private static final Icon PAUSE_INVERSE_ICON = UIResources.getSizedColorizedIcon(UIResources.getUrl(PLATFORM_ICONS, PAUSE_ICON_FILE), ACTION_BUTTON_IMAGE_SIZE, UIManager.getColor("Table.background"));

    private static final String DETAILS_STRING_FORMAT = "Heading: %s%s<br>Speed: %s";
    private static final String DELETE_POPUP_STRING_FORMAT = "<html>Delete Drifter Configuration for <b>%s<b>?</html>";

    @Inject
    private DrifterController controller;

    private final Action simulateTimeAction = new ActionListAction<IMapObject>("Simulate Time Delay", DELAY_ICON, SimulateTimeDrifterDialog::show, null);
    private final Action editAction = new ActionListAction<IMapObject>("Edit", EDIT_ACTION_BUTTON_ICON, EditDrifterConfigurationDialog::show, null);
    private final Action deleteAction = new ActionListAction<IMapObject>("Delete", DELETE_ACTION_BUTTON_ICON,
            e -> ActionPopupFactory
                    .yesNo(DELETE_POPUP_STRING_FORMAT.formatted(e.requireValue().getName()), (Component) e.getSource(), () -> controller.deleteConfiguration(e.requireValue(), false))
                    .show());
    private final Action restartAction = new ActionListAction<IMapObject>("Restart", REFRESH_ACTION_BUTTON_ICON, mo -> controller.restart(mo), null);
    private final ActionListAction<IMapObject> pauseResumeAction = ActionListAction.<IMapObject>builder()
            .nameIconValueConsumer("Resume", PLAY_ACTION_BUTTON_ICON, mapObject -> controller.togglePaused(mapObject))
            .selectedNameIconBackground("Pause", PAUSE_INVERSE_ICON, Color.WHITE)
            .build();

    private final List<Action> actions = List.of(pauseResumeAction, restartAction, simulateTimeAction, editAction, deleteAction);

    final EntityColumn<IMapObject, Double> speedColumn;

    /**
     * Construct a new definition, and add standard property change mappings for drifter, heading, and speed properties.
     */
    public DrifterEntityDefinition()
    {
        speedColumn = EntityColumn.<IMapObject, Double>builder().name("Speed").type(Double.class).value(IMapObject::getSpeedInMps).cached(true).build();
        addColumns(speedColumn);
        addPropertyChangeMapping(IMapObject.DRIFTER, getActionsColumn());
        addPropertyChangeMapping(IMapObject.HEADING, getSummaryColumn());
        addPropertyChangeMapping(List.of(IMapObject.SPEED), List.of(getSummaryColumn(), speedColumn));
    }

    @Override
    @Nullable
    protected ActionList<IMapObject> getActions(@Nonnull IMapObject entity)
    {
        var configuration = controller.requireDrifterConfiguration(entity);
        restartAction.setEnabled(controller.getRoute(configuration) != null);
        pauseResumeAction.setSelected(!configuration.paused());
        return new ActionList<>(actions, entity);
    }

    @Override
    @Nullable
    protected String getDetailsHtml(@Nonnull IMapObject entity)
    {
        var configuration = controller.requireDrifterConfiguration(entity);
        var route = controller.getRoute(configuration);
        var speed = getData().getValue(entity, speedColumn);
        //noinspection DataFlowIssue speed is never null
        return DETAILS_STRING_FORMAT.formatted(
                UserPreferenceEntry.DISPLAY_BEARING_UNIT.getValueAs(BearingUnits.class).format(entity.getLocation(), 0, entity.getHeading()),
                route == null ? "" : " on " + route.getName(),
                UserPreferenceEntry.DISPLAY_SPEED_UNITS.getValueAs(SpeedUnits.class).format(speed, 0));
    }
}
