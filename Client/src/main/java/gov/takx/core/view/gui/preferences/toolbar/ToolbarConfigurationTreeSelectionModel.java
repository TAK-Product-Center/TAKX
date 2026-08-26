package gov.takx.core.view.gui.preferences.toolbar;

import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 * Selection model for toolbar configuration tree to prevent selection of a placeholder node due to a mouse click.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarConfigurationTreeSelectionModel extends DefaultTreeSelectionModel
{
    public ToolbarConfigurationTreeSelectionModel()
    {
        setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    }

    @Override
    public void setSelectionPath(@Nullable TreePath path)
    {
        // Prevent selection of any placeholder nodes due to mouse click
        if (path != null && !(path.getLastPathComponent() instanceof PlaceholderTreeNode))
        {
            super.setSelectionPath(path);
        }
    }
}
