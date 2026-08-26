package gov.takx.core.chat;

import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IIndividualContact;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.core.persist.ConversationPersist;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores conversations by the UID of the contact associated with each conversation.
 *
 * @since 3.0
 */
@ApplicationScoped
public class ConversationStore
{
    private final Map<String, Conversation> conversations = new ConcurrentHashMap<>();
    private final ConversationPersist conversationPersist;

    @Inject
    ConversationStore(ConversationPersist conversationPersist)
    {
        this.conversationPersist = conversationPersist;

    }

    @SuppressWarnings("unused")
    private void onServicesStarted(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices event)
    {
        // Load all persisted conversations
        conversationPersist.queryAll().forEach(conversation -> conversations.put(conversation.getUid(), conversation));
    }

    @SuppressWarnings("unused")
    private void onServicesStopped(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices event)
    {
        conversations.clear();
    }

    /**
     * @return All {@link Conversation}s
     * @since 5.2
     */
    @Nonnull
    public Collection<Conversation> getConversations()
    {
        return Collections.unmodifiableCollection(conversations.values());
    }

    /**
     * Gets the conversation associated with the given contact, creating a new conversation for said contact if one does
     * not yet exist.
     *
     * @param contact The contact associated with the conversation to get
     * @return The conversation associated with the given contact
     */
    @Nonnull
    public synchronized Conversation getConversationForContact(@Nonnull IContact contact)
    {
        final String uid = contact.getUid();
        Conversation conversation = conversations.get(uid);

        if (conversation == null)
        {
            conversation = new Conversation(contact.getUid(), contact.getName(), contact instanceof IIndividualContact);
            conversationPersist.persist(conversation);
            conversations.put(uid, conversation);
        }

        return conversation;
    }

    /**
     * Updates the conversation associated with the given contact so that the conversation contains any new information
     * about said contact (e.g. a new display name).
     *
     * @param contact The updated contact
     */
    public void updateConversationForContact(@Nonnull IContact contact)
    {
        final Conversation conversation = getConversationForContact(contact);

        conversation.setName(contact.getName());
        conversationPersist.persist(conversation);
    }

    /**
     * @return The total number of unread messages across all conversations
     */
    public int getUnreadMessageCount()
    {
        return conversations.values().stream().mapToInt(Conversation::getUnreadMessageCount).sum();
    }
}
