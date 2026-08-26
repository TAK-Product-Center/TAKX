package gov.takx.commons.swing.actionlist;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.lang.ObjectUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.ActionEvent;

/**
 * Extension of {@link ActionEvent} that holds information related to the {@link ActionListTableCellRenderer}. Specifically
 * the row and cell value.
 *
 * @since 5.3 (made top-level in 5.7)
 * @param <T> the value type for the corresponding {@link ActionList}
 */
@IncubatingApi(since = "5.3")
public class ActionListActionEvent<T> extends ActionEvent
{
    private final ActionEvent sourceEvent;
    private final int row;
    private final ActionList<T> actionList;

    /**
     * Construct a new instance for the given source event, for an action that occurred on the given row and for the given
     * {@code actionList} value.
     * <p>
     * @apiNote This is exposed to accommodate {@link ActionEventCreator}. When that is removed the visibility can be reduced.
     * @param sourceEvent the source event, from the swing component that the cell editor generated
     * @param row the table row
     * @param actionList the table cell value
     * @since 5.7
     */
    protected ActionListActionEvent(@Nonnull ActionEvent sourceEvent, int row, @Nonnull ActionList<T> actionList)
    {
        super(sourceEvent.getSource(), ACTION_PERFORMED, sourceEvent.getActionCommand());
        this.row = row;
        this.sourceEvent = sourceEvent;
        this.actionList = actionList;
    }

    /**
     * @return the row the action event occurred on
     */
    public int getRow()
    {
        return row;
    }

    /**
     * @return the action list
     * @since 5.7
     */
    @Nullable
    public ActionList<T> getActionList()
    {
        return actionList;
    }

    /**
     * @return the value that the action was performed on
     * @since 5.7
     */
    @Nullable
    public T getValue()
    {
        return actionList != null ? actionList.value() : null;
    }

    /**
     * @return the source event
     * @since 5.8
     */
    @Nonnull
    public ActionEvent getSourceEvent()
    {
        return sourceEvent;
    }

    /**
     * @return the value that the action was performed on
     * @since 5.8
     * @throws IllegalStateException if action list or value is {@code null}
     */
    @Nonnull
    public T requireValue()
    {
        return ObjectUtils.requireNonNullState(getValue(), "No value for action event");
    }
}
