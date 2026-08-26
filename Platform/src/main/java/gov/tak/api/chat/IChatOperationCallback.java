package gov.tak.api.chat;

import jakarta.annotation.Nonnull;

/**
 * Callback interface used by the chat service to inform implementing classes of a chat service operation's status.
 *
 * @since 3.2
 */
@FunctionalInterface
public interface IChatOperationCallback
{

    /**
     * Called by the chat service after an operation has been attempted.
     *
     * @param chatOperationResult The result of the operation attempt
     */
    void operationAttempted(@Nonnull ChatOperationResult chatOperationResult);

}
