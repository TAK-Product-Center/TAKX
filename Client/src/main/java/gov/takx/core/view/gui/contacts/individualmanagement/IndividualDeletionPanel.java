package gov.takx.core.view.gui.contacts.individualmanagement;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.view.gui.contacts.lists.ImprovedSelectionJList;
import gov.takx.core.view.gui.contacts.lists.IndividualContactCheckListItem;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import java.awt.Font;

/**
 * Displays a list of users that can be selected for deletion.
 *
 * @since 5.1
 */
@PseudoBean
public class IndividualDeletionPanel extends JPanel
{
    @Inject
    private IndividualDeletionPanelController controller;

    public IndividualDeletionPanel()
    {
        initComponents();
    }

    /**
     * @return The user list
     */
    @Nonnull
    public JList<IndividualContactCheckListItem> getList()
    {
        return contactList;
    }

    public void setOkayButtonEnabled(boolean enabled)
    {
        okButton.setEnabled(enabled);
    }

    private void okButtonActionPerformed()
    {
        controller.deleteSelectedContacts();
    }

    private void cancelButtonActionPerformed()
    {
        controller.close();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var titleLabel = new JLabel();
        var scrollPane1 = new JScrollPane();
        contactList = new ImprovedSelectionJList<>();
        var buttonPanel = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("individualDeletionPanel");
        setLayout(new FormLayout(
                "default:grow",
                "2*(default, $lgap), default"));

        //---- titleLabel ----
        titleLabel.setText("Remove Contacts");
        titleLabel.setFont(titleLabel.getFont().deriveFont(titleLabel.getFont().getStyle() | Font.BOLD, titleLabel.getFont().getSize() + 1f));
        titleLabel.setName("titleLabel");
        add(titleLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

        //======== scrollPane1 ========
        {
            scrollPane1.setName("scrollPane1");

            //---- contactList ----
            contactList.setName("contactList");
            scrollPane1.setViewportView(contactList);
        }
        add(scrollPane1, cc.xy(1, 3));

        //======== buttonPanel ========
        {
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "default:grow, 2*($lcgap, default)",
                    "default"));

            //---- okButton ----
            okButton.setText("OK");
            okButton.setEnabled(false);
            okButton.setName("okButton");
            okButton.addActionListener(e -> okButtonActionPerformed());
            buttonPanel.add(okButton, cc.xy(3, 1));

            //---- cancelButton ----
            cancelButton.setText("Cancel");
            cancelButton.setName("cancelButton");
            cancelButton.addActionListener(e -> cancelButtonActionPerformed());
            buttonPanel.add(cancelButton, cc.xy(5, 1));
        }
        add(buttonPanel, cc.xy(1, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private ImprovedSelectionJList<IndividualContactCheckListItem> contactList;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
