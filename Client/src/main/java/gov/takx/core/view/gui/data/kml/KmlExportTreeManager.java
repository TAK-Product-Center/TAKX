package gov.takx.core.view.gui.data.kml;

import gov.takx.commons.swing.CombiningTreeSelectionModel;
import gov.takx.commons.swing.TriStateJCheckBox;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

/**
 * Manager class to open the KML export dialog and to manage mouse clicks on the various tree components,
 * as the operator decides which objects to export.
 */
public class KmlExportTreeManager extends MouseAdapter implements TreeSelectionListener
{
    private final CombiningTreeSelectionModel selectionModel;
    private final KmlExportDialog dlg;
    private final JTree tree;
    private final int hotspot = new JCheckBox().getPreferredSize().width;
    private final boolean okay;

    /**
     * Constructor.  Set the selection model from the tree model and open the dialog to allow the user
     * to select which nodes to export.
     *
     * @param node top level node
     */
    public KmlExportTreeManager(DefaultMutableTreeNode node)
    {
        tree = new JTree(node);
        selectionModel = new CombiningTreeSelectionModel(tree.getModel());
        tree.setCellRenderer(new KmlExportTreeCellRenderer(tree.getCellRenderer(), selectionModel));
        tree.addMouseListener(this);
        selectionModel.addTreeSelectionListener(this);

        dlg = new KmlExportDialog(tree);
        dlg.setVisible(true);
        okay = dlg.isOkay();
    }

    public boolean isOkay()
    {
        return okay;
    }

    /**
     * Mouse was clicked on the tree.  Update the selection model.
     *
     * @param me mouse event
     */
    @Override
    public void mouseClicked(MouseEvent me)
    {
        TreePath path = tree.getPathForLocation(me.getX(), me.getY());
        if (path == null || me.getX() > tree.getPathBounds(path).x + hotspot) return;

        boolean selected = selectionModel.isPathSelected(path, true);
        selectionModel.removeTreeSelectionListener(this);

        try
        {
            if (selected)
            {
                selectionModel.removeSelectionPath(path);
            } else
            {
                selectionModel.addSelectionPath(path);
            }
        } finally
        {
            selectionModel.addTreeSelectionListener(this);
            tree.treeDidChange();
        }

        TreePath[] paths = selectionModel.getSelectionPaths();
        dlg.enableExport(paths != null && paths.length > 0);
    }

    public TreeSelectionModel getSelectionModel()
    {
        return selectionModel;
    }

    @Override
    public void valueChanged(TreeSelectionEvent e)
    {
        tree.treeDidChange();
    }

    /**
     * Return a list of node user objects selected from a tree node and its descendents.
     *
     * @param node tree node to walk to find the selected nodes
     * @return list of selected node's user objects
     * @since 5.5.1, moved here from KmExporter
     */
    public List<Object> getSelectedUserObjectsFromNode(DefaultMutableTreeNode node)
    {
        final List<Object> selectedObjects = new ArrayList<>();
        if (node.isLeaf())
        {
            selectedObjects.add(node.getUserObject());
        } else
        {
            for (int children = node.getChildCount(), i = 0; i < children; i++)
            {
                selectedObjects.addAll(getSelectedUserObjectsFromNode((DefaultMutableTreeNode) node.getChildAt(i)));
            }
        }
        return selectedObjects;
    }

    /**
     * Renderer of the tree cells for the tree in the KML export dialog.  Makes use of {@link TriStateJCheckBox} for
     * paths that are partially selected.
     *
     * @since 5.5.1, moved here from an external class
     */
    private static class KmlExportTreeCellRenderer extends JPanel implements TreeCellRenderer
    {
        private final CombiningTreeSelectionModel selectionModel;
        private final TreeCellRenderer delegate;
        private final TriStateJCheckBox checkBox = new TriStateJCheckBox();

        protected KmlExportTreeCellRenderer(TreeCellRenderer delegate, CombiningTreeSelectionModel selectionModel)
        {
            this.delegate = delegate;
            this.selectionModel = selectionModel;
            setLayout(new BorderLayout());
            setOpaque(false);
            checkBox.setOpaque(false);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
        {
            Component renderer = delegate.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

            TreePath path = tree.getPathForRow(row);
            if (path != null)
            {
                if (selectionModel.isPathSelected(path, true))
                {
                    checkBox.setState(TriStateJCheckBox.State.SELECTED);
                } else
                {
                    checkBox.setState(selectionModel.isPartiallySelected(path) ? TriStateJCheckBox.State.INDETERMINATE : TriStateJCheckBox.State.NOT_SELECTED);
                }
            }
            removeAll();
            add(checkBox, BorderLayout.WEST);
            add(renderer, BorderLayout.CENTER);
            return this;
        }
    }
}
