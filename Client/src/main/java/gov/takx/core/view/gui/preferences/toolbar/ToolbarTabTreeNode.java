package gov.takx.core.view.gui.preferences.toolbar;

import gov.takx.core.toolbar.ToolbarTab;
import jakarta.annotation.Nonnull;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Tree node that contains a toolbar tab.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarTabTreeNode extends DefaultMutableTreeNode
{
    public final ToolbarTab tab;

    public ToolbarTabTreeNode(@Nonnull ToolbarTab tab)
    {
        super(tab);
        this.tab = tab;
    }

    /**
     * @return The child count, adjusted to ignore the placeholder node if one is present.
     */
    public int getAdjustedChildCount()
    {
        int childCount = getChildCount();
        if (childCount > 0 && getLastChild() instanceof PlaceholderTreeNode) childCount--;

        return childCount;
    }
}
