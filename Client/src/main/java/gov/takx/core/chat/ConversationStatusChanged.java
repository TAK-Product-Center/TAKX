package gov.takx.core.chat;

import gov.tak.api.chat.ChatMessageStatus;

/**
 * Fired when the status of a conversation changes.
 *
 * @see ChatMessageStatus
 * @since 3.4
 */
public record ConversationStatusChanged(Conversation conversation)
{
}