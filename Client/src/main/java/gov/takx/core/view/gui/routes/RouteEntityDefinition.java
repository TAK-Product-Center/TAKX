package gov.takx.core.view.gui.routes;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IRouteMapObject;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.commons.swing.actionlist.ActionList;
import gov.takx.commons.swing.actionlist.ActionListAction;
import gov.takx.commons.swing.actionlist.ActionListActionEvent;
import gov.takx.commons.swing.entity.mapobject.MapObjectEntityDefinition;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.Action;

import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;

import static gov.takx.core.navigation.NavigationConstants.NAVIGATE_TO_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.DELETE_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.EDIT_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.SETTINGS_ACTION_BUTTON_ICON;

/**
 * Entity definition for Route map objects.
 *
 * @since 5.7
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class RouteEntityDefinition extends MapObjectEntityDefinition<IRouteMapObject>
{
    @Inject
    private RoutesManager routesManager;

    private final Action settingsAction = new ActionListAction<IRouteMapObject>("Settings", SETTINGS_ACTION_BUTTON_ICON, mo -> routesManager.openRouteDetailsPanel(mo), null);
    private final Action editAction = new ActionListAction<IRouteMapObject>("Edit", EDIT_ACTION_BUTTON_ICON, mo -> routesManager.startRouteEditing(mo), null);
    private final Action navigateAction = new ActionListAction<IRouteMapObject>("Navigate", NAVIGATE_TO_ACTION_BUTTON_ICON, mo -> routesManager.navigateRoute(mo), null);
    private final Action deleteAction = new ActionListAction<>("Delete", DELETE_ACTION_BUTTON_ICON, this::delete);
    private final List<Action> actions = List.of(settingsAction, navigateAction, editAction, deleteAction);
    private final Supplier<Boolean> isEditInProgress;

    /**
     * Construct a new instance using the given supplier to determine if a Route edit is in progress.
     *
     * @param isEditInProgress supplier that returns {@code true} if a Route edit is in progress, otherwise {@code false}
     */
    public RouteEntityDefinition(@Nonnull Supplier<Boolean> isEditInProgress)
    {
        this.isEditInProgress = isEditInProgress;

        // Monitor EDITABLE to control the edit button in the actions column for that entity
        addPropertyChangeMapping(ShapeProperties.EDITABLE, getActionsColumn());
    }

    private void delete(@Nonnull ActionListActionEvent<IRouteMapObject> e)
    {
        ActionPopupFactory.yesNo(
                "<html>Delete Route: <b>" + e.requireValue().getName() + "</b>?</html>",
                (Component) e.getSource(),
                () -> routesManager.deleteRoute(e.requireValue()))
                .show();
    }

    @Override
    @Nullable
    public ActionList<IRouteMapObject> getActions(@Nonnull IRouteMapObject entity)
    {
        final boolean notEditing = !isEditInProgress.get(); // Disable all buttons when the route is being edited

        settingsAction.setEnabled(notEditing);
        editAction.setEnabled(notEditing && entity.isEditable());
        deleteAction.setEnabled(notEditing);
        navigateAction.setEnabled(notEditing);
        return new ActionList<>(actions, entity);
    }
}
