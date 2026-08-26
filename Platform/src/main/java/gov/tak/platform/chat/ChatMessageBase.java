package gov.tak.platform.chat;

import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.chat.ChatMessageStatus;
import gov.tak.api.chat.IChatMessage;
import jakarta.annotation.Nonnull;

/**
 * Internal implementation of {@link IChatMessage}. Host applications can extend this class to support persistence.
 * 
 * @since 3.4
 */
@IncubatingApi(since = "3.4")
public class ChatMessageBase implements IChatMessage
{
    /**
     * Constant that indicates that a long value has not been set.
     *
     * @see AChatMessageBuilder#receiveTime
     * @see IChatMessage#sentByLocalContact()
     */
    public static final long UNSET_RECEIVE_TIME = -1;

    // Group contact UID for group conversations or individual contact UID for direct conversations
    protected String conversationUid;

    protected String text;
    protected long sendTime;
    protected long receiveTime;
    protected String uid;
    protected String senderUid;
    protected String senderName;
    protected ChatMessageStatus status = ChatMessageStatus.none;

    /**
     * No arg constructor that should only be used to support reification.
     */
    protected ChatMessageBase()
    {
    }

    /**
     * @param text The chat message content
     * @param sendTime The time the message was sent, in milliseconds since epoch
     * @param receiveTime The time the message was received, in milliseconds since epoch
     * @param uid The UID of the message
     * @param senderUid The sender's UID
     * @param senderName The sender's name or {@code null} if this is an outgoing message
     * @param conversationUid The message's conversation UID
     */
    public ChatMessageBase(@Nonnull String text, long sendTime, long receiveTime, @Nonnull String uid,
                           @Nonnull String senderUid, @Nonnull String senderName, @Nonnull String conversationUid)
    {
        this.text = text;
        this.sendTime = sendTime;
        this.receiveTime = receiveTime;
        this.uid = uid;
        this.senderUid = senderUid;
        this.senderName = senderName;
        this.conversationUid = conversationUid;
    }

    @Nonnull
    @Override
    public String getText()
    {
        return text;
    }

    @Override
    public long getSendTime()
    {
        return sendTime;
    }

    @Override
    public long getReceiveTime()
    {
        return receiveTime;
    }

    @Nonnull
    @Override
    public String getUid()
    {
        return uid;
    }

    @Nonnull
    @Override
    public String getSenderUid()
    {
        return senderUid;
    }

    @Nonnull
    @Override
    public String getSenderName()
    {
        return senderName;
    }

    @Nonnull
    @Override
    public String getConversationUid()
    {
        return conversationUid;
    }

    @Override
    @Nonnull
    public ChatMessageStatus getStatus()
    {
        return status;
    }

    @Override
    public void setStatus(@Nonnull ChatMessageStatus status)
    {
        this.status = status;
    }

    @Override
    public boolean sentByLocalContact()
    {
        // no receive time implies that this message was sent by the local user
        return receiveTime == UNSET_RECEIVE_TIME;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }

        return uid.equals(((ChatMessageBase) o).uid);
    }

    @Override
    public int hashCode()
    {
        int result = text.hashCode();
        result = 31 * result + (int) (sendTime ^ (sendTime >>> 32));
        result = 31 * result + (int) (receiveTime ^ (receiveTime >>> 32));
        result = 31 * result + uid.hashCode();
        return result;
    }
}