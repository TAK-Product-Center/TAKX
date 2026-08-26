package gov.takx.commons.swing.entity;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents the definition of an entity to be used with the Entity table API. Contains the list of {@link EntityColumn} columns
 * as well as a map of properties that should trigger column updates.
 * 
 * @param <E> the entity type
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntityDefinition<E>
{
    private final List<EntityColumn<E, ?>> columns = new ArrayList<>();
    private final Multimap<String, EntityColumn<E, ?>> propertyChangeMap = ArrayListMultimap.create();
    private final EntityColumnData<E> data = new EntityColumnData<>();

    /**
     * @param index the column index
     * @return the column at the given index
     */
    @Nonnull
    protected EntityColumn<E, ?> getColumn(int index)
    {
        return columns.get(index);
    }

    /**
     * @return the columns in this model
     */
    @Nonnull
    protected List<EntityColumn<E, ?>> getColumns()
    {
        return List.copyOf(columns);
    }

    /**
     * @return the map of property names to entity columns that should be updated when the property changes
     */
    @Nonnull
    protected Multimap<String, EntityColumn<E, ?>> getPropertyChangeMap()
    {
        return ArrayListMultimap.create(propertyChangeMap);
    }

    /**
     * @param property the property name
     * @param column the column that should update when the property changes
     */
    protected void addPropertyChangeMapping(@Nonnull String property, @Nonnull EntityColumn<E, ?> column)
    {
        propertyChangeMap.put(property, column);
    }

    /**
     * @param properties the List of property names
     * @param columns the List of columns that should update when any of the properties change
     */
    @SuppressWarnings("unused")
    protected void addPropertyChangeMapping(@Nonnull List<String> properties, @Nonnull List<EntityColumn<E, ?>> columns)
    {
        properties.forEach(property -> propertyChangeMap.putAll(property, columns));
    }

    /**
     * @param columns columns to add to this definition
     */
    @SafeVarargs
    protected final void addColumns(@Nonnull EntityColumn<E, ?>... columns)
    {
        for (var column : columns)
        {
            this.columns.add(column);
            data.addColumn(column);
        }
    }

    /**
     * @return the entity data cache
     */
    protected EntityColumnData<E> getData()
    {
        return data;
    }

    /**
     * Add a property change listener to an entity. Subclasses should override to call the proper method for the generic type
     * {@code <E>}.
     *
     * @param entity the model entity
     * @param propertyName the property name
     * @param listener the listener that will change events
     */
    protected void addPropertyChangeListener(@Nonnull E entity, @Nonnull String propertyName, @Nonnull PropertyChangeListener listener)
    {
    }

    /**
     * Remove a property change listener from an entity. Subclasses should override to call the proper method for the
     * generic type {@code <E>}.
     *
     * @param entity the model entity
     * @param propertyName the property name
     * @param propertyChangeListener the listener that should longer receive change events
     */
    protected void removePropertyChangeListener(@Nonnull E entity, @Nonnull String propertyName, @Nonnull PropertyChangeListener propertyChangeListener)
    {
    }
}
