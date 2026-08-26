package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.TriStateJCheckBox;
import jakarta.annotation.Nonnull;

import java.util.EventObject;
import java.util.List;

/**
 * Event fired when selection state changes for entities in the {@link EntityTableSelectionModel}.
 *
 * @param <E> the entity type
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntitySelectionEvent<E> extends EventObject
{
    private final TriStateJCheckBox.State state;
    private final List<E> entities;

    /**
     * Constructs an event with the given source, selection state, and entity List.
     *
     * @param source the object on which the Event initially occurred
     * @param state the selection state of the entity
     * @param entities the entities that were adjusted
     * @throws IllegalArgumentException if source is null
     */
    public EntitySelectionEvent(@Nonnull Object source, @Nonnull TriStateJCheckBox.State state, @Nonnull List<E> entities)
    {
        super(source);
        this.state = state;
        this.entities = entities;
    }

    /**
     * @return the current selection state of the entities
     */
    @Nonnull
    public TriStateJCheckBox.State getState()
    {
        return state;
    }

    /**
     * @return the List of objects whose selection states have changed
     */
    @Nonnull
    public List<E> getEntities()
    {
        return List.copyOf(entities);
    }
}
