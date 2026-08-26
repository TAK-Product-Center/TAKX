package gov.takx.api.persist;

import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IPersistableGroupContact;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.List;

/**
 * Responsible for reading and writing {@link IGroupContact}s to and from the database.
 *
 * @since 3.2
 */
public interface IGroupContactPersist
{
    /**
     * Persists a group contact in the database.
     *
     * @param groupContact The group contact to persist
     */
    void persistGroupContact(@Nonnull IGroupContact groupContact);

    /**
     * Queries the database to see if a group contact with the given unique ID exists.
     *
     * @param uniqueId The UID of the contact to query
     * @return {@code true} if a group contact with the given id exists, {@code false} if not
     */
    boolean groupContactExists(@Nonnull String uniqueId);

    /**
     * Queries the database for the group contact with the given unique ID.
     *
     * @param uniqueId The UID of the contact to query
     * @return The queried group contact, {@code null} if not found
     */
    @Nullable
    IPersistableGroupContact findGroupContact(@Nonnull String uniqueId);

    /**
     * Queries the database for all group contacts that have been persisted.
     *
     * @return A set containing all group contacts that have been persisted
     */
    @Nonnull
    List<IGroupContact> findAllGroupContacts();

    /**
     * Deletes a group contact from the database.
     *
     * @param contactUid The unique Id of the group contact to delete
     */
    void deleteGroupContact(@Nonnull String contactUid);
}
