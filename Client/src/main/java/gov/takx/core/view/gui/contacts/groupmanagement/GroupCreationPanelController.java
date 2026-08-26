package gov.takx.core.view.gui.contacts.groupmanagement;

import gov.tak.api.contact.IContact;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;

/**
 * Controller for the group creation panel.
 *
 * @since 3.0
 */
@ApplicationScoped
public class GroupCreationPanelController extends AGroupPanelController
{
    @Inject
    public GroupCreationPanelController(ContactsPanelController contactsPanelController,
                                        GroupContactManager groupContactManager)
    {
        super(contactsPanelController, groupContactManager);
    }

    /**
     * Informs the {@link GroupContactManager} that a new group contact should be created.
     * {@inheritDoc}
     */
    @Override
    void onContactsSelected(@Nonnull String groupName, @Nonnull Set<IContact> contacts)
    {
        contactsPanelController.showPrimaryContactsPanel();
        groupContactManager.createGroup(groupName, contacts, groupContact);
    }
}
