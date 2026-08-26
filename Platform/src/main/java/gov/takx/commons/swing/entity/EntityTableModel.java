package gov.takx.commons.swing.entity;

import com.google.common.collect.Multimap;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.commons.lang.FastIndexList;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.commons.swing.AbstractTableModelExt;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.beans.PropertyChangeListener;
import java.util.List;
import java.util.Map;

/**
 * Table model intended to be used with {@link EntityTable} and {@link EntityDefinition} or subclasses.
 * <p>Like other swing models, this should only be modified from the EDT. This includes adding / removing
 * objects and firing listeners. The usage of {@link InvokeOnEDT} is <b>intentionally</b> limited to
 * the {@link #entityPropertyChanged} method since that method is far outside the bounds of what most Swing models deal with.
 * Additionally, this implementation uses {@link Map} and {@link List} operations that assume that no two objects are equal according to
 * {@link Object#equals(Object)}.
 *
 * @param <E> the type of entities expected in this model
 * @since 5.6 (Moved from client and renamed from AEntityTableModel in 5.7)
 */
@IncubatingApi(since = "5.7")
public class EntityTableModel<E> extends AbstractTableModelExt
{
    /**
     * @since 5.8
     */
    protected final ListenerManager listenerManager = new ListenerManager();
    private final EntityColumnData<E> entityColumnData;
    private final EntityDefinition<E> entityDefinition;
    private final List<E> entities = new FastIndexList<>();

    /**
     * Construct a new model using the given entity definition.
     *
     * @param entityDefinition the entity definition to use for generating cell values
     */
    public EntityTableModel(@Nonnull EntityDefinition<E> entityDefinition)
    {
        this.entityDefinition = entityDefinition;
        entityColumnData = entityDefinition.getData();
    }

    @Override
    public int getRowCount()
    {
        return entities.size();
    }

    @Override
    public int getColumnCount()
    {
        return entityColumnData.getColumnCount();
    }

    @Override
    @Nonnull
    public String getColumnName(int columnIndex)
    {
        return entityColumnData.getColumn(columnIndex).name();
    }

    @Override
    @Nonnull
    public Class<?> getColumnClass(int columnIndex)
    {
        return entityColumnData.getColumn(columnIndex).type();
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex)
    {
        return isColumnEditable(columnIndex);
    }

    @Override
    @Nullable
    public Object getValueAt(int rowIndex, int columnIndex)
    {
        return getValueAt(entities.get(rowIndex), columnIndex);
    }

    /**
     * Fires a table cell updated event for an entity and column.
     *
     * @param entity the entity
     * @param column the column
     */
    public void fireTableCellUpdated(@Nonnull E entity, @Nonnull EntityColumn<E, ?> column)
    {
        fireTableCellUpdated(entity, getColumnIndex(column));
    }

    /**
     * Fires a table cell updated event for an entity and column.
     *
     * @param entity the entity
     * @param columnIndex the column index
     * @since 5.7
     */
    public void fireTableCellUpdated(@Nonnull E entity, int columnIndex)
    {
        var entityIndex = indexOf(entity);
        if (entityIndex > -1 && columnIndex > -1)
        {
            entityColumnData.clearValue(entity, columnIndex);
            fireTableCellUpdated(entityIndex, columnIndex);
        }
    }

    /**
     * Fires table cell updated events for all cells in the given column.
     *
     * @param column the column
     */
    public void fireTableColumnUpdated(@Nonnull EntityColumn<E, ?> column)
    {
        var index = getColumnIndex(column);
        entityColumnData.clearValues(index);
        fireTableColumnUpdated(index);
    }

    /**
     * @return the list of entity columns in this model
     */
    @Nonnull
    public List<EntityColumn<E, ?>> getColumns()
    {
        return entityColumnData.getColumns();
    }

    /**
     * @param column the column to add to this model
     */
    public void addColumn(@Nonnull EntityColumn<E, ?> column)
    {
        entityColumnData.addColumn(column);
    }

    /**
     * @param column the column to lookup
     * @return the model index of the given column
     */
    public int getColumnIndex(@Nonnull EntityColumn<E, ?> column)
    {
        return entityColumnData.getColumnIndex(column);
    }

