package gov.takx.commons.swing;

import com.google.common.collect.Comparators;
import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An implementation of {@link TreeModel} that wraps another model and adds the ability to sort tree nodes using a
 * {@link Comparator}. Nodes are sorted only underneath their parent, obviously. This model is kept up-to-date via listeners
 * added to the wrapped model, and thus depends on the reliability of those listeners to keep the model in-sync.
 *
 * @since 5.3
 */
@IncubatingApi(since = "5.3")
public class SortFilterTreeModel extends AbstractTreeModel
{
    private final Map<Object, List<Object>> sortedChildren = new HashMap<>();
    private final Map<Object, Map<Object, Integer>> unsortedIndexes = new HashMap<>(); // Initial insertion indexes are cached as the fail-safe sort method
    private final Map<Object, Comparator<Object>> nodeComparatorCache = new HashMap<>();
    private final List<ITreeNodeComparator> comparators = new ArrayList<>();
    private final Map<Object, Predicate<Object>> nodeFilterCache = new HashMap<>(); // Cache of filters
    private final List<ITreeNodeFilter> filters = new ArrayList<>();
    private Predicate<Object> leafFilter;
    private final TreeModel delegate;

    /**
     * Construct a new instance, using the given model as a source.
     *
     * @param delegate the model to delegate to
     * @param defaultComparator the comparator to use for sorting tree nodes
     */
    public SortFilterTreeModel(@Nonnull TreeModel delegate, @Nullable Comparator<Object> defaultComparator)
    {
        this.delegate = delegate;
        if (defaultComparator != null) addComparator(defaultComparator);
        refreshChildren(delegate.getRoot());
        delegate.addTreeModelListener(new DelegateListener());
    }

    /**
     * Construct a new instance, using the given model as a source.
     *
     * @param delegate the model to delegate to
     * @since 5.4
     */
    public SortFilterTreeModel(@Nonnull TreeModel delegate)
    {
        this(delegate, null);
    }

    /**
     * Adds a {@link ITreeNodeFilter} to this model.
     *
     * @param filter the filter to apply to the delegate model
     * @since 5.4
     */
    public void addFilter(@Nonnull ITreeNodeFilter filter)
    {
        filters.add(filter);
        nodeFilterCache.clear();
        refreshChildren(delegate.getRoot());
        fireTreeStructureChanged(this, null);
    }

