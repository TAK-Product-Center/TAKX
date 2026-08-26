package gov.takx.commons.swing;

import com.formdev.flatlaf.ui.FlatTreeUI;

import javax.swing.JTree;
import javax.swing.plaf.TreeUI;
import javax.swing.tree.TreePath;

import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;

/**
 * Custom tree that allows the component used to render a row to grow to the full width of the tree.
 *
 * @since 3.4
 */
public class GrowingRowTree extends JTree
{
    @Override
    public void setUI(TreeUI ui)
    {
        super.setUI(new GrowingRowTreeUI());
    }

    /**
     * @since 3.4 (was formerly inner class CustomTreeUI in TreePanel)
     */
    public static class GrowingRowTreeUI extends FlatTreeUI
    {
        @Override
        public Rectangle getPathBounds(JTree tree, TreePath path)
        {
            Rectangle bounds = super.getPathBounds(tree, path);
            if (bounds != null)
            {
                int treeWidth = tree.getWidth();
                if (treeWidth > 0) bounds.width = treeWidth - bounds.x;
            }

            return bounds;
        }

        /**
         * Extends the bounds of the row to fill the width of the tree prior to painting the row.
         *
         * {@inheritDoc}
         */
        @Override
        protected void paintRow(Graphics g,
                                Rectangle clipBounds,
                                Insets insets,
                                Rectangle bounds,
                                TreePath path,
                                int row,
                                boolean isExpanded,
                                boolean hasBeenExpanded,
                                boolean isLeaf)
        {
            int width = tree.getWidth() - bounds.x;
            if (width > 0) bounds.width = width;
            super.paintRow(g, clipBounds, insets, bounds, path, row, isExpanded, hasBeenExpanded, isLeaf);
        }
    }
}
