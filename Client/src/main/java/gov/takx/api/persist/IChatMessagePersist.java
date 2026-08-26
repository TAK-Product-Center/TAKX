package gov.takx.api.persist;

import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.IChatMessageFinder;
import gov.takx.core.chat.IPersistableChatMessage;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Provides {@link IChatMessage} persist services.
 *
 * @since 3.4
 */
public interface IChatMessagePersist extends IChatMessageFinder
{
    @Override
    List<IPersistableChatMessage> getConversationMessages(String conversationUid);

    /**
     * @param message The message to persist
     */
    void persistMessage(@Nonnull IPersistableChatMessage message);

    /**
     * @param conversationUid The UID of the conversation to delete
     */
    void deleteConversation(@Nonnull String conversationUid);
}