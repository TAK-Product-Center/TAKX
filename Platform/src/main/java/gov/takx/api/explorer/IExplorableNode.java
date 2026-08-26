package gov.takx.api.explorer;

import javax.swing.Icon;
import javax.swing.tree.MutableTreeNode;

/**
 * Interface for nodes displayed on the explorer tree.
 *
 * @since 1.48
 */
public interface IExplorableNode extends MutableTreeNode
{
    /**
     * Get the icon used when displaying this node on the tree.
     *
     * @return the icon used when displaying this node on the tree.
     */
    Icon getIcon();

    /**
     * Get the explorable associated with this node.
     *
     * @return the explorable associated with this node.
     */
    IExplorable getExplorable();

    /**
     * Get the comparator used to sort children of this node.
     *
     * @return The current comparator, or {@code null} if there was none.
     * @since 2.29
     */
    ITreeNodeComparator getComparator();
}
