package gov.takx.core.chat;

import gov.tak.api.chat.IChatMessage;
import gov.tak.platform.chat.AChatMessageBuilder;
import gov.takx.api.annotation.Internal;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Builds {@link IPersistableChatMessage}s.
 *
 * @since 3.0
 */
public class ChatMessageBuilder extends AChatMessageBuilder<ChatMessageBuilder>
{
    /**
     * Optional list of chat links. For internal use only.
     *
     * @since 5.2
     * @see ChatDataPackageService
     */
    @Internal
    private List<ChatLink> chatLinks;

    /**
     * @param senderUid The sender's UID
     * @param senderName The sender's name
     * @param text  The text to be used when building a chat message
     * @param sendTime The timestamp representing the send-time of the message, in milliseconds since epoch. Must be a
     *                 positive, non-zero {@code long}.
     * @param conversationUid The {@link gov.tak.api.contact.IGroupContact} UID for group conversations or the
     *                       individual UID for direct conversations.
     */
    public ChatMessageBuilder(@Nonnull String senderUid, @Nonnull String senderName, @Nonnull String text,
                              long sendTime, @Nonnull String conversationUid)
    {
        super(senderUid, senderName, text, sendTime, conversationUid);
    }

    /**
     * @param original The original message that will be used to initialize the builder values.
     */
    public ChatMessageBuilder(@Nonnull IChatMessage original)
    {
        super(original);
    }

    /**
     * Adds {@link ChatLink ChatLinks} to the chat message. These links describe the contents of a chat data package.
     * This feature is for internal use only.
     *
     * @param chatLinks The chat links describing a chat data package's contents
     * @return This builder
     * @since 5.2
     * @see ChatDataPackageService
     */
    @Internal
    public ChatMessageBuilder withLinks(@Nullable List<ChatLink> chatLinks)
    {
        this.chatLinks = chatLinks;
        return this;
    }

    @Override
    @Nonnull
    public IPersistableChatMessage build()
    {
        return new ChatMessage(text, sendTime, receiveTime, uid, senderUid, senderName, conversationUid, chatLinks);
    }
}
