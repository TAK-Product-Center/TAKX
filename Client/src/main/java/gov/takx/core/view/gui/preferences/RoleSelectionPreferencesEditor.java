package gov.takx.core.view.gui.preferences;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.cot.detail.Role;
import gov.tak.api.maps.Marker;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.core.comms.roles.RolesManager;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.commons.swing.AlwaysOnTopFileChooser;
import gov.takx.commons.swing.AlwaysOnTopFileChooserFactory;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

import static gov.takx.platform.preferences.PreferenceEntry.SA_REPORTING_ROLE;

/**
 * Preference selection for Roles.
 * @since 5.6
 */
@PseudoBean
public class RoleSelectionPreferencesEditor extends JPanel implements IEditorComponent
{
    @Inject
    private RolesManager rolesManager;

    @Inject
    private IMapObjectManager mapObjectManager;

    @PostConstruct
    private void postConstruct()
    {
        initComponents();
        initializeTree();
    }

    /**
     * Setup and initialize the Role tree.
     */
    @SuppressWarnings("unchecked")
    private void initializeTree()
    {
        Role selfRole = rolesManager.getRole((Marker) mapObjectManager.getSelfMarker());
        rolesTree.setShowsRootHandles(true);
        rolesTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        rolesTree.setCellRenderer(new RoleTreeCellRenderer());

        var rootNode = new DefaultMutableTreeNode("ROOT");
        var searchText = searchField.getText().toLowerCase(Locale.ROOT);
        buildTree(rootNode, rolesManager.getRoles(), searchText);
        rolesTree.setModel(new DefaultTreeModel(rootNode));

        TreeNode[] pathToRole;
        var e = (Enumeration<DefaultMutableTreeNode>) (Object) rootNode.depthFirstEnumeration();
        while (e.hasMoreElements())
        {
            DefaultMutableTreeNode node = e.nextElement();
            if (!StringUtils.isBlank(searchText)) rolesTree.expandPath(new TreePath(node.getPath()));
            if (node.getUserObject() instanceof Role r && r == selfRole)
            {
                pathToRole = ((DefaultTreeModel) rolesTree.getModel()).getPathToRoot(node);
                rolesTree.setSelectionPath(pathToRole != null ? new TreePath(pathToRole) : null);
            }
        }
    }

    /**
     * Recursively build the {@link Role Roles} tree starting at the root.
     *
     * @param root       The node to build the tree under
     * @param roles      The roles to add to the given root node
     * @param searchText the text to search for
     */
    private boolean buildTree(@Nonnull DefaultMutableTreeNode root, @Nonnull List<Role> roles, @Nonnull String searchText)
    {
        boolean shouldShow = false;
        for (Role r : roles)
        {
            var child = new DefaultMutableTreeNode(r);
            List<Role> children = r.getChildren();
            boolean ancestorSearch = false;
            for (TreeNode treeNode : root.getPath())
            {
                if (treeNode instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof Role role &&
                        role.getName().toLowerCase().contains(searchText))
                {
                    ancestorSearch = true;
                    break;
                }
            }
            if (buildTree(child, children, searchText) || ancestorSearch || r.getName().toLowerCase().contains(searchText))
            {
                root.add(child);
                shouldShow = true;
            }
        }
        return shouldShow;
    }

    /**
     * Forces a reload of custom roles + updates the tree/UI. Enables users to drop files into the relevant folder or
     * import via the UI and see them in the tree.
     */
    private void reloadRolesClicked()
    {
        rolesManager.reloadRoles();
        initializeTree();
    }

