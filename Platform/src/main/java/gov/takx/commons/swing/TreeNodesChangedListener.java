package gov.takx.commons.swing;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

/**
 * This is a FunctionalInterface that allows users to easily handle tree nodes changed events without having to extend base classes
 * or implement unnecessary methods. <br/>
 * To use it with a method reference, the method
 * must be cast to {@link TreeNodesChangedListener} so that the default methods are inherited.
 * <br/>
 * Usage:
 * <br/>
 * <pre>
 * {@code
 * treeModel.addTreeModelListener((TreeNodesChangedListener)this::handleTreeNodesChanged);
 * ...
 * void handleTreeNodesChanged(TreeModelEvent e){...}
 * }
 * </pre>
 * Note: Because duplicate method references are not equal to one another, they cannot be used inline if you need to remove the listener
 * at later time. The method reference will need to be assigned to a variable first, which can then be used to remove the listener.
 *
 * @since 5.5.1
 */
@FunctionalInterface
public interface TreeNodesChangedListener extends TreeModelListener
{
    @Override
    void treeNodesChanged(TreeModelEvent e);

    @Override
    default void treeNodesInserted(TreeModelEvent e)
    {
    }

    @Override
    default void treeNodesRemoved(TreeModelEvent e)
    {
    }

    @Override
    default void treeStructureChanged(TreeModelEvent e)
    {
    }
}
