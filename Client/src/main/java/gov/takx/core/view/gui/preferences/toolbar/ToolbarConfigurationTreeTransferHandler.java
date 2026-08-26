package gov.takx.core.view.gui.preferences.toolbar;

import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.core.toolbar.AToolbarComponent;
import gov.takx.core.toolbar.ToolbarConfiguration;
import gov.takx.core.toolbar.ToolbarTab;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static gov.takx.core.view.gui.preferences.toolbar.ToolbarConfigurationTransferable.COMPONENT_DATA_FLAVOR;
import static gov.takx.core.view.gui.preferences.toolbar.ToolbarConfigurationTransferable.TAB_DATA_FLAVOR;

/**
 * Transfer handler for drag and drop of toolbar configuration tree components to another area in the tree.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarConfigurationTreeTransferHandler extends TransferHandler
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Reference to the configuration dialog to allow reuse of methods for adding/moving buttons/tabs
     */
    private final ToolbarConfigurationDialog dialog;

    public ToolbarConfigurationTreeTransferHandler(ToolbarConfigurationDialog dialog)
    {
        this.dialog = dialog;
    }

    @Override
    public boolean canImport(@Nonnull TransferSupport info)
    {
        if (!info.isDrop()) return false;

        // Get the drop location, which provides the path that should become the parent of the new data
        JTree.DropLocation dropLocation = (JTree.DropLocation) info.getDropLocation();
        TreePath parentPath = dropLocation.getPath();

        // In some situations, the path will be null and import is not supported for those cases
        if (parentPath == null) return false;

        Object lastPathComponent = parentPath.getLastPathComponent();

        // Only import components into tabs, and only import tabs into configurations
        return (info.isDataFlavorSupported(COMPONENT_DATA_FLAVOR) && lastPathComponent instanceof ToolbarTabTreeNode) ||
                (info.isDataFlavorSupported(TAB_DATA_FLAVOR) && lastPathComponent instanceof ToolbarConfigurationTreeNode);
    }

    @Override
    protected Transferable createTransferable(@Nonnull JComponent component)
    {
        JTree tree = (JTree) component;

        TreeNode node = (TreeNode) tree.getLastSelectedPathComponent();
        DataFlavor flavor = null;
        Object object = null;

        if (node instanceof ToolbarComponentTreeNode componentNode)
        {
            flavor = COMPONENT_DATA_FLAVOR;
            object = componentNode.component;
        } else if (node instanceof ToolbarTabTreeNode tabNode)
        {
            flavor = TAB_DATA_FLAVOR;
            object = tabNode.tab;
        }

        return new ToolbarConfigurationTransferable(flavor, object);
    }

    @Override
    public int getSourceActions(JComponent component)
    {
        // When a user drags and drops within the toolbar tree, we always want move the data, not copy it.
        return component instanceof JTree ? TransferHandler.MOVE : TransferHandler.NONE;
    }

    @Override
    public boolean importData(@Nonnull TransferSupport info)
    {
        if (!canImport(info)) return false;

        boolean imported = false;

        Transferable transfer = info.getTransferable();
        JTree.DropLocation dropLocation = (JTree.DropLocation) info.getDropLocation();
        TreePath parentPath = dropLocation.getPath();
        MutableTreeNode parentNode = (MutableTreeNode) parentPath.getLastPathComponent();

        // The child index will be -1 if the drop was over the parent itself. In this case, add the data at the end of
        // the parent's list of children
        int childIndex = dropLocation.getChildIndex() == -1 ? parentNode.getChildCount() : dropLocation.getChildIndex();

        try
        {
            if (info.isDataFlavorSupported(COMPONENT_DATA_FLAVOR))
            {
                List<AToolbarComponent> components = getComponentsFromTransferable(transfer);
                if (components.size() > 1)
                {
                    ToolbarTab tab = ((ToolbarTabTreeNode) parentNode).tab;

                    List<MutableTreeNode> treeNodes = new ArrayList<>();
                    for (AToolbarComponent component : components)
                    {
                        // First, we need to make sure this is a legal move. That is, the tab must not already contain
                        // components with the same ids as any of these components
                        if (tab.containsComponentId(component.getId()))
                        {
                            displayErrorMessageToUser("The " + tab.getDisplayText() + " tab already contains a copy of the " +
                                    component + " component.");
                            return false;
                        }

                        treeNodes.add(new ToolbarComponentTreeNode(component.copy()));
                    }

                    dialog.addNodesToTree(parentNode, childIndex, treeNodes, true);
                    imported = true;
                } else if (components.size() == 1)
                {
                    AToolbarComponent component = components.get(0);
                    ToolbarTab oldTab = component.requireTab();
                    ToolbarTab newTab = ((ToolbarTabTreeNode) parentNode).tab;

                    // Since we are removing the old node first, it may be necessary to adjust the index, if the old
                    // node was in the same parent above the new location.
                    if (oldTab.getId().equals(newTab.getId()))
                    {
                        if (oldTab.getComponentIndex(component) < childIndex)
                        {
                            childIndex--;
                        }
                    } else
                    {
                        // If moving to a different tab, we need to make sure this is a legal move. That is, the tab
                        // must not already contain that component
                        boolean componentFound = newTab.getComponents().stream().anyMatch(b -> b.getId().equals(component.getId()));
                        if (componentFound)
                        {
                            displayErrorMessageToUser("The " + newTab.getDisplayText() + " tab already contains a copy of the " +
                                    component + " component.");
                            return false;
                        }
                    }

                    // Node must first be removed in case it is being moved to the same tab, in which case the add
                    // would fail since duplicate components are not allowed within a tab.
                    boolean isExpanded = dialog.isTreeSelectionExpanded();
                    dialog.removeSelectedNodeFromTree();

                    ToolbarComponentTreeNode newNode = new ToolbarComponentTreeNode(component.copy());
                    dialog.addNodesToTree(parentNode, childIndex, Collections.singletonList(newNode), isExpanded);
                    imported = true;
                }
            } else if (info.isDataFlavorSupported(TAB_DATA_FLAVOR))
            {
                ToolbarTab tab = (ToolbarTab) transfer.getTransferData(TAB_DATA_FLAVOR);
                imported = transferTab(parentNode, childIndex, tab);
            }
        } catch (Exception e)
        {
            logger.error("Error importing data for drag/drop operation", e);
        }

        return imported;
    }

    /**
     * Retrieves the component(s) contained in the transferable. The transfer flavor should be checked before calling
     * this method.
     *
     * @param transfer A representation of the data being transferred
     * @return The component(s) contained in the transferable.
     * @since 2.16
     */
    @Nonnull
    private List<AToolbarComponent> getComponentsFromTransferable(@Nonnull Transferable transfer)
    {
        List<AToolbarComponent> components = new ArrayList<>();

        try
        {
            Object data = transfer.getTransferData(COMPONENT_DATA_FLAVOR);
            if (data instanceof List dataList)
            {
                for (Object dataObject : dataList)
                {
                    if (dataObject instanceof AToolbarComponent component)
                    {
                        components.add(component);
                    }
                }
            } else if (data instanceof AToolbarComponent component)
            {
                components.add(component);
            }
        } catch (Exception e)
        {
            logger.error("Error transferring component(s)", e);
        }

        return components;
    }

    @InvokeOnEDT
    private void displayErrorMessageToUser(@Nonnull String errorMessage)
    {
        AlwaysOnTopJOptionPane.showMessageDialog(errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Completes the transfer of a tab to its new location in the tree.
     *
     * @param parentNode The new parent node to which the tab should be transferred
     * @param childIndex The index where the tab should be transferred under the parent node
     * @param tab The tab being transferred
     * @return True if import was successful
     * @since 2.16
     */
    private boolean transferTab(@Nonnull MutableTreeNode parentNode, int childIndex, @Nonnull ToolbarTab tab)
    {
        ToolbarConfiguration configuration = tab.requireConfiguration();
        int originalChildIndex = configuration.getTabIndex(tab);

        // Since we are removing the old node first, it may be necessary to adjust the index, if the old
        // node was above the new location.
        if (originalChildIndex < childIndex) childIndex--;

        // Node must first be removed or else the add would fail since duplicate tabs are not allowed within a
        // configuration.
        boolean isExpanded = dialog.isTreeSelectionExpanded();
        dialog.removeSelectedNodeFromTree();

        ToolbarTabTreeNode newNode = ToolbarConfigurationTreeModel.createNodesForTab(tab);
        dialog.addNodesToTree(parentNode, childIndex, Collections.singletonList(newNode), isExpanded);

        return true;
    }
}
