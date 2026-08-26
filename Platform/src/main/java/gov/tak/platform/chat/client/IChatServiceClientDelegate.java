package gov.tak.platform.chat.client;

import gov.tak.api.chat.IChatOperationCallback;
import gov.tak.api.contact.IIndividualContact;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Delegate to be used by the {@link IChatServiceClient}. This contract should contain any methods needed by the client
 * that should not be exposed in {@link gov.tak.api.chat.IChatService}.
 *
 * @since 3.0 (moved from gov.tak.api in 3.4)
 */
public interface IChatServiceClientDelegate
{
    /**
     * @param contact The sender
     * @param messageUid The UID of the message being confirmed
     * @param read {@code true} if confirming that the message has been read
     * @param operationCallback Optional callback notified with send request result
     * @since 3.4
     */
    void sendReceipt(@Nonnull IIndividualContact contact, @Nonnull String messageUid, boolean read,
                     @Nullable IChatOperationCallback operationCallback);

    /**
     * This is the protocol provided by the host that is guaranteed to be supported by the chat service. It will take
     * priority over other plugin-supplied protocols when sending messages to contacts that can be reached via multiple
     * protocols.
     *
     * @return The default protocol
     * @since 3.4
     */
    String getDefaultProtocol();
}
