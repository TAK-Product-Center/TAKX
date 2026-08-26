package gov.takx.commons.swing.actionlist;

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.factories.FormFactory;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.ColumnSpec;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import com.jgoodies.forms.layout.Sizes;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.DefaultJPanelTableCellRenderer;
import gov.takx.commons.swing.FormLayoutUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.plaf.UIResource;
import javax.swing.table.TableCellRenderer;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extension of {@link DefaultJPanelTableCellRenderer} that handles rendering for the
 * {@link ActionListTableCellRendererEditor}. This class renders buttons in a {@link FormLayout}, with column gaps at the
 * beginning and end of the layout as well as between each button.
 *
 * @apiNote This is exposed to accommodate {@link ActionEventCreator}. When that is removed the visibility can be reduced to
 * package-private.
 * @since 5.3 (made top-level in 5.7)
 */
@IncubatingApi(since = "5.3")
public class ActionListTableCellRenderer extends DefaultJPanelTableCellRenderer implements TableCellRenderer, UIResource
{
    private final Map<Action, AbstractButton> buttonMap = new HashMap<>();
    private final FormLayout layout = new FormLayout();
    private final CellConstraints cellConstraints = new CellConstraints();
    private final List<Action> actions = new ArrayList<>();
    private int actionCount;

    /**
     * Construct a new instance to render buttons in this panel using a {@link FormLayout}.
     */
    ActionListTableCellRenderer()
    {
        layout.appendRow(new RowSpec(RowSpec.DEFAULT, Sizes.DEFAULT, RowSpec.DEFAULT_GROW));
        setLayout(layout);
        setOpaque(true);
    }

    @Override
    public ActionListTableCellRenderer getTableCellRendererComponent(@Nonnull JTable table, @Nullable Object value, boolean isSelected, boolean hasFocus, int row, int column)
    {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        var actionList = (ActionList<?>) value;
        List<Action> actions = actionList != null && actionList.actions() != null ? actionList.actions() : Collections.emptyList();
        resizeLayout(actions.size());

        // Adds buttons to the layout for the necessary Actions
        for (var i = 0; i < actions.size(); i++)
        {
            var action = actions.get(i);
            var button = getOrCreateButton(table, action);
            configureButton(button, action, actionList.value(), row);
            addButtonToLayout(action, button, i);
        }

        // Clears any remaining Actions from the layout
        for (var i = actions.size(); i < this.actions.size(); i++)
        {
            addButtonToLayout(null, null, i);
        }
        return this;
    }

    /**
     * Creates a new {@link AbstractButton} for the specified action.
     *
     * @param table the Table that this button will be rendered in
     * @param action the Action that this button will trigger
     * @return the AbstractButton
     */
    @Nonnull
    AbstractButton createButton(@Nonnull JTable table, @Nonnull Action action)
    {
        var button = new JButton();
        button.setOpaque(true);
        return button;
    }

    /**
     * Configure the given button based on the action, object, and row information.
     *
     * @param button the button to configure
     * @param action the action (See {@link ActionList#actions()})
     * @param value the value (See {@link ActionList#value()})
     * @param row the row
     * @since 5.7
     */
    @SuppressWarnings("unchecked")
    void configureButton(AbstractButton button, Action action, Object value, int row)
    {
        button.setEnabled(action.isEnabled());
        button.setVisible(true);
        button.setSelected(false);
        button.setForeground(getForeground());

        // Default background to null so that table color can show through
        Color background = null;
        var description = action.getValue(Action.SHORT_DESCRIPTION);
        var name = action.getValue(Action.NAME);
        var icon = (Icon) action.getValue(Action.SMALL_ICON);
        var text = description != null ? description : name;

        if (action instanceof ActionListAction<?> actionListAction)
        {
            button.setSelected(actionListAction.isSelected());
            if (actionListAction.isSelected())
            {
                if (actionListAction.getSelectedIcon() != null) icon = actionListAction.getSelectedIcon();
                if (actionListAction.getSelectedBackground() != null) background = actionListAction.getSelectedBackground();
                if (actionListAction.getSelectedName() != null) text = actionListAction.getSelectedName();
            }
        }
        button.setBackground(background);
        button.setIcon(icon);
        button.setToolTipText(text != null ? text.toString() : null);

        if (action instanceof ActionListAction<?> actionListAction)
        {
            var buttonConfigurer = (ActionListButtonConfigurer<Object>) actionListAction.getButtonConfigurer();
            if (buttonConfigurer != null) buttonConfigurer.configureButton(button, action, value, row);
        }
    }

    /**
     * Gets, or creates, a new {@link JButton} for the specified {@link Action}. We re-use button instances because the click event and
     * action event handling is manual anyway.
     *
     * @param table the Table the button will be rendered in
     * @param action the Action the button will trigger
     * @return the existing or newly created button
     * @since 5.3 (added table parameter in 5.6)
     */
    @Nonnull
    @VisibleForTesting
    AbstractButton getOrCreateButton(@Nonnull JTable table, Action action)
    {
        AbstractButton button;
        if (!buttonMap.containsKey(action))
        {
            button = createButton(table, action);
            buttonMap.put(action, button);
        } else
        {
            button = buttonMap.get(action);
        }
        return button;
    }

    private void addButtonToLayout(Action action, AbstractButton button, int index)
    {
        var current = actions.get(index);
        if (current != action)
        {
            if (current != null)
            {
                var currentButton = buttonMap.get(current);
                remove(currentButton);
            }
            actions.set(index, action);
            if (button != null)
            {
                var cons = cellConstraints.xy((index * 2) + 2, 1);
                add(button, cons);
            }
        }
    }

    private void resizeLayout(int newActionCount)
    {
        if (actionCount != newActionCount)
        {
            var currentColumnCount = layout.getColumnCount();

            if (newActionCount == 0)
            {
                for (var i = 0; i < currentColumnCount; i++) updateOrAddColumn(i, FormLayoutUtils.HIDDEN_COLUMN);
            } else
            {
                updateOrAddColumn(0, FormFactory.RELATED_GAP_COLSPEC);
                if (newActionCount > actionCount)
                {
                    // show/add needed columns
                    var column = actionCount * 2 + 1; // start at the first missing action column
                    for (var i = actionCount; i < newActionCount; i++)
                    {
                        updateOrAddColumn(column++, FormFactory.DEFAULT_COLSPEC);
                        updateOrAddColumn(column++, FormFactory.RELATED_GAP_COLSPEC);
                    }
                    for (var i = actions.size(); i < newActionCount; i++) actions.add(null); // pre-fill action list so that get/set ops always succeed
                } else
                {
                    // hide unneeded columns
                    var column = newActionCount * 2 + 1; // start at the first unneeded action column
                    for (var i = column; i < currentColumnCount; i++) updateOrAddColumn(i, FormLayoutUtils.HIDDEN_COLUMN);
                }
            }
            doLayout();
            actionCount = newActionCount;
        }
    }

    private void updateOrAddColumn(int index, ColumnSpec spec)
    {
        if (index < layout.getColumnCount())
        {
            layout.setColumnSpec(index + 1, spec);
        } else
        {
            layout.appendColumn(spec);
        }
    }
}