    /**
     * Handle file selection and has the role manager import the data.
     * <p/>
     * Can handle multi-file imports.
     */
    private void importJsonClicked()
    {
        AlwaysOnTopFileChooser fileChooser = AlwaysOnTopFileChooserFactory.getFileChooser(this);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("JSON File", "json"));
        if (fileChooser.showOpenDialog(JFrame.getFrames()[0]) != JFileChooser.APPROVE_OPTION) return;
        rolesManager.importRoles(fileChooser.getSelectedFiles());
        reloadRolesClicked();
    }

    /**
     * Search text field action handler. Fired on key releases to keep the tree up-to-date.
     */
    private void searchFieldUpdated()
    {
        initializeTree();
    }

    @Override
    public void updateFromPreference(@Nonnull IPreferenceEntry entry)
    {
        // no-op
    }

    @Override
    public void applyToPreference(@Nonnull IPreferenceEntry entry)
    {
        var node = (DefaultMutableTreeNode) rolesTree.getLastSelectedPathComponent();
        if (node != null && node.isLeaf() && node.getUserObject() instanceof Role role)
        {
            SA_REPORTING_ROLE.putAndSave(role.getName());
            var attributeSet = mapObjectManager.getSelfMarker().getAttributeSet();
            if (rolesManager.isDefault(role))
            {
                attributeSet.setAttribute(StandardAttributeNames.ATAK_ROLE_TYPE, role.getName());
                attributeSet.removeAttribute(StandardAttributeNames.CUSTOM_ROLE);
                attributeSet.removeAttribute(StandardAttributeNames.CUSTOM_ROLE_ABBREVIATION);
                attributeSet.removeAttribute(StandardAttributeNames.USE_COT_TYPE_ICON);
            } else
            {
                attributeSet.setAttribute(StandardAttributeNames.ATAK_ROLE_TYPE, role.getFallback());
                attributeSet.setAttribute(StandardAttributeNames.CUSTOM_ROLE, role.getName());
                attributeSet.setAttribute(StandardAttributeNames.CUSTOM_ROLE_ABBREVIATION, role.getAbbreviation());
                attributeSet.setAttribute(StandardAttributeNames.USE_COT_TYPE_ICON, role.useTypeIcon());
            }
        }
    }

    /**
     * A renderer specifically for a tree containing {@link Role Roles}.
     */
    private static class RoleTreeCellRenderer extends DefaultTreeCellRenderer
    {
        private static final JLabel NULL_LABEL;

        static
        {
            NULL_LABEL = new JLabel();
            NULL_LABEL.setVisible(false);
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus)
        {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
            if (node.getUserObject() instanceof Role role)
            {
                JLabel label = (JLabel) super.getTreeCellRendererComponent(tree, value, selected, true, leaf, row, hasFocus);
                label.setText(role.getName());
                return label;
            }
            return NULL_LABEL;
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var rootPanel = new JPanel();
        var searchPanel = new JPanel();
        var searchLabel = new JLabel();
        searchField = new JTextField();
        var rolesTreeScrollPaneWrapper = new JScrollPane();
        rolesTree = new JTree();
        var buttonPanel = new JPanel();
        var jsonImportButton = new JButton();
        var reloadRolesButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setMinimumSize(new Dimension(260, 290));
        setPreferredSize(new Dimension(260, 290));
        setName("this");
        setLayout(new FormLayout(
                "$lcgap, default:grow, $lcgap",
                "$lgap, top:default, $lgap"));

        //======== rootPanel ========
        {
            rootPanel.setMinimumSize(new Dimension(250, 0));
            rootPanel.setPreferredSize(new Dimension(250, 285));
            rootPanel.setName("rootPanel");
            rootPanel.setLayout(new FormLayout(
                    "default:grow",
                    "2*(top:default, $lgap), top:default"));

            //======== searchPanel ========
            {
                searchPanel.setName("searchPanel");
                searchPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "default"));

                //---- searchLabel ----
                searchLabel.setText("Search:");
                searchLabel.setName("searchLabel");
                searchPanel.add(searchLabel, cc.xy(1, 1));

                //---- searchField ----
                searchField.setName("searchField");
                searchField.addKeyListener(new KeyAdapter()
                {
                    @Override
                    public void keyReleased(KeyEvent e)
                    {
                        searchFieldUpdated();
                    }
                });
                searchPanel.add(searchField, cc.xy(3, 1));
            }
            rootPanel.add(searchPanel, cc.xy(1, 1));

            //======== rolesTreeScrollPaneWrapper ========
            {
                rolesTreeScrollPaneWrapper.setMinimumSize(new Dimension(150, 200));
                rolesTreeScrollPaneWrapper.setPreferredSize(new Dimension(108, 200));
                rolesTreeScrollPaneWrapper.setName("rolesTreeScrollPaneWrapper");

                //---- rolesTree ----
                rolesTree.setRootVisible(false);
                rolesTree.setName("rolesTree");
                rolesTreeScrollPaneWrapper.setViewportView(rolesTree);
            }
            rootPanel.add(rolesTreeScrollPaneWrapper, cc.xy(1, 3, CellConstraints.DEFAULT, CellConstraints.FILL));

            //======== buttonPanel ========
            {
                buttonPanel.setName("buttonPanel");
                buttonPanel.setLayout(new FormLayout(
                        "default:grow, $lcgap, default:grow",
                        "top:default"));

                //---- jsonImportButton ----
                jsonImportButton.setText("Import Roles");
                jsonImportButton.setName("jsonImportButton");
                jsonImportButton.addActionListener(e -> importJsonClicked());
                buttonPanel.add(jsonImportButton, cc.xy(1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                //---- reloadRolesButton ----
                reloadRolesButton.setText("Reload Roles");
                reloadRolesButton.setName("reloadRolesButton");
                reloadRolesButton.addActionListener(e -> reloadRolesClicked());
                buttonPanel.add(reloadRolesButton, cc.xy(3, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
            }
            rootPanel.add(buttonPanel, cc.xy(1, 5));
        }
        add(rootPanel, cc.xy(2, 2));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JTextField searchField;
    private JTree rolesTree;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
