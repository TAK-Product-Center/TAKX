package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

/**
 * A model listener for {@link EntityTreeTableModel}, designed to be simpler than {@link javax.swing.event.TreeModelListener}.
 *
 * @param <E> the entity type
 * @since 5.7
 */
@FunctionalInterface
@IncubatingApi(since = "5.7")
public interface EntityTreeModelListener<E>
{
    /**
     * Invoked when the model changes.
     * 
     * @param event the event
     */
    void modelChanged(@Nonnull EntityTreeModelEvent<E> event);
}
