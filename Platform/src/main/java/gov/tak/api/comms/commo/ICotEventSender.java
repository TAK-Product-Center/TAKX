package gov.tak.api.comms.commo;

import gov.tak.api.cot.event.CotEvent;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.Collection;

/**
 * Contract for service that sends {@link CotEvent} messages to configured end points.
 *
 * @since 3.0
 */
@InjectableService
@NoPluginImplementations
public interface ICotEventSender
{
    /**
     * Broadcast the given CoT Event to all connected endpoints.
     *
     * @param event Event to send
     */
    void broadcastEvent(@Nonnull CotEvent event);

    /**
     * Send the given CoT Event to the specified contacts.
     *
     * @param event       Event to send
     * @param contactUids UIDs of Contacts to which to send the message
     * @throws IllegalArgumentException if the {@code contactUids} list is empty
     * @since 3.5 (signature change)
     */
    void sendEvent(@Nonnull CotEvent event, @Nonnull Collection<String> contactUids);
}
