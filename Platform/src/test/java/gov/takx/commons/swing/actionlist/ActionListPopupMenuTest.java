package gov.takx.commons.swing.actionlist;

import gov.takx.commons.swing.SimpleAction;
import gov.takx.commons.swing.entity.TestEntity;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JMenuItem;

import java.awt.event.MouseEvent;
import java.util.List;

/**
 * @since 5.8
 */
public class ActionListPopupMenuTest
{
    @Test
    public void test()
    {
        var popupMenu = new ActionListPopupMenu<TestEntity>();
        var action0 = new SimpleAction("Action 0", null, null);
        var entity = new TestEntity("E 0", 0);
        var actionList = new ActionList<>(List.of(action0), entity);
        var button = new JButton();
        var mouseEvent = new MouseEvent(button, 0, 0, 0, 0, 0, 1, true, 0);

        popupMenu.setActionList(actionList, mouseEvent);
        Assertions.assertThat(popupMenu.getComponentCount()).isEqualTo(actionList.actions().size());
        for (var i = 0; i < actionList.actions().size(); i++)
        {
            var action = actionList.actions().get(i);
            var jMenuItem = (JMenuItem) popupMenu.getComponent(i);
            var menuItemAction = jMenuItem.getAction();
            Assertions.assertThat(menuItemAction.getValue(Action.NAME)).isEqualTo(action.getValue(Action.NAME));
        }
    }
}
