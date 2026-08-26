package gov.takx.platform.utils;

import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NodeUUID;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Offers utility methods for working with {@link IContact}s and {@link IGroupContact}s.
 *
 * @since 3.5
 */
@InjectableService
@ApplicationScoped
public class ContactUtils
{
    private final String clientUid;

    @Inject
    ContactUtils(@NodeUUID UUID nodeUuid)
    {
        clientUid = nodeUuid.toString();
    }

    /**
     * Provides a subset of the group members that will optionally include the client and/or group contacts.
     *
     * @param groupContact The group to process
     * @param includeClient {@code true} if the client {@link IContact} should be included
     * @param includeGroups {@code true} if groups should be included
     * @return The filtered set of group members.
     * @since 3.5
     */
    public Set<IContact> getGroupMembers(@Nonnull IGroupContact groupContact, boolean includeClient,
                                         boolean includeGroups)
    {
        return groupContact.getGroupMembers().stream()
                .filter(contact -> (includeGroups || contact instanceof IIndividualContact)
                        && (includeClient || !contact.getUid().equals(clientUid)))
                .collect(Collectors.toSet());
    }
}