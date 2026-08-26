package gov.tak.api.comms.commo;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;

import java.util.concurrent.CompletableFuture;

/**
 * Contract for listeners to receive notification of CoT Events received from Common Commo.
 *
 * @since 3.0
 */
public interface ICotEventListener
{
    /**
     * A CoT Event has been received.
     *
     * @param event CoT Event
     * @return CompletableFuture to obtain the import result once the event is processed
     */
    CompletableFuture<ImportResult> cotEventReceived(CotEvent event);
}
