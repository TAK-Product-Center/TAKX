package gov.takx.core.view.gui.contacts.groupmanagement;

import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.lists.IndividualContactCheckListItem;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultListModel;

import java.lang.invoke.MethodHandles;
import java.util.Set;

/**
 * Controller for the group edit panel.
 *
 * @since 3.0
 */
@ApplicationScoped
public class GroupEditPanelController extends AGroupPanelController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    public GroupEditPanelController(ContactsPanelController contactsPanelController,
                                    GroupContactManager groupContactManager)
    {
        super(contactsPanelController, groupContactManager);
    }

    @Override
    public void prepare(@Nullable IGroupContact groupContact)
    {
        if (groupContact == null)
        {
            logger.error("groupContact should not be null when editing a group");
            return;
        }

        super.prepare(groupContact);

        groupPanel.setGroupName(groupContact.getName());

        final Set<IContact> groupMembers = groupContact.getGroupMembers();
        final DefaultListModel<IndividualContactCheckListItem> contactListModel = contactsListController.getModel();

        // Select any contacts that are already part of the group
        for (int i = 0; i < contactListModel.getSize(); i++)
        {
            final IndividualContactCheckListItem checkListItem = contactListModel.getElementAt(i);
            final String uid = checkListItem.getContact().getUid();

            // groupMembers are commo contacts so compare by UID
            if (groupMembers.stream().anyMatch(groupMember -> groupMember.getUid().equals(uid)))
            {
                checkListItem.setSelected(true);
            }
        }
    }

    /**
     * Informs the {@link GroupContactManager} that a contact has been updated. If the group members have not changed,
     * then no action is taken.
     * {@inheritDoc}
     */
    @Override
    void onContactsSelected(@Nonnull String groupName, @Nonnull Set<IContact> contacts)
    {
        if (groupContact == null)
        {
            logger.warn("Group contact has not been set on GroupEditPanelController, unable to update group.");
            return;
        }

        // Avoid updating if the group members were not changed.
        if (groupContact.getGroupMembers().equals(contacts))
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("Attempted to update group contact \"{}\" with no changes. Skipping update.",
                        groupContact.getName());
            }

            return;
        }

        contactsPanelController.onSelect(groupContact);
        groupContactManager.updateGroup(groupContact, contacts, null);
    }
}
