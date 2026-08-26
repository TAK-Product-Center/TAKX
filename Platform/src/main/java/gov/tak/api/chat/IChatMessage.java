package gov.tak.api.chat;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

/**
 * Represents a chat message with message content and timestamps indicating send and receive times.
 *
 * @since 3.0
 */
@IncubatingApi(since = "3.0")
public interface IChatMessage
{
    /**
     * @return The message text
     */
    @Nonnull
    String getText();

    /**
     * @return A timestamp indicating the time the message was sent, in milliseconds since epoch (i.e. UTC)
     */
    long getSendTime();

    /**
     * @return A timestamp indicating the time the message was received, in milliseconds since epoch (i.e. UTC), or
     * a negative number if no valid receive time has been set
     */
    long getReceiveTime();

    /**
     * @return The unique ID of a given message
     */
    @Nonnull
    String getUid();

    /**
     * @return The sender's UID
     */
    @Nonnull
    String getSenderUid();

    /**
     * @return The name of the message sender at the time the message was sent
     */
    @Nonnull
    String getSenderName();

    /**
     * @return The conversation UID. This will be the group contact UID for group conversations or the remote user's
     * contact UID for direct conversations
     * @since 3.4
     */
    @Nonnull
    String getConversationUid();

    /**
     * @return The status for sent messages, {@code null} for received messages.
     * @since 3.4
     */
    @Nonnull
    ChatMessageStatus getStatus();

    /**
     * @return {@code true} if this message was sent by the local user or {@code false} if it was received from another
     * contact.
     * @since 3.4
     */
    boolean sentByLocalContact();

    /**
     * @param status The message status
     * @since 3.4
     */
    void setStatus(@Nonnull ChatMessageStatus status);
}
