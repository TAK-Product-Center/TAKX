package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import java.util.EventListener;

/**
 * Listener for selection changes in an {@link SelectColumnSelectionModel}.
 * 
 * @param <E> the type of entities in the selection model
 * @since 5.7 (Renamed from EntitySelectionListener in 5.8)
 */
@FunctionalInterface
@IncubatingApi(since = "5.7")
public interface SelectColumnSelectionListener<E> extends EventListener
{
    /**
     * Called when an entity selection state changes
     * 
     * @param entity the entity whose state has changed
     */
    void valueChanged(@Nonnull SelectColumnSelectionEvent<E> entity);
}
