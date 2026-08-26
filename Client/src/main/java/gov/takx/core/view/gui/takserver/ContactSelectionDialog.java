package gov.takx.core.view.gui.takserver;

import com.atakmap.commoncommo.Contact;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.RowSpec;
import gov.tak.api.comms.commo.IContactManager;
import gov.tak.api.contact.IContactNameResolver;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A dialog for selecting a set of {@link Contact TAK Contacts} from what
 * is {@link IContactManager#getAvailableContacts() currently available}.
 *
 * @since 5.0
 */
@PseudoBean
public class ContactSelectionDialog extends AStandardDialog
{
    private static final RowSpec ROW_SPEC = RowSpec.decode("fill:pref");

    private final List<ContactEntryPanel> contactEntries = new ArrayList<>();
    private final Consumer<List<Contact>> contactsHandler;
    private final Predicate<Contact> contactBlacklist;

    @Inject
    private IContactManager contactManager;

    @Inject
    private IContactNameResolver contactNameResolver;

    /**
     * @param contactsConsumer Consumer which is fed a list of selected contacts.
     */
    public ContactSelectionDialog(@Nonnull Consumer<List<Contact>> contactsConsumer)
    {
        this(contactsConsumer, contact -> false);
    }

    /**
     * @param contactsConsumer Consumer which is fed a list of selected contacts.
     * @param contactBlacklist Predicate to hide certain contacts from the list.
     */
    public ContactSelectionDialog(@Nonnull Consumer<List<Contact>> contactsConsumer,
                                  @Nonnull Predicate<Contact> contactBlacklist)
    {
        this.contactsHandler = contactsConsumer;
        this.contactBlacklist = contactBlacklist;
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        fillContactsList();
    }

    /**
     * Populates the contacts list with currently available TAK contacts.
     */
    @InvokeOnEDT
    private void fillContactsList()
    {
        // TODO: Look into de-duplication of rendering/layout
        //  - Possibly replace with JTree/GroupContactCheckboxCellRenderer combo?
        CellConstraints cc = new CellConstraints();
        FormLayout layout = (FormLayout) contactsPanel.getLayout();
        for (Contact contact : contactManager.getAvailableContacts())
        {
            if (contactBlacklist.test(contact)) continue;

            String callsign = contactNameResolver.resolveNameOrElse(contact.contactUID, contact.contactUID);
            ContactEntryPanel contactPanel = new ContactEntryPanel(contact, callsign);
            layout.appendRow(ROW_SPEC);
            contactsPanel.add(contactPanel, cc.xy(1, contactsPanel.getComponentCount() + 1));
            contactEntries.add(contactPanel);
        }
        contactsPanel.invalidate();
        contactsPanel.repaint();
    }

    /**
     * Closes the dialog and passes selected contacts to the handler.
     */
    private void onOk()
    {
        List<Contact> selectedContacts = contactEntries.stream()
                .filter(ContactEntryPanel::isSelected)
                .map(ContactEntryPanel::getContact)
                .collect(Collectors.toList());
        contactsHandler.accept(selectedContacts);
        dispose();
    }

    /**
     * Closes the dialog with no action.
     */
    private void onCancel()
    {
        dispose();
    }

    /**
     * Wrapper for a {@link Contact} and a {@link JCheckBox} for selection.
     */
    private static class ContactEntryPanel extends JPanel
    {
        private final Contact contact;
        private final JCheckBox check;

        private ContactEntryPanel(@Nonnull Contact contact, @Nonnull String callsign)
        {
            this.contact = contact;
            check = new JCheckBox(callsign);

            setLayout(new BorderLayout());
            add(check, BorderLayout.CENTER);
        }

        @Nonnull
        private Contact getContact()
        {
            return contact;
        }

        private boolean isSelected()
        {
            return check.isSelected();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var scrollWrapper = new JScrollPane();
        contactsPanel = new JPanel();
        var buttonBar = new JPanel();
        okButton = new JButton();
        cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Select Contacts");
        setMinimumSize(new Dimension(250, 300));
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(new EmptyBorder(12, 12, 12, 12));
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //======== scrollWrapper ========
                {
                    scrollWrapper.setName("scrollWrapper");

                    //======== contactsPanel ========
                    {
                        contactsPanel.setName("contactsPanel");
                        contactsPanel.setLayout(new FormLayout(
                                "default:grow",
                                ""));
                    }
                    scrollWrapper.setViewportView(contactsPanel);
                }
                contentPanel.add(scrollWrapper, cc.xy(1, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(new EmptyBorder(12, 0, 0, 0));
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new GridBagLayout());
                ((GridBagLayout) buttonBar.getLayout()).columnWidths = new int[]{0, 85, 80};
                ((GridBagLayout) buttonBar.getLayout()).columnWeights = new double[]{1.0, 0.0, 0.0};

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> onOk());
                buttonBar.add(okButton, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 5), 0, 0));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> onCancel());
                buttonBar.add(cancelButton, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0,
                        GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                        new Insets(0, 0, 0, 0), 0, 0));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel contactsPanel;
    private JButton okButton;
    private JButton cancelButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
