package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.TreeModelEvent;

/**
 * Event fired when {@link EntityTreeTableModel} models change, a simplified replacement for {@link TreeModelEvent}.
 *
 * @param entity the entity this event is for
 * @param child a child of the entity
 * @param type the event type
 * @param <E> the entity type
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public record EntityTreeModelEvent<E>(@Nonnull E entity, @Nullable E child, @Nonnull Type type)
{
    /**
     * The event type.
     */
    @IncubatingApi(since = "5.7")
    public enum Type
    {
        /**
         * Event type for a change to an entity in the model.
         */
        Change,
        /**
         * Event for an insert into the model.
         */
        Insert,
        /**
         * Event type for a removal from the model.
         */
        Remove,
        /**
         * Event type for a significant structure change.
         */
        Structure
    }
}
