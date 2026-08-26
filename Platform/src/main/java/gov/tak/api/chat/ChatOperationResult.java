package gov.tak.api.chat;

import gov.tak.api.contact.IContact;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Set;

/**
 * Holds the result details of an attempted chat service operation.
 *
 * @see IChatOperationCallback
 * @since 3.2
 */
@SuppressWarnings("ClassCanBeRecord") // to be moved to takkernel
public final class ChatOperationResult
{
    /**
     * Represents a specific chat service operation
     */
    public enum Operation
    {
        SEND_MESSAGE,
        SEND_RECEIPT,
        CREATE_GROUP,
        UPDATE_GROUP,
        DELETE_GROUP
    }

    /**
     * Represents the result of a chat service operation attempt
     */
    public enum Status
    {
        SUCCESS,
        FAILURE
    }

    private final Operation operation;
    private final Status status;
    private final String message;
    private final IContact contact;

    /**
     * @param operation The operation that was attempted
     * @param status The result of the attempt
     * @param message A message explaining a failure, or {@code null} if the operation was a success.
     * @param contact The contact associated with the operation
     */
    public ChatOperationResult(@Nonnull Operation operation, @Nonnull Status status,
                               @Nullable String message, @Nullable IContact contact)
    {
        this.operation = operation;
        this.status = status;
        this.message = message;
        this.contact = contact;
    }

    /**
     * @return The operation that was attempted
     */
    @Nonnull
    public Operation getOperation()
    {
        return operation;
    }

    /**
     * @return The status of the attempted operation
     */
    @Nonnull
    public Status getStatus()
    {
        return status;
    }

    /**
     * @return A message explaining a failure, or {@code null} if the operation was a success.
     */
    @Nullable
    public String getMessage()
    {
        return message;
    }

    /**
     * @return The target individual or group {@link IContact}. May be {@code null}, e.g. if a call to
     * {@link gov.tak.api.contact.IGroupContactManager#createGroup(String, Set, gov.tak.api.contact.IGroupContact, String, IChatOperationCallback)}
     * fails and no contact is created.
     */
    @Nullable
    public IContact getContact()
    {
        return contact;
    }
}
