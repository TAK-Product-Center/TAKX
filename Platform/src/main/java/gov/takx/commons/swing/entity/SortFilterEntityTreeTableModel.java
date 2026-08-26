package gov.takx.commons.swing.entity;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Extension of {@link EntityTreeTableModel} that supports filtering and sorting. In the case of tables these two features
 * are provided by the UI component, but the swing {@link javax.swing.JTable} supports neither, and implementing it in the
 * model proved to be more straightforward than attempting to adjust the component.
 *
 * @param <E> the entity type stored in this model
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class SortFilterEntityTreeTableModel<E> extends EntityTreeTableModel<E>
{
    private final EntityTreeTableModel<E> baseModel;

    private final List<ITreeNodeFilter<E>> filters = new ArrayList<>();
    private final Map<E, Predicate<E>> nodeFilterCache = new HashMap<>();

    private final List<ITreeNodeComparator<E>> comparators = new ArrayList<>();
    private final Map<E, Comparator<E>> nodeComparatorCache = new HashMap<>();

    private Predicate<E> leafFilter;

    /**
     * Create a new model instance that filters data from a base model.
     * 
     * @param baseModel the base model to pull data and receive events from
     */
    public SortFilterEntityTreeTableModel(@Nonnull EntityTreeTableModel<E> baseModel)
    {
        super(baseModel.getEntityDefinition(), baseModel.getHierarchicalEntityColumn(), baseModel.getChildAddedRemovedProperty());
        this.baseModel = baseModel;
        // retain only base model columns
        for (var column : entityColumnData.getColumns()) if (!baseModel.getColumns().contains(column)) entityColumnData.removeColumn(column);
        setRoot(baseModel.getRoot());
        refreshChildrenFromBase(getRoot(), true);
        baseModel.addEntityTreeModelListener(this::modelChanged);
    }

    /**
     * Sets a predicate filter on this model that is applied in a bottom-up fashion. Similar to how the standard filter is
     * applied at the top and will hide children if parents fail, this filter is applied at the leaves, and will hide parents
     * if all their children fail.<p> While there are no technical barriers against making this type of filter just as robust
     * as standard filters (e.g. allowing per node filters) those features are not currently necessary.
     *
     * @param filter the Predicate to apply to leaves of the delegate model, or {@code null} for no leaf filter
     */
    public void setLeafFilter(Predicate<E> filter)
    {
        leafFilter = filter;
        refreshChildrenFromBase(getRoot(), true);
        modelSupport.fireTreeStructureChanged(getRoot());
    }

    /**
     * Adds a {@link ITreeNodeFilter} to this model.
     *
     * @param filter the filter to apply to the delegate model
     */
    public void addFilter(@Nonnull ITreeNodeFilter<E> filter)
    {
        filters.add(filter);
        nodeFilterCache.clear();
        refreshChildrenFromBase(getRoot(), true);
        modelSupport.fireTreeStructureChanged(getRoot());
    }

    /**
     * Adds a filter for all nodes in this tree.
     *
     * @param filter the Predicate to apply to the delegate model to produce the filtered contents of this tree model
     */
    public void addFilter(@Nonnull Predicate<E> filter)
    {
        if (filter instanceof ITreeNodeFilter<E> treeNodeFilter)
        {
            addFilter(treeNodeFilter);
        } else
        {
            addFilter(o -> true, filter);
        }
    }

    /**
     * Adds a filter for a specific node in this tree.
     * Note: Beware that due to the use of an {@link Object} parameter, this method may catch other {@code addFilter} calls
     * if the passed predicates do not use the {@link Object} generic type parameter.
     *
     * @param node the node whose children should be filtered by the predicate
     * @param filter the predicate
     */
    @SuppressWarnings("unused")
    public void addFilter(@Nonnull E node, @Nonnull Predicate<E> filter)
    {
        addFilter(n -> n.equals(node), filter);
    }

    /**
     * Add a filter to this model with a separate predicate to determine which parent nodes it should apply to.
     *
     * @param parentFilter the Predicate to determine if the filter applies to children of a given node
     * @param childFilter the Predicate to use to filter the nodes children
     */
    public void addFilter(@Nonnull Predicate<E> parentFilter, @Nonnull Predicate<E> childFilter)
    {
        addFilter(new DelegateTreeNodeFilter<>(parentFilter, childFilter));
    }

    /**
     * Removes a filter from this model. This method will remove the specified filter regardless of whether there is a
     * parent check predicate that was applied to it.
     *
     * @param toRemove the Predicate filter to remove
     * @return {@code true} if any filters were removed, otherwise {@code false}
     */
    @SuppressWarnings("UnusedReturnValue")
    public boolean removeFilter(@Nonnull Predicate<E> toRemove)
    {
        if (filters.removeIf(filter -> filter.equals(toRemove) || filter instanceof DelegateTreeNodeFilter<E> d && d.childPredicate().equals(toRemove)))
        {
            nodeFilterCache.clear();
            refreshChildrenFromBase(getRoot(), true);
            modelSupport.fireTreeStructureChanged(getRoot());
            return true;
        } else
        {
            return false;
        }
    }

    /**
     * Adds a Comparator for all nodes in this model.
     *
     * @param comparator the comparator to use to sort the nodes children
     */
    @SuppressWarnings("unused")
    public void addComparator(@Nonnull Comparator<E> comparator)
    {
        addComparator(o -> true, comparator);
    }

    /**
     * Add a comparator to this model.
     *
     * @param checkFunction a Predicate to test if the comparator applies to a given node
     * @param comparator the Comparator to use to sort the nodes children
     */
    public void addComparator(@Nonnull Predicate<E> checkFunction, @Nonnull Comparator<E> comparator)
    {
        addComparator(null, checkFunction, comparator);
    }

    /**
     * Adds a comparator for a specific node in this model and triggers a refresh of that node.
     *
     * @param node the node whose children should be sorted by the comparator
     * @param comparator the comparator
     */
    public void addComparator(@Nonnull E node, @Nonnull Comparator<E> comparator)
    {
        addComparator(node, n -> n.equals(node), comparator);
    }

    /**
     * Add a comparator to this model.
     *
     * @param refreshNode the node to refresh ancestors, or {@code null} to refresh the entire tree
     * @param checkFunction a Predicate to test if the comparator applies
     * @param comparator the Comparator to use to sort the nodes
     * @since 5.7.1
     */
    public void addComparator(@Nullable E refreshNode, @Nonnull Predicate<E> checkFunction, @Nonnull Comparator<E> comparator)
    {
        comparators.add(0, new DelegateTreeNodeComparator<>(checkFunction, comparator));
        nodeComparatorCache.clear();
        var cascade = false;
        if (refreshNode == null)
        {
            cascade = true;
            refreshNode = getRoot();
        }
        refreshChildrenFromBase(refreshNode, cascade);
        modelSupport.fireTreeStructureChanged(refreshNode);
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     */
    @Override
    public void addEntity(@Nonnull E parent, @Nonnull E entity)
    {
        throw new UnsupportedOperationException("Cannot add objects directly to this model");
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     */
    @Override
    public void addEntity(@Nonnull E parent, @Nonnull E entity, int index)
    {
        throw new UnsupportedOperationException("Cannot add objects directly to this model");
    }

    /**
     * Throws {@link UnsupportedOperationException}.
     */
    @Override
    public void removeEntity(@Nonnull E parent, @Nonnull E entity)
    {
        throw new UnsupportedOperationException("Cannot remove objects directly from this model");
    }

    /**
     * No-op.
     */
    @Override
    protected void addListeners(@Nonnull E entity)
    {
        // No-op since this model is only for filtering data from a base model
    }

    /**
     * No-op.
     */
    @Override
    protected void removeListeners(@Nonnull E entity)
    {
        // No-op since this model is only for filtered data from a base model
    }

    private void addObjectAtIndex(@Nonnull E entity, @Nonnull E child)
    {
        var comparator = getComparator(entity);
        var children = getChildrenInternal(entity);
        var foundIndex = Collections.binarySearch(children, child, comparator);
        if (foundIndex < 0)
        {
            var insertIndex = -1 - foundIndex;
            super.addEntity(entity, child, insertIndex);
        }
    }

    private void refreshChildrenFromBase(@Nonnull E parent, boolean cascade)
    {
        var comparator = getComparator(parent);
        var filter = getFilter(parent);
        List<E> children = getChildrenInternal(parent);
        children.forEach(child -> getParentsInternal(child).remove(parent));
        children.clear();
        var filtered = baseModel.getChildren(parent).stream().filter(filter).sorted(comparator).toList();
        if (cascade || leafFilter != null)
        {
            filtered.forEach(child -> refreshChildrenFromBase(child, true));
        }
        if (leafFilter != null)
        {
            filtered = filtered.stream().filter(child -> leafFilter.test(child) || !getChildrenInternal(child).isEmpty()).toList();
        }
        children.addAll(filtered);
        children.forEach(child -> getParentsInternal(child).add(parent));
    }

    @SuppressWarnings("unchecked")
    private Comparator<E> getComparator(E treeNode)
    {
        return nodeComparatorCache.computeIfAbsent(treeNode,
                node -> comparators.stream()
                        .filter(tnc -> tnc.applyToChildren(node))
                        .map(Comparator.class::cast)
                        .reduce(Comparator::thenComparing)
                        .map(c -> c.thenComparingInt(n -> baseModel.getIndexOfChild(treeNode, n)))
                        .orElse(Comparator.comparingInt(n -> baseModel.getIndexOfChild(treeNode, n))));
    }

    @SuppressWarnings("unchecked")
    private Predicate<E> getFilter(E treeNode)
    {
        return nodeFilterCache.computeIfAbsent(treeNode, node -> filters.stream()
                .filter(tnc -> tnc.applyToChildren(node))
                .map(Predicate.class::cast)
                .reduce(Predicate::and)
                .orElse(o -> true));
    }

    private void modelChanged(EntityTreeModelEvent<E> event)
    {
        var entity = event.entity();
        var child = event.child();
        switch (event.type())
        {
            case Change ->
            {
                var parents = baseModel.getParents(entity);
                for (var parent : parents)
                {
                    var children = getChildrenInternal(parent);
                    var filter = getFilter(parent);
                    var currentTest = filter.test(entity);
                    var priorTest = children.contains(entity);
                    if (currentTest == priorTest)
                    {
                        if (currentTest) modelSupport.fireEntityChanged(entity);
                    } else if (currentTest)
                    {
                        addObjectAtIndex(parent, entity);
                    } else
                    {
                        super.removeEntity(parent, entity);
                    }
                }
            }
            case Insert ->
            {
                if (getFilter(entity).test(child)) addObjectAtIndex(entity, child);
            }
            case Remove -> super.removeEntity(entity, child);
            case Structure ->
            {
                refreshChildrenFromBase(entity, true);
                modelSupport.fireTreeStructureChanged(entity);
            }
        }
    }

    /**
     * Interface for applying a comparator to children of nodes within this model.
     *
     * @param <E> the entity type
     */
    @IncubatingApi(since = "5.7")
    public interface ITreeNodeComparator<E> extends Comparator<E>
    {
        /**
         * @param treeNode the tree node to check
         * @return {@code true} if this comparator should be applied to the children of the node, otherwise {@code false}
         */
        boolean applyToChildren(@Nonnull E treeNode);
    }

    /**
     * Interface for filtering items within this model.
     *
     * @param <E> the entity type
     */
    @IncubatingApi(since = "5.7")
    public interface ITreeNodeFilter<E> extends Predicate<E>
    {
        /**
         * @param treeNode the tree node to check
         * @return {@code true} if this filter should be applied to children of the given node, otherwise {@code false}
         */
        boolean applyToChildren(@Nonnull E treeNode);
    }

    /**
     * Basic implementation of {@link ITreeNodeComparator} that stores delegate predicate and comparator as record fields.
     *
     * @param parentPredicate the parent check Predicate
     * @param comparator the Comparator to sort children by
     */
    private record DelegateTreeNodeComparator<E>(Predicate<E> parentPredicate, Comparator<E> comparator) implements ITreeNodeComparator<E>
    {
        @Override
        public boolean applyToChildren(@Nonnull E treeNode)
        {
            return parentPredicate.test(treeNode);
        }

        @Override
        public int compare(E treeNode1, E treeNode2)
        {
            return comparator.compare(treeNode1, treeNode2);
        }
    }

    /**
     * Basic implementation of {@link ITreeNodeFilter} that stores delegate parent and child filters as properties.
     *
     * @param parentPredicate the parent check Predicate
     * @param childPredicate the child filter Predicate
     */
    private record DelegateTreeNodeFilter<E>(Predicate<E> parentPredicate, Predicate<E> childPredicate) implements ITreeNodeFilter<E>
    {
        @Override
        public boolean applyToChildren(@Nonnull E treeNode)
        {
            return parentPredicate.test(treeNode);
        }

        @Override
        public boolean test(@Nonnull E treeNode)
        {
            return childPredicate.test(treeNode);
        }
    }
}
