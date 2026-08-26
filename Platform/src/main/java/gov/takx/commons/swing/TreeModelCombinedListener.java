package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

/**
 * A functional interface that allows consuming {@link TreeModelEvent} events by implementing only a single method. Source
 * events are wrapped in a {@link CombinedTreeModelEvent} with an enum {@link Type} that indicates the listener method
 * that was initially called.
 *
 * @since 5.7
 */
@FunctionalInterface
@IncubatingApi(since = "5.7")
public interface TreeModelCombinedListener extends TreeModelListener
{
    /**
     * All events passed to this listener are redirected to this method.
     *
     * @param event a {@code TreeModelEvent} describing changes to a tree model
     */
    void treeNodesUpdated(@Nonnull CombinedTreeModelEvent event);

    @Override
    default void treeNodesChanged(TreeModelEvent event)
    {
        treeNodesUpdated(new CombinedTreeModelEvent(Type.Change, event));
    }

    @Override
    default void treeNodesInserted(TreeModelEvent event)
    {
        treeNodesUpdated(new CombinedTreeModelEvent(Type.Insert, event));
    }

    @Override
    default void treeNodesRemoved(TreeModelEvent event)
    {
        treeNodesUpdated(new CombinedTreeModelEvent(Type.Remove, event));
    }

    @Override
    default void treeStructureChanged(TreeModelEvent event)
    {
        treeNodesUpdated(new CombinedTreeModelEvent(Type.Structure, event));
    }

    /**
     * Indicates the type of tree model event that was wrapped.
     */
    enum Type
    {
        /**
         * Corresponds to {@link TreeModelListener#treeNodesChanged(TreeModelEvent)} events.
         */
        Change,
        /**
         * Corresponds to {@link TreeModelListener#treeNodesRemoved(TreeModelEvent)} events.
         */
        Remove,
        /**
         * Corresponds to {@link TreeModelListener#treeNodesInserted(TreeModelEvent)} events.
         */
        Insert,
        /**
         * Corresponds to {@link TreeModelListener#treeStructureChanged(TreeModelEvent)} events.
         */
        Structure
    }

    /**
     * A TreeModelEvent with a type that can be used to determine which change was method initially called.
     */
    class CombinedTreeModelEvent extends TreeModelEvent
    {
        private final Type type;

        private CombinedTreeModelEvent(Type type, TreeModelEvent source)
        {
            super(source.getSource(), source.getTreePath(), source.getChildIndices(), source.getChildren());
            this.type = type;
        }

        /**
         * @return the type of event that was wrapped
         */
        public Type getType()
        {
            return type;
        }
    }
}
