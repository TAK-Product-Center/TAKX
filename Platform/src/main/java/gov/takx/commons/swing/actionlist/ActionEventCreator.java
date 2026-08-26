package gov.takx.commons.swing.actionlist;

import gov.tak.api.annotation.DeprecatedApi;
import gov.takx.api.annotation.Internal;

import java.awt.event.ActionEvent;

/**
 * Interface for customizing the creation of {@link ActionEvent} instances.
 *
 * @since 5.3 (made top-level in 5.7)
 * @deprecated All necessary context can now be passed in {@link ActionListActionEvent} so this feature is no longer
 * necessary. This class and all associated methods can be removed.
 */
@FunctionalInterface
@Deprecated(since = "5.7", forRemoval = true)
@DeprecatedApi(since = "5.7", forRemoval = true, removeAt = "5.10")
@Internal
public interface ActionEventCreator
{
    /**
     * Create an {@link ActionEvent} from a given source event.  Consumers may want to create a subclass in order to carry
     * additional information with the original event.
     *
     * @param renderer The renderer that invoked this method
     * @param event The source event
     * @param row The table row on which the source event was triggered (where the button was clicked)
     * @return Derived action event
     * @see ActionListActionEvent
     */
    ActionEvent createActionEvent(ActionListTableCellRenderer renderer, ActionEvent event, int row);
}
