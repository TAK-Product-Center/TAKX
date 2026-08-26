package gov.tak.core.chat.protocol.geochat;

import gov.tak.api.chat.IChatMessage;
import gov.tak.api.contact.IContact;
import jakarta.annotation.Nonnull;

/**
 * Simple wrapper encapsulating the contents of a parsed GeoChat message.
 *
 * @since 3.0
 */
class GeoChatParsedMessage
{
    private final IContact contact;
    private final IChatMessage message;

    /**
     * @param contact The contact that the GeoChat message is from
     * @param message The chat message content
     */
    GeoChatParsedMessage(@Nonnull IContact contact, @Nonnull IChatMessage message)
    {
        this.contact = contact;
        this.message = message;
    }

    /**
     * @return The contact that the parsed GeoChat message was sent from
     */
    public IContact getContact()
    {
        return contact;
    }

    /**
     * @return The parsed chat message
     */
    public IChatMessage getMessage()
    {
        return message;
    }
}
