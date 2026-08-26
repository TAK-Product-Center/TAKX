package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.swing.TriStateJCheckBox;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is designed to track selections made using the {@link EntityTable#selectColumn selectColumn} of an {@link EntityTable}
 * and thus does not replace or interfere with the standard table {@link javax.swing.ListSelectionModel}, which tracks selections
 * made by clicking rows. Because the select column uses {@link TriStateJCheckBox.State}, this class
 * also uses it. However, this does not force consumers to use a 3-state setup, as that checkbox will operate by default in
 * 2-state mode. This class is capable of working with both forms.
 * 
 * @param <E> The type of entities tracked in this model
 * @since 5.7 (Renamed from EntityTableSelectionModel in 5.8)
 */
@IncubatingApi(since = "5.7")
public class SelectColumnSelectionModel<E>
{
    private final List<SelectColumnSelectionListener<E>> selectionListeners = new ArrayList<>();
    private final Map<E, TriStateJCheckBox.State> selected = new HashMap<>(); // This map should never hold State.NOT_SELECTED values

    /**
     * @param entity the entity to select, using the {@link TriStateJCheckBox.State#SELECTED} state
     */
    public void addSelection(@Nonnull E entity)
    {
        setSelectionState(entity, TriStateJCheckBox.State.SELECTED);
    }

    /**
     * @param entity the entity to unselect
     */
    public void removeSelection(@Nonnull E entity)
    {
        setSelectionState(entity, TriStateJCheckBox.State.NOT_SELECTED);
    }

    /**
     * Clears the entire selection.
     */
    public void clearSelection()
    {
        var cleared = List.copyOf(selected.keySet());
        selected.clear();
        var event = new SelectColumnSelectionEvent<>(this, TriStateJCheckBox.State.NOT_SELECTED, cleared);
        selectionListeners.forEach(listener -> listener.valueChanged(event));
    }

    /**
     * @param entity the entity to check 
     * @return {@code true} if the entity is either {@link TriStateJCheckBox.State#SELECTED} or {@link TriStateJCheckBox.State#INDETERMINATE}
     */
    public boolean isSelected(@Nonnull E entity)
    {
        return selected.containsKey(entity);
    }

    /**
     * @param entity the entity to check
     * @return the entities selection state
     */
    @SuppressWarnings("unused")
    @Nonnull
    public TriStateJCheckBox.State getSelectionState(@Nonnull E entity)
    {
        return selected.getOrDefault(entity, TriStateJCheckBox.State.NOT_SELECTED);
    }

    /**
     * @param entity the entity to set the selection state for
     * @param state the new selection state
     */
    @SuppressWarnings("unused")
    public void setSelectionState(@Nonnull E entity, @Nonnull TriStateJCheckBox.State state)
    {
        if (state == TriStateJCheckBox.State.NOT_SELECTED)
        {
            selected.remove(entity);
        } else
        {
            selected.put(entity, state);
        }

        var event = new SelectColumnSelectionEvent<>(this, state, List.of(entity));
        selectionListeners.forEach(listener -> listener.valueChanged(event));
    }

    /**
     * @param listener the selection listener to add
     */
    @SuppressWarnings("unused")
    public void addSelectionListener(@Nonnull SelectColumnSelectionListener<E> listener)
    {
        selectionListeners.add(listener);
    }

    /**
     * @param listener the selection listener to remove
     */
    @SuppressWarnings("unused")
    public void removeSelectionListener(@Nonnull SelectColumnSelectionListener<E> listener)
    {
        selectionListeners.remove(listener);
    }
}
