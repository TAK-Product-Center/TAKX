package gov.takx.core.chat;

import gov.tak.api.chat.ChatMessageStatus;
import gov.tak.api.chat.IChatMessage;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.persist.IChatMessagePersist;
import gov.takx.api.persist.IPersistable;
import gov.takx.core.view.gui.contacts.conversation.ConversationMessageAddedEvent;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostLoad;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import org.hibernate.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents an ongoing conversation and the messages that are part of it.
 *
 * @since 3.0 (made entity, moved in 5.2)
 */
@Entity
@Table(indexes = {@Index(name = "conversation_uid", columnList = "uid", unique = true)})
@PseudoBean
public class Conversation implements IPersistable
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;
    private String uid;
    @Column(length = Length.LONG32)
    private String name;
    private boolean direct;

    @Transient
    private final SortedSet<IPersistableChatMessage> messages = new TreeSet<>(new MessageComparator());
    @Transient
    private int unreadMessageCount = 0;
    @Transient
    private String lastUnreadMessageUid = null;
    @Transient
    private IPersistableChatMessage lastSentMessage = null;
    @Transient
    @Inject
    private IChatMessagePersist chatMessagePersist;
    @Transient
    @Inject
    private Event<ConversationMessageAddedEvent> conversationMessageAddedEvent;

    /**
     * @since 5.2
     */
    @SuppressWarnings("unused") // Hibernate
    protected Conversation()
    {
    }

    /**
     * @param uid The conversation UID
     * @param name The conversation name
     * @param direct {@code true} if this is a direct conversation with another contact, {@code false} if 
     *                           it's a group conversation
     * @since 5.2
     */
    public Conversation(@Nonnull String uid, @Nonnull String name, boolean direct)
    {
        this.uid = uid;
        this.name = name;
        this.direct = direct;
    }

    /**
     * Called by Hibernate after this entity is loaded.
     * 
     * @since 5.2
     */
    @SuppressWarnings("unused")
    @PostLoad
    protected void onLoad()
    {
        // Restore previous conversation messages
        messages.addAll(chatMessagePersist.getConversationMessages(uid));

        // Restore the conversation status. We only track the status of direct conversations. The status is based on the 
        // last sent message.
        if (direct && !messages.isEmpty())
        {
            lastSentMessage = messages.stream()
                    .filter(IChatMessage::sentByLocalContact)
                    .max(Comparator.comparingLong(IChatMessage::getSendTime))
                    .orElse(null);
        }
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    /**
     * @return The name of the conversation
     */
    @Nonnull
    public String getName()
    {
        return name;
    }

    /**
     * Sets the conversation name.
     *
     * @param name The new conversation name
     * @since 5.2
     */
    public void setName(@Nonnull String name)
    {
        this.name = name;
    }

    /**
     * @return The conversation UID; this will be the UID of the contact associated with the conversation
     * @since 5.2
     */
    @Nonnull
    public String getUid()
    {
        return uid;
    }

    /**
     * @return {@code true} if this is a conversation with another individual contact, {@code false} if it is a group
     * conversation
     * @since 5.2
     */
    public boolean isDirect()
    {
        return direct;
    }

    /**
     * @return An immutable sorted-set of the messages in the conversation
     */
    @Nonnull
    public SortedSet<IPersistableChatMessage> getMessages()
    {
        return Collections.unmodifiableSortedSet(messages);
    }

    /**
     * Adds a received message to the conversation and counts it as being unread.
     *
     * @param message The unread message to add
     */
    public void addUnreadMessage(@Nonnull IPersistableChatMessage message)
    {
        if (addMessage(message))
        {
            unreadMessageCount++;
            lastUnreadMessageUid = message.getUid();
        }
    }

    /**
     * Adds a received message to the conversation and counts it as being read. This method simply calls 
     * {@link #addMessage(IPersistableChatMessage)} and exists to help the caller distinguish between adding unread messages, read
     * and sent messages.
     *
     * @param message The read message to add
     */
    public void addReadMessage(@Nonnull IPersistableChatMessage message)
    {
        addMessage(message);
    }

    /**
     * Adds a sent message to the conversation. If it's a direct message, the message will be tracked for use by UI
     * elements that need to check the status of the last sent message.
     *
     * @param message The message
     * @since 3.4
     */
    public void addSentMessage(@Nonnull IPersistableChatMessage message)
    {
        if (addMessage(message) && direct) lastSentMessage = message;
    }

    /**
     * Adds a message to the conversation.
     *
     * @param message The message to add
     * @return {@code true} if the message was added, {@code false} if it was already part of the conversation
     */
    private boolean addMessage(@Nonnull IPersistableChatMessage message)
    {
        logger.trace("Adding message: {}", message);

        if (messages.add(message))
        {
            chatMessagePersist.persistMessage(message);
            conversationMessageAddedEvent.fire(new ConversationMessageAddedEvent(this));
            return true;
        } else
        {
            return false;
        }
    }

    /**
     * @return The number of unread messages in the conversation
     */
    public int getUnreadMessageCount()
    {
        return unreadMessageCount;
    }

    /**
     * Clears the unread message count of the conversation.
     */
    public void clearUnreadMessages()
    {
        lastUnreadMessageUid = null;
        unreadMessageCount = 0;
    }

    /**
     * @return The last unread message UID, or {@code null} if all received messages have been read.
     * @since 3.4
     */
    @Nullable
    public String getLastUnreadMessageUid()
    {
        return lastUnreadMessageUid;
    }

    /**
     * @return The UID of the last sent direct message, or {@code null} if no direct messages have been sent.
     * @since 3.4
     */
    @Nullable
    public String getLastSentMessageUid()
    {
        return lastSentMessage != null ? lastSentMessage.getUid() : null;
    }

    /**
     * Sets the status of this conversation. The status will only be updated if the message specified by the UID is the
     * most recently sent direct message and the status is greater (none < sent < delivered < read) than the current
     * status.
     *
     * @param messageUid The message UID
     * @param cotType The message CoT type
     * @since 3.4
     * @see ChatMessageStatus
     */
    public void setStatus(@Nonnull String messageUid, @Nonnull String cotType)
    {
        ChatMessageStatus status = ChatMessageStatus.parseCotType(cotType);

        // Check ordinal value because "read" messages may arrive before "delivered" messages
        if (lastSentMessage != null && lastSentMessage.getUid().equals(messageUid) && status.isGreater(lastSentMessage.getStatus()))
        {
            lastSentMessage.setStatus(status);
            chatMessagePersist.persistMessage(lastSentMessage);
        }
    }

    /**
     * @return The status of the most recently sent message.
     * @since 3.4
     */
    @Nonnull
    public ChatMessageStatus getStatus()
    {
        return lastSentMessage != null ? lastSentMessage.getStatus() : ChatMessageStatus.none;
    }

    /**
     * Compares messages by send time to support sorting within a conversation.
     *
     * @since 3.4
     */
    private static class MessageComparator implements Comparator<IChatMessage>
    {
        @Override
        public int compare(IChatMessage message1, IChatMessage message2)
        {
            return Long.compare(message1.getSendTime(), message2.getSendTime());
        }
    }
}
