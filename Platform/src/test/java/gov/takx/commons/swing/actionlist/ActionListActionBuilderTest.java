package gov.takx.commons.swing.actionlist;

import gov.takx.platform.ui.UIResources;
import org.hibernate.internal.util.MutableObject;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.util.Collections;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @since 5.8
 */
public class ActionListActionBuilderTest
{
    @Test
    public void builderTest()
    {
        var value = "my value";
        var actionList = new ActionList<>(Collections.emptyList(), value);
        var sourceEvent = new ActionEvent(this, -1, "");
        var event = new ActionListActionEvent<>(sourceEvent, -1, actionList);

        var name = "test";
        var icon = UIResources.ADD_ICON;
        var valueHolder = new MutableObject<>();
        Consumer<Object> valueConsumer = valueHolder::set;
        var selName = "selected";
        var selIcon = UIResources.REMOVE_ICON;
        var selColor = Color.YELLOW;

        // Ensure chaining works
        var actionListAction = ActionListAction.builder()
                .selectedName("")
                .selectedIcon(null)
                .selectedBackground(null)
                .valueConsumer(v -> {
                })
                .eventConsumer(e -> {
                }).build();

        actionListAction = ActionListAction.builder()
                .nameIconValueConsumer(name, icon, valueConsumer)
                .selectedNameIconBackground(selName, selIcon, selColor)
                .build();

        assertThat(actionListAction.getName()).isEqualTo(name);
        assertThat(actionListAction.getIcon()).isEqualTo(icon);
        assertThat(actionListAction.getSelectedName()).isEqualTo(selName);
        assertThat(actionListAction.getSelectedIcon()).isEqualTo(selIcon);
        assertThat(actionListAction.getSelectedBackground()).isEqualTo(selColor);

        actionListAction.actionPerformed(event);
        assertThat(valueHolder.get()).isEqualTo(value);
        valueHolder.set(null);

        actionListAction = ActionListAction.builder()
                .nameIconEventConsumer(name, icon, e -> valueHolder.set(e.requireValue()))
                .build();

        actionListAction.actionPerformed(event);
        assertThat(valueHolder.get()).isEqualTo(value);
    }
}
