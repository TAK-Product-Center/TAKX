package gov.takx.core.view.gui.contacts.groupmanagement;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.view.gui.contacts.lists.GroupContactTreeNode;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.TreeNode;

import java.awt.Font;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Allows the user to delete a user-managed group.
 *
 * @since 3.0
 */
@PseudoBean
public class GroupDeletionPanel extends JPanel
{
    @Inject
    private GroupDeletionPanelController controller;

    public GroupDeletionPanel()
    {
        initComponents();
    }

    /**
     * @return The tree displayed by this panel
     * @since 3.5
     */
    public JTree getTree()
    {
        return groupTree;
    }

    /**
     * @param enabled Pass {@code true} if the "Okay" button should be enabled,{@code false} if it should be disabled.
     * @since 3.5
     */
    public void setOkayButtonEnabled(boolean enabled)
    {
        okButton.setEnabled(enabled);
    }

    private void okButtonActionPerformed()
    {
        final GroupContactTreeNode rootNode = (GroupContactTreeNode) groupTree.getModel().getRoot();
        List<IGroupContact> groupsToDelete = new ArrayList<>();

        for (Enumeration<TreeNode> enumeration = rootNode.breadthFirstEnumeration(); enumeration.hasMoreElements();)
        {
            GroupContactTreeNode currentNode = (GroupContactTreeNode) enumeration.nextElement();
            if (currentNode.isSelected()) groupsToDelete.add(currentNode.getContact());
        }

        final int choice = JOptionPane.showConfirmDialog(JFrame.getFrames()[0],
                "Are you sure you want to remove the following groups?\n\n" +
                        groupsToDelete.stream().map(IContact::getName).collect(Collectors.joining(", ")),
                "Remove Groups", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION) controller.deleteGroups(groupsToDelete);
    }

    private void cancelButtonActionPerformed()
    {
        controller.cancel();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var titleLabel = new JLabel();
        var groupTreeScrollPane = new JScrollPane();
        groupTree = new JTree();
        var buttonPanel = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("groupDeletionPanel");
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new FormLayout(
                "default:grow",
                "default, $lgap, fill:default:grow, $lgap, default"));

        //---- titleLabel ----
        titleLabel.setText("Delete Groups");
        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | Font.BOLD, titleLabel.getFont().getSize() + 1f));
        add(titleLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //======== groupTreeScrollPane ========
        {
            groupTreeScrollPane.setOpaque(false);

            //---- groupTree ----
            groupTree.setRootVisible(false);
            groupTreeScrollPane.setViewportView(groupTree);
        }
        add(groupTreeScrollPane, cc.xy(1, 3));

        //======== buttonPanel ========
        {
            buttonPanel.setLayout(new FormLayout(
                    "default:grow, 2*($lcgap, default)",
                    "default"));

            //---- okButton ----
            okButton.setText("OK");
            okButton.setEnabled(false);
            okButton.addActionListener(e -> okButtonActionPerformed());
            buttonPanel.add(okButton, cc.xy(3, 1));

            //---- cancelButton ----
            cancelButton.setText("Cancel");
            cancelButton.addActionListener(e -> cancelButtonActionPerformed());
            buttonPanel.add(cancelButton, cc.xy(5, 1));
        }
        add(buttonPanel, cc.xy(1, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTree groupTree;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
