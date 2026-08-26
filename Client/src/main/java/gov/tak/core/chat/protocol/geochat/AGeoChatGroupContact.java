package gov.tak.core.chat.protocol.geochat;

import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.contact.GroupContactBase;
import gov.tak.platform.contact.IndividualContactBuilder;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * Base class for a GeoChat group contact (i.e. a non-user-owned group contact).
 *
 * @since 3.0
 */
public abstract class AGeoChatGroupContact extends GroupContactBase
{
    private static final AttributeSet ATTRIBUTES = GeoChatContactProvider.createDefaultGeoChatContactAttributeSet();

    /**
     * Constructor for group contacts that don't need to add the client as a group member
     *
     * @param uid The group's UID
     * @param displayName The group's display name
     * @since 3.5
     */
    protected AGeoChatGroupContact(@Nonnull String uid, @Nonnull String displayName)
    {
        this(uid, displayName, null, null);
    }

    /**
     * @param uid The group's UID
     * @param displayName The group's display name
     * @param clientUid The UID of the client
     * @param clientDisplayName The display name of the client
     */
    protected AGeoChatGroupContact(@Nonnull String uid, @Nonnull String displayName, @Nullable String clientUid,
                                   @Nullable String clientDisplayName)
    {
        super(uid, displayName, ATTRIBUTES,
                clientUid != null ? Set.of(createClientContact(clientUid, clientDisplayName)) : Collections.emptySet(),
                null);
    }

    /**
     * Creates an individual contact representing the client as a contact.
     *
     * @param clientUid The client UID to use when creating the individual contact
     * @param clientName The client name to use when creating the individual contact
     * @return The created individual contact
     */
    private static IIndividualContact createClientContact(String clientUid, String clientName)
    {
        return new IndividualContactBuilder()
                .withUniqueContactId(clientUid)
                .withDisplayName(clientName)
                .withAttributes(new AttributeSet(ATTRIBUTES))
                .build();
    }
}
