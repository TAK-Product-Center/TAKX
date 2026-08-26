package gov.tak.api.contact;

import gov.tak.api.comms.commo.IContactManager;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Contract of a service capable of resolving a contact by its uid to its callsign, if known.
 *
 * @see IContactManager#getAvailableContacts() List all available contacts.
 * @since 3.5
 */
@InjectableService
@NoPluginImplementations
public interface IContactNameResolver
{
    /**
     * @param contactUid Contact identifier to resolve.
     *
     * @return Resolved name/callsign of contact, if known. Otherwise {@code null}.
     */
    @Nullable
    String resolveName(@Nullable String contactUid);

    /**
     * @param contactUid Contact identifier to resolve.
     *
     * @return Resolved name/callsign of contact, if known. Otherwise {@code "Unknown"}.
     *
     * @since 5.0
     */
    @Nonnull
    String resolveNameOrElseUnknown(@Nullable String contactUid);

    /**
     * @param contactUid Contact identifier to resolve.
     * @param fallbackCallsign Fallback callsign to use if the contact name cannot be resolved.
     *
     * @return Resolved name/callsign of contact, if known. Otherwise, the provided fallback callsign.
     *
     * @since 5.0
     */
    @Nonnull
    String resolveNameOrElse(@Nullable String contactUid, @Nonnull String fallbackCallsign);
}
