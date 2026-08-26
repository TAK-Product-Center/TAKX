package gov.takx.core.view.gui.preferences.toolbar;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Placeholder node to allow drag and drop to work as expected in a JTree. Currently, Swing does not allow dropping a
 * node as the last child of a parent when insert mode is used. This prevents buttons from being place at the end of a
 * tab without dropping them on the tab node itself, which may not be intuitive. Using invisible placeholder nodes as the
 * last child makes it appear as if the user is adding the node as the last child as expected.  The
 * {@link ToolbarConfigurationTreeModel} is responsible for ensuring the placeholder nodes remain as the last child.
 * {@link ToolbarConfigurationTreeCellRenderer} is responsible for making the nodes appear invisible (by rendering as
 * a single pixel label). Logic in {@link ToolbarConfigurationTreeSelectionModel} and
 * {@link ToolbarConfigurationDialog#initConfigTree()} prevents the invisible nodes from being selected.
 *
 * @since 2.0
 */
public class PlaceholderTreeNode extends DefaultMutableTreeNode
{
}
