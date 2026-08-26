package gov.takx.commons.swing.actionlist;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nullable;

import javax.swing.Action;

import java.util.List;

/**
 * The cell value class used for table columns that use {@link ActionListTableCellRendererEditor}.
 *
 * @param actions the list of actions to be rendered
 * @param value a custom value that can be set by the table model, to provide extra context to action listeners
 * @since 5.7
 * @param <T> the value type
 */
@IncubatingApi(since = "5.7")
public record ActionList<T>(@Nullable List<Action> actions, @Nullable T value)
{
}
