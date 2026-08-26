package gov.takx.core.view.gui.preferences.toolbar;

import gov.takx.core.toolbar.ToolbarConfiguration;
import jakarta.annotation.Nonnull;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Tree node that contains a toolbar configuration.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarConfigurationTreeNode extends DefaultMutableTreeNode
{
    public final ToolbarConfiguration configuration;

    public ToolbarConfigurationTreeNode(@Nonnull ToolbarConfiguration configuration)
    {
        super(configuration);
        this.configuration = configuration;
    }

    /**
     * @return The child count, adjusted to ignore the placeholder node if one is present.
     */
    public int getAdjustedChildCount()
    {
        int childCount = getChildCount();
        return (childCount > 0 && getLastChild() instanceof PlaceholderTreeNode) ? childCount - 1 : childCount;
    }
}
