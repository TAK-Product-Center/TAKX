package gov.tak.core.comms.commo;

import com.atakmap.commoncommo.Contact;
import com.atakmap.commoncommo.ContactPresenceListener;
import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.comms.commo.IContactManager;
import gov.takx.api.event.StartBackgroundProjectServices;
import gov.takx.api.event.StopProjectServices;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.Typed;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static gov.takx.core.project.ServiceOrdering.START_BACKGROUND_GenericService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Implementation of {@link IContactManager} service. This manager exists exclusively to track commo {@link Contact}s.
 *
 * @since 3.0
 */
@ApplicationScoped
@Typed({ContactManager.class, IContactManager.class})
public class ContactManager implements IContactManager, ContactPresenceListener
{
    private final Map<String, Contact> uidToContact = new ConcurrentHashMap<>();

    @Override
    public void contactAdded(@Nonnull Contact contact)
    {
        uidToContact.put(contact.contactUID, contact);
    }

    @Override
    public void contactRemoved(@Nonnull Contact contact)
    {
        uidToContact.remove(contact.contactUID);
    }

    @Nonnull
    @Override
    public Collection<Contact> getAvailableContacts()
    {
        return Collections.unmodifiableCollection(uidToContact.values());
    }

    @Nullable
    @Override
    public Contact findContactByUid(@Nonnull String uid)
    {
        return uidToContact.get(uid);
    }

    /**
     * Start this service.
     */
    private void startService(@Observes @Priority(START_BACKGROUND_GenericService) StartBackgroundProjectServices ignored,
                              ICommonCommoManager commoManager)
    {
        commoManager.addContactPresenceListener(this);
    }

    /**
     * Stop this service, tear down all resources.
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event,
                             ICommonCommoManager commoManager)
    {
        commoManager.removeContactPresenceListener(this);

        uidToContact.clear();
    }
}
