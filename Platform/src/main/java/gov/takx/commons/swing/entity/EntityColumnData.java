package gov.takx.commons.swing.entity;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.lang.FastIndexList;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Manages a List of {@link EntityColumn} instances and their value caches. This class mainly serves to
 * prevent unnecessary calls to {@link EntityColumn#value()} functions, which can be potentially costly. When used as a
 * cache, it is the responsibility of the {@link EntityDefinition} to configure property change event mappings such that the
 * consuming {@link EntityTableModel} can adjust this cache when entity changes occur. That aside, the presence of this
 * class should be completely transparent to users of the Entity API.
 *
 * @param <E>
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntityColumnData<E>
{
    private final FastIndexList<EntityColumn<E, ?>> columns = new FastIndexList<>();
    @VisibleForTesting
    final List<Map<E, Object>> valueMaps = new ArrayList<>();

    /**
     * Get an entity column value.
     *
     * @param entity the entity
     * @param columnIndex the column index
     * @return the value for the given entity and column
     */
    @Nullable
    public Object getValue(@Nonnull E entity, int columnIndex)
    {
        return getValue(entity, getColumn(columnIndex), columnIndex);
    }

    /**
     * Get an entity column value.
     *
     * @param entity the entity
     * @param column the column
     * @return the value for the given entity and column
     * @param <V> the value type
     */
    @Nullable
    public <V> V getValue(@Nonnull E entity, @Nonnull EntityColumn<E, V> column)
    {
        return getValue(entity, column, getColumnIndex(column));
    }

    /**
     * Clears the value cache for an entity and column.
     *
     * @param entity the entity
     * @param columnIndex the column index
     */
    public void clearValue(@Nonnull E entity, int columnIndex)
    {
        var cache = valueMaps.get(columnIndex);
        if (cache != null) cache.remove(entity);
    }

    /**
     * Clears the value cache for an entity and array of column indices.
     *
     * @param entity the entity
     * @param columnIndices the column indices
     */
    public void clearValues(@Nonnull E entity, @Nonnull int[] columnIndices)
    {
        for (var i : columnIndices) clearValue(entity, i);
    }

    /**
     * Clears the value cache for all entities of a given column.
     *
     * @param columnIndex the column index
     */
    public void clearValues(int columnIndex)
    {
        var cache = valueMaps.get(columnIndex);
        if (cache != null) cache.clear();
    }

    /**
     * Clears the value cache for all columns for a specific entity.
     *
     * @param entity the entity
     */
    public void clearValues(@Nonnull E entity)
    {
        for (var i = 0; i < columns.size(); i++) clearValue(entity, i);
    }

    /**
     * Clears the value cache.
     */
    public void clearValues()
    {
        for (var i = 0; i < columns.size(); i++)
        {
            var cache = valueMaps.get(i);
            if (cache != null) cache.clear();
        }
    }

    /**
     * @return the size of the column List
     */
    public int getColumnCount()
    {
        return columns.size();
    }

    /**
     * Add a new entity column.
     *
     * @param column the entity column
     */
    public void addColumn(@Nonnull EntityColumn<E, ?> column)
    {
        columns.add(column);
        valueMaps.add(column.cached() ? new HashMap<>() : null);
    }

    /**
     * Remove an entity column.
     *
     * @param column the entity column to remove
     */
    @SuppressWarnings("unused")
    public void removeColumn(@Nonnull EntityColumn<E, ?> column)
    {
        var index = getColumnIndex(column);
        columns.remove(index);
        valueMaps.remove(index);
    }

    /**
     * Remove all columns from the column list and value cache.
     */
    @SuppressWarnings("unused")
    public void removeAllColumns()
    {
        columns.clear();
        valueMaps.clear();
    }

    /**
     * @param columnIndex the column index to return
     * @return the column at the specified index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Nonnull
    public EntityColumn<E, ?> getColumn(int columnIndex)
    {
        return columns.get(columnIndex);
    }

    /**
     * @return a copy of the List of all columns
     */
    @Nonnull
    public List<EntityColumn<E, ?>> getColumns()
    {
        return List.copyOf(columns);
    }

    /**
     * @param column the column to find
     * @return the index of the specified column in the column list
     */
    public int getColumnIndex(@Nonnull EntityColumn<E, ?> column)
    {
        return columns.indexOf(column);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private <V> V getValue(@Nonnull E object, @Nonnull EntityColumn<E, V> column, int columnIndex)
    {
        return column.cached() ? (V) valueMaps.get(columnIndex).computeIfAbsent(object,
                k -> column.value().apply(object)) : column.value().apply(object);
    }
}
