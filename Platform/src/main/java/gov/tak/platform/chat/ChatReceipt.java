package gov.tak.platform.chat;

import gov.tak.api.contact.IContact;
import jakarta.annotation.Nonnull;

/**
 * Holds information about an incoming chat receipt message.
 *
 * @since 3.4
 */
public record ChatReceipt(@Nonnull IContact contact, @Nonnull String messageUid, @Nonnull String cotType)
{
}
