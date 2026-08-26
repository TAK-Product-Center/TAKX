package gov.takx.core.view.gui.contacts.groupmanagement;

import com.google.errorprone.annotations.OverridingMethodsMustInvokeSuper;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.lists.IndividualContactCheckBoxCellRenderer;
import gov.takx.core.view.gui.contacts.lists.IndividualContactCheckListItem;
import gov.takx.core.view.gui.contacts.lists.IndividualContactListController;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.JList;

import java.util.Set;

/**
 * Base class for group management panel controllers to help facilitate adding/removing contacts from a user-managed
 * group contact.
 *
 * @since 3.0
 */
public abstract class AGroupPanelController
{
    protected final ContactsPanelController contactsPanelController;
    protected final GroupContactManager groupContactManager;

    protected IGroupContact groupContact;
    protected AGroupPanel groupPanel;
    protected IndividualContactListController<IndividualContactCheckListItem> contactsListController;

    protected AGroupPanelController(@Nonnull ContactsPanelController contactsPanelController,
                                    @Nonnull GroupContactManager groupContactManager)
    {
        this.contactsPanelController = contactsPanelController;
        this.groupContactManager = groupContactManager;
    }

    /**
     * @param groupPanel The group panel to set on this controller
     */
    public void setPanel(@Nonnull AGroupPanel groupPanel)
    {
        this.groupPanel = groupPanel;
        groupPanel.getContactList().setCellRenderer(new IndividualContactCheckBoxCellRenderer());
    }

    /**
     * Sets the {@link IGroupContact} (if editing) and initializes the {@link IndividualContactListController}.
     * 
     * @param groupContact The group contact associated with the controller's operation, e.g. the group being edited or
     *                     the parent of the subgroup being created. May be {@code null} if creating a new top-level user
     *                     group.
     * @since 3.5 (moved from implementors)
     */
    @OverridingMethodsMustInvokeSuper
    public void prepare(@Nullable IGroupContact groupContact)
    {
        this.groupContact = groupContact;

        final JList<IndividualContactCheckListItem> list = groupPanel.getContactList();

        contactsListController = new IndividualContactListController<>(list,
                IndividualContactCheckListItem::new, IndividualContactCheckListItem::getContact, item -> {
                    item.setSelected(!item.isSelected());
                    list.repaint();
                });
    }

    /**
     * Notifies subclasses that contacts have been selected via {@link #onContactsSelected(String, Set)} and releases
     * the {@link IndividualContactListController} resources.
     *
     * @param groupName The group name
     * @param contacts The selected contacts
     * @since 3.5
     */
    public void contactsSelected(@Nonnull String groupName, @Nonnull Set<IContact> contacts)
    {
        onContactsSelected(groupName, contacts);
        contactsListController.releaseResources();
    }

    /**
     * Called when contact selection is complete.
     *
     * @param groupName The group name
     * @param contacts The selected contacts
     * @since 3.5
     */
    abstract void onContactsSelected(@Nonnull String groupName, @Nonnull Set<IContact> contacts);

    /**
     * Called when the user clicks "Cancel" on the group panel
     */
    public void cancel()
    {
        if (groupContact == null)
        {
            contactsPanelController.showPrimaryContactsPanel();
        } else
        {
            contactsPanelController.onSelect(groupContact);
        }

        contactsListController.releaseResources();
    }
}
