package gov.takx.commons.swing.actionlist;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.annotation.DeprecatedApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.Action;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

import java.awt.Component;

/**
 * Editor that uses {@link ActionListTableCellRenderer} to render buttons, and then attaches action listeners to them so that
 * they actually function within table cells.
 *
 * @since 5.6 (made top-level in 5.7)
 */
class ActionListTableCellEditor extends AbstractCellEditor implements TableCellEditor
{
    @VisibleForTesting
    final ActionListTableCellRenderer renderer;
    private ActionList<?> value;
    private int lastEditedRow = -1;
    private ActionEventCreator eventCreator;
    private boolean stopEditOnAction;

    /**
     * Construct a new instance that uses a fresh {@link ActionListTableCellRenderer} to create buttons.
     */
    ActionListTableCellEditor()
    {
        renderer = new ActionListTableCellRenderer()
        {
            @Nonnull
            @Override
            AbstractButton createButton(@Nonnull JTable table, @Nonnull Action action)
            {
                var button = super.createButton(table, action);
                button.addActionListener(actionEvent -> {
                    // Fire the action for this button index on the last row edited
                    var event = eventCreator == null ? new ActionListActionEvent<>(actionEvent, lastEditedRow, value) : eventCreator.createActionEvent(renderer, actionEvent, lastEditedRow);
                    action.actionPerformed(event);
                    if (stopEditOnAction) table.editingStopped(null);
                });
                return button;
            }
        };
    }

    /**
     * @param eventCreator the instance to use when creating action events
     * @since 5.7
     */
    protected void setEventCreator(ActionEventCreator eventCreator)
    {
        this.eventCreator = eventCreator;
    }

    @Override
    @Nonnull
    public Component getTableCellEditorComponent(@Nonnull JTable table, @Nullable Object value, boolean isSelected, int row, int column)
    {
        this.value = (ActionList<?>) value;
        lastEditedRow = table.convertRowIndexToModel(row);
        return renderer.getTableCellRendererComponent(table, value, isSelected, false, row, column);
    }

    @Override
    @Nullable
    public Object getCellEditorValue()
    {
        return value;
    }

    /**
     * This flag a temporary fix for issues in the explorer. When rows are deleted the table is not properly re-rendered
     * and buttons from the prior editor can remain in place for the new row. This is more of an issue with that table
     * and when used correctly this flag is not necessary. Once we transition to using EntityTable this property and all
     * associated logic will be removed.
     *
     * @param stopEditOnAction {@code true} if table editing should be cancelled after an action is performed, otherwise
     * {@code false}
     * @deprecated this feature is only necessary to address an explorer bug
     */
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    @Deprecated(forRemoval = true)
    public void setStopEditOnAction(boolean stopEditOnAction)
    {
        this.stopEditOnAction = stopEditOnAction;
    }

    /**
     * @return {@code true} if table editing should be cancelled after an action is performed, otherwise {@code false}
     * @deprecated this feature is only necessary to address an explorer bug
     */
    @DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
    @Deprecated(forRemoval = true)
    public boolean isStopEditOnAction()
    {
        return stopEditOnAction;
    }
}
