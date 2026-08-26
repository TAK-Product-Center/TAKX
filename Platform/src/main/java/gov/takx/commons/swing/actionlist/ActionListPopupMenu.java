package gov.takx.commons.swing.actionlist;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.SimpleAction;
import jakarta.annotation.Nonnull;

import javax.swing.Action;
import javax.swing.JPopupMenu;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseEvent;

/**
 * Popup menu that adds items according to an {@link ActionList} instance.
 * 
 * @param <T> the value type (should match the value type in {@link ActionList})
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public class ActionListPopupMenu<T> extends JPopupMenu
{
    /**
     * Show the popup menu for a list of actions and value, derived from the actionList parameter.
     * 
     * @param actionList the action list containing a list of actions and a value
     * @param mouseEvent the mouse event that triggered the popup
     */
    public void show(@Nonnull ActionList<T> actionList, @Nonnull MouseEvent mouseEvent)
    {
        removeAll();
        if (actionList.actions() != null)
        {
            setActionList(actionList, mouseEvent);
            show(mouseEvent.getComponent(), mouseEvent.getX(), mouseEvent.getY());
        }
    }

    @VisibleForTesting
    void setActionList(@Nonnull ActionList<T> actionList, @Nonnull MouseEvent mouseEvent)
    {
        if (actionList.actions() != null)
        {
            actionList.actions().forEach(action -> add(createPopupAction(actionList, mouseEvent, action)));
        }
    }

    /**
     * Creates an action for the popup menu from a given ActionList action. Currently, this only supports setting the action
     * name, enabled flag, and tooltip, but support for other properties could be added in the future.
     */
    private Action createPopupAction(@Nonnull ActionList<?> actionList, @Nonnull MouseEvent mouseEvent, @Nonnull Action action)
    {
        var name = action.getValue(Action.NAME).toString();
        if (action instanceof ActionListAction<?> actionListAction)
        {
            if (actionListAction.isSelected()) name = actionListAction.getSelectedName();
        }
        var popupAction = new SimpleAction(name, actionEvent -> {
            var actionListActionEvent = new ActionListActionEvent<>(actionEvent, -1, actionList);
            actionListActionEvent.setSource(new PointComponent(mouseEvent.getLocationOnScreen()));
            action.actionPerformed(actionListActionEvent);
        });
        if (action instanceof SimpleAction simpleAction)
        {
            popupAction.setTooltipText(simpleAction.getTooltipText());
        }
        popupAction.setEnabled(action.isEnabled());
        return popupAction;
    }

    /**
     * A component implementation that returns a specific value from {@link #getLocationOnScreen()}. This is necessary to
     * pass the screen location from a popup menu to an {@link Action}, which is usually impossible because the popup menu
     * closes before the action in invoked.
     */
    private static class PointComponent extends Component
    {
        private final Point point;

        private PointComponent(Point point)
        {
            this.point = point;
        }

        @Override
        public Point getLocationOnScreen()
        {
            return point;
        }
    }
}
