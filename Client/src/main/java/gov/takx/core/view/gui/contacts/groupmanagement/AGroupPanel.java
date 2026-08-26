package gov.takx.core.view.gui.contacts.groupmanagement;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.contact.IContact;
import gov.takx.core.view.gui.contacts.lists.ImprovedSelectionJList;
import gov.takx.core.view.gui.contacts.lists.IndividualContactCheckListItem;
import jakarta.annotation.Nonnull;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.border.*;

import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * Base class for group management panels that shows all the contacts that can be added or removed from a given group.
 *
 * @since 3.0
 */
public abstract class AGroupPanel extends JPanel
{
    private AGroupPanelController controller;

    protected AGroupPanel(String title)
    {
        initComponents();
        titleLabel.setText(title);
    }

    /**
     * @param controller the controller that will respond to user actions
     * @since 3.5
     */
    public void setController(@Nonnull AGroupPanelController controller)
    {
        this.controller = controller;
    }

    /**
     * @param name Sets the name of the group that is being edited
     * @since 3.5
     */
    public void setGroupName(@Nonnull String name)
    {
        groupNameTextField.setText(name);
    }

    /**
     * @return The contact list
     * @since 3.5
     */
    @Nonnull
    public JList<IndividualContactCheckListItem> getContactList()
    {
        return contactList;
    }

    /**
     * Called when the "OK" button is clicked by the user.
     */
    private void okButtonActionPerformed()
    {
        selectAllCheckBox.setSelected(false);
        controller.contactsSelected(groupNameTextField.getText(), getSelectedGroupMembers());
    }

    /**
     * Called when the "Cancel" button is clicked by the user.
     */
    private void cancelButtonActionPerformed()
    {
        controller.cancel();
    }

    /**
     * Gets the group members that are currently selected (i.e. their checkboxes are set to "selected").
     *
     * @return The currently selected group members
     */
    @Nonnull
    protected Set<IContact> getSelectedGroupMembers()
    {
        final Set<IContact> groupMembers = new HashSet<>();
        final var iterator =
                ((DefaultListModel<IndividualContactCheckListItem>) contactList.getModel()).elements().asIterator();

        while (iterator.hasNext())
        {
            final IndividualContactCheckListItem item = iterator.next();
            if (item.isSelected()) groupMembers.add(item.getContact());
        }

        return groupMembers;
    }

    /**
     * Called when the select all checkbox is clicked by the user. Based on the selection, all group members are either
     * selected or deselected.
     */
    private void selectAllCheckBoxActionPerformed()
    {
        final var contactListModel = (DefaultListModel<IndividualContactCheckListItem>) contactList.getModel();
        final boolean selectAll = selectAllCheckBox.isSelected();

        if (selectAll)
        {
            contactList.setSelectionInterval(0, contactListModel.size());
        } else
        {
            contactList.clearSelection();
        }

        for (int i = 0; i < contactListModel.size(); i++)
        {
            contactListModel.getElementAt(i).setSelected(selectAll);
        }
    }

    /**
     * Enables the "OK" button if the group name is non-empty or disables it and
     * sets an appropriate tooltip if the criteria is not met.
     *
     * @since 3.4.1
     */
    private void updateOkButton()
    {
        final boolean hasGroupName = !groupNameTextField.getText().isBlank();

        okButton.setEnabled(hasGroupName);
        okButton.setToolTipText(hasGroupName ? "" : "Set name");
    }

    /**
     * Disable the "OK" button if the group name field is empty.
     *
     * @since 3.3
     */
    private void groupNameTextFieldKeyReleased()
    {
        updateOkButton();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        titleLabel = new JLabel();
        var groupNameLabel = new JLabel();
        groupNameTextField = new JTextField();
        selectAllCheckBox = new JCheckBox();
        var contactListScrollPane = new JScrollPane();
        contactList = new ImprovedSelectionJList<>();
        var buttonPanel = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(5, 5, 5, 5));
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "2*(default, $lgap), pref, $rgap, fill:default:grow, $lgap, fill:pref"));

        //---- titleLabel ----
        titleLabel.setText("Title");
        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | Font.BOLD, titleLabel.getFont().getSize() + 1f));
        add(titleLabel, cc.xywh(1, 1, 3, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //---- groupNameLabel ----
        groupNameLabel.setText("Name:");
        add(groupNameLabel, cc.xy(1, 3));

        //---- groupNameTextField ----
        groupNameTextField.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyReleased(KeyEvent e)
            {
                groupNameTextFieldKeyReleased();
            }
        });
        add(groupNameTextField, cc.xy(3, 3));

        //---- selectAllCheckBox ----
        selectAllCheckBox.setText("Select All");
        selectAllCheckBox.addActionListener(e -> selectAllCheckBoxActionPerformed());
        add(selectAllCheckBox, cc.xywh(1, 5, 3, 1));

        //======== contactListScrollPane ========
        {
            contactListScrollPane.setViewportView(contactList);
        }
        add(contactListScrollPane, cc.xywh(1, 7, 3, 1));

        //======== buttonPanel ========
        {
            buttonPanel.setLayout(new FormLayout(
                    "default:grow, 2*($lcgap, default)",
                    "default"));

            //---- okButton ----
            okButton.setText("OK");
            okButton.addActionListener(e -> okButtonActionPerformed());
            buttonPanel.add(okButton, cc.xy(3, 1));

            //---- cancelButton ----
            cancelButton.setText("Cancel");
            cancelButton.addActionListener(e -> cancelButtonActionPerformed());
            buttonPanel.add(cancelButton, cc.xy(5, 1));
        }
        add(buttonPanel, cc.xywh(1, 9, 3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel titleLabel;
    protected JTextField groupNameTextField;
    JCheckBox selectAllCheckBox;
    private ImprovedSelectionJList<IndividualContactCheckListItem> contactList;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
