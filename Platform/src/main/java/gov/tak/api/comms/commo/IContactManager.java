package gov.tak.api.comms.commo;

import com.atakmap.commoncommo.Contact;
import gov.tak.api.contact.IContactNameResolver;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Collection;

/**
 * Contract for service that tracks {@link Contact Contacts} reported by Common Commo.
 *
 * @see ICommonCommoManager
 * @see IContactNameResolver#resolveName(String) Resolve names of contracts provided by this service.
 * @since 3.0
 */
@InjectableService
@NoPluginImplementations
public interface IContactManager
{
    /**
     * @return List of currently available contacts.
     *
     * @since 3.5
     */
    @Nonnull
    Collection<Contact> getAvailableContacts();

    /**
     * Find a contact by its UID.
     *
     * @param uid UID of contact to find
     * @return Contact or {@code null} if not found
     */
    @Nullable
    Contact findContactByUid(@Nonnull String uid);
}
