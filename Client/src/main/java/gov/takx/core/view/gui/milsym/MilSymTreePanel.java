package gov.takx.core.view.gui.milsym;

import com.formdev.flatlaf.ui.FlatTreeUI;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.symbology.ISymbolTable;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.milsym.IMilStd2525Service;
import gov.takx.api.milsym.SymbolWrapper;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXTree;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.tree.AbstractLayoutCache;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * A panel that shows {@link ISymbolTable.Symbol} entries grouped
 * by their containing folder structure.
 *
 * @since 5.1
 */
public class MilSymTreePanel extends AMilSymFilterablePanel
{
    private final String initialSymbol;
    private DefaultMutableTreeNode rootNode;
    private DefaultTreeModel model;
    private JTree tree;

    /**
     * @param milStd2525Service MIL-STD-2525 service to pull symbols from.
     * @param initialSymbol The initial symbol to show as being selected. Can be {@code null} to start with no selection or expanded paths.
     * @param onSelectEntry Action to run when clicked on.
     */
    public MilSymTreePanel(@Nonnull IMilStd2525Service milStd2525Service,
                           @Nullable String initialSymbol,
                           @Nonnull Consumer<ISymbolTable.Symbol> onSelectEntry)
    {
        super(milStd2525Service, onSelectEntry);

        this.initialSymbol = initialSymbol;

        initComponents();

        scrollPanel.getVerticalScrollBar().setUnitIncrement(20);
    }

    @Override
    @InvokeOnEDT
    protected synchronized void updateSearch(@Nonnull List<SymbolWrapper> symbolWrappers)
    {
        // The panel isn't intended to support full searching/filtering.
        // Just refresh what is visible. Things that aren't visible will re-render when expanded.
        if (tree != null)
        {
            updateVisibleItems(rootNode);
            return;
        }

        contentWrapper.removeAll();
        contentWrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Build full path dirs to the contained symbols.
        rootNode = new DefaultMutableTreeNode("ROOT");
        model = new DefaultTreeModel(rootNode);
        buildModel(rootNode, symbolWrappers);

        // Initialize the tree and lock its cell displays to the width of the scroll pane viewport.
        IntSupplier widthLookup = () -> scrollPanel.getWidth();
        JTree localTree = new JXTree();
        tree = localTree;
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setUI(new FlatTreeUI()
        {
            private final int minWidth = computeMinWidth();

            @Override
            protected AbstractLayoutCache.NodeDimensions createNodeDimensions()
            {
                return new NodeDimensionsHandler()
                {
                    @Override
                    public Rectangle getNodeDimensions(Object value, int row, int depth, boolean expanded, Rectangle size)
                    {
                        int indentation = getRowX(row, depth);
                        int parentWidth = widthLookup.getAsInt();

                        Rectangle dimensions = super.getNodeDimensions(value, row, depth, expanded, size);
                        dimensions.width = parentWidth - indentation - 50;
                        if (dimensions.width < minWidth) dimensions.width = minWidth;
                        return dimensions;
                    }
                };
            }

            /**
             * @return The minimum space to display the initial set of {@link ISymbolTable.Folder} entries in the tree.
             */
            private int computeMinWidth()
            {
                // Note: We're using the 'localTree' in the outer method scope and not the field 'tree'.
                // Accessing the outer class's field "tree" won't work here since this is called from
                // the inner class's default constructor.
                int minWidth = 0;
                Font font = localTree.getFont();
                FontRenderContext frc = localTree.getFontMetrics(font).getFontRenderContext();
                ISymbolTable.Folder root = milStd2525Service.getSymbolTreeRoot();
                for (ISymbolTable.Folder child : root.getChildren())
                {
                    Rectangle2D bounds = font.getStringBounds(child.getName(), frc);
                    minWidth = (int) Math.max(minWidth, bounds.getWidth() + 25 /* padding */);
                }
                return minWidth;
            }
        });
        tree.setCellRenderer(new TreeCellRenderer()
        {
            private static final Border DIR_LABEL_PADDING = BorderFactory.createEmptyBorder(3, 0, 3, 0);

            @Override
            public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                          boolean expanded, boolean leaf, int row, boolean hasFocus)
            {
                DefaultMutableTreeNode node = ((DefaultMutableTreeNode) value);
                Object data = node.getUserObject();
                if (data instanceof String dir)
                {
                    // String values are used for group names
                    JLabel label = new JLabel(dir);
                    if (selected && hasFocus) label.setForeground(Color.BLACK);
                    label.setBorder(DIR_LABEL_PADDING);
                    return label;
                } else if (data instanceof SymbolWrapper symbolWrapper)
                {
                    MilSymEntryPanel panel = new MilSymEntryPanel(milStd2525Service, symbolWrapper, adapter);
                    if (selected && hasFocus) panel.setLabelForegroundColor(Color.BLACK);
                    return panel;
                } else
                {
                    throw new IllegalStateException("Encountered tree node with unexpected value: " + data);
                }
            }
        });
        tree.setModel(model);

        // Navigate to the initial symbol selection if one exists.
        if (initialSymbol != null) setSelectedSymbolPath(initialSymbol);

        // Click to pass selected node to the on-select consumer.
        tree.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mousePressed(MouseEvent e)
            {
                // Previously, this code used tree.getLastSelectedPathComponent() to obtain the node the user
                // clicked on.  This worked when we required a double click to select an icon.  However, when we
                // changed it to only require a single click, clicking on a non-leaf node, or even the expansion
                // symbol, would result in this code "selecting" the previously selected node.
                //
                // So, instead of relying on the selection, translate the mouse event to the clicked node and use
                // that to determine what the user selected.

                var path = tree.getPathForLocation(e.getX(), e.getY());
                final var node = path != null ? (DefaultMutableTreeNode) path.getLastPathComponent() : null;
                if (node != null && node.isLeaf())
                {
                    final var userObject = node.getUserObject();

                    if (userObject instanceof SymbolWrapper wrapper)
                    {
                        onSelectEntry.accept(wrapper.getSymbol());
                    } else if (userObject instanceof ISymbolTable.Symbol symbol)
                    {
                        onSelectEntry.accept(symbol);
                    }
                }
            }
        });
        contentWrapper.add(BorderLayout.CENTER, tree);

        // There is some odd space to the right which is just empty. It's not an issue because
        // the content of things in the UI is still mapped to what is visible.
        // Hiding the horizontal scroll-bar has no negative effect on UX.
        scrollPanel.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPanel.addComponentListener(new ComponentAdapter()
        {
            @Override
            public void componentResized(ComponentEvent e)
            {
                updateVisibleItems(rootNode);
                contentWrapper.doLayout();
                contentWrapper.revalidate();
                SwingUtilities.invokeLater(() -> tree.setPreferredSize(null));
            }
        });
    }

    /**
     * Navigates to and selects the given symbol code in the tree,
     * expanding all parents up to the item as needed.
     *
     * @param symbol Symbol code to select in the tree.
     * @since 5.2
     */
    @InvokeOnEDT
    @SuppressWarnings("unchecked")
    public void setSelectedSymbolPath(@Nullable String symbol)
    {
        // Collapse tree, show no initial selection.
        if (symbol == null)
        {
            for (int i = tree.getRowCount() - 1; i >= 0; i--) tree.collapseRow(i);
            return;
        }

        // Get the base symbol code (no special amplifiers or such)
        String filteredInitialSymbol = milStd2525Service.getBaseSymbolCode(symbol);
        if (filteredInitialSymbol == null) return;

        // Find the path to the given symbol.
        TreeNode[] pathToSymbol = null;
        var e = (Enumeration<DefaultMutableTreeNode>) (Object) rootNode.depthFirstEnumeration();
        while (e.hasMoreElements())
        {
            DefaultMutableTreeNode node = e.nextElement();
            if (node.getUserObject() instanceof SymbolWrapper wrapper && wrapper.getSymbol().getCode().equals(filteredInitialSymbol))
            {
                pathToSymbol = model.getPathToRoot(node);
                break;
            }
        }

        // Expand the path to the symbol and select it.
        if (pathToSymbol != null)
        {
            TreePath path = new TreePath(pathToSymbol);
            tree.expandPath(path);
            tree.setSelectionPath(path);
        }
    }

    /**
     * @param node Node to update visible children of.
     */
    private void updateVisibleItems(@Nonnull TreeNode node)
    {
        TreeNode[] pathToRoot = model.getPathToRoot(node);

        // The path is not visible. Skip.
        if (!tree.isExpanded(new TreePath(pathToRoot))) return;

        // Update visible children.
        Iterator<? extends TreeNode> it = node.children().asIterator();
        while (it.hasNext())
        {
            TreeNode next = it.next();
            if (next.isLeaf())
            {
                model.nodeChanged(next);
            } else
            {
                updateVisibleItems(next);
            }
        }
    }

    /**
     * @param root Root to build on.
     * @param symbolWrappers Symbols to append to the model outlined by the given root.
     */
    @SuppressWarnings("unchecked")
    private static void buildModel(@Nonnull DefaultMutableTreeNode root, @Nonnull List<SymbolWrapper> symbolWrappers)
    {
        for (SymbolWrapper wrapper : symbolWrappers)
        {
            DefaultMutableTreeNode node = root;
            for (ISymbolTable.Folder folder : wrapper.getFolderPath().stream().skip(1).toList())
            {
                String folderName = folder.getName();
                Iterator<? extends DefaultMutableTreeNode> iterator =
                        (Iterator<? extends DefaultMutableTreeNode>) (Object) node.children().asIterator();
                boolean found = false;
                while (iterator.hasNext())
                {
                    DefaultMutableTreeNode next = iterator.next();
                    String nextTitle = (String) next.getUserObject();
                    if (nextTitle.equals(folderName))
                    {
                        node = next;
                        found = true;
                        break;
                    }
                }

                if (!found)
                {
                    DefaultMutableTreeNode child = new DefaultMutableTreeNode(folderName);
                    node.add(child);
                    node = child;
                }
            }

            node.add(new DefaultMutableTreeNode(wrapper));
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        scrollPanel = new JScrollPane();
        contentWrapper = new JPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "fill:default:grow"));

        //======== scrollPanel ========
        {
            scrollPanel.setName("scrollPanel");

            //======== contentWrapper ========
            {
                contentWrapper.setName("contentWrapper");
                contentWrapper.setLayout(new BorderLayout());
            }
            scrollPanel.setViewportView(contentWrapper);
        }
        add(scrollPanel, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JScrollPane scrollPanel;
    private JPanel contentWrapper;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
