package gov.tak.api.chat;

import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @since 3.4
 */
public interface IChatMessageFinder
{
    /**
     * @param conversationUid The conversation UID
     * @return All messages associated with the conversation
     */
    @Nonnull
    List<? extends IChatMessage> getConversationMessages(@Nonnull String conversationUid);
}