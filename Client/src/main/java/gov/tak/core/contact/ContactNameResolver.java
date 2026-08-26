package gov.tak.core.contact;

import gov.tak.api.comms.takserver.IServerContactManager;
import gov.tak.api.comms.takserver.ITakServerConnection;
import gov.tak.api.comms.takserver.ITakServerConnectionManager;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactNameResolver;
import gov.tak.api.contact.IContactService;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Objects;
import java.util.UUID;

/**
 * Service implementation for contact name resolving.
 *
 * @since 3.5
 */
@ApplicationScoped
public class ContactNameResolver implements IContactNameResolver
{
    private static final String UNKNOWN = "Unknown";
    private final ITakServerConnectionManager serverConnectionManager;
    private final IServerContactManager serverContactManager;
    private final IContactService contactService;
    private final String nodeUuid;

    @Inject
    public ContactNameResolver(ITakServerConnectionManager serverConnectionManager,
                               IServerContactManager serverContactManager,
                               IContactService contactService,
                               @NodeUUID UUID nodeUuid)
    {
        this.serverConnectionManager = serverConnectionManager;
        this.serverContactManager = serverContactManager;
        this.contactService = contactService;
        this.nodeUuid = nodeUuid.toString();
    }

    @Nullable
    @Override
    public String resolveName(@Nullable String contactUid)
    {
        if (contactUid == null) return null;

        if (contactUid.equals(nodeUuid)) return PreferenceEntry.CALL_SIGN.getStringValue();

        IContact contact = contactService.getContact(contactUid);
        if (contact != null && !contactUid.equals(contact.getName())) return contact.getName();

        for (ITakServerConnection connection : serverConnectionManager.getTakServerConnections())
        {
            String callsign = serverContactManager.getContactCallsign(connection.getId(), contactUid);
            if (!contactUid.equals(callsign)) return callsign;
        }

        return contactUid;
    }

    @Nonnull
    @Override
    public String resolveNameOrElseUnknown(@Nullable String contactUid)
    {
        return resolveNameOrElse(contactUid, UNKNOWN);
    }

    @Nonnull
    @Override
    public String resolveNameOrElse(@Nullable String contactUid, @Nonnull String fallbackCallsign)
    {
        return Objects.requireNonNullElse(resolveName(contactUid), fallbackCallsign);
    }
}
