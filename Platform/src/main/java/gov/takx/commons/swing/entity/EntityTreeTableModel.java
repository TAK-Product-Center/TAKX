package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.explorer.ILazyLoadingExplorable;
import gov.takx.commons.lang.FastIndexList;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.commons.swing.TreeModelIterator;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.treetable.AbstractTreeTableModel;

import javax.swing.tree.TreePath;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * TreeTableModel intended to be used with {@link EntityTreeTable}. The internal map of objects is generic so that it can be
 * used with various objects that follow a similar pattern.
 * <p>
 * Like other swing models, this should only be modified from the EDT. This includes adding / removing objects and firing
 * listeners. The usage of annotations like {@link InvokeOnEDT} is intentionally limited to specific locations in this class
 * which are specific to this implementation. Additionally, this implementation uses Map and List operations assume that no
 * two objects are equal according to {@link Object#equals(Object)}.
 *
 * @param <E> the entity type that this model works with
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntityTreeTableModel<E> extends AbstractTreeTableModel
{
    protected final EntityColumnData<E> entityColumnData = new EntityColumnData<>();
    /**
     * @since 5.8
     */
    protected final ListenerManager listenerManager = new ListenerManager();
    protected final EntityTreeModelSupport<E> modelSupport;

    private final Map<E, FastIndexList<E>> children = new HashMap<>();
    private final Map<E, List<E>> parents = new HashMap<>();
    private final Set<E> entities = new HashSet<>();

    private final String childAddedRemovedProperty;
    private final EntityColumn<E, ?> hierarchicalColumn;
    private final EntityDefinition<E> entityDefinition;
    private E root;

    /**
     * Construct a new model for an entity definition, and a specific hierarchical column.
     *
     * @param entityDefinition the entity definition to use
     * @param hierarchicalColumn the column to use as the hierarchy column in the tree
     * @param childAddedRemovedProperty the property name for {@link PropertyChangeEvent} events that will indicate child additional or removal
     */
    public EntityTreeTableModel(@Nonnull EntityDefinition<E> entityDefinition, @Nonnull EntityColumn<E, ?> hierarchicalColumn, @Nullable String childAddedRemovedProperty)
    {
        this.entityDefinition = entityDefinition;
        this.hierarchicalColumn = hierarchicalColumn;
        this.childAddedRemovedProperty = childAddedRemovedProperty;
        modelSupport = new EntityTreeModelSupport<>(this, super.modelSupport);
        entityDefinition.getColumns().forEach(this::addColumn);
    }

    /**
     * @return the entity model used by this model
     */
    @Nonnull
    public EntityDefinition<E> getEntityDefinition()
    {
        return entityDefinition;
    }

    /**
     * @param column the column to add to this model
     */
    public void addColumn(@Nonnull EntityColumn<E, ?> column)
    {
        entityColumnData.addColumn(column);
    }

    /**
     * @param parent the parent of the entity to remove
     * @param entity the entity to remove
     */
    public void removeEntity(@Nonnull E parent, @Nonnull E entity)
    {
        var index = getChildrenInternal(parent).indexOfRemove(entity);
        if (index != -1)
        {
            var parents = getParentsInternal(entity);
            parents.remove(parent);
            if (entities.contains(entity) && parents.isEmpty())
            {
                listenerManager.unregister(entity);
                entities.remove(entity);
            }
            modelSupport.fireEntityRemoved(parent, entity, index);
        }
    }

    /**
     * @param parent the parent of the entity to add
     * @param entity the entity to add
     */
    public void addEntity(@Nonnull E parent, @Nonnull E entity)
    {
        var index = getChildrenInternal(parent).size();
        addEntityInternal(parent, entity, index);
    }

    /**
     * Adds an entity at a specific index within the parents child list. Duplicates are ignored.
     *
     * @param parent the parent that the entity should be added to
     * @param entity the entity to add
     * @param index the index where the entity should be added
     * @throws IndexOutOfBoundsException if the index is invalid for the parent's current List of children
     * @throws IllegalArgumentException if the entity is already present in the parent's List of children
     */
    public void addEntity(@Nonnull E parent, @Nonnull E entity, int index)
    {
        addEntityInternal(parent, entity, index);
    }

    /**
     * @param child an entity in this model
     * @return an unmodifiable List of the parents of the given entity
     */
    @Nonnull
    public List<E> getParents(@Nonnull E child)
    {
        return parents.containsKey(child) ? List.copyOf(parents.get(child)) : Collections.emptyList();
    }

    /**
     * @param parent an entity in this model
     * @return an unmodifiable List of children of the given object
     */
    @Nonnull
    public List<E> getChildren(@Nonnull E parent)
    {
        return children.containsKey(parent) ? List.copyOf(children.get(parent)) : Collections.emptyList();
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
    public int getHierarchicalColumn()
    {
        return getColumnIndex(hierarchicalColumn);
    }

    @Override
    public boolean isCellEditable(Object node, int column)
    {
        return entityColumnData.getColumn(column).editable();
    }

    @Override
    public E getRoot()
    {
        return root;
    }

    /**
     * @param columnIndex the column index
     * @return the column at the given index
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    @Nonnull
    public EntityColumn<E, ?> getColumn(int columnIndex)
    {
        return entityColumnData.getColumn(columnIndex);
    }

    /**
     * @param root the object to use as the root of this model
     */
    public void setRoot(@Nonnull E root)
    {
        if (this.root == root) return;
        if (this.root != null) throw new IllegalStateException("Root cannot be changed once set");
        this.root = root;
        entities.add(root);
        addListeners(root);
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object getValueAt(Object object, int columnIndex)
    {
        return entityColumnData.getValue((E) object, columnIndex);
    }

    @SuppressWarnings("unchecked")
    @Override
    public E getChild(Object parent, int index)
    {
        return getChildrenInternal((E) parent).get(index);
    }

    @SuppressWarnings("unchecked")
    @Override
    public int getChildCount(Object parent)
    {
        return getChildrenInternal((E) parent).size();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isLeaf(Object node)
    {
        return !(node instanceof ILazyLoadingExplorable) && getChildrenInternal((E) node).isEmpty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public int getIndexOfChild(Object parent, Object child)
    {
        return getChildrenInternal((E) parent).indexOf((E) child);
    }

    @Override
    public void valueForPathChanged(TreePath path, Object newValue)
    {
        // No-op. This implementation does not support edits.
    }

    @Override
    public void setValueAt(Object value, Object node, int column)
    {
        // No-op. This implementation does not support edits. 
    }

    /**
     * @return the property name for {@link PropertyChangeEvent} events that will indicate child additional or removal
     */
    @Nullable
    public String getChildAddedRemovedProperty()
    {
        return childAddedRemovedProperty;
    }

    /**
     * @return the column that is rendered as the tree node
     */
    @Nonnull
    public EntityColumn<E, ?> getHierarchicalEntityColumn()
    {
        return hierarchicalColumn;
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
     * @param column the column to lookup
     * @return the model index of the given column
     */
    public int getColumnIndex(@Nonnull EntityColumn<E, ?> column)
    {
        return entityColumnData.getColumnIndex(column);
    }

    /**
     * @param entity An entity within this model
     * @return The total number of descendants under the given entity in this model
     * @apiNote This is inefficient. Call it sparingly.
     */
    @SuppressWarnings("unused")
    public int getDescendantCount(E entity)
    {
        var count = getChildCount(entity);
        count += new TreeModelIterator.NodeChildIterator<>(this, entity).toStream().mapToInt(this::getDescendantCount).sum();
        return count;
    }

    /**
     * @param entity An entity within this model
     * @return The total number of leaves under the given entity in this model
     * @apiNote This is inefficient. Call it sparingly.
     */
    @SuppressWarnings("unused")
    public int getLeafCount(E entity)
    {
        var count = getChildCount(entity) == 0 ? 1 : 0;
        count += new TreeModelIterator.NodeChildIterator<>(this, entity).toStream().mapToInt(this::getLeafCount).sum();
        return count;
    }

    /**
     * @param listener a listener to receive model events from this model
     */
    public void addEntityTreeModelListener(@Nonnull EntityTreeModelListener<E> listener)
    {
        modelSupport.addEntityTreeModelListener(listener);
    }

    /**
     * @param listener a listener that should no longer receive model events from this model
     */
    @SuppressWarnings("unused")
    public void removeEntityTreeModelListener(@Nonnull EntityTreeModelListener<E> listener)
    {
        modelSupport.removeEntityTreeModelListener(listener);
    }

    /**
     * @param entity an entity within this model
     * @return A List of all paths to parents of a given entity in this model. For entities not present in the model the List
     * will be empty. For the root the List will contain 1 empty List.
     */
    @Nonnull
    public List<List<E>> getAllPaths(@Nonnull E entity)
    {
        List<List<E>> allPaths = new ArrayList<>();
        if (entity == root)
        {
            allPaths.add(new ArrayList<>());
        } else
        {
            for (var parent : getParents(entity))
            {
                var parentPaths = getAllPaths(parent);
                for (var parentPath : parentPaths)
                {
                    parentPath.add(parent);
                    allPaths.add(parentPath);
                }
            }
        }
        return allPaths;
    }

    /**
     * Fire a {@link javax.swing.event.TreeModelEvent} notifying all listeners than an object in this model has changed.
     *
     * @param entity the object within this model that has changed
     */
    public void fireEntityChanged(@Nonnull E entity)
    {
        modelSupport.fireEntityChanged(entity);
    }

    /**
     * @return the model support object for firing entity model events
     */
    @Nonnull
    public EntityTreeModelSupport<E> getEntityTreeModelSupport()
    {
        return modelSupport;
    }

    /**
     * @return the entity column data cache
     */
    @Nonnull
    public EntityColumnData<E> getEntityColumnData()
    {
        return entityColumnData;
    }

    /**
     * Adds an object to this model.
     *
     * @param parent the parent of the new object
     * @param entity the object to add
     * @param index the index within the parent to add the new object at
     */
    protected void addEntityInternal(@Nonnull E parent, @Nonnull E entity, int index)
    {
        getChildrenInternal(parent).add(index, entity);
        getParentsInternal(entity).add(parent);
        modelSupport.fireEntityInserted(parent, entity, index);
        if (!entities.contains(entity))
        {
            addListeners(entity);
            entities.add(entity);
        }
    }

    /**
     * @param entity An object within this model
     * @return the live List of parents for the object
     */
    @Nonnull
    protected List<E> getParentsInternal(@Nonnull E entity)
    {
        return parents.computeIfAbsent(entity, k -> new FastIndexList<>());
    }

    /**
     * @param parent An object within this model
     * @return the live List of children for the object
     */
    @Nonnull
    protected FastIndexList<E> getChildrenInternal(@Nonnull E parent)
    {
        return children.computeIfAbsent(parent, k -> new FastIndexList<>());
    }

    /**
     *  Attaches appropriate listeners to the given object.
     *
     * @param entity An object within this model
     */
    protected void addListeners(@Nonnull E entity)
    {
        if (childAddedRemovedProperty != null)
        {
            var listener = (PropertyChangeListener) event -> entityChildPropertyChanged(event, entity);
            entityDefinition.addPropertyChangeListener(listenerManager, entity, childAddedRemovedProperty, listener);
        }
        var entityPropertyChangeMap = entityDefinition.getPropertyChangeMap();
        entityPropertyChangeMap.keySet().forEach(prop -> {
            // TODO DCW TAKX-4205 implement column level change events, then pull column list from the above map and fire only events for those columns
            var columns = entityPropertyChangeMap.get(prop);
            var columnIndexes = columns.stream().mapToInt(this::getColumnIndex).filter(i -> i > -1).toArray();
            var listener = (PropertyChangeListener) event -> entityPropertyChanged(event, entity, columnIndexes);
            entityDefinition.addPropertyChangeListener(listenerManager, entity, prop, listener);
        });
    }

    @SuppressWarnings("unused")
    @InvokeOnEDT
    private void entityPropertyChanged(@Nonnull PropertyChangeEvent event, @Nonnull E entity, @Nonnull int[] columnIndices)
    {
        entityColumnData.clearValues(entity, columnIndices);
        modelSupport.fireEntityChanged(entity);
    }

    @SuppressWarnings("unchecked")
    @InvokeOnEDT
    private void entityChildPropertyChanged(@Nonnull PropertyChangeEvent event, @Nonnull E entity)
    {
        // Note it is up to the user to ensure these casts are valid for the given PCE
        var newChild = (E) event.getNewValue();
        if (newChild == null)
        {
            removeEntity(entity, (E) event.getOldValue());
        } else
        {
            addEntity(entity, newChild);
        }
    }
}
