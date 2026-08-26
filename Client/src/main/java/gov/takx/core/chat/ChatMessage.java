package gov.takx.core.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.tak.api.chat.ChatMessageStatus;
import gov.tak.platform.chat.ChatMessageBase;
import gov.takx.api.annotation.Internal;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Transient;
import org.hibernate.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;

/**
 * This implementation adds persistence support to takkernel's {@link ChatMessageBase}.
 *
 * @since 3.0 (refactored from AChatMessage in 3.4)
 * @see ChatMessageBuilder#build()
 */
@Internal
@Entity
public class ChatMessage extends ChatMessageBase implements IPersistableChatMessage
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /** @since 5.2 */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    /** @since 5.2 */
    private final static TypeReference<List<ChatLink>> TYPE_REFERENCE = new TypeReference<>()
    {
    };

    private Integer id;

    /** @since 5.2 */
    private String chatLinksJson;

    /** @since 5.2 */
    @Transient
    private List<ChatLink> chatLinks;

    /**
     * For Hibernate
     */
    @SuppressWarnings("unused")
    protected ChatMessage()
    {
    }

    /**
     * @param text The chat message content
     * @param sendTime The time the message was sent, in milliseconds since epoch
     * @param receiveTime The time the message was received, in milliseconds since epoch
     * @param uid The UID of the message
     * @param senderUid The sender's UID
     * @param senderName The sender's name
     * @param conversationUid The message's conversation UID
     */
    ChatMessage(@Nonnull String text, long sendTime, long receiveTime, @Nonnull String uid,
                @Nonnull String senderUid, @Nonnull String senderName, @Nonnull String conversationUid,
                @Nullable List<ChatLink> chatLinks)
    {
        super(text, sendTime, receiveTime, uid, senderUid, senderName, conversationUid);
        this.chatLinks = chatLinks;
        chatLinksJson = toJson(chatLinks);
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Override
    public Integer getId()
    {
        return id;
    }

    @SuppressWarnings("unused") // Hibernate
    private void setId(Integer id)
    {
        this.id = id;
    }

    @Override
    @Column(nullable = false, length = Length.LONG32)
    @Nonnull
    public String getText()
    {
        return text;
    }

    @SuppressWarnings("unused") // Hibernate
    private void setText(String text)
    {
        this.text = text;
    }

    @SuppressWarnings("RedundantMethodOverride") // Hibernate
    @Override
    public long getSendTime()
    {
        return super.getSendTime();
    }

    @SuppressWarnings("unused") // Hibernate
    private void setSendTime(long sendTime)
    {
        this.sendTime = sendTime;
    }

    @SuppressWarnings("RedundantMethodOverride") // Hibernate 
    @Override
    public long getReceiveTime()
    {
        return super.getReceiveTime();
    }

    @SuppressWarnings("unused") // Hibernate
    private void setReceiveTime(long receiveTime)
    {
        this.receiveTime = receiveTime;
    }

    @Column(nullable = false, unique = true)
    @Nonnull
    @Override
    public String getUid()
    {
        return uid;
    }

    @SuppressWarnings("unused") // Hibernate
    private void setUid(String uid)
    {
        this.uid = uid;
    }

    @Column(nullable = false)
    @Nonnull
    @Override
    public String getSenderUid()
    {
        return senderUid;
    }

    @SuppressWarnings("unused") // Hibernate
    private void setSenderUid(String senderUid)
    {
        this.senderUid = senderUid;
    }

    @Column(nullable = false)
    @Nonnull
    @Override
    public String getSenderName()
    {
        return senderName;
    }

    @SuppressWarnings("unused") // Hibernate
    private void setSenderName(String senderName)
    {
        this.senderName = senderName;
    }

    @Column(nullable = false)
    @Nonnull
    @Override
    public String getConversationUid()
    {
        return conversationUid;
    }

    @SuppressWarnings("unused") // Hibernate
    private void setConversationUid(String conversationUid)
    {
        this.conversationUid = conversationUid;
    }

    @Enumerated(EnumType.STRING)
    @Nonnull
    @Override
    public ChatMessageStatus getStatus()
    {
        return status;
    }

    @SuppressWarnings("RedundantMethodOverride") // Hibernate
    @Override
    public void setStatus(@Nonnull ChatMessageStatus status)
    {
        super.setStatus(status);
    }

    /**
     * Returns the data package content links if this is a chat data package message.
     *
     * @return The chat links associated with this message, or an empty list if there are no links
     * @see ChatDataPackageService
     * @since 5.2
     */
    @Transient
    @Nonnull
    @Internal
    public List<ChatLink> getChatLinks()
    {
        return chatLinks != null ? chatLinks : Collections.emptyList();
    }

    /**
     * @return The chat links JSON (for Hibernate)
     * @since 5.2
     */
    @Column(length = Length.LONG32)
    @SuppressWarnings("unused") // Hibernate
    private String getChatLinksJson()
    {
        return chatLinksJson;
    }

    /**
     * @param chatLinksJson The chat links JSON (from Hibernate)
     * @since 5.2
     */
    @SuppressWarnings("unused") // Hibernate
    private void setChatLinksJson(String chatLinksJson)
    {
        this.chatLinksJson = chatLinksJson;
        if (chatLinksJson != null) chatLinks = fromJson(chatLinksJson);
    }

    /**
     * @param chatLinks A list of {@link ChatLink ChatLinks}
     * @return A JSON String representation of the links
     * @since 5.2
     */
    @Nullable
    private String toJson(@Nullable List<ChatLink> chatLinks)
    {
        if (chatLinks == null || chatLinks.isEmpty()) return null;

        try
        {
            return OBJECT_MAPPER.writeValueAsString(chatLinks);
        } catch (JsonProcessingException e)
        {
            logger.error("Error converting chat links to JSON", e);
            return null;
        }
    }

    /**
     * @param chatLinksJson A JSON String representation of a set of {@link ChatLink ChatLinks}
     * @return A list of restored {@link ChatLink ChatLinks}
     * @since 5.2
     */
    @Nullable
    private List<ChatLink> fromJson(@Nonnull String chatLinksJson)
    {
        try
        {
            return OBJECT_MAPPER.readValue(chatLinksJson, TYPE_REFERENCE);
        } catch (JsonProcessingException e)
        {
            logger.error("Error restoring chat links from JSON: {}", chatLinksJson, e);
            return null;
        }
    }
}
