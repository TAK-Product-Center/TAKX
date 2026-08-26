package gov.takx.api.explorer;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.commons.swing.actionlist.ActionList;
import gov.takx.commons.swing.actionlist.ActionListActionEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.ActionEvent;

/**
 * Extension of {@link ActionEvent} for actions fired from explorer API components. Holds references to the {@link IExplorable}.
 *
 * @since 5.3
 * @deprecated Use {@link ActionListActionEvent}
 */
@DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
@Deprecated(since = "5.7", forRemoval = true)
public class ExplorableActionEvent extends ActionListActionEvent<IExplorable>
{
    /**
     * Construct a new instance from a given source action event, that occurred on a given row and tied to the given explorable.
     *
     * @param actionEvent the source action event that triggered the action
     * @param row the table model row
     * @param explorable the explorable the action was triggered on
     * @since 5.7
     */
    public ExplorableActionEvent(@Nonnull ActionEvent actionEvent, int row, @Nonnull IExplorable explorable)
    {
        super(actionEvent, row, new ActionList<>(null, explorable));
    }

    /**
     * Construct a new instance with the given parameters.
     *
     * @param explorable the explorable the action was triggered on
     * @param actionEvent the source action event that triggered the action
     */
    @SuppressWarnings("unused")
    public ExplorableActionEvent(@Nonnull IExplorable explorable, @Nonnull ActionEvent actionEvent)
    {
        super(actionEvent, -1, new ActionList<>(null, explorable));
    }

    /**
     * @return the explorable that the action was triggered on
     */
    @Nullable
    public IExplorable getExplorable()
    {
        return getValue();
    }
}