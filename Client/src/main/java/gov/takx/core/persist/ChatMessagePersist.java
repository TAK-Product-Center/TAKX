package gov.takx.core.persist;

import gov.tak.api.chat.IChatMessage;
import gov.takx.api.persist.IChatMessagePersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.core.chat.ChatMessage;
import gov.takx.core.chat.IPersistableChatMessage;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Provides {@link IChatMessage} persist services.
 * 
 * @since 3.4
 */
@ApplicationScoped
public class ChatMessagePersist implements IChatMessagePersist
{
    private static final String CHAT_MESSAGES_TABLE = ChatMessage.class.getSimpleName();
    private static final String CONVERSATION_UID_PARAM_NAME = "conversationUid";

    // query all messages in conversation
    private static final String QUERY_CONVERSATION_MESSAGES =
            "FROM " + CHAT_MESSAGES_TABLE + " WHERE conversationUid=:" + CONVERSATION_UID_PARAM_NAME;

    // delete all messages in conversation
    private static final String DELETE_CONVERSATION =
            "DELETE FROM " + CHAT_MESSAGES_TABLE + " WHERE conversationUid=:" + CONVERSATION_UID_PARAM_NAME;

    private final IPersistServiceProvider persistServiceProvider;

    @Inject
    public ChatMessagePersist(IPersistServiceProvider persistServiceProvider)
    {
        this.persistServiceProvider = persistServiceProvider;
    }

    @Override
    @Nonnull
    public List<IPersistableChatMessage> getConversationMessages(@Nonnull String conversationUid)
    {
        return persistServiceProvider.getPersistService()
                .query(QUERY_CONVERSATION_MESSAGES, new HQLParameter(CONVERSATION_UID_PARAM_NAME, conversationUid));
    }

    @Override
    public void persistMessage(@Nonnull IPersistableChatMessage message)
    {
        persistServiceProvider.getPersistService().persistEntity(message);
    }

    @Override
    public void deleteConversation(@Nonnull String conversationUid)
    {
        persistServiceProvider.getPersistService()
                .executeNativeUpdateSQL(DELETE_CONVERSATION, new HQLParameter(CONVERSATION_UID_PARAM_NAME, conversationUid));
    }
}