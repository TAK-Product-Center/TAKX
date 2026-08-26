package gov.takx.core.view.gui.chat;

import com.google.common.annotations.VisibleForTesting;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IGroupContactManagerListener;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IIndividualContactManagerListener;
import gov.tak.api.contact.IndividualContactState;
import gov.tak.api.contact.IndividualContactUtils;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.annotation.UserPreferenceType;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.preferences.PreferenceEntryChange;
import gov.takx.api.toolbar.ButtonType;
import gov.takx.core.chat.Conversation;
import gov.takx.core.chat.ConversationStore;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.UISpaceManager;
import gov.takx.core.view.gui.contacts.ContactsIconUpdater;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import gov.takx.core.view.gui.contacts.conversation.ConversationMessageAddedEvent;
import gov.takx.core.view.gui.contacts.conversation.UnreadMessageIconUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import javax.swing.Icon;

import static gov.takx.core.project.ServiceOrdering.START_ServicesStarted;
import static gov.takx.core.project.ServiceOrdering.STOP_ServiceStopping;

/**
 * Manages the {@link ChatsPanel}.
 *
 * @since 5.2
 */
@ApplicationScoped
public class ChatsPanelController
{
    private static final Icon UNKNOWN_ICON = UIResources.getActionButtonIcon(ResourceDirectory.PLATFORM_ICONS, "user.png");

    private final ConversationStore conversationStore;
    private final GroupContactManager groupContactManager;
    private final IndividualContactManager individualContactManager;
    private final ContactsIconUpdater contactsIconUpdater;

    private final ChatsPanel chatsPanel;

    @Inject
    public ChatsPanelController(ConversationStore conversationStore, GroupContactManager groupContactManager,
                                IndividualContactManager individualContactManager, UISpaceManager uiSpaceManager,
                                ContactsPanelController contactsPanelController, ContactsIconUpdater contactsIconUpdater)
    {
        this.conversationStore = conversationStore;
        this.groupContactManager = groupContactManager;
        this.individualContactManager = individualContactManager;
        this.contactsIconUpdater = contactsIconUpdater;

        chatsPanel = new ChatsPanel(
                () -> {
                    uiSpaceManager.showComponent(CoreComponentType.CONTACTS_PANEL);
                    contactsPanelController.showPrimaryContactsPanel();
                },
                conversation -> {
                    uiSpaceManager.showComponent(CoreComponentType.CONTACTS_PANEL);
                    contactsPanelController.showConversation(conversation);
                },
                this::getConversationIcon, UnreadMessageIconUtils::updateLabel);
    }

    /**
     * Registers the listener that will initialize this controller. This must be done after the {@link ConversationStore}
     * loads persisted conversations.
     *
     * @param event the startup event
     */
    @SuppressWarnings("unused")
    private void onServiceStarted(@Observes @Priority(START_ServicesStarted) StartProjectServices event,
                                  @PlatformUnderTest boolean underTest)
    {
        if (!underTest)
        {
            contactsIconUpdater.addPanelListener(chatsPanel, ButtonType.CHATS.getImage(), chatsPanel::refreshUnreadMessageCounts);
        }
        init();
    }

    @SuppressWarnings("unused")
    private void onServiceStopped(@Observes @Priority(STOP_ServiceStopping) StopProjectServices event,
                                  @PlatformUnderTest boolean underTest)
    {
        if (!underTest) contactsIconUpdater.removePanelListener(chatsPanel);
        chatsPanel.releaseResources();
    }

    /**
     * Notified on a change to the time preference so the conversation times can be updated.
     *
     * @since 5.2
     */
    @SuppressWarnings("unused")
    @InvokeOnEDT
    private void onTimePreferenceChange(@Observes @UserPreferenceType(UserPreferenceEntry.DISPLAY_LOCAL_TIME) PreferenceEntryChange ignored)
    {
        chatsPanel.refreshTimeLabels();
        chatsPanel.repaint();
    }

    /**
     * Restores persisted conversations and configures listeners.
     */
    @VisibleForTesting
    void init()
    {
        // Add the latest message from each persisted conversation
        conversationStore.getConversations().stream()
                .filter(conversation -> !conversation.getMessages().isEmpty())
                .forEach(chatsPanel::addMessage);
        chatsPanel.refreshList();

        // Listen for contact name/icon changes. The managers will remove the listeners when the project closes

        individualContactManager.addListener(new IIndividualContactManagerListener()
        {
            @Override
            public void contactStateChanged(@Nonnull IIndividualContact contact)
            {
                updateContactNameAndIcon(contact);

                // Remove stale unread message counts
                chatsPanel.refreshUnreadMessageCounts();
            }

            @Override
            public void contactUpdated(@Nonnull IIndividualContact contact)
            {
                updateContactNameAndIcon(contact);
            }

            @Override
            public void contactRemoved(@Nonnull IIndividualContact contact)
            {
                updateContactNameAndIcon(contact);
                chatsPanel.refreshList();
            }
        });

        groupContactManager.addListener(new IGroupContactManagerListener()
        {
            @Override
            public void contactAdded(@Nonnull IGroupContact contact)
            {
                chatsPanel.updateConversation(contact.getUid(), contact.getName());
            }

            @Override
            public void contactRemoved(@Nonnull IGroupContact contact)
            {
                // Remove stale unread message counts
                chatsPanel.refreshUnreadMessageCounts();
            }
        });
    }

    /**
     * @return The panel managed by this controller
     */
    @Nonnull
    public ChatsPanel getPanel()
    {
        return chatsPanel;
    }

    /**
     * Updates the {@link ChatsPanel} model when a new message is sent or received.
     *
     * @param event The conversation event
     */
    @SuppressWarnings("unused")
    private void onConversationMessageAdded(@Observes ConversationMessageAddedEvent event)
    {
        chatsPanel.onNewMessage(event.conversation());
    }

    /**
     * @param conversation The conversation
     * @return The icon to use for the conversation
     */
    @Nonnull
    private Icon getConversationIcon(@Nonnull Conversation conversation)
    {
        final String uid = conversation.getUid();

        if (!conversation.isDirect())
        {
            final IGroupContact contact = groupContactManager.getGroupContact(uid);
            return contact != null ? groupContactManager.getIcon(contact) : GroupContactManager.GROUP_ICON;
        } else
        {
            final Icon icon = individualContactManager.getContactIconWithStatus(uid);
            return icon != null ? icon : UNKNOWN_ICON;
        }
    }

    /**
     * @param contact The contact to update
     * @since 5.2
     */
    private void updateContactNameAndIcon(@Nonnull IIndividualContact contact)
    {
        // Name may revert to UID when contact is gone
        if (IndividualContactUtils.getState(contact) != IndividualContactState.gone)
        {
            chatsPanel.updateConversation(contact.getUid(), contact.getName());
        }
    }
}
