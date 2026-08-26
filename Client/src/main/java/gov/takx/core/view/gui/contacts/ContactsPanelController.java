package gov.takx.core.view.gui.contacts;

import gov.tak.api.chat.ChatMessageStatus;
import gov.tak.api.chat.ChatOperationResult;
import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.IChatMessageBuilder;
import gov.tak.api.chat.IChatMessageFinder;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IGroupContactManagerListener;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.location.ILocationProviderManager;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.platform.chat.ChatReceipt;
import gov.tak.platform.chat.ChatService;
import gov.tak.platform.chat.client.IChatServiceClient;
import gov.tak.platform.chat.client.IChatServiceClientDelegate;
import gov.takx.api.annotation.CallSign;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.persist.IChatMessagePersist;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.api.ui.UISpaceVisibilityEvent;
import gov.takx.core.chat.ChatMessageBuilder;
import gov.takx.core.chat.Conversation;
import gov.takx.core.chat.ConversationStatusChanged;
import gov.takx.core.chat.ConversationStore;
import gov.takx.core.chat.IPersistableChatMessage;
import gov.takx.core.view.gui.contacts.conversation.ConversationPanelController;
import gov.takx.core.view.gui.contacts.groupmanagement.GroupCreationPanelController;
import gov.takx.core.view.gui.contacts.groupmanagement.GroupDeletionPanelController;
import gov.takx.core.view.gui.contacts.groupmanagement.GroupEditPanelController;
import gov.takx.core.view.gui.contacts.lists.ContactsPanelGroupContactTreeController;
import gov.takx.core.view.gui.contacts.lists.IContactSelectionListener;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

/**
 * Main controller for the Contacts panel. Responsible for configuring and communicating with other panel controllers
 * as well as calling Contacts panel methods to show different panels based on user interactions.
 * <p>
 * Note: This class casts {@link IChatMessage}s to {@link IPersistableChatMessage}s. The cast should always work because
 * messages can only be created using {@link ChatMessageBuilder#build()} which returns {@link IPersistableChatMessage}s.
 * The messages are arriving from the singleton {@link ChatService} which lives in takkernel and this solution seemed
 * cleaner than trying to use generics.
 * @since 3.0
 */
@ApplicationScoped
public class ContactsPanelController implements IChatServiceClient, IContactSelectionListener<IContact>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String nodeUuidString;
    private final ILocationProviderManager locationProviderManager;
    private final IChatMessagePersist chatMessagePersist;
    private final IndividualContactManager individualContactManager;
    private final GroupContactManager groupContactManager;
    private final ConversationStore conversationStore;
    private final ContactsPanelGroupContactTreeController contactsPanelGroupContactTreeController;
    private final ConversationPanelController conversationPanelController;
    private final GroupCreationPanelController groupCreationPanelController;
    private final GroupDeletionPanelController groupDeletionPanelController;
    private final GroupEditPanelController groupEditPanelController;

    private ContactsPanel contactsPanel;
    private ContactsIconUpdater contactsIconUpdater;
    private ChatService chatService;
    private IChatServiceClientDelegate delegate;

    private String name;
    private final IPreferenceMonitor<String> nameMonitor = PreferenceEntry.CALL_SIGN.newStringMonitor(this::setName, false);
    private final Event<ConversationStatusChanged> conversationStatusChangedEvent;
    private final GroupContactManagerListener groupContactManagerListener = new GroupContactManagerListener();

    @Inject
    public ContactsPanelController(@NodeUUID UUID nodeUuid, @CallSign String name,
                                   ILocationProviderManager locationProviderManager,
                                   ConversationStore conversationStore,
                                   ContactsPanelGroupContactTreeController contactsPanelGroupContactTreeController,
                                   ConversationPanelController conversationPanelController,
                                   GroupCreationPanelController groupCreationPanelController,
                                   GroupDeletionPanelController groupDeletionPanelController,
                                   GroupEditPanelController groupEditPanelController,
                                   Event<ConversationStatusChanged> conversationStatusChangedEvent,
                                   IChatMessagePersist chatMessagePersist,
                                   IndividualContactManager individualContactManager,
                                   GroupContactManager groupContactManager,
                                   ContactsIconUpdater contactsIconUpdater)
    {
        nodeUuidString = nodeUuid.toString();
        this.name = name;
        this.locationProviderManager = locationProviderManager;
        this.conversationStore = conversationStore;
        this.contactsPanelGroupContactTreeController = contactsPanelGroupContactTreeController;
        this.conversationPanelController = conversationPanelController;
        this.groupCreationPanelController = groupCreationPanelController;
        this.groupDeletionPanelController = groupDeletionPanelController;
        this.groupEditPanelController = groupEditPanelController;
        this.conversationStatusChangedEvent = conversationStatusChangedEvent;
        this.chatMessagePersist = chatMessagePersist;
        this.individualContactManager = individualContactManager;
        this.groupContactManager = groupContactManager;
        this.contactsIconUpdater = contactsIconUpdater;
    }

    @Override
    public void setChatService(@Nonnull ChatService chatService, @Nonnull IChatServiceClientDelegate delegate)
    {
        this.chatService = chatService;
        this.delegate = delegate;
    }

    @Nonnull
    @Override
    public IChatMessageBuilder getChatMessageBuilder(@Nonnull String text, long sendTime, @Nonnull String conversationUid)
    {
        return getChatMessageBuilder(getUid(), getName(), text, sendTime, conversationUid);
    }

    @Nonnull
    @Override
    public IChatMessageBuilder getChatMessageBuilder(@Nonnull String senderUid, @Nonnull String senderName,
                                                     @Nonnull String text, long sendTime,
                                                     @Nonnull String conversationUid)
    {
        return new ChatMessageBuilder(senderUid, senderName, text, sendTime, conversationUid);
    }

    @Nonnull
    @Override
    public IChatMessageFinder getChatMessageFinder()
    {
        return chatMessagePersist;
    }

    /**
     * @param enable {@code true} if the remove groups button should be enabled, {@code false} otherwise
     * @since 3.5
     */
    public void enableRemoveGroups(boolean enable)
    {
        if (contactsPanel != null) contactsPanel.enableRemoveGroups(enable);
    }

    /**
     * @param contactsPanel The primary contact panel to set on this controller
     */
    void setPanel(@Nonnull ContactsPanel contactsPanel)
    {
        this.contactsPanel = contactsPanel;
        contactsIconUpdater.addPanelListener(contactsPanel, ButtonType.CONTACTS.getImage(), null);
    }

    /**
     * Starts the controller by registering listeners and initializing the UI.
     */
    void start()
    {
        groupContactManager.addListener(groupContactManagerListener);
        enableRemoveGroups(groupContactManager.hasUserGroups());
        nameMonitor.start();
    }

    /**
     * Sets a new call sign on this controller, propagating it to the {@link ContactsPanel}.
     *
     * @param name The call sign to set on this controller
     * @since 3.1
     */
    private void setName(String name)
    {
        this.name = name;
        contactsPanel.setUserName(name);
    }

    /**
     * Remove all our registered listeners when the project is closing.
     */
    @SuppressWarnings("unused")
    private void onProjectClosing(@Observes ProjectClosing ignore)
    {
        groupContactManager.removeListener(groupContactManagerListener);
        nameMonitor.stop();
        if (contactsPanel != null) contactsIconUpdater.removePanelListener(contactsPanel);
    }

    @Nonnull
    @Override
    public String getUid()
    {
        return nodeUuidString;
    }

    @Nonnull
    @Override
    public String getName()
    {
        return name;
    }

    @Nonnull
    @Override
    public GeoPoint getLocation()
    {
        final GeoPointMetaData geoPointMetaData = locationProviderManager.getCurrentLocation();

        if (geoPointMetaData == null)
        {
            logger.debug("Unable to acquire current location, using GeoPoint.ZERO_POINT location.");
            return GeoPoint.ZERO_POINT;
        }

        final gov.tak.api.maps.coords.GeoPoint takxLocation = geoPointMetaData.get();

        return new GeoPoint(
                takxLocation.getLatitude(),
                takxLocation.getLongitude(),
                takxLocation.getAltitude(),
                takxLocation.getCE(),
                takxLocation.getLE());
    }

    @Override
    public void messageReceived(@Nonnull IContact sender, @Nonnull IChatMessage message)
    {
        if (sender instanceof IIndividualContact)
        {
            individualContactManager.updateContactState(message.getSenderUid(), message.getSendTime());
        }

        final IPersistableChatMessage persistableChatMessage = ((IPersistableChatMessage) message);
        final Conversation conversation = conversationStore.getConversationForContact(sender);

        if (conversationPanelController.isConversationVisible(conversation))
        {
            conversation.addReadMessage(persistableChatMessage);
            conversationPanelController.addMessage(message);
            sendReceipt(sender, message.getUid(), true);
        } else
        {
            conversation.addUnreadMessage(persistableChatMessage);
            final boolean firstUnread = conversation.getUnreadMessageCount() == 1;

            // Required to refresh the group's tree node width when the unread message icon is added
            if (firstUnread && sender instanceof IGroupContact groupContact)
            {
                contactsPanelGroupContactTreeController.unreadStatusChanged(groupContact.getUid());
            }

            if (conversationPanelController.isConversationActive(conversation))
            {
                conversationPanelController.addMessage(message);
            }
            sendReceipt(sender, message.getUid(), false);
        }

        contactsPanel.repaint();
        contactsPanel.revalidate();

        contactsIconUpdater.updateUnreadMessageCount(conversationStore.getUnreadMessageCount());

        logger.debug("Received message from {}: \"{}\"", message.getSenderName(), message.getText());
    }

    @Override
    public void receiveReceipt(@Nonnull ChatReceipt receipt)
    {
        final Conversation conversation = conversationStore.getConversationForContact(receipt.contact());
        conversation.setStatus(receipt.messageUid(), receipt.cotType());
        conversationStatusChangedEvent.fire(new ConversationStatusChanged(conversation));
    }

    /**
     * Populates the conversation panel with the selected contact's conversation and opens the conversation panel.
     * {@inheritDoc}
     */
    @Override
    public void onSelect(IContact entry)
    {
        showConversation(conversationStore.getConversationForContact(entry));
    }

    /**
     * @param conversation The conversation to display
     * @since 5.2
     */
    public void showConversation(@Nonnull Conversation conversation)
    {
        conversationPanelController.setActiveConversation(conversation);
        contactsPanel.showConversationPanel();
        checkAndClearUnreadMessageCount(conversation);
    }

    /**
     * Checks to see if there's an active {@link Conversation} when the {@link ContactsPanel} is opened. If so, sends a
     * read receipt to the sender (if needed) and clears the unread message count.
     *
     * @param uiSpaceVisibilityEvent The event
     * @since 3.4
     * @see #checkAndClearUnreadMessageCount(Conversation)
     */
    @SuppressWarnings("unused")
    @InvokeOnEDT
    public void panelVisibilityChanged(@Observes UISpaceVisibilityEvent uiSpaceVisibilityEvent)
    {
        if (uiSpaceVisibilityEvent.isVisible() && uiSpaceVisibilityEvent.getComponent() == contactsPanel)
        {
            final Conversation conversation = conversationPanelController.getActiveConversation();

            if (conversation != null) checkAndClearUnreadMessageCount(conversation);
        }
    }

    /**
     * Sets the group on the group creation panel and tells the primary contacts panel to show the group creation
     * panel.
     *
     * @param parent The parent {@link IGroupContact} if creating a subgroup, or {@code null} if creating top-level group
     */
    public void addGroupRequested(@Nullable IGroupContact parent)
    {
        groupCreationPanelController.prepare(parent);
        contactsPanel.showGroupCreationPanel();
    }

    /**
     * Sets the group on the group delete panel and tells the primary contacts panel to show the group deletion
     * panel.
     *
     * @param parent The parent {@link IGroupContact} if deleting subgroups, or {@code null} if deleting top-level groups
     */
    public void deleteGroupRequested(@Nullable IGroupContact parent)
    {
        groupDeletionPanelController.prepare(parent);
        contactsPanel.showGroupDeletionPanel();
    }

    /**
     * Sets the group on the group editing panel and tells the primary contacts panel to show the group editing
     * panel.
     *
     * @param groupContact The group contact to set on the group editing panel
     */
    public void showGroupEditPanel(@Nonnull IGroupContact groupContact)
    {
        groupEditPanelController.prepare(groupContact);
        contactsPanel.showGroupEditPanel();
    }

    /**
     * Shows the primary contacts panel to the user.
     */
    public void showPrimaryContactsPanel()
    {
        contactsPanel.showPrimaryContactsPanel();
    }

    /**
     * Sends a message to a given contact through the chat service API and adds the sent message to the appropriate
     * conversation in the conversation store.
     *
     * @param conversationContact The contact to send the message to
     * @param text                The message text to send
     */
    public void sendMessage(@Nonnull IContact conversationContact, @Nonnull String text)
    {
        final IChatMessage message =
                new ChatMessageBuilder(getUid(), getName(), text, System.currentTimeMillis(), conversationContact.getUid()).build();

        logger.debug("Sending chat message to {}: \"{}\"", conversationContact, text);

        chatService.sendMessage(message,
                result -> {
                    if (result.getStatus() == ChatOperationResult.Status.SUCCESS)
                    {
                        message.setStatus(ChatMessageStatus.sent);

                        final Conversation conversation = conversationStore.getConversationForContact(conversationContact);
                        conversationStatusChangedEvent.fire(new ConversationStatusChanged(conversation));
                    }

                    logChatOperationResult(result);
                });
    }

    @Override
    public void messageSent(@Nonnull IContact recipient, @Nonnull IChatMessage message)
    {
        final var conversation = conversationStore.getConversationForContact(recipient);

        if (conversationPanelController.isConversationActive(conversation))
        {
            conversationPanelController.addMessage(message);
        }

        conversation.addSentMessage((IPersistableChatMessage) message);
    }

    /**
     * Logs the result of a chat operation.
     *
     * @param chatOperationResult The result of the attempted chat operation
     */
    private void logChatOperationResult(ChatOperationResult chatOperationResult)
    {
        if (chatOperationResult.getStatus() == ChatOperationResult.Status.SUCCESS)
        {
            if (logger.isDebugEnabled()) logger.debug("{} request successful", chatOperationResult.getOperation());
        } else
        {
            logger.warn(chatOperationResult.getMessage());
        }
    }

    /**
     * Checks a {@link Conversation} to see if there are any unread messages that need to be acknowledged. If so, sends
     * a receipt to the sender (if needed) and clears the unread message count.
     *
     * @param conversation The conversation to check
     * @since 3.4
     */
    private void checkAndClearUnreadMessageCount(@Nonnull Conversation conversation)
    {
        final String conversationUid = conversation.getUid();
        final boolean directConversation = conversation.isDirect();
        final String unreadMessageUid = conversation.getLastUnreadMessageUid();

        if (unreadMessageUid != null && directConversation)
        {
            sendReceipt(individualContactManager.getContact(conversationUid), unreadMessageUid, true);
        }

        conversation.clearUnreadMessages();
        contactsIconUpdater.updateUnreadMessageCount(conversationStore.getUnreadMessageCount());

        // Required to refresh the groups' tree node width when the unread message count is removed
        if (!directConversation) contactsPanelGroupContactTreeController.unreadStatusChanged(conversationUid);
    }

    /**
     * Sends a chat receipt to the sender if the message came from an {@link IIndividualContact}.
     *
     * @param contact    The sender
     * @param messageUid The UID of the message being confirmed
     * @param read       {@code true} if confirming that the message has been read
     * @since 3.4
     */
    private void sendReceipt(IContact contact, String messageUid, boolean read)
    {
        if (contact instanceof IIndividualContact individualContact)
        {
            delegate.sendReceipt(individualContact, messageUid, read, this::logChatOperationResult);
        }
    }

    /**
     * {@link GroupContactManager} listener. This differs from the {@link gov.tak.api.chat.IChatServiceListener} in that
     * it's called for <em>both</em> local changes initiated through the {@link gov.tak.api.chat.IChatService} and
     * remote changes handled by the parser and {@link IContactService}.
     * @since 3.5
     */
    private class GroupContactManagerListener implements IGroupContactManagerListener
    {
        @Override
        public void contactAdded(@Nonnull IGroupContact contact)
        {
            enableRemoveGroups(groupContactManager.hasUserGroups());
        }

        @Override
        public void contactRemoved(@Nonnull IGroupContact contact)
        {
            contactsIconUpdater.updateUnreadMessageCount(conversationStore.getUnreadMessageCount());
            enableRemoveGroups(groupContactManager.hasUserGroups());
        }
    }
}
