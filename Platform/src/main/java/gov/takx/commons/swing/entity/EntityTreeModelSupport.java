package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.lang.ExceptionUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.tree.TreeModelSupport;

import javax.swing.tree.TreePath;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Support class for {@link EntityTreeTableModel} that handles notifying {@link EntityTreeModelListener} and
 * {@link javax.swing.event.TreeModelListener} instances. Largely an Entity API wrapper around {@link TreeModelSupport}.
 *
 * @param <E> the entity type
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class EntityTreeModelSupport<E>
{
    private final List<EntityTreeModelListener<E>> entityTreeModelListeners = new ArrayList<>();
    private final EntityTreeTableModel<E> entityTreeTableModel;
    private final TreeModelSupport modelSupport;
    private final EntityColumnData<E> entityColumnData;

    /**
     * Construct a new instance for a given table model.
     *
     * @param entityTreeTableModel the model we will be firing events for
     * @param treeModelSupport the base tree model support object
     */
    public EntityTreeModelSupport(@Nonnull EntityTreeTableModel<E> entityTreeTableModel, @Nonnull TreeModelSupport treeModelSupport)
    {
        this.entityTreeTableModel = entityTreeTableModel;
        modelSupport = treeModelSupport;
        entityColumnData = entityTreeTableModel.entityColumnData;
    }

    /**
     * @param listener a listener to receive model events from this model
     */
    public void addEntityTreeModelListener(@Nonnull EntityTreeModelListener<E> listener)
    {
        entityTreeModelListeners.add(listener);
    }

    /**
     * @param listener a listener that should no longer receive model events from this model
     */
    @SuppressWarnings("unused")
    public void removeEntityTreeModelListener(@Nonnull EntityTreeModelListener<E> listener)
    {
        entityTreeModelListeners.remove(listener);
    }

    /**
     * Notifies listeners that an entity within this model has changed. Clears value cache, fires 
     * {@link EntityTreeModelListener} instances, and fires {@link javax.swing.event.TreeModelListener}
     * instances.
     *
     * @param entity the entity that changed
     */
    public void fireEntityChanged(@Nonnull E entity)
    {
        entityColumnData.clearValues(entity);
        fireEntityEvent(entity, null, EntityTreeModelEvent.Type.Change, newPath -> modelSupport.firePathChanged(new TreePath(newPath.toArray())));
    }

    /**
     * Notifies listeners that an entity within this model has had a child removed. Clears value cache, fires
     * {@link EntityTreeModelListener} instances, and fires {@link javax.swing.event.TreeModelListener}
     * instances.
     *
     * @param parent the parent entity
     * @param entity the entity that was removed
     * @param index the index that was removed
     */
    public void fireEntityRemoved(@Nonnull E parent, @Nonnull E entity, int index)
    {
        entityColumnData.clearValues(entity);
        fireEntityEvent(parent, entity, EntityTreeModelEvent.Type.Remove, newPath -> modelSupport.fireChildRemoved(new TreePath(newPath.toArray()), index, entity));
    }

    /**
     * Notifies listeners that an entity within this model has had a child inserted. Fires
     * {@link EntityTreeModelListener} instances, and fires {@link javax.swing.event.TreeModelListener}
     * instances.
     *
     * @param parent the parent entity
     * @param entity the entity that was inserted
     * @param index the index that was inserted
     */
    public void fireEntityInserted(@Nonnull E parent, @Nonnull E entity, int index)
    {
        entityColumnData.clearValues(entity);
        fireEntityEvent(parent, entity, EntityTreeModelEvent.Type.Insert, newPath -> modelSupport.fireChildAdded(new TreePath(newPath.toArray()), index, entity));
    }

    /**
     * Fires change events for an entity and all ancestors.
     *
     * @param entity the entity to start with
     */
    public void fireEntityHierarchyChanged(@Nonnull E entity)
    {
        fireEntityChanged(entity);
        entityTreeTableModel.getParents(entity).forEach(this::fireEntityHierarchyChanged);
    }

    /**
     * Notifies listeners that an entity within this model has had its child structure changed.
     *
     * @param entity the entity that was changed
     */
    public void fireTreeStructureChanged(@Nonnull E entity)
    {
        fireEntityEvent(entity, null, EntityTreeModelEvent.Type.Structure, newPath -> modelSupport.fireTreeStructureChanged(new TreePath(newPath.toArray())));
    }

    /**
     * Creates an entity event for a given parent and child and fires it to all entity listeners. Then constructs all paths
     * for the parent and sends them to the given consumer. This is so that normal swing tree events (which require full
     * paths) can be fired to the base tree model support class.
     *
     * @param parent the parent entity
     * @param entity the child entity or {@code null} if the event is only related to a single entity
     * @param type the event type
     * @param consumer a consumer for all paths to the parent entity
     */
    private void fireEntityEvent(@Nonnull E parent, @Nullable E entity, @Nonnull EntityTreeModelEvent.Type type, @Nonnull Consumer<List<E>> consumer)
    {
        if (!entityTreeModelListeners.isEmpty())
        {
            var entityEvent = new EntityTreeModelEvent<>(parent, entity, type);
            ExceptionUtils.safeDoOperation(entityTreeModelListeners, listener -> listener.modelChanged(entityEvent));
        }
        if (modelSupport.getTreeModelListeners().length > 0)
        {
            var paths = entityTreeTableModel.getAllPaths(parent);
            for (var newPath : paths)
            {
                newPath.add(parent);
                consumer.accept(newPath);
            }
        }
    }
}
