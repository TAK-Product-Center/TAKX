package gov.takx.core.persist;

import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.core.chat.Conversation;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;

/**
 * Handles {@link Conversation} persistence operations.
 *
 * @since 5.2
 */
@ApplicationScoped
public class ConversationPersist
{
    private final IPersistServiceProvider persistServiceProvider;

    @Inject
    ConversationPersist(IPersistServiceProvider persistServiceProvider)
    {
        this.persistServiceProvider = persistServiceProvider;
    }

    /**
     * @return All persisted conversations
     */
    public List<Conversation> queryAll()
    {
        return persistServiceProvider.getPersistService().query("from Conversation");
    }

    /**
     * @param conversation The conversation to persist
     */
    public void persist(@Nonnull Conversation conversation)
    {
        persistServiceProvider.getPersistService().persistEntity(conversation);
    }
}
