package gov.tak.platform.chat;

import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.IChatMessageBuilder;
import jakarta.annotation.Nonnull;

import java.util.UUID;

/**
 * Base class for {@link IChatMessageBuilder} implementations.
 *
 * @since 3.4
 */
public abstract class AChatMessageBuilder<T extends IChatMessageBuilder> implements IChatMessageBuilder
{
    protected final String senderUid;
    protected final long sendTime;
    protected final String conversationUid;
    protected final String senderName;

    // If receiveTime remains UNSET, it will indicate this message was sent (not received)
    protected long receiveTime = ChatMessageBase.UNSET_RECEIVE_TIME;
    protected String uid = UUID.randomUUID().toString();
    protected String text;

    /**
     * @param senderUid The sender's UID
     * @param senderName The sender's name
     * @param text  The text to be used when building a chat message
     * @param sendTime The timestamp representing the send-time of the message, in milliseconds since epoch. Must be a
     *                 positive, non-zero {@code long}.
     * @param conversationUid The {@link gov.tak.api.contact.IGroupContact} UID for group conversations or the
     *                       individual UID for direct conversations.
     * @throws IllegalArgumentException if recieveTime is less than or equal to 0
     */
    protected AChatMessageBuilder(@Nonnull String senderUid, @Nonnull String senderName, @Nonnull String text,
                                  long sendTime, @Nonnull String conversationUid)
            throws IllegalArgumentException
    {
        if (sendTime <= 0) throw new IllegalArgumentException("sendTime must be greater than 0, but was " + sendTime);

        this.senderUid = senderUid;
        this.senderName = senderName;
        this.text = text;
        this.sendTime = sendTime;
        this.conversationUid = conversationUid;
    }

    /**
     * @param original The original message that will be used to initialize the builder values.
     */
    protected AChatMessageBuilder(@Nonnull IChatMessage original)
    {
        text = original.getText();
        sendTime = original.getSendTime();
        receiveTime = original.getReceiveTime();
        uid = original.getUid();
        senderUid = original.getSenderUid();
        senderName = original.getSenderName();
        conversationUid = original.getConversationUid();
    }

    @Override
    public T withText(@Nonnull String text)
    {
        this.text = text;

        //noinspection unchecked
        return (T) this;
    }

    @Override
    public T withReceiveTime(long receiveTime) throws IllegalArgumentException
    {
        if (receiveTime <= 0) throw new IllegalArgumentException("receiveTime must be greater than 0, but was " + receiveTime);

        this.receiveTime = receiveTime;

        //noinspection unchecked
        return (T) this;
    }

    @Override
    public T withUid(String uid)
    {
        this.uid = uid;

        //noinspection unchecked
        return (T) this;
    }
}