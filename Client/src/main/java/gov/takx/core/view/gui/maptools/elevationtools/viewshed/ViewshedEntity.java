package gov.takx.core.view.gui.maptools.elevationtools.viewshed;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.commons.swing.actionlist.ActionList;
import gov.takx.commons.swing.actionlist.ActionListAction;
import gov.takx.commons.swing.entity.common.CommonEntityDefinition;
import gov.takx.core.globe.ViewshedManager;
import gov.takx.core.globe.tak.layer.ViewshedLayer;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;

import javax.swing.Action;

import java.awt.Component;
import java.util.List;

import static gov.takx.platform.ui.UIResources.DELETE_ACTION_BUTTON_ICON;
import static gov.takx.platform.ui.UIResources.GOTO_ACTION_BUTTON_ICON;

/**
 * Entity definition for {@link ViewshedLayer} instances.
 * @since 5.7
 */
@PseudoBean
public class ViewshedEntity extends CommonEntityDefinition<ViewshedLayer>
{
    private static final String DELETE_POPUP_STRING_FORMAT = "<html>Delete <b>%s<b>?</html>";

    @Inject
    private ViewshedManager viewshedManager;

    @Inject
    private IGlobeManager globeManager;

    @SuppressWarnings("DataFlowIssue") // the getActions method always passes a value to the ActionList
    private final Action deleteAction = new ActionListAction<ViewshedLayer>("Delete", DELETE_ACTION_BUTTON_ICON)
            .withEventConsumer(e -> ActionPopupFactory
                    .yesNo(DELETE_POPUP_STRING_FORMAT.formatted(getName(e.getValue())), (Component) e.getSource(), () -> viewshedManager.deleteViewshed(e.getValue()))
                    .show());
    @SuppressWarnings("DataFlowIssue") // the getActions method always passes a value to the ActionList
    private final Action gotoAction = new ActionListAction<ViewshedLayer>("Goto", GOTO_ACTION_BUTTON_ICON)
            .withEventConsumer(e -> globeManager.getLastFocusedGlobe().goTo((ILocation) e.getValue().getPointOfInterest()));

    private final List<Action> actions = List.of(gotoAction, deleteAction);

    @Nullable
    @Override
    protected String getName(@Nonnull ViewshedLayer entity)
    {
        return entity.getName();
    }

    @Nullable
    @Override
    protected ActionList<ViewshedLayer> getActions(@Nonnull ViewshedLayer entity)
    {
        return new ActionList<>(actions, entity);
    }
}
