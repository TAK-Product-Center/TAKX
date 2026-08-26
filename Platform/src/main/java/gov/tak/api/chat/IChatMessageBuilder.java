package gov.tak.api.chat;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

/**
 * Builder used to create new chat messages.
 *
 * @since 3.4
 */
@IncubatingApi(since = "3.0")
public interface IChatMessageBuilder
{
    /**
     * @param text The message text
     * @return This ChatMessageBuilder instance
     */
    @Nonnull
    IChatMessageBuilder withText(@Nonnull String text);

    /**
     * @param receiveTime The timestamp representing the receive-time of the message. Must be a positive, non-zero {@code long}.
     * @return This ChatMessageBuilder instance
     * @throws IllegalArgumentException if the given receive time is less than or equal to zero
     * todo: should plugins be able to create "received" messages?
     */
    @Nonnull
    IChatMessageBuilder withReceiveTime(long receiveTime) throws IllegalArgumentException;

    /**
     * @param uid The message's unique id
     * @return This ChatMessageBuilder instance
     */
    @Nonnull
    IChatMessageBuilder withUid(@Nonnull String uid);

    /**
     * @return A new chat message instance.
     */
    @Nonnull
    IChatMessage build();
}