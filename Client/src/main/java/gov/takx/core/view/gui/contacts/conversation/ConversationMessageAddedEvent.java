package gov.takx.core.view.gui.contacts.conversation;

import gov.takx.core.chat.Conversation;
import jakarta.annotation.Nonnull;

/**
 * Event fired by a {@link Conversation} when a new {@link gov.tak.api.chat.IChatMessage} is added.
 *
 * @param conversation The conversation that was updated
 * @since 5.2
 */
public record ConversationMessageAddedEvent(@Nonnull Conversation conversation)
{
}
