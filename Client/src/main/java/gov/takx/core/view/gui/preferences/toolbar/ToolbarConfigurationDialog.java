package gov.takx.core.view.gui.preferences.toolbar;

import com.google.common.base.Strings;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.toolbar.TabType;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.commons.swing.GrowingRowTree;
import gov.takx.core.toolbar.AToolbarComponent;
import gov.takx.core.toolbar.ToolbarButton;
import gov.takx.core.toolbar.ToolbarConfiguration;
import gov.takx.core.toolbar.ToolbarConfigurationManager;
import gov.takx.core.toolbar.ToolbarSeparator;
import gov.takx.core.toolbar.ToolbarTab;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultListModel;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static gov.takx.core.view.gui.preferences.toolbar.ToolbarConfigurationTreeModel.COMPONENT_NODE_INDEX;
import static gov.takx.core.view.gui.preferences.toolbar.ToolbarConfigurationTreeModel.TAB_NODE_INDEX;
import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Dialog to create/edit a toolbar configuration.
 *
 * @since 2.0 (Renamed in 3.4)
 */
@SuppressWarnings({"CdiManagedBeanInconsistencyInspection", "Convert2MethodRef"})
@PseudoBean
public class ToolbarConfigurationDialog extends AStandardDialog
{
    @Inject
    private ToolbarConfigurationManager toolbarConfigManager;

    /**
     * The name of the original configuration (needed for updating an existing configuration in the case that an
     * existing configuration is being modified). This field is not used when a new configuration is being created.
     */
    private String originalConfigurationName;

    /**
     * The configuration which is being edited.
     */
    private final ToolbarConfiguration configuration;

    /**
     * Indicates if this is a newly created configuration, or if an existing configuration is being modified.
     */
    private final boolean isNewConfiguration;

    /**
     * Listener for configuration changes caused by this dialog.
     */
    private final IToolbarConfigurationDialogListener listener;

    /**
     * The configuration tree model.
     */
    private ToolbarConfigurationTreeModel treeModel;

    /**
     * The model for the toolbar button list.
     */
    private final DefaultListModel<ToolbarButton> buttonListModel = new DefaultListModel<>();

    /**
     * The width of a checkbox (used for handling mouse clicks on the tree)
     */
    private final int checkboxWidth = new JCheckBox().getPreferredSize().width;

    public ToolbarConfigurationDialog(@Nullable Dialog owner, @Nonnull ToolbarConfiguration configuration,
                                      boolean isNewConfiguration, @Nullable IToolbarConfigurationDialogListener listener)
    {
        super(owner);
        initComponents();

        this.isNewConfiguration = isNewConfiguration;
        this.configuration = configuration;
        this.listener = listener;

        if (isNewConfiguration)
        {
            setTitle("Create New Toolbar Configuration");
        } else
        {
            setTitle("Edit Toolbar Configuration");
            originalConfigurationName = configuration.getName();
        }

        configNameTextField.setText(configuration.getName());
        minimizeToolbarCheckBox.setSelected(configuration.isMinimized());
    }

    @PostConstruct
    private void postConstruct()
    {
        moveDownButton.setText(null);
        moveDownButton.setIcon(UIResources.getStandardSizedIcon(PLATFORM_ICONS, "arrow_down.png"));

        moveUpButton.setText(null);
        moveUpButton.setIcon(UIResources.getStandardSizedIcon(PLATFORM_ICONS, "arrow_up.png"));

        configNameTextField.setSelectionStart(0);
        configNameTextField.setSelectionEnd(configuration.getName().length());

        // Note: the set isn't maintained in sorted order in the config manager because the value that it is being
        // sorted on (the identification string) can change, which means buttons would need to be removed and re-added
        // to the set to keep them in order. So instead, we just sort them here when needed, which only happens when a
        // user opens the toolbar configuration UI and views the list of buttons.
        final List<ToolbarButton> sortedReferenceButtons = toolbarConfigManager.getReferenceButtons().stream()
                .sorted(ToolbarButton::compareTo).toList();
        sortedReferenceButtons.forEach(b -> buttonListModel.addElement(b));
        buttonList.setModel(buttonListModel);

        initConfigTree();
    }

    /**
     * Initializes the tree which contains the toolbar configuration with a model based on the current configuration.
     */
    private void initConfigTree()
    {
        treeModel = ToolbarConfigurationTreeModel.build(configuration);
        toolbarConfigTree.setModel(treeModel);
        ToolbarConfigurationTreeCellRenderer renderer = new ToolbarConfigurationTreeCellRenderer();
        toolbarConfigTree.setShowsRootHandles(true);
        toolbarConfigTree.setCellRenderer(renderer);
        toolbarConfigTree.setInvokesStopCellEditing(true);
        toolbarConfigTree.setSelectionModel(new ToolbarConfigurationTreeSelectionModel());
        toolbarConfigTree.setTransferHandler(new ToolbarConfigurationTreeTransferHandler(this));

        // Override up/down key actions to prevent selection of placeholder nodes
        Action defaultSelectNextAction = toolbarConfigTree.getActionMap().get("selectNext");
        Action defaultSelectPreviousAction = toolbarConfigTree.getActionMap().get("selectPrevious");

        AbstractAction selectNextAction = new AbstractAction("skipPlaceholdersOnDown")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                defaultSelectNextAction.actionPerformed(e);
                TreePath secondSelection = toolbarConfigTree.getLeadSelectionPath();

                // Check if a placeholder node is selected after the action
                if (secondSelection.getLastPathComponent() instanceof PlaceholderTreeNode)
                {
                    defaultSelectNextAction.actionPerformed(e);
                    TreePath thirdSelection = toolbarConfigTree.getLeadSelectionPath();

                    // If nothing changed on the last selection, we are at the end of the tree and should set the
                    // selection back to the original node.
                    if (secondSelection.equals(thirdSelection))
                    {
                        defaultSelectPreviousAction.actionPerformed(e);
                    }
                }
            }
        };

        toolbarConfigTree.getActionMap().put("selectNext", selectNextAction);

        AbstractAction selectPreviousAction = new AbstractAction("skipPlaceholdersOnUp")
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                defaultSelectPreviousAction.actionPerformed(e);

                // Check if a placeholder node is selected after the action
                if (toolbarConfigTree.getLeadSelectionPath().getLastPathComponent() instanceof PlaceholderTreeNode)
                {
                    defaultSelectPreviousAction.actionPerformed(e);
                }
            }
        };

        toolbarConfigTree.getActionMap().put("selectPrevious", selectPreviousAction);

        // Expand all of the tabs by default (this is how MS Word implements its configuration tree)
        DefaultMutableTreeNode root = (DefaultMutableTreeNode) treeModel.getRoot();
        int tabCount = root.getChildCount();

        for (int i = 0; i < tabCount; i++)
        {
            DefaultMutableTreeNode child = (DefaultMutableTreeNode) root.getChildAt(i);
            toolbarConfigTree.expandPath(new TreePath(child.getPath()));
        }
    }

    /**
     * Handles a save button action. Checks if an invalid configuration name has been provided (i.e. the name of an
     * existing configuration), preventing the save and alerting the user. If there are no issues, saves the
     * configuration and closes the dialog.
     */
    private void saveButtonActionPerformed()
    {
        // First update the configuration with the name from the text box
        configuration.setName(configNameTextField.getText());

        // If this is a new configuration or the configuration name was changed, ensure that a configuration with the
        // same name does not already exist
        if ((isNewConfiguration || !originalConfigurationName.equals(configuration.getName())) &&
                toolbarConfigManager.getConfiguration(configuration.getName()) != null)
        {
            AlwaysOnTopJOptionPane.showMessageDialog(
                    "A configuration already exists with this name. Please choose a different configuration name.",
                    "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Add or update the configuration with the toolbarConfigManager
        if (isNewConfiguration)
        {
            toolbarConfigManager.addConfiguration(configuration);
            if (listener != null) listener.configurationAdded(configuration);

        } else
        {
            toolbarConfigManager.updateConfiguration(originalConfigurationName, configuration);
            if (listener != null) listener.configurationChanged(originalConfigurationName, configuration);
        }

        dispose();
    }

    /**
     * Handles a cancel button action.
     */
    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    /**
     * Handles a change to the selection in the toolbar config tree. Relevant buttons are disabled/enabled based on
     * the new selection.
     */
    private void toolbarConfigTreeSelectionChanged()
    {
        addButton.setEnabled(false);
        removeButton.setEnabled(false);
        renameButton.setEnabled(false);
        addSeparatorButton.setEnabled(false);

        moveUpButton.setEnabled(false);
        moveDownButton.setEnabled(false);

        TreePath selectionPath = toolbarConfigTree.getSelectionPath();

        if (selectionPath != null)
        {
            TreeNode treeNode = (TreeNode) selectionPath.getLastPathComponent();

            // The remove button should only be enabled when a deletable element is selected in the tree
            removeButton.setEnabled(Strings.isNullOrEmpty(isSelectedNodeDeletable()));

            // Enable the move up/down buttons when a node is selected and that node can be moved up or down
            moveUpButton.setEnabled(canTreeNodeMove(treeNode, true));
            moveDownButton.setEnabled(canTreeNodeMove(treeNode, false));

            final boolean tabSelected = treeNode instanceof ToolbarTabTreeNode;
            final boolean buttonSelected = treeNode instanceof ToolbarComponentTreeNode componentTreeNode &&
                    componentTreeNode.component instanceof ToolbarButton;
            renameButton.setEnabled(tabSelected || buttonSelected);

            // The add button should be enabled if there is at least one button selected in the available buttons list
            if (buttonList.getSelectedIndex() > -1) addButton.setEnabled(true);

            addSeparatorButton.setEnabled(true);
        }
    }

    /**
     * Determines if a tree node can move in the specified direction in the tree.
     *
     * @param treeNode The tree node
     * @param moveUp   True if node needs to move up; false if node needs to move down
     * @return True if node can move in the specified direction
     */
    private boolean canTreeNodeMove(TreeNode treeNode, boolean moveUp)
    {
        if (treeNode instanceof ToolbarComponentTreeNode componentNode)
        {
            return findParentForComponentMove(componentNode, moveUp).isPresent();
        } else if (treeNode instanceof ToolbarTabTreeNode)
        {
            return findParentForTabMove(treeNode, moveUp).isPresent();
        }

        // Configurations are not movable
        return false;
    }

    /**
     * Returns the parent node to which a component should move when requested to move in the specified direction. If the
     * component cannot move, the return value will be absent. Note: parent node may be the same as the current parent node
     * if the component is able to move among its siblings.
     *
     * @param componentTreeNode The component node to move
     * @param moveUp True if the component should move up; false if it should move down
     * @return The parent node to which a component should move, or absent if component cannot move.
     * @since 3.4
     */
    private Optional<TreeNode> findParentForComponentMove(ToolbarComponentTreeNode componentTreeNode, boolean moveUp)
    {
        ToolbarTabTreeNode parentTabTreeNode = (ToolbarTabTreeNode) componentTreeNode.getParent();
        AToolbarComponent component = componentTreeNode.component;

        int siblingComponentCount = parentTabTreeNode.getAdjustedChildCount();
        int componentIndex = parentTabTreeNode.getIndex(componentTreeNode);

        // First check if component is able to move among its siblings
        if (moveUp ? componentIndex > 0 : componentIndex < siblingComponentCount - 1)
        {
            // Return the original parent tab
            return Optional.of(parentTabTreeNode);
        }

        TreeNode parentConfigurationNode = parentTabTreeNode.getParent();

        int siblingTabCount = parentConfigurationNode.getChildCount();
        int tabIndex = parentConfigurationNode.getIndex(parentTabTreeNode);

        // Finally check if component can move to a new tab in the configuration
        if (moveUp ? tabIndex > 0 : tabIndex < siblingTabCount - 1)
        {
            // Determine the tab to which the component should move: the bottommost/topmost tab depending on the move
            // direction, skipping any tabs which already contains a copy of the component
            int startingIndex = moveUp ? tabIndex - 1 : tabIndex + 1;
            Optional<TreeNode> tabNodeOptional = findTabWithoutComponent(component,
                    (ToolbarConfigurationTreeNode) parentConfigurationNode, startingIndex, moveUp);

            if (tabNodeOptional.isPresent())
            {
                return tabNodeOptional;
            }
        }

        return Optional.empty();
    }

    /**
     * Attempts to locate a tab that does not contain a copy of the provided component.
     *
     * @param component The component for which to look
     * @param configTreeNode The tab tree node containing the components
     * @param startingIndex The index at which to start searching
     * @param searchAbove Indication of whether to search above provided index or below
     * @return The first tab node without the provided component (starting at the specified index) or empty if one does
     * not exist.
     * @since 3.4
     */
    private Optional<TreeNode> findTabWithoutComponent(AToolbarComponent component, ToolbarConfigurationTreeNode configTreeNode,
                                                       int startingIndex, boolean searchAbove)
    {
        List<ToolbarTab> tabs = configTreeNode.configuration.getTabs();
        final UUID componentId = component.getId();

        if (searchAbove)
        {
            for (int tabIndex = startingIndex; tabIndex >= 0; tabIndex--)
            {
                if (!tabs.get(tabIndex).containsComponentId(componentId))
                {
                    return Optional.of(configTreeNode.getChildAt(tabIndex));
                }
            }
        } else
        {
            for (int tabIndex = startingIndex; tabIndex < tabs.size(); tabIndex++)
            {
                if (!tabs.get(tabIndex).containsComponentId(componentId))
                {
                    return Optional.of(configTreeNode.getChildAt(tabIndex));
                }
            }
        }

        return Optional.empty();
    }

    /**
     * Returns the parent node to which a tab should move when requested to move in the specified direction.  If the
     * tab cannot move, the return value will be absent.  Note: parent node will be the same as the current parent
     * node if the tab is able to move among its siblings.
     *
     * @param tabTreeNode The tab tree node
     * @param moveUp       True if the tab should move up; false if it should move down
     * @return The parent node to which a tab should move, or absent if tab cannot move.
     */
    private Optional<TreeNode> findParentForTabMove(TreeNode tabTreeNode, boolean moveUp)
    {
        ToolbarConfigurationTreeNode parentConfigurationTreeNode = (ToolbarConfigurationTreeNode) tabTreeNode.getParent();

        int siblingTabCount = parentConfigurationTreeNode.getAdjustedChildCount();
        int tabIndex = parentConfigurationTreeNode.getIndex(tabTreeNode);

        // Check if tab is able to move among its siblings
        if (moveUp ? tabIndex > 0 : tabIndex < siblingTabCount - 1)
        {
            // Return the original parent node
            return Optional.of(parentConfigurationTreeNode);
        }

        return Optional.empty();
    }

    /**
     * Adds a new tab below the currently selected tab.
     */
    private void addTabButtonActionPerformed()
    {
        final String displayText = AlwaysOnTopJOptionPane.showInputDialog("Please provide the display text for the new tab", "Add Tab");
        if (displayText == null) return; // Will be null if user cancelled

        ToolbarConfigurationTreeNode parentConfigurationNode = (ToolbarConfigurationTreeNode) toolbarConfigTree.getModel().getRoot();

        // Get the currently selected tab
        TreePath selectionPath = toolbarConfigTree.getSelectionPath();

        int newTabIndex;

        // If there is a tab in the selected path, the new tab will be added immediately below it
        if ((selectionPath != null) && (selectionPath.getPathCount() > TAB_NODE_INDEX))
        {
            ToolbarTabTreeNode selectedTabNode = (ToolbarTabTreeNode) selectionPath.getPath()[TAB_NODE_INDEX];
            newTabIndex = parentConfigurationNode.getIndex(selectedTabNode) + 1;
        } else
        {
            // If a tab is not selected, the new tab will be added as the last tab in the configuration
            newTabIndex = parentConfigurationNode.getAdjustedChildCount();
        }

        final ToolbarTab tab = ToolbarTab.createUserTab(displayText);
        ToolbarTabTreeNode newTabNode = ToolbarConfigurationTreeModel.createNodesForTab(tab);
        treeModel.insertNodeInto(newTabNode, parentConfigurationNode, newTabIndex);

        TreeNode[] nodePath = treeModel.getPathToRoot(newTabNode);
        toolbarConfigTree.setSelectionPath(new TreePath(nodePath));
        toolbarConfigTree.scrollPathToVisible(toolbarConfigTree.getSelectionPath());
    }

    /**
     * Adds the selected buttons to the currently selected tab.
     */
    private void addButtonActionPerformed()
    {
        TreePath selectionPath = toolbarConfigTree.getSelectionPath();
        TreeNode lastPathComponent = (TreeNode) selectionPath.getLastPathComponent();

        MutableTreeNode parentNode;
        int childIndex;

        // If a button is selected, add the new buttons immediately after it
        if (lastPathComponent instanceof ToolbarComponentTreeNode)
        {
            parentNode = (MutableTreeNode) lastPathComponent.getParent();
            childIndex = parentNode.getIndex(lastPathComponent) + 1;
        } else
        {
            parentNode = (MutableTreeNode) lastPathComponent;
            childIndex = ((ToolbarTabTreeNode) parentNode).getAdjustedChildCount();
        }

        ToolbarTab tab = ((ToolbarTabTreeNode) parentNode).tab;
        List<ToolbarButton> buttons = buttonList.getSelectedValuesList();
        List<MutableTreeNode> treeNodes = new ArrayList<>();
        for (ToolbarButton button : buttons)
        {
            // First, we need to make sure this is a legal move. That is, the tab must not already contain a button with
            // this id
            if (tab.containsComponentId(button.getId()))
            {
                AlwaysOnTopJOptionPane.showMessageDialog("The " + tab.getDisplayText() +
                        " tab already contains a copy of the " + button.getDisplayText() + " button.",
                        "Error", JOptionPane.ERROR_MESSAGE);

                return;
            }

            treeNodes.add(new ToolbarComponentTreeNode(button.copy()));
        }

        addNodesToTree(parentNode, childIndex, treeNodes, true);

        if (!buttons.isEmpty())
        {
            // Update selection in available buttons list to be the next button after the added button(s) if it exists
            updateListSelectionAfterRemoval(buttonList.getMinSelectionIndex() + 1, buttonList, buttonListModel);
        }
    }

    /**
     * Adds the provided nodes to the parent node at the indicated index.
     *
     * @param parentNode The parent node to which to add the nodes
     * @param childIndex The child index at which to add the nodes
     * @param nodes The nodes to add
     * @param expandPath Indicates whether the path should be expanded
     */
    public void addNodesToTree(@Nonnull MutableTreeNode parentNode, int childIndex,
                               @Nonnull List<MutableTreeNode> nodes, boolean expandPath)
    {
        for (MutableTreeNode treeNode : nodes)
        {
            treeModel.insertNodeInto(treeNode, parentNode, childIndex++);
        }

        if (!nodes.isEmpty())
        {
            // The selection should be set to the last added button
            TreeNode[] nodePath = treeModel.getPathToRoot(nodes.get(nodes.size() - 1));
            toolbarConfigTree.setSelectionPath(new TreePath(nodePath));

            if (expandPath)
            {
                toolbarConfigTree.expandPath(toolbarConfigTree.getSelectionPath());
            }

            toolbarConfigTree.scrollPathToVisible(toolbarConfigTree.getSelectionPath());
        }
    }

    /**
     * Removes the selected button from the toolbar configuration.
     */
    private void removeButtonActionPerformed()
    {
        removeSelectedNodeFromTree();
    }

    /**
     * Removes the selected node from the tree and updates the selection accordingly.
     */
    public void removeSelectedNodeFromTree()
    {
        TreePath selectionPath = toolbarConfigTree.getSelectionPath();

        if (selectionPath != null)
        {
            int row = toolbarConfigTree.getRowForPath(selectionPath);

            MutableTreeNode node = (MutableTreeNode) selectionPath.getLastPathComponent();
            treeModel.removeNodeFromParent(node);

            selectNextAvailable(row);
            toolbarConfigTree.scrollPathToVisible(toolbarConfigTree.getSelectionPath());
        }
    }

    /**
     * Determines if the current selection path is expanded.  This is necessary for correctly setting the expanded
     * state of a node when it is moved.
     *
     * @return True if selection is expanded
     */
    public boolean isTreeSelectionExpanded()
    {
        TreePath selectionPath = toolbarConfigTree.getSelectionPath();
        return selectionPath != null && toolbarConfigTree.isExpanded(selectionPath);
    }

    /**
     * Selects the correct row after the provided row has been removed.  If it was not the last row, the row after it is
     * selected (which is now in the same position).  If it was the last row, the row above is selected.
     *
     * @param row The index of the removed row
     */
    private void selectNextAvailable(int row)
    {
        if (row >= toolbarConfigTree.getRowCount())
        {
            row--;
        }

        toolbarConfigTree.setSelectionRow(row);
    }

    /**
     * Handles a change to the selection in the available button list.  The button for adding button(s) is enabled or
     * disabled as appropriate.
     */
    private void buttonListValueChanged()
    {
        addButton.setEnabled(false);

        int[] selectedIndices = buttonList.getSelectedIndices();
        TreePath selectionPath = toolbarConfigTree.getSelectionPath();

        if (selectedIndices.length > 0 && selectionPath != null)
        {
            addButton.setEnabled(true);
        }
    }

    /**
     * Handles an up button action by moving up the currently selected node.
     */
    private void moveUpButtonActionPerformed()
    {
        moveCurrentlySelectedNode(true);
    }

    /**
     * Handles a down button action by moving down the currently selected node.
     */
    private void moveDownButtonActionPerformed()
    {
        moveCurrentlySelectedNode(false);
    }

    /**
     * Moves the currently selected node in the specified direction as follows:
     * <p>
     * When direction is up:
     * - A button with a higher sibling moves up among its siblings.
     * - A button without a higher sibling moves up to the bottom of the next higher tab.
     * - A tab switches places with the tab above it.
     * <p>
     * When direction is down:
     * - A button with a lower sibling moves down among its siblings.
     * - A button without a lower sibling moves down to the top of the next lower tab.
     * - A tab switches places with the tab below it.
     *
     * @param moveUp True if node should move up; false if node should move down
     */
    private void moveCurrentlySelectedNode(boolean moveUp)
    {
        TreePath selectionPath = toolbarConfigTree.getSelectionPath();

        if (selectionPath != null)
        {
            boolean isExpanded = toolbarConfigTree.isExpanded(selectionPath);

            MutableTreeNode childNode = (MutableTreeNode) selectionPath.getLastPathComponent();
            MutableTreeNode parentNode = (MutableTreeNode) childNode.getParent();

            Optional<TreeNode> newParentNodeOptional = Optional.empty();

            if (childNode instanceof ToolbarComponentTreeNode componentNode)
            {
                newParentNodeOptional = findParentForComponentMove(componentNode, moveUp);
            } else if (childNode instanceof ToolbarTabTreeNode)
            {
                newParentNodeOptional = findParentForTabMove(childNode, moveUp);
            }

            // Move is only valid if a parent is returned
            if (newParentNodeOptional.isPresent())
            {
                MutableTreeNode newParentNode = (MutableTreeNode) newParentNodeOptional.get();
                int newIndex;

                // Check if the new parent node is the same as the current parent, indicating the child node should
                // move among its siblings
                if (newParentNode.equals(parentNode))
                {
                    int currentIndex = parentNode.getIndex(childNode);
                    newIndex = moveUp ? currentIndex - 1 : currentIndex + 1;
                } else
                {
                    // Otherwise, the node is moving to the top/bottom of the new parent (based on direction)
                    newIndex = moveUp ? newParentNode.getChildCount() : 0;
                }

                treeModel.removeNodeFromParent(childNode);
                treeModel.insertNodeInto(childNode, newParentNode, newIndex);

                // Update the selection to the new node location and expand the tree and set the scroll path so it is visible
                TreePath path = new TreePath(treeModel.getPathToRoot(childNode));
                toolbarConfigTree.setSelectionPath(path);
                toolbarConfigTree.scrollPathToVisible(toolbarConfigTree.getSelectionPath());

                if (isExpanded)
                {
                    toolbarConfigTree.expandPath(toolbarConfigTree.getSelectionPath());
                }
            }
        }
    }

    /**
     * Determines if the currently selected node is deletable. If not, an error message will be returned.
     *
     * @return An empty or {@code null} string indicates the node is deletable. Otherwise, the string will contain an
     * error message describing why the node cannot be deleted.
     */
    private String isSelectedNodeDeletable()
    {
        Object lastPathComponent = toolbarConfigTree.getSelectionPath().getLastPathComponent();
        String errorMessage = null;

        if (lastPathComponent instanceof ToolbarTabTreeNode tabNode)
        {
            if (tabNode.tab.getType() != TabType.USER_CREATED)
            {
                errorMessage = "Only user-created tabs can be deleted. Hide the tab instead.";
            }
        }

        return errorMessage;
    }

    /**
     * Handles a key release on the toolbar config tree.  The following key combinations are supported:
     * - DELETE: Removes the selected element from the tree, unless it is a default tab
     * - SPACE: Toggles the checkbox of the selected element (if applicable)
     * - CTRL+UP: Moves the selected node up
     * - CTRL+DOWN: Moves the selected node down
     *
     * @param e The key event
     */
    private void toolbarConfigTreeKeyReleased(KeyEvent e)
    {
        if (e.getKeyCode() == KeyEvent.VK_DELETE)
        {
            // When delete is pressed, delete the node from the tree, as long as it is deletable
            String errorMessage = isSelectedNodeDeletable();
            if (!Strings.isNullOrEmpty(errorMessage))
            {
                AlwaysOnTopJOptionPane.showMessageDialog(errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            removeSelectedNodeFromTree();
        } else if (e.getKeyCode() == KeyEvent.VK_UP)
        {
            if (e.isControlDown())
            {
                // This is a shortcut for moving a node up: Ctrl+Up
                moveCurrentlySelectedNode(true);
                e.consume();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_DOWN)
        {
            if (e.isControlDown())
            {
                // This is a shortcut for moving a node down: Ctrl+Down
                moveCurrentlySelectedNode(false);
                e.consume();
            }
        } else if (e.getKeyCode() == KeyEvent.VK_SPACE)
        {
            // If the space bar is pressed on a node with a checkbox, toggle the checkbox state and update visibility
            // for affected tab
            TreePath path = toolbarConfigTree.getSelectionPath();
            if (path.getLastPathComponent() instanceof ToolbarTabTreeNode)
            {
                updateVisibility(path);
            }
        }
    }

    /**
     * Handles a key press on the toolbar config tree.
     *
     * @param e The key event
     */
    private void toolbarConfigTreeKeyPressed(KeyEvent e)
    {
        // Consumes the event if control is pressed together with the UP or DOWN key.  This is necessary to prevent the
        // down button from having the normal action (moving the selection down) before the key release occurs, which
        // moves the selected node down.
        if (e.isControlDown() && (e.getKeyCode() == KeyEvent.VK_UP || e.getKeyCode() == KeyEvent.VK_DOWN))
        {
            e.consume();
        }
    }

    /**
     * Adds the provided buttons to the list at the indicated index and updates the selection to select the new buttons.
     *
     * @param jList     The JList to which the buttons should be added
     * @param listModel The model for the list
     * @param index     The index at which to add the buttons
     * @param buttons   The buttons to add
     */
    public void addButtonsToList(JList<ToolbarButton> jList, DefaultListModel<ToolbarButton> listModel, int index,
                                 List<ToolbarButton> buttons)
    {
        // If any buttons will be added, clear selection before moving the buttons, as each button will be added to
        // the selection as it is moved
        if (!buttons.isEmpty() && buttons.stream().anyMatch(b -> b.isValid()))
        {
            jList.clearSelection();
        }

        for (ToolbarButton button : buttons)
        {
            if (!button.isValid()) continue;

            ToolbarButton newButton = button.copy();
            listModel.insertElementAt(newButton, index++);
            int buttonIndex = listModel.indexOf(newButton);
            jList.addSelectionInterval(buttonIndex, buttonIndex);
        }

        jList.ensureIndexIsVisible(jList.getMinSelectionIndex());
    }

    /**
     * Removes the provided buttons from the list and updates the selection accordingly.
     *
     * @param buttonList The JList from which the buttons should be removed
     * @param listModel  The model for the list
     * @param buttons    The buttons to remove
     */
    public void removeButtonsFromList(JList<?> buttonList, DefaultListModel<ToolbarButton> listModel,
                                      List<ToolbarButton> buttons)
    {
        int newSelectionIndex = buttonList.getMinSelectionIndex();

        for (ToolbarButton button : buttons)
        {
            listModel.removeElement(button);
        }

        // Update selection to be the next button after the removed button(s) if it exists
        updateListSelectionAfterRemoval(newSelectionIndex, buttonList, listModel);
    }

    /**
     * Updates the selection in the provided button list to the specified index if it exists. If it does not, the
     * selection is set to the previous index.
     *
     * @param newSelectionIndex The desired selection index
     * @param buttonList        The button list for which to update the selection
     * @param buttonListModel   The button list model
     */
    public void updateListSelectionAfterRemoval(int newSelectionIndex, JList<?> buttonList, ListModel<ToolbarButton> buttonListModel)
    {
        int buttonListSize = buttonListModel.getSize();
        if (buttonListSize > 0)
        {
            if (newSelectionIndex >= buttonListSize)
            {
                newSelectionIndex--;
            }

            buttonList.setSelectedIndex(newSelectionIndex);
            buttonList.ensureIndexIsVisible(newSelectionIndex);
        }
    }

    /**
     * Handles a mouse click on the toolbar config tree. If the click was on a checkbox, the visibility is updated
     * for the affected tab.
     *
     * @param mouseEvent The mouse event
     */
    private void toolbarConfigTreeMouseClicked(MouseEvent mouseEvent)
    {
        TreePath path = toolbarConfigTree.getPathForLocation(mouseEvent.getX(), mouseEvent.getY());

        // If no node was clicked or a button node was clicked, return because there is no checkbox to update
        if (path == null || path.getLastPathComponent() instanceof ToolbarComponentTreeNode) return;

        // See if the click was inside a checkbox; if not return
        Rectangle pathBounds = toolbarConfigTree.getPathBounds(path);
        if (pathBounds == null || mouseEvent.getX() > pathBounds.x + checkboxWidth) return;

        updateVisibility(path);
    }

    /**
     * Updates the visibility of the affected tab when the state of a visibility checkbox is changed.
     *
     * @param path The tree path for the updated node
     */
    private void updateVisibility(TreePath path)
    {
        if (path.getLastPathComponent() instanceof ToolbarTabTreeNode tabNode)
        {
            ToolbarTab tab = tabNode.tab;
            tab.setVisible(!tab.isVisible(), true);
        }

        toolbarConfigTree.treeDidChange();
    }

    /**
     * Hands a rename button action. Displays the rename dialog with the currently selected node's name and updates the
     * name of the node if it is changed by the user.
     */
    private void renameButtonActionPerformed()
    {
        String originalName = null;
        IToolbarConfigurationRenameListener listener = null;
        TreeNode selectedNode = (TreeNode) toolbarConfigTree.getSelectionPath().getLastPathComponent();

        if (selectedNode instanceof ToolbarComponentTreeNode componentNode)
        {
            AToolbarComponent component = componentNode.component;
            if (component instanceof ToolbarButton button)
            {
                originalName = button.getDisplayText();
                listener = newName -> {
                    button.setDisplayText(newName);
                    treeModel.nodeChanged(selectedNode);
                };
            }
        } else if (selectedNode instanceof ToolbarTabTreeNode tabNode)
        {
            ToolbarTab tab = tabNode.tab;
            originalName = tab.getDisplayText();
            listener = newName -> {
                tab.setDisplayText(newName);
                treeModel.nodeChanged(selectedNode);
            };
        }

        if (listener != null)
        {
            ToolbarConfigurationRenameDialog renameDialog =
                    new ToolbarConfigurationRenameDialog(this, originalName, listener);
            renameDialog.setVisible(true);
        }
    }

    private void minimizeToolbarCheckBoxStateChanged()
    {
        configuration.setMinimized(minimizeToolbarCheckBox.isSelected());
    }

    private void addSeparatorActionButtonPerformed()
    {
        // Get the currently selected node
        TreePath selectionPath = toolbarConfigTree.getSelectionPath();

        if (selectionPath != null)
        {
            // Double check that the selection path contains a tab
            if (selectionPath.getPathCount() > TAB_NODE_INDEX)
            {
                ToolbarTabTreeNode tabTreeNode = (ToolbarTabTreeNode) selectionPath.getPath()[TAB_NODE_INDEX];
                int newSeparatorIndex = tabTreeNode.getAdjustedChildCount();

                // If there is a component in the selected path, the new separator will be added immediately below it
                if (selectionPath.getPathCount() > COMPONENT_NODE_INDEX)
                {
                    ToolbarComponentTreeNode selectedComponentNode = (ToolbarComponentTreeNode) selectionPath.getPath()[COMPONENT_NODE_INDEX];
                    newSeparatorIndex = tabTreeNode.getIndex(selectedComponentNode) + 1;
                }

                final ToolbarSeparator separator = new ToolbarSeparator(tabTreeNode.tab.getDisplayText(), newSeparatorIndex);
                ToolbarComponentTreeNode newSeparatorNode = new ToolbarComponentTreeNode(separator);
                treeModel.insertNodeInto(newSeparatorNode, tabTreeNode, newSeparatorIndex);

                TreeNode[] nodePath = treeModel.getPathToRoot(newSeparatorNode);

                toolbarConfigTree.setSelectionPath(new TreePath(nodePath));
                toolbarConfigTree.expandPath(toolbarConfigTree.getSelectionPath());
                if (!toolbarConfigTree.isVisible(toolbarConfigTree.getSelectionPath()))
                {
                    toolbarConfigTree.scrollPathToVisible(toolbarConfigTree.getSelectionPath());
                }
            }
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var namePanel = new JPanel();
        var nameLabel = new JLabel();
        configNameTextField = new JTextField();
        var toolbarPanel = new JPanel();
        var availableButtonsLabel = new JLabel();
        var toolbarLabel = new JLabel();
        var buttonListScrollPane = new JScrollPane();
        buttonList = new JList<>();
        buttonList.setCellRenderer(new ToolbarButtonListCellRenderer());
        buttonList.setTransferHandler(new ToolbarButtonListTransferHandler(this));
        var toolbarScrollPane = new JScrollPane();
        toolbarConfigTree = new GrowingRowTree();
        addButton = new JButton();
        moveUpButton = new JButton();
        removeButton = new JButton();
        moveDownButton = new JButton();
        var toolbarButtonsPanel = new JPanel();
        var addTabButton = new JButton();
        addSeparatorButton = new JButton();
        renameButton = new JButton();
        minimizeToolbarCheckBox = new JCheckBox();
        var buttonBar = new JPanel();
        var saveButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Toolbar Configuration");
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, fill:default:grow"));

                //======== namePanel ========
                {
                    namePanel.setName("namePanel");
                    namePanel.setLayout(new FormLayout(
                            "default, $lcgap, default:grow",
                            "default"));

                    //---- nameLabel ----
                    nameLabel.setText("Configuration Name");
                    nameLabel.setName("nameLabel");
                    namePanel.add(nameLabel, cc.xy(1, 1));

                    //---- configNameTextField ----
                    configNameTextField.setName("configNameTextField");
                    namePanel.add(configNameTextField, cc.xy(3, 1));
                }
                contentPanel.add(namePanel, cc.xy(1, 1));

                //======== toolbarPanel ========
                {
                    toolbarPanel.setName("toolbarPanel");
                    toolbarPanel.setLayout(new FormLayout(
                            "default:grow, $lcgap, default, $lcgap, default:grow, $lcgap, default",
                            "default, $lgap, fill:default:grow, 2*($lgap, default), $lgap, fill:default:grow, 2*($lgap, default)"));
                    ((FormLayout) toolbarPanel.getLayout()).setColumnGroups(new int[][]{{1, 5}});

                    //---- availableButtonsLabel ----
                    availableButtonsLabel.setText("Available Buttons:");
                    availableButtonsLabel.setName("availableButtonsLabel");
                    toolbarPanel.add(availableButtonsLabel, cc.xy(1, 1));

                    //---- toolbarLabel ----
                    toolbarLabel.setText("Toolbar:");
                    toolbarLabel.setName("toolbarLabel");
                    toolbarPanel.add(toolbarLabel, cc.xy(5, 1));

                    //======== buttonListScrollPane ========
                    {
                        buttonListScrollPane.setName("buttonListScrollPane");

                        //---- buttonList ----
                        buttonList.setDragEnabled(true);
                        buttonList.setDropMode(DropMode.INSERT);
                        buttonList.setName("buttonList");
                        buttonList.addListSelectionListener(e -> buttonListValueChanged());
                        buttonListScrollPane.setViewportView(buttonList);
                    }
                    toolbarPanel.add(buttonListScrollPane, cc.xywh(1, 3, 1, 8));

                    //======== toolbarScrollPane ========
                    {
                        toolbarScrollPane.setName("toolbarScrollPane");

                        //---- toolbarConfigTree ----
                        toolbarConfigTree.setRootVisible(false);
                        toolbarConfigTree.setDragEnabled(true);
                        toolbarConfigTree.setDropMode(DropMode.ON_OR_INSERT);
                        toolbarConfigTree.setName("toolbarConfigTree");
                        toolbarConfigTree.addTreeSelectionListener(e -> toolbarConfigTreeSelectionChanged());
                        toolbarConfigTree.addKeyListener(new KeyAdapter()
                        {
                            @Override
                            public void keyPressed(KeyEvent e)
                            {
                                toolbarConfigTreeKeyPressed(e);
                            }

                            @Override
                            public void keyReleased(KeyEvent e)
                            {
                                toolbarConfigTreeKeyReleased(e);
                            }
                        });
                        toolbarConfigTree.addMouseListener(new MouseAdapter()
                        {
                            @Override
                            public void mouseClicked(MouseEvent e)
                            {
                                toolbarConfigTreeMouseClicked(e);
                            }
                        });
                        toolbarScrollPane.setViewportView(toolbarConfigTree);
                    }
                    toolbarPanel.add(toolbarScrollPane, cc.xywh(5, 3, 1, 8));

                    //---- addButton ----
                    addButton.setText("Add >>");
                    addButton.setEnabled(false);
                    addButton.setToolTipText("Adds the selected button(s) to the selected tab");
                    addButton.setName("addButton");
                    addButton.addActionListener(e -> addButtonActionPerformed());
                    toolbarPanel.add(addButton, cc.xy(3, 5));

                    //---- moveUpButton ----
                    moveUpButton.setText("Move Up");
                    moveUpButton.setEnabled(false);
                    moveUpButton.setName("moveUpButton");
                    moveUpButton.addActionListener(e -> moveUpButtonActionPerformed());
                    toolbarPanel.add(moveUpButton, cc.xy(7, 5));

                    //---- removeButton ----
                    removeButton.setText("<< Remove");
                    removeButton.setEnabled(false);
                    removeButton.setToolTipText("Removes the selected button from the toolbar");
                    removeButton.setName("removeButton");
                    removeButton.addActionListener(e -> removeButtonActionPerformed());
                    toolbarPanel.add(removeButton, cc.xy(3, 7));

                    //---- moveDownButton ----
                    moveDownButton.setText("Move Down");
                    moveDownButton.setEnabled(false);
                    moveDownButton.setName("moveDownButton");
                    moveDownButton.addActionListener(e -> moveDownButtonActionPerformed());
                    toolbarPanel.add(moveDownButton, cc.xy(7, 7));

                    //======== toolbarButtonsPanel ========
                    {
                        toolbarButtonsPanel.setName("toolbarButtonsPanel");
                        toolbarButtonsPanel.setLayout(new FormLayout(
                                "2*(default, $lcgap), default:grow",
                                "$lgap, fill:default"));

                        //---- addTabButton ----
                        addTabButton.setText("Add Tab");
                        addTabButton.setToolTipText("Adds a new tab below the currently selected task (or at the end if no task selected)");
                        addTabButton.setName("addTabButton");
                        addTabButton.addActionListener(e -> addTabButtonActionPerformed());
                        toolbarButtonsPanel.add(addTabButton, cc.xy(1, 2));

                        //---- addSeparatorButton ----
                        addSeparatorButton.setText("Add Separator");
                        addSeparatorButton.setToolTipText("Adds a separator to the currently selected tab");
                        addSeparatorButton.setEnabled(false);
                        addSeparatorButton.setName("addSeparatorButton");
                        addSeparatorButton.addActionListener(e -> addSeparatorActionButtonPerformed());
                        toolbarButtonsPanel.add(addSeparatorButton, cc.xy(3, 2));

                        //---- renameButton ----
                        renameButton.setText("Rename...");
                        renameButton.setEnabled(false);
                        renameButton.setName("renameButton");
                        renameButton.addActionListener(e -> renameButtonActionPerformed());
                        toolbarButtonsPanel.add(renameButton, cc.xy(5, 2));
                    }
                    toolbarPanel.add(toolbarButtonsPanel, cc.xy(5, 11));

                    //---- minimizeToolbarCheckBox ----
                    minimizeToolbarCheckBox.setText("Minimize Toolbar");
                    minimizeToolbarCheckBox.setVisible(false);
                    minimizeToolbarCheckBox.setName("minimizeToolbarCheckBox");
                    minimizeToolbarCheckBox.addChangeListener(e -> minimizeToolbarCheckBoxStateChanged());
                    toolbarPanel.add(minimizeToolbarCheckBox, cc.xy(1, 13));
                }
                contentPanel.add(toolbarPanel, cc.xy(1, 3));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "2*($lcgap, default), $glue, $button, $rgap, $button",
                        "pref"));

                //---- saveButton ----
                saveButton.setText("Save");
                saveButton.setName("saveButton");
                saveButton.addActionListener(e -> saveButtonActionPerformed());
                buttonBar.add(saveButton, cc.xy(6, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(8, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField configNameTextField;
    private JList<ToolbarButton> buttonList;
    private GrowingRowTree toolbarConfigTree;
    private JButton addButton;
    private JButton moveUpButton;
    private JButton removeButton;
    private JButton moveDownButton;
    private JButton addSeparatorButton;
    private JButton renameButton;
    private JCheckBox minimizeToolbarCheckBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
