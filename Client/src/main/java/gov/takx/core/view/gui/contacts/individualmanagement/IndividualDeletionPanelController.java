package gov.takx.core.view.gui.contacts.individualmanagement;

import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IIndividualContact;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.lists.IndividualContactCheckBoxCellRenderer;
import gov.takx.core.view.gui.contacts.lists.IndividualContactCheckListItem;
import gov.takx.core.view.gui.contacts.lists.IndividualContactListController;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.swing.JFrame;
import javax.swing.JOptionPane;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages the {@link IndividualDeletionPanel}'s data and backing logic.
 */
@ApplicationScoped
public class IndividualDeletionPanelController
{
    private final ContactsPanelController contactsPanelController;
    private final IContactService contactService;
    private IndividualContactListController<IndividualContactCheckListItem> listController;
    private IndividualDeletionPanel panel;

    @Inject
    public IndividualDeletionPanelController(ContactsPanelController contactsPanelController, IContactService contactService)
    {
        this.contactsPanelController = contactsPanelController;
        this.contactService = contactService;
    }

    /**
     * @param panel The panel that this controller will manage.
     */
    public void setPanel(@Nonnull IndividualDeletionPanel panel)
    {
        this.panel = panel;
        panel.getList().setCellRenderer(new IndividualContactCheckBoxCellRenderer());
    }

    /**
     * Prepare to display the {@link IndividualDeletionPanel} by creating a new {@link IndividualContactListController}.
     * This should be called each time the panel is displayed.
     */
    public void prepare()
    {
        listController =
                new IndividualContactListController<>(panel.getList(), IndividualContactCheckListItem::new,
                        IndividualContactCheckListItem::getContact,
                        item -> {
                            item.setSelected(!item.isSelected());
                            panel.setOkayButtonEnabled(isAnyRecipientSelected());
                        });
    }

    /**
     * Deletes the contacts selected by the user.
     */
    public void deleteSelectedContacts()
    {
        final List<IIndividualContact> contactsToDelete = new ArrayList<>();

        listController.getModel().elements().asIterator().forEachRemaining(item -> {
            if (item.isSelected()) contactsToDelete.add(item.getContact());
        });

        final int choice = JOptionPane.showConfirmDialog(JFrame.getFrames()[0],
                "Are you sure you want to remove the following contact(s)? Note: The contact(s) may be " +
                        "restored if new contact data is received.\n\n" +
                        contactsToDelete.stream().map(IContact::getName).collect(Collectors.joining(", ")),
                "Remove Contacts", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (choice == JOptionPane.YES_OPTION)
        {
            contactsToDelete.forEach(contact -> contactService.removeContact(contact.getUid()));
            close();
        }
    }

    /**
     * Shuts down the {@link IndividualContactListController}, resets the UI and switches the view back to the primary
     * contacts panel.
     */
    public void close()
    {
        listController.releaseResources();
        listController = null;

        panel.setOkayButtonEnabled(false);

        contactsPanelController.showPrimaryContactsPanel();
    }

    /**
     * Checks the {@link IIndividualContact} list to see if any contacts are selected.
     *
     * @return boolean indicating if any contacts are currently selected
     */
    private boolean isAnyRecipientSelected()
    {
        final var entries = listController.getModel().elements().asIterator();
        while (entries.hasNext()) if (entries.next().isSelected()) return true;

        return false;
    }
}
