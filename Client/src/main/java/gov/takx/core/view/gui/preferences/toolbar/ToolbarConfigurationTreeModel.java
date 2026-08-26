package gov.takx.core.view.gui.preferences.toolbar;

import gov.takx.core.toolbar.AToolbarComponent;
import gov.takx.core.toolbar.ToolbarConfiguration;
import gov.takx.core.toolbar.ToolbarTab;
import jakarta.annotation.Nonnull;

import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;

import java.util.List;
import java.util.Optional;

/**
 * Model for toolbar configuration tree, which handles updating the configuration as nodes are added to or
 * removed from the tree.
 *
 * @since 2.0 (Renamed in 3.4)
 */
public class ToolbarConfigurationTreeModel extends DefaultTreeModel
{
    /**
     * The index level for each type of node.
     */
    public static final int CONFIGURATION_NODE_INDEX = 0;
    public static final int TAB_NODE_INDEX = 1;
    public static final int COMPONENT_NODE_INDEX = 2;

    /**
     * The configuration being modeled by the tree.
     */
    private final ToolbarConfiguration configuration;

    private ToolbarConfigurationTreeModel(@Nonnull ToolbarConfigurationTreeNode root)
    {
        super(root);
        configuration = root.configuration;
    }

    /**
     * Builds the toolbar configuration tree model using the provided configuration.
     *
     * @param configuration The configuration from which to build the model
     * @return The toolbar configuration tree model with the provided configuration a the root
     */
    @Nonnull
    public static ToolbarConfigurationTreeModel build(@Nonnull ToolbarConfiguration configuration)
    {
        ToolbarConfigurationTreeNode root = new ToolbarConfigurationTreeNode(configuration);
        addTabsToTree(root, configuration.getTabs());

        return new ToolbarConfigurationTreeModel(root);
    }

    /**
     * Creates the necessary nodes to represent a tab and its children.
     *
     * @return The tab tree node
     */
    public static ToolbarTabTreeNode createNodesForTab(ToolbarTab tab)
    {
        ToolbarTabTreeNode node = new ToolbarTabTreeNode(tab);
        addComponentsToTree(node, tab.getComponents());

        return node;
    }

    private static void addTabsToTree(@Nonnull ToolbarConfigurationTreeNode parentNode, @Nonnull List<ToolbarTab> tabs)
    {
        for (ToolbarTab tab : tabs)
        {
            ToolbarTabTreeNode node = createNodesForTab(tab);
            parentNode.add(node);
        }

        // Only add the placeholder node if tabs is not empty. Otherwise, an expand button is shown and may confuse
        // the user.
        if (!tabs.isEmpty())
        {
            // Add a placeholder node after the last tab to make drag and drop work as expected
            parentNode.add(new PlaceholderTreeNode());
        }
    }

    private static void addComponentsToTree(@Nonnull ToolbarTabTreeNode parentNode, @Nonnull List<AToolbarComponent> components)
    {
        for (AToolbarComponent component : components)
        {
            ToolbarComponentTreeNode node = new ToolbarComponentTreeNode(component);
            parentNode.add(node);
        }

        // Only add the placeholder node if components is not empty. Otherwise, an expand button is shown and may confuse
        // the user.
        if (!components.isEmpty())
        {
            // Add a placeholder node after the last button to make drag and drop work as expected
            parentNode.add(new PlaceholderTreeNode());
        }
    }

    @Override
    public void insertNodeInto(@Nonnull MutableTreeNode newChild, @Nonnull MutableTreeNode parent, int index)
    {
        boolean added = false;
        boolean addPlaceholder = false;
        if (newChild instanceof ToolbarTabTreeNode tabNode)
        {
            ToolbarConfigurationTreeNode configurationNode = (ToolbarConfigurationTreeNode) parent;

            // If this is the first child added, make sure a placeholder node gets added
            if (configurationNode.getChildCount() == 0)
            {
                addPlaceholder = true;
            }

            // Make sure tab is added before placeholder node, if one is present
            if (configurationNode.getChildCount() > 0 && configurationNode.getChildCount() == index)
            {
                index--;
            }

            configurationNode.configuration.insertTab(tabNode.tab, index, false);
            added = true;
        } else if (newChild instanceof ToolbarComponentTreeNode buttonNode)
        {
            ToolbarTabTreeNode tabNode = (ToolbarTabTreeNode) parent;

            // If this is the first child added, make sure a placeholder node gets added
            if (tabNode.getChildCount() == 0)
            {
                addPlaceholder = true;
            }

            // Make sure button is added before placeholder node
            if (tabNode.getChildCount() > 0 && tabNode.getChildCount() == index)
            {
                index--;
            }

            added = tabNode.tab.insertComponent(buttonNode.component, index, false, true);
        }

        if (added)
        {
            super.insertNodeInto(newChild, parent, index);
        }

        if (addPlaceholder)
        {
            super.insertNodeInto(new PlaceholderTreeNode(), parent, ++index);
        }
    }

    @Override
    public void removeNodeFromParent(@Nonnull MutableTreeNode node)
    {
        Optional<MutableTreeNode> placeholderToRemove = Optional.empty();

        if (node instanceof ToolbarTabTreeNode tabNode)
        {
            ToolbarConfigurationTreeNode configurationNode = (ToolbarConfigurationTreeNode) tabNode.getParent();

            configuration.removeTab(tabNode.tab);

            // If the last tab is being removed, remove the placeholder as well so the expand button will not be shown.
            if (configuration.getTabs().isEmpty())
            {
                placeholderToRemove = Optional.of((MutableTreeNode) configurationNode.getLastChild());
            }
        } else if (node instanceof ToolbarComponentTreeNode buttonNode)
        {
            ToolbarTabTreeNode tabNode = (ToolbarTabTreeNode) buttonNode.getParent();
            tabNode.tab.removeComponent(buttonNode.component, true);

            // If the last button is being removed, remove the placeholder as well so the expand button will not be shown.
            if (tabNode.tab.getComponents().isEmpty())
            {
                placeholderToRemove = Optional.of((MutableTreeNode) tabNode.getLastChild());
            }
        }

        super.removeNodeFromParent(node);

        placeholderToRemove.ifPresent(super::removeNodeFromParent);
    }
}