    /**
     * Adds a filter for all nodes in this tree.
     *
     * @param filter the Predicate to apply to the delegate model to produce the filtered contents of this tree model
     * @since 5.4
     */
    public void addFilter(@Nonnull Predicate<Object> filter)
    {
        if (filter instanceof ITreeNodeFilter treeNodeFilter)
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
     * @since 5.4
     */
    public void addFilter(@Nonnull Object node, @Nonnull Predicate<Object> filter)
    {
        addFilter(n -> n.equals(node), filter);
    }

    /**
     * Add a filter to this model with a separate predicate to determine which parent nodes it should apply to.
     *
     * @param parentFilter the Predicate to determine if the filter applies to children of a given node
     * @param childFilter the Predicate to use to filter the nodes children
     * @since 5.4
     */
    public void addFilter(@Nonnull Predicate<Object> parentFilter, @Nonnull Predicate<Object> childFilter)
    {
        addFilter(new DelegateTreeNodeFilter(parentFilter, childFilter));
    }

    /**
     * Removes a filter from this model. This method will remove the specified filter regardless of whether there is a
     * parent check predicate that was applied to it.
     *
     * @param toRemove the Predicate filter to remove
     * @return {@code true} if any filters were removed, otherwise {@code false}
     * @since 5.4
     */
    public boolean removeFilter(@Nonnull Predicate<Object> toRemove)
    {
        if (filters.removeIf(filter -> filter.equals(toRemove) || filter instanceof DelegateTreeNodeFilter d && d.childPredicate.equals(toRemove)))
        {
            nodeFilterCache.clear();
            refreshChildren(delegate.getRoot());
            fireTreeStructureChanged(this, null);
            return true;
        } else
        {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private Predicate<Object> getFilter(Object treeNode)
    {
        return nodeFilterCache.computeIfAbsent(treeNode, node -> filters.stream()
                .filter(tnc -> tnc.applyToChildren(node))
                .map(Predicate.class::cast)
                .reduce(Predicate::and)
                .orElse(o -> true));
    }

    /**
     * Sets a predicate filter on this model that is applied in a bottom-up fashion. Similar to how the standard filter is applied
     * at the top and will hide children if parents fail, this filter is applied at the leaves, and will hide parents
     * if all their children fail.<br/>
     * Performance Note: There are performance impacts of applying this since it requires recursively iterating all children.
     *
     * @param filter the Predicate to apply to leaves of the delegate model, or {@code null} for no leaf filter
     */
    public void setLeafFilter(@Nullable Predicate<Object> filter)
    {
        leafFilter = filter;
        sortedChildren.clear();
        refreshChildren(getRoot());
        fireTreeStructureChanged(this, null); // Must do
    }

    /**
     * @return the Predicate applied to leaves of the delegate model to produce the filtered contents of this tree model
     */
    @Nullable
    public Predicate<Object> getLeafFilter()
    {
        return leafFilter;
    }

    /**
     * Adds a Comparator for all nodes in this tree.
     *
     * @param comparator the comparator to use to sort the nodes children
     * @since 5.4
     */
    public void addComparator(@Nonnull Comparator<Object> comparator)
    {
        addComparator(o -> true, comparator);
    }

    /**
     * Add a comparator to this model.
     *
     * @implNote  This could be made more efficient if necessary by creating a second {@link #refreshChildren(Object)}
     * implementation that also accepts a {@link Predicate}, such that only the nodes that are being sorted will be refreshed.
     * @param checkFunction a Predicate to test if the comparator applies to a given node
     * @param comparator the Comparator to use to sort the nodes children
     */
    public void addComparator(@Nonnull Predicate<Object> checkFunction, @Nonnull Comparator<Object> comparator)
    {
        comparators.add(0, new DelegateTreeNodeComparator(checkFunction, comparator));
        nodeComparatorCache.clear();
        refreshChildren(delegate.getRoot());
        fireTreeStructureChanged(this, null);
    }

    /**
     * Adds a comparator for a specific node in this tree and triggers a refresh of that node.
     *
     * @param node the node whose children should be sorted by the comparator
     * @param comparator the comparator
     */
    public void addComparator(@Nonnull Object node, @Nonnull Comparator<Object> comparator)
    {
        comparators.add(0, new DelegateTreeNodeComparator(n -> n.equals(node), comparator));
        nodeComparatorCache.clear();
        refreshChildren(node);
        fireTreeStructureChanged(this, null);
    }

    private Map<Object, Integer> getUnsortedIndexes(Object node)
    {
        return unsortedIndexes.get(node);
    }

    @SuppressWarnings("unchecked")
    private Comparator<Object> getComparator(Object treeNode)
    {
        return nodeComparatorCache.computeIfAbsent(treeNode, node -> comparators.stream()
                .filter(tnc -> tnc.applyToChildren(node))
                .map(Comparator.class::cast)
                .reduce(Comparator::thenComparing)
                .map(c -> c.thenComparingInt(getUnsortedIndexes(treeNode)::get))
                .orElse(Comparator.comparingInt(getUnsortedIndexes(treeNode)::get)));
    }

    private List<Object> getUnsortedChildren(Object parent)
    {
        List<Object> children = new ArrayList<>();
        if (parent == null)
        {
            children.add(getRoot());
        } else
        {
            int size = delegate.getChildCount(parent);
            for (int i = 0; i < size; i++)
            {
                children.add(delegate.getChild(parent, i));
            }
        }
        return children;
    }

    /**
     * Refreshes children of a node, cascading the operation down.
     *
     * @implNote Internal code that calls this method are responsible for firing tree structure changed events. In most
     * cases this will be a call to {@link AbstractTreeModel#fireTreeStructureChanged(Object, TreePath)} with a null path,
     * which triggers a full tree refresh. While it is more efficient to fire the event only on nodes that have changed, the
     * tree model api makes it impossible to get the parent of a node, which makes it impossible to construct a proper
     * {@link TreePath} reference. If the top level refresh becomes a performance problem then the solution would be to keep
     * a 1:* map of node parents (yes, a node can have more than one parent) and then fire the event only for parents.
     * @param parent the node to refresh and cascade down from
     */
    private void refreshChildren(Object parent)
    {
        List<Object> sorted = createSortedChildren(parent);
        sortedChildren.put(parent, sorted);
        sorted.forEach(this::refreshChildren);
    }

    private List<Object> createSortedChildren(Object parent)
    {
        Predicate<Object> parentFilter = getFilter(parent);
        Stream<Object> children = getUnsortedChildren(parent).stream().filter(parentFilter);
        if (leafFilter != null)
        {
            children = children.filter(child -> leafFilter.test(child) || !createSortedChildren(child).isEmpty());
        }
        var unsorted = children.toList();
        // When we manually refresh the children we should also refresh insertion indexes
        var indexes = unsortedIndexes.computeIfAbsent(parent, k -> new HashMap<>());
        indexes.clear();
        for (int i = 0; i < unsorted.size(); i++)
        {
            indexes.put(unsorted.get(i), i);
        }
        return unsorted.stream().sorted(getComparator(parent)).collect(Collectors.toCollection(ArrayList::new));
    }

    private List<Object> getSortedChildren(Object parent)
    {
        return sortedChildren.computeIfAbsent(parent, p -> new ArrayList<>());
    }

    private List<Object> resortChildren(Object parent)
    {
        List<Object> sortedChildren = getSortedChildren(parent);
        sortedChildren.sort(getComparator(parent));
        return sortedChildren;
    }

    @Override
    @Nullable
    public Object getRoot()
    {
        return delegate.getRoot();
    }

    @Override
    @Nonnull
    public Object getChild(@Nonnull Object parent, int index)
    {
        return getSortedChildren(parent).get(index);
    }

    @Override
    public int getChildCount(@Nonnull Object parent)
    {
        return getSortedChildren(parent).size();
    }

    @Override
    public boolean isLeaf(@Nonnull Object node)
    {
        return delegate.isLeaf(node);
    }

    @Override
    public void valueForPathChanged(@Nonnull TreePath path, Object newValue)
    {
        delegate.valueForPathChanged(path, newValue);
    }

    @Override
    public int getIndexOfChild(@Nonnull Object parent, Object child)
    {
        final int index = indexOfChild(parent, child);
        return index < 0 ? -1 : index;
    }

    /**
     * @return Index of the given {@code child} in the {@code parent}'s sorted child list according to a binary search
     * @since 5.5.1
     */
    private int indexOfChild(Object parent, Object child)
    {
        return Collections.binarySearch(getSortedChildren(parent), child, getComparator(parent));
    }

    private boolean isPathVisible(Object[] path)
    {
        if (path != null)
        {
            Object parent = path[0];
            for (int i = 1; i < path.length; i++)
            {
                Object node = path[i];
                if (indexOfChild(parent, node) < 0) return false;  // One of the parents is not visible
                parent = node;
            }
            return true;
        }
        return true; // Root cannot be filtered out
    }

    private int getSortIndex(Comparator<Object> comparator, List<Object> sortedChildren, Object child)
    {
        int foundIndex = Collections.binarySearch(sortedChildren, child, comparator);
        return foundIndex < 0 ? -1 - foundIndex : foundIndex;
    }

    /**
     * Interface for applying a comparator to children of nodes within this model.
     *
     * @since 5.4
     */
    public interface ITreeNodeComparator extends Comparator<Object>
    {
        /**
         * @param treeNode the tree node to check
         * @return {@code true} if this comparator should be applied to the children of the node, otherwise {@code false}
         */
        boolean applyToChildren(@Nonnull Object treeNode);
    }

    /**
     * Interface for filtering items within this model.
     *
     * @since 5.4
     */
    public interface ITreeNodeFilter extends Predicate<Object>
    {
        /**
         * @param treeNode the tree node to check
         * @return {@code true} if this filter should be applied to children of the given node, otherwise {@code false}
         */
        boolean applyToChildren(@Nonnull Object treeNode);
    }

    /**
     * Basic implementation of {@link ITreeNodeFilter} that stores delegate parent and child filters as properties.
     *
     * @param parentPredicate the parent check Predicate
     * @param childPredicate the child filter Predicate
     * @since 5.4
     */
    private record DelegateTreeNodeFilter(Predicate<Object> parentPredicate, Predicate<Object> childPredicate) implements ITreeNodeFilter
    {
        @Override
        public boolean applyToChildren(@Nonnull Object treeNode)
        {
            return parentPredicate.test(treeNode);
        }

        @Override
        public boolean test(@Nonnull Object treeNode)
        {
            return childPredicate.test(treeNode);
        }
    }

    /**
     * Basic implementation of {@link ITreeNodeComparator} that stores delegate predicate and comparator as record fields.
     *
     * @param parentPredicate the parent check Predicate
     * @param comparator the Comparator to sort children by
     * @since 5.4
     */
    private record DelegateTreeNodeComparator(Predicate<Object> parentPredicate, Comparator<Object> comparator) implements ITreeNodeComparator
    {
        @Override
        public boolean applyToChildren(@Nonnull Object treeNode)
        {
            return parentPredicate.test(treeNode);
        }

        @Override
        public int compare(Object treeNode1, Object treeNode2)
        {
            return comparator.compare(treeNode1, treeNode2);
        }
    }

    /**
     * This class handles {@link TreeModelEvent}s from the delegate {@link TreeModel}. When these events occur, it translates
     * the child indices into the correct indices for the sorted child lists, and then re-fires new events with that information
     * to listeners of this {@link SortFilterTreeModel}. We're using an inner class is to prevent having to expose the
     * public methods below to users of the {@link SortFilterTreeModel}.
     */
    private class DelegateListener implements TreeModelListener
    {

        /**
         * @param delegateEvent a {@code TreeModelEvent} describing changes to a tree model
         */
        @Override
        public void treeNodesChanged(TreeModelEvent delegateEvent)
        {
            // This event can have a null path, which means the root changed
            Object[] delegatePath = delegateEvent.getPath();

            // Need to ensure that the entire path is visible
            if (!isPathVisible(delegatePath)) return;

            if (delegatePath == null)
            {
                fireTreeNodesChanged(delegateEvent.getSource(), null, null, null);
                return;
            }

            Object[] delegateChildren = delegateEvent.getChildren();
            Object parent = delegatePath[delegatePath.length - 1];
            Predicate<Object> parentFilter = getFilter(parent);
            List<Object> sortedChildren = getSortedChildren(parent);
            Comparator<Object> comparator = getComparator(parent);

            List<Integer> changeIndexes = new ArrayList<>();
            List<Object> changeChildren = new ArrayList<>();
            List<Integer> removeIndexes = new ArrayList<>();
            List<Object> removeChildren = new ArrayList<>();
            List<Object> insertChildren = new ArrayList<>();
            List<Object> moveChildren = new ArrayList<>();

            // Translate events into inserts, removals, and changes, based on the filter
            for (Object child : delegateChildren)
            {
                boolean passesFilter = parentFilter.test(child);
                int currentIndex = sortedChildren.indexOf(child);
                if (currentIndex == -1)
                {
                    if (passesFilter)
                    {
                        insertChildren.add(child);
                    }
                } else if (passesFilter)
                {
                    // If its in order then just process as a change, otherwise need to check the sort
                    if (Comparators.isInOrder(sortedChildren, comparator))
                    {
                        changeIndexes.add(currentIndex);
                        changeChildren.add(child);
                    } else
                    {
                        moveChildren.add(child);
                    }
                } else
                {
                    removeIndexes.add(currentIndex);
                    removeChildren.add(child);
                }
            }
            if (!moveChildren.isEmpty())
            {
                // Moved nodes can be handled as changes as long as we find the correct new index
                List<Object> oldSort = new ArrayList<>(sortedChildren);
                sortedChildren = resortChildren(parent);
                for (int i = 0; i < oldSort.size(); i++)
                {
                    if (oldSort.get(i) != sortedChildren.get(i) && !changeIndexes.contains(i))
                    {
                        changeIndexes.add(i);
                        changeChildren.add(oldSort.get(i));
                    }
                }
            }
            if (!changeChildren.isEmpty())
            {
                fireTreeNodesChanged(delegateEvent.getSource(), delegatePath, changeIndexes.stream().mapToInt(Integer::intValue).toArray(), changeChildren.toArray());
            }
            if (!removeIndexes.isEmpty())
            {
                sortedChildren.removeAll(removeChildren);
                fireTreeNodesRemoved(delegateEvent.getSource(), delegatePath, removeIndexes.stream().mapToInt(Integer::intValue).toArray(), removeChildren.toArray());
            }
            if (!insertChildren.isEmpty())
            {
                List<Integer> insertIndexes = new ArrayList<>();
                for (Object child : insertChildren)
                {
                    int insertIndex = getSortIndex(comparator, sortedChildren, child);
                    sortedChildren.add(insertIndex, child);
                    insertIndexes.add(insertIndex);
                }
                fireTreeNodesInserted(delegateEvent.getSource(), delegatePath, insertIndexes.stream().mapToInt(Integer::intValue).toArray(), insertChildren.toArray());
            }
        }

        @Override
        public void treeNodesInserted(TreeModelEvent e)
        {
            Object[] delegatePath = e.getPath();
            if (!isPathVisible(delegatePath)) return;

            Object[] delegateChildren = e.getChildren();
            Object parent = delegatePath[delegatePath.length - 1];
            Predicate<Object> parentFilter = getFilter(parent);
            List<Object> sortedChildren = getSortedChildren(parent);
            List<Integer> indexes = new ArrayList<>();
            List<Object> children = new ArrayList<>();
            var delegateIndexes = e.getChildIndices();
            var insertIndexes = unsortedIndexes.computeIfAbsent(parent, k -> new HashMap<>());
            var comparator = getComparator(parent);
            for (int i = 0; i < delegateChildren.length; i++)
            {
                var child = delegateChildren[i];
                if (parentFilter.test(child))
                {
                    insertIndexes.put(child, delegateIndexes[i]);
                    int insertIndex = getSortIndex(comparator, sortedChildren, child);
                    sortedChildren.add(insertIndex, child);
                    children.add(child);
                    indexes.add(insertIndex);
                }
            }
            if (!children.isEmpty())
            {
                fireTreeNodesInserted(e.getSource(), delegatePath, indexes.stream().mapToInt(Integer::intValue).toArray(), children.toArray());
            }
        }

        @Override
        public void treeNodesRemoved(TreeModelEvent e)
        {
            Object[] delegatePath = e.getPath();
            if (!isPathVisible(delegatePath)) return;

            Object[] delegateChildren = e.getChildren();
            Object parent = delegatePath[delegatePath.length - 1];
            final var comparator = getComparator(parent);
            List<Object> sortedChildren = getSortedChildren(parent);
            List<Integer> indexes = new ArrayList<>();
            List<Object> children = new ArrayList<>();
            var insertIndexes = unsortedIndexes.get(parent);

            // Beware of optimizing this. These two loops may look redundant but the reason is that we first need to calculate a
            // complete set of indexes to remove from the model, which is necessary for the call to #fireTreeNodesRemoved.
            // We then need to remove those objects from our internal list, which requires iteratively finding/removing
            // them.
            for (Object child : delegateChildren)
            {
                int foundIndex = Collections.binarySearch(sortedChildren, child, comparator);
                if (foundIndex > -1)
                {
                    indexes.add(foundIndex);
                    children.add(child);
                }
            }
            if (!children.isEmpty())
            {
                children.forEach(child -> {
                    final int i = Collections.binarySearch(sortedChildren, child, comparator);
                    if (i > -1)
                    {
                        sortedChildren.remove(i);
                        insertIndexes.remove(child);
                    }
                });
                fireTreeNodesRemoved(e.getSource(), delegatePath, indexes.stream().mapToInt(Integer::intValue).toArray(), children.toArray());
            }
        }

        @Override
        public void treeStructureChanged(TreeModelEvent e)
        {
            Object[] delegatePath = e.getPath();
            if (!isPathVisible(delegatePath)) return;

            // TODO TAKX-3254 I think this can be made more efficient by checking the childIndices
            Object parent = delegatePath != null ? delegatePath[delegatePath.length - 1] : null;
            refreshChildren(parent);
            fireTreeStructureChanged(e.getSource(), delegatePath, null, null);

        }
    }
}