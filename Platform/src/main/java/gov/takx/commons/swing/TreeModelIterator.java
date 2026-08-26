package gov.takx.commons.swing;

import jakarta.annotation.Nonnull;

import javax.swing.tree.TreeModel;

import java.util.Iterator;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Stack;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * An iterator that iterates nodes within a {@link TreeModel} instance.  The iteration is pre-order, depth first.
 * <p/>
 * Note: The behaviour of this iterator is undefined if the tree model is mutated after this iterator is created.
 *
 * @param <T> Type of nodes within the given {@link TreeModel}
 * @since 5.3
 */
public class TreeModelIterator<T> implements Iterator<T>
{
    private final Stack<Iterator<T>> stack = new Stack<>();
    private final TreeModel model;

    /**
     * Construct a new instance.  Iteration results will include the given {@code startNode}, and thus
     * will always contain at least one value.
     *
     * @param model the tree model to use
     * @param startNode the node to begin iteration at
     */
    public TreeModelIterator(@Nonnull TreeModel model, @Nonnull T startNode)
    {
        this(model, startNode, true);
    }

    /**
     * Construct a new instance.  If {@code includeStartNode} is {@code false}, then the iteration may yield no values
     * if the given {@code startNode} has no descendents.
     *
     * @param model the tree model to use
     * @param startNode the node to begin iteration at
     * @param includeStartNode Pass {@code true} to include the given {@code startNode} in the iteration,
     *                         or {@code false} to exclude it, yielding an iteration of the starting node's descendents
     */
    public TreeModelIterator(@Nonnull TreeModel model, @Nonnull T startNode, boolean includeStartNode)
    {
        this.model = model;
        stack.push(List.of(startNode).iterator());

        if (!includeStartNode) next(); // Skip startNode if configured
    }

    @Override
    public boolean hasNext()
    {
        return !stack.empty() && stack.peek().hasNext();
    }

    @Override
    public T next()
    {
        var iterator = stack.peek();
        T node = iterator.next();

        if (!iterator.hasNext()) stack.pop();

        var childIterator = new NodeChildIterator<>(model, node);
        if (childIterator.hasNext()) stack.push(childIterator);

        return node;
    }

    /**
     * <em>Warning:</em> The produced stream operates directly upon this iterator. Traversal of elements should be
     * accomplished through the returned stream. The behaviour of the stream is undefined if the iterator is
     * operated on after the stream is returned.
     *
     * @return a stream that returns elements by iterating this iterator
     */
    @Nonnull
    public Stream<T> toStream()
    {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
    }

    /**
     * An iterator that returns the direct children of a specific node within a {@link TreeModel}, in the same order
     * as they are defined in the model.
     * <p/>
     * Note: If the tree model is mutated after this iterator is created, there are no guarantees on which tree nodes
     * will be returned.
     * 
     * @see TreeModel#getChildCount(Object)
     * @see TreeModel#getChild(Object, int) 
     */
    public static class NodeChildIterator<T> implements Iterator<T>
    {
        private final TreeModel model;
        private final T node;
        private int index;

        /**
         * Construct a new instance.
         *
         * @param model the tree model
         * @param node the node whose children are to be iterated
         */
        public NodeChildIterator(TreeModel model, T node)
        {
            this.model = model;
            this.node = node;
        }

        @Override
        public boolean hasNext()
        {
            return model.getChildCount(node) > index;
        }

        @SuppressWarnings("unchecked")
        @Override
        public T next()
        {
            return (T) model.getChild(node, index++);
        }

        /**
         * <em>Warning:</em> The produced stream operates directly upon this iterator. Traversal of elements should be
         * accomplished through the returned stream. The behaviour of the stream is undefined if the iterator is
         * operated on after the stream is returned.
         *
         * @return a stream that returns elements by iterating this iterator
         */
        @Nonnull
        public Stream<T> toStream()
        {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(this, Spliterator.ORDERED), false);
        }
    }
}
