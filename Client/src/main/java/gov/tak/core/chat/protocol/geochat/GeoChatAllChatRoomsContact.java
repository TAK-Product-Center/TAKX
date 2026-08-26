package gov.tak.core.chat.protocol.geochat;

import jakarta.annotation.Nonnull;

/**
 * Default contact representing the "All Chat Rooms" group.
 *
 * @since 3.0
 */
public final class GeoChatAllChatRoomsContact extends AGeoChatGroupContact
{
    public static final String ALL_CHAT_ROOMS_GROUP_NAME = "All Chat Rooms";

    /**
     * @param clientUid The UID of the client
     * @param clientName The display name of the client
     */
    public GeoChatAllChatRoomsContact(@Nonnull String clientUid, @Nonnull String clientName)
    {
        super(ALL_CHAT_ROOMS_GROUP_NAME, ALL_CHAT_ROOMS_GROUP_NAME, clientUid, clientName);
    }
}
