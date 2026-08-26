package gov.takx.core.chat;

import gov.tak.api.chat.IChatMessage;
import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * Provides utility methods helpful when working with {@link gov.tak.api.chat.IChatMessage}.
 *
 * @since 5.2
 */
public class ChatMessageUtils
{
    /**
     * @param message The message to inspect
     * @return The message's {@link ChatLink ChatLinks}, or an empty list if there are no links or if links are not 
     * supported
     */
    @Nonnull
    public static List<ChatLink> getChatLinks(@Nonnull IChatMessage message)
    {
        return message instanceof ChatMessage chatMessage ? chatMessage.getChatLinks() : Collections.emptyList();
    }

    /**
     * @param message The message to inspect
     * @return {@code true} if the message contains {@link ChatLink ChatLinks}, {@code false} otherwise
     */
    public static boolean hasChatLinks(@Nonnull IChatMessage message)
    {
        return !getChatLinks(message).isEmpty();
    }

    private ChatMessageUtils()
    {
    }
}
