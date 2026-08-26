package gov.takx.commons.swing.actionlist;

import gov.takx.commons.swing.SimpleAction;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.mutable.MutableObject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTable;

import java.util.ArrayList;
import java.util.List;

/**
 * @since 5.7
 */
public class ActionListTableCellRendererEditorTest
{
    @SuppressWarnings("DataFlowIssue")
    @Test
    void testRenderer()
    {
        var rendererEditor = new ActionListTableCellRendererEditor();
        var renderer = rendererEditor.renderer;

        Icon icon = new ImageIcon();
        var action = new SimpleAction("Test Action", icon, null);
        var action2 = new ActionListToggleAction<>("Test Action", icon, o -> true);
        var actionList = new ActionList<>(List.of(action, action2), null);
        var table = new JTable();
        renderer.getTableCellRendererComponent(table, actionList, false, false, 0, 0);

        for (var act : actionList.actions())
        {
            var simpleAction = (SimpleAction) act;
            var button = renderer.getOrCreateButton(table, simpleAction);
            Assertions.assertThat(button.getToolTipText()).isEqualTo(simpleAction.getName());
            Assertions.assertThat(button.getIcon()).isEqualTo(simpleAction.getIcon());
        }
        var button2 = renderer.getOrCreateButton(table, action2);
        Assertions.assertThat(button2.isSelected()).isTrue();
    }

    @Test
    @SuppressWarnings("DataFlowIssue")
    void testEditor()
    {
        var rendererEditor = new ActionListTableCellRendererEditor();
        var editor = rendererEditor.editor;

        var triggered = new MutableBoolean(false);
        Icon icon = new ImageIcon();
        Object value = "A value";
        List<Action> actions = new ArrayList<>();
        var actionList = new ActionList<>(actions, value);
        var consumed = new MutableObject<>();

        var action1 = new SimpleAction("Test Action", icon, e -> {
            var event = (ActionListActionEvent<?>) e;
            triggered.setTrue();
            Assertions.assertThat(event.getActionList()).isEqualTo(actionList);
        });
        var action2 = new ActionListToggleAction<>("Test Action", icon, o -> true);
        var action3 = new ActionListAction<>("Test Action", icon).withValueConsumer(consumed::setValue);
        actions.addAll(List.of(action1, action2, action3));
        var table = new JTable();
        editor.getTableCellEditorComponent(table, actionList, false, 0, 0);

        // Click all buttons
        for (var action : actionList.actions())
        {
            editor.renderer.getOrCreateButton(table, action).doClick();
        }
        Assertions.assertThat(triggered.getValue()).isTrue();
        Assertions.assertThat(consumed.getValue()).isEqualTo(value);
    }
}
