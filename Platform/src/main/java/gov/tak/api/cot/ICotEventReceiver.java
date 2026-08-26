package gov.tak.api.cot;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.NoPluginImplementations;

import java.util.concurrent.CompletableFuture;

/**
 * Contract for service that receives and processes CoT events.
 *
 * @since 3.1
 */
@InjectableService
@NoPluginImplementations
public interface ICotEventReceiver
{
    /**
     * Submit a CoT event for processing (import).  Handling is done asynchronously, so this method returns a
     * CompletableFuture to access the result once it is complete.
     *
     * @param event CoT Event to process
     * @return Future for obtaining the result
     */
    CompletableFuture<ImportResult> cotEventReceived(CotEvent event);
}
