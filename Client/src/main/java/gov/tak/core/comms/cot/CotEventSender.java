package gov.tak.core.comms.cot;

import com.atakmap.commoncommo.CoTSendMethod;
import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.Contact;
import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.comms.commo.IContactManager;
import gov.tak.api.comms.commo.ICotEventSender;
import gov.tak.api.cot.event.CotEvent;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.Vector;

/**
 * Implementation of  {@link ICotEventSender}.
 *
 * @since 3.0
 */
@ApplicationScoped
public class CotEventSender implements ICotEventSender
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final ICommonCommoManager commoManager;
    private final IContactManager contactManager;

    @Inject
    CotEventSender(ICommonCommoManager commoManager, IContactManager contactManager)
    {
        this.commoManager = commoManager;
        this.contactManager = contactManager;
    }

    @Override
    public void broadcastEvent(@Nonnull CotEvent event)
    {
        try
        {
            commoManager.broadcastCoT(event.toString());
        } catch (CommoException e)
        {
            logger.error("Error sending CoT Event: {}", event, e);
        }
    }

    @Override
    public void sendEvent(@Nonnull CotEvent event, @Nonnull Collection<String> contactUids)
    {
        if (contactUids.isEmpty()) throw new IllegalArgumentException("'contactUids' must not be empty");

        if (logger.isDebugEnabled()) logger.debug("Send to {}: {}", contactUids, event);

        final Vector<Contact> contacts = convertUidsToContacts(contactUids);

        if (!contacts.isEmpty())
        {
            try
            {
                commoManager.sendCoT(contacts, event.toString(), CoTSendMethod.ANY);
            } catch (CommoException e)
            {
                logger.error("Error sending CoT Event: {}", event, e);
            }
        } else
        {
            logger.warn("No Contacts found for: {}", contactUids);
        }
    }

    /**
     * Convert a list of UIDs to a Vector of Contacts.
     *
     * @param contactUids UIDs to process
     * @return Vector of contacts
     */
    private Vector<Contact> convertUidsToContacts(Collection<String> contactUids)
    {
        final Vector<Contact> contacts = new Vector<>(contactUids.size());

        for (String uid : contactUids)
        {
            final Contact contact = contactManager.findContactByUid(uid);

            if (contact != null)
            {
                contacts.add(contact);
            } else
            {
                logger.error("Unknown contact {}", uid, new RuntimeException());
            }
        }

        return contacts;
    }
}
