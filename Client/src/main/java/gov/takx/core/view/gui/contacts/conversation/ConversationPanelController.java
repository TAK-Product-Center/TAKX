package gov.takx.core.view.gui.contacts.conversation;

import gov.tak.api.chat.IChatMessage;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IGroupContactManagerListener;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IIndividualContactManagerListener;
import gov.tak.api.contact.IndividualContactState;
import gov.tak.api.contact.IndividualContactUtils;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.ui.ComponentPosition;
import gov.takx.api.ui.IUISpaceManager;
import gov.takx.core.chat.ChatLink;
import gov.takx.core.chat.ChatMessageUtils;
import gov.takx.core.chat.Conversation;
import gov.takx.core.chat.ConversationStatusChanged;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import gov.takx.core.view.gui.contacts.datapackage.ChatDataPackageDialog;
import gov.takx.core.view.gui.contacts.datapackage.ChatLinksPanel;
import gov.takx.core.view.gui.contacts.datapackage.ChatLinksPanelController;
import gov.takx.platform.ui.ComponentDescriptor;
import gov.takx.platform.ui.ComponentDescriptorBuilder;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JPanel;

import java.lang.invoke.MethodHandles;
import java.util.List;

import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;

/**
 * Controller for the {@link ConversationPanel}. Responsible for populating the panel with a new conversation, adding
 * new messages, and relaying message-send requests (among other things) to the {@link ContactsPanelController}.
 *
 * @since 3.0
 */
@ApplicationScoped
public class ConversationPanelController
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ComponentDescriptor COMPONENT_DESCRIPTOR =
            new ComponentDescriptorBuilder(ChatLinksPanel.class, "Chat Data Package")
                    .setComponentPosition(ComponentPosition.RIGHT)
                    .setTooltipText("Displays chat Data Package contents")
                    .setIcon(UIResources.getUrl(TAK_ASSETS_PNG, "Mission-Package_32x32.png"))
                    .build();
    private final ContactsPanelController contactsPanelController;
    private final IndividualContactManager individualContactManager;
    private final GroupContactManager groupContactManager;
    private final IUISpaceManager uiSpaceManager;
    private final ChatLinksPanelController chatLinksPanelController;

    private ConversationPanel conversationPanel;

    @Nullable
    private Conversation activeConversation;

    @Inject
    public ConversationPanelController(ContactsPanelController contactsPanelController,
                                       IndividualContactManager individualContactManager,
                                       GroupContactManager groupContactManager, IUISpaceManager uiSpaceManager,
                                       ChatLinksPanelController chatLinksPanelController)
    {
        this.contactsPanelController = contactsPanelController;
        this.individualContactManager = individualContactManager;
        this.groupContactManager = groupContactManager;
        this.uiSpaceManager = uiSpaceManager;
        this.chatLinksPanelController = chatLinksPanelController;

        individualContactManager.addListener(new IndividualContactConversationUpdater());
        groupContactManager.addListener(new GroupContactConversationUpdater());
    }

    /**
     * @param conversationPanel The conversation panel that the controller is responsible for pushing data to and relaying
     *                          requests from
     */
    public void setPanel(@Nonnull ConversationPanel conversationPanel)
    {
        this.conversationPanel = conversationPanel;
    }

    /**
     * Refresh the panel when the conversation state changes.
     *
     * @param event The change event
     * @since 3.4
     */
    @SuppressWarnings("unused")
    @InvokeOnEDT
    private void onStatusChange(@Observes ConversationStatusChanged event)
    {
        final Conversation conversation = event.conversation();

        if (isConversationVisible(conversation)) conversationPanel.updateSentMessageStatus(conversation);
    }

    /**
     * Adds a chat message to the conversation panel.
     *
     * @param message The chat message to add to the panel
     */
    public void addMessage(@Nonnull IChatMessage message)
    {
        conversationPanel.addMessage(message);

        logger.debug("Message pushed to conversation panel: {}", message);
    }

    /**
     * Populates the conversation panel with a new conversation by preparing the panel and individually adding messages
     * to the panel so that they're added in chronological order.
     *
     * @param conversation The conversation to use in populating the conversation panel
     * @since 3.4 (renamed)
     */
    public void setActiveConversation(@Nonnull Conversation conversation)
    {
        final String contactUid = conversation.getUid();

        boolean owned = false;
        String unavailableMessage;

        if (!conversation.isDirect())
        {
            // Contact might be null if offline (e.g. Teams, Roles and deleted groups) or if deleted group is opened via
            // Chats UI
            final var groupContact = groupContactManager.getGroupContactIncludeRoots(contactUid);

            owned = groupContact != null && groupContactManager.ownsGroup(groupContact);

            // Enable the remove subgroups button if owned and any of the group members are groups
            conversationPanel.enableRemoveSubgroups(owned &&
                    groupContact.getGroupMembers().stream().anyMatch(member -> member instanceof IGroupContact));

            final var icon = groupContact != null ? groupContactManager.getIcon(groupContact) : GroupContactManager.GROUP_ICON;

            conversationPanel.setContactIcon(icon);

            unavailableMessage = groupContact == null ? "Group unavailable" : null;
        } else
        {
            final var individualContact = individualContactManager.getContact(contactUid);

            // Contact might be null if offline contact conversation is opened via Chats UI
            if (individualContact != null)
            {
                conversationPanel.setContactIcon(individualContactManager.getContactIconWithStatus(contactUid));
            }

            unavailableMessage = isContactGone(individualContact) ? "Contact unavailable" : null;
        }

        conversationPanel.prepareConversationForDisplay(conversation.getName(), owned,
                !contactUid.equals(groupContactManager.getAllChatRoomsContact().getUid()), unavailableMessage);

        for (IChatMessage message : conversation.getMessages()) conversationPanel.addMessage(message);

        conversationPanel.updateSentMessageStatus(conversation);
        conversationPanel.repaint();

        activeConversation = conversation;

        if (logger.isDebugEnabled())
        {
            logger.debug("Populated conversation panel with new conversation: \"{}\"", conversation.getName());
        }
    }

    /**
     * @return the active {@link Conversation} or {@code null} if no conversation is active. An active conversation may
     * not be visible to the user.
     * @since 3.4
     */
    @Nullable
    public Conversation getActiveConversation()
    {
        return activeConversation;
    }

    /**
     * @param conversation The {@link Conversation} to check
     * @return {@code true} if the conversation is active. The conversation may not be visible to the user.
     * @since 3.4
     */
    public boolean isConversationActive(@Nonnull Conversation conversation)
    {
        return isConversationActive(conversation.getUid());
    }

    /**
     * Checks if the given conversation is currently visible to the user.
     *
     * @param conversation The conversation to check
     * @return {@code true} if the given conversation is currently being shown to the user via the conversation panel
     */
    public boolean isConversationVisible(@Nonnull Conversation conversation)
    {
        return isConversationActive(conversation) && conversationPanel.isShowing();
    }

    /**
     * Passes a message along to the {@link ContactsPanelController} to be sent to a contact through the chat service.
     *
     * @param message The message to send
     */
    void sendMessage(@Nonnull String message)
    {
        // activeConversation should not be null when send option is available
        contactsPanelController.sendMessage(requireContact(), message);
    }

    /**
     * Informs the contacts panel controller that the user pressed the "Back" button and that the main contacts panel
     * should be shown.
     */
    void backButtonClicked()
    {
        // activeConversation should not be null when back button is available
        final Conversation conversation = requireActiveConversation();
        activeConversation = null;

        // Check to see if we should back up to a parent group
        if (!conversation.isDirect())
        {
            final IGroupContact groupContact = groupContactManager.getGroupContactIncludeRoots(conversation.getUid());
            final IGroupContact parent = groupContact != null ? groupContact.getParentContact() : null;

            // If the parent's parent is null, it's the "Groups" group and we skip it
            if (parent != null && parent.getParentContact() != null)
            {
                contactsPanelController.onSelect(parent);
                return;
            }
        }

        contactsPanelController.showPrimaryContactsPanel();
    }

    /**
     * Informs the {@link ContactsPanelController} that the user pressed the "Edit Groups" button and that the group 
     * editing panel should be shown.
     */
    void editGroupButtonClicked()
    {
        // activeConversation should not be null when edit button is available
        contactsPanelController.showGroupEditPanel(requireGroupContact());
    }

    /**
     * @since 3.4.1
     */
    void addSubgroupButtonClicked()
    {
        // activeConversation should not be null when addSubgroup button is available
        contactsPanelController.addGroupRequested(requireGroupContact());
    }

    /**
     * @since 3.4.1
     */
    void removeSubgroupButtonClicked()
    {
        // activeConversation should not be null when delete option is available
        contactsPanelController.deleteGroupRequested(requireGroupContact());
    }

    /**
     * Opens the {@link ChatDataPackageDialog}.
     *
     * @since 5.2
     */
    void sendDataPackage()
    {
        new ChatDataPackageDialog(requireContact()).setVisible(true);
    }

    /**
     * Responds to requests to display the {@link gov.takx.core.chat.ChatLink ChatLinks} associated with an
     * {@link IChatMessage}.
     *
     * @param chatMessage The chat message
     * @since 5.2
     */
    void showDataPackageDetails(@Nonnull IChatMessage chatMessage)
    {
        final List<ChatLink> chatLinks = ChatMessageUtils.getChatLinks(chatMessage);

        if (!chatLinks.isEmpty())
        {
            //noinspection DataFlowIssue
            chatLinksPanelController.setChatLinks(activeConversation.getName(), chatLinks, !chatMessage.sentByLocalContact());

            final JPanel panel = chatLinksPanelController.getPanel();

            // This will add the panel if it hasn't been added yet; subsequent calls will have no effect
            uiSpaceManager.addComponent(COMPONENT_DESCRIPTOR, panel);
            uiSpaceManager.showComponent(panel);
        }
    }

    /**
     * @param contactUid The contact UID to check
     * @return {@code true} if the conversation is active. The conversation may not be visible to the user.
     * @since 5.1
     */
    private boolean isConversationActive(@Nonnull String contactUid)
    {
        return activeConversation != null && activeConversation.getUid().equals(contactUid);
    }

    /**
     * @return The contact associated with the active conversation
     * @throws IllegalStateException if the conversation is {@code null} or if there is no contact associated with the
     * conversation UID
     * @since 5.2
     */
    @Nonnull
    private IContact requireContact()
    {
        final String uid = requireActiveConversation().getUid();

        // Check for a group contact
        IContact contact = groupContactManager.getGroupContactIncludeRoots(uid);

        // Check for an individual contact
        if (contact == null) contact = individualContactManager.getContact(uid);

        if (contact == null) throw new IllegalStateException("No contact is associated with UID " + uid);

        return contact;
    }

    /**
     * @return The group contact associated with the active conversation
     * @throws IllegalStateException if the conversation is {@code null} or if there is no group contact associated with
     * the conversation UID
     * @since 5.2
     */
    @Nonnull
    private IGroupContact requireGroupContact()
    {
        final String uid = requireActiveConversation().getUid();
        final IGroupContact groupContact = groupContactManager.getGroupContact(uid);

        if (groupContact == null) throw new IllegalStateException("No group contact is associated with UID " + uid);

        return groupContact;
    }

    /**
     * @return the active {@link Conversation} 
     * @throws IllegalStateException if the conversation is {@code null}
     * @since 3.5
     */
    @Nonnull
    private Conversation requireActiveConversation()
    {
        if (activeConversation == null) throw new IllegalStateException("activeConversation should not be null");

        return activeConversation;
    }

    /**
     * @param contact The contact to check
     * @return {@code true} if the contact is gone, {@code false} otherwise
     * @since 5.6
     */
    private boolean isContactGone(@Nullable IIndividualContact contact)
    {
        return contact == null || IndividualContactUtils.getState(contact) == IndividualContactState.gone;
    }

    /**
     * Listens for contact updates and keeps the name and icon in sync.
     *
     * @since 3.4.1
     */
    private class IndividualContactConversationUpdater implements IIndividualContactManagerListener
    {
        @Override
        public void contactUpdated(@Nonnull IIndividualContact contact)
        {
            if (isConversationActive(contact.getUid()))
            {
                // Name may revert to UID when contact is gone
                if (!isContactGone(contact)) conversationPanel.setConversationName(contact.getName());

                conversationPanel.setContactIcon(individualContactManager.getContactIconWithStatus(contact.getUid()));
            }
        }

        @Override
        public void contactStateChanged(@Nonnull IIndividualContact contact)
        {
            if (isConversationActive(contact.getUid()))
            {
                conversationPanel.setContactIcon(individualContactManager.getContactIconWithStatus(contact.getUid()));
                conversationPanel.setConversationEnabled(!isContactGone(contact), "Contact unavailable");
            }
        }

        @Override
        public void contactRemoved(@Nonnull IIndividualContact contact)
        {
            if (isConversationActive(contact.getUid()))
            {
                conversationPanel.setContactIcon(null);
                conversationPanel.setConversationEnabled(false, "Contact unavailable");
            }
        }
    }

    /**
     * Responds to {@link gov.tak.api.contact.IGroupContactManager} events and updates the active conversation if needed.
     * 
     * @since 5.1
     */
    private class GroupContactConversationUpdater implements IGroupContactManagerListener
    {
        @Override
        public void contactAdded(@Nonnull IGroupContact contact)
        {
            setEnabled(contact, true);
        }

        @Override
        public void contactRemoved(@Nonnull IGroupContact contact)
        {
            setEnabled(contact, false);
        }

        private void setEnabled(IGroupContact groupContact, boolean enable)
        {
            if (isConversationActive(groupContact.getUid()))
            {
                conversationPanel.setConversationEnabled(enable, "Group unavailable");
            }
        }
    }
}