    /**
     * Returns the value from a given entity to be displayed in the specified column index.
     *
     * @param entity the object to get the value from
     * @param columnIndex the column index
     * @return the value from the given entity that should be displayed in the given column index
     */
    @Nullable
    public Object getValueAt(@Nonnull E entity, int columnIndex)
    {
        return entityColumnData.getValue(entity, columnIndex);
    }

    /**
     * @return the entity definition
     */
    @Nonnull
    public EntityDefinition<E> getEntityDefinition()
    {
        return entityDefinition;
    }

    /**
     * @param columnIndex the column index
     * @return the column at the given index
     */
    @Nonnull
    public EntityColumn<E, ?> getColumn(int columnIndex)
    {
        return entityColumnData.getColumn(columnIndex);
    }

    /**
     * @param columnIndex the column index to check
     * @return {@code true} if the column at the given index is flagged editable, otherwise {@code false}
     */
    public boolean isColumnEditable(int columnIndex)
    {
        return entityColumnData.getColumn(columnIndex).editable();
    }

    /**
     * @return the List of objects present in this model
     */
    @Nonnull
    public List<E> getEntities()
    {
        return List.copyOf(entities);
    }

    /**
     * @param newEntities the List of objects to be contained in this model
     */
    public void setEntities(@Nonnull List<E> newEntities)
    {
        // The assumption here is that the Lists are different enough that it is not worth checking for common items between the Lists
        clearInternal(false);
        var map = entityDefinition.getPropertyChangeMap();
        for (var entity : newEntities) addListeners(entity, map);
        entities.addAll(newEntities);
        fireTableDataChanged();
    }

    /**
     * @param entity the object to add to this model
     */
    public void addEntity(@Nonnull E entity)
    {
        var index = entities.size();
        entities.add(entity);
        addListeners(entity, entityDefinition.getPropertyChangeMap());
        fireTableRowsInserted(index, index);
    }

    /**
     * @param entity the object to remove from this model
     * @return the index of the row that was removed or {@code -1} if it was not present
     */
    @SuppressWarnings("UnusedReturnValue")
    public int removeEntity(@Nonnull E entity)
    {
        var index = indexOf(entity);
        if (index != -1)
        {
            // To ensure we remove listeners from the correct entity, we perform that operation only on the entity removed from our list
            var removed = entities.remove(index);
            listenerManager.unregister(removed);
            entityColumnData.clearValues(entity);
            fireTableRowsDeleted(index, index);
        }
        return index;
    }

    /**
     * Removes all entities from this model.
     */
    public void clear()
    {
        clearInternal(true);
    }

    /**
     * @param entity the object to find
     * @return the index of the given object in the model's internal List, or {@code -1} if it is not found
     */
    public int indexOf(@Nonnull E entity)
    {
        return entities.indexOf(entity);
    }

    /**
     * @param row the row index to retrieve
     * @return the object at the given index
     */
    @Nonnull
    public E getRow(int row)
    {
        return entities.get(row);
    }

    private void clearInternal(boolean fireEvent)
    {
        var size = entities.size();
        if (size > 0)
        {
            listenerManager.unregisterAll();
            entities.clear();
            entityColumnData.clearValues();
            if (fireEvent) fireTableRowsDeleted(0, size - 1);
        }
    }

    /**
     * Adds property change listeners based on the entity definition. To improve performance this method uses the property
     * names and columns as they are configured <i>at the time the entity is added to this model</i>. If the entity
     * definition changes then existing objects in this model will not have any new listeners until they are removed
     * and re-added.
     */
    private void addListeners(E entity, Multimap<String, EntityColumn<E, ?>> map)
    {
        map.keySet().forEach(prop -> {
            var columns = map.get(prop);
            var columnIndexes = columns.stream().mapToInt(this::getColumnIndex).toArray();
            var listener = (PropertyChangeListener) event -> entityPropertyChanged(entity, columnIndexes);
            entityDefinition.addPropertyChangeListener(listenerManager, entity, prop, listener);
        });
    }

    @InvokeOnEDT
    private void entityPropertyChanged(@Nonnull E entity, @Nonnull int[] columnIndices)
    {
        entityColumnData.clearValues(entity, columnIndices);
        fireTableCellsUpdated(new int[]{indexOf(entity)}, columnIndices);
    }

}
