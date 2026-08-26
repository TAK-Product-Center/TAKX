package gov.tak.platform.cot.importer;

import gov.tak.api.cot.ICotEventReceiver;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * A helper class for importing a series of {@link CotEvent} items, with two main modes of handling {@link ImportResult#DEFERRED deferred imports}.
 *
 * @see #setIncremental(boolean) For more information on handling deferred imports.
 * @since 5.0
 */
@Dependent
public class CotImportHelper
{
    private final List<CotEvent> deferredEvents = new CopyOnWriteArrayList<>();
    private final ICotEventReceiver cotEventReceiver;
    private Consumer<CotEvent> onImportSuccess;
    private Consumer<CotEvent> onImportFailure;
    private boolean incremental;

    @Inject
    public CotImportHelper(@Nonnull ICotEventReceiver cotEventReceiver)
    {
        this.cotEventReceiver = cotEventReceiver;
    }

    /**
     * By default, the helper will keep deferred events in a list. Those events will not get imported until you call {@link #importDeferredEvents()}.
     * This will trigger all of those events to be re-imported. In situations where you know how many events you have to import this default
     * is ideal as you should only have to call {@link #importDeferredEvents()} once. You can verify that there are no remaining events via {@link #hasDeferredEvents()}
     * returning {@code false}.
     * <p/>
     * Alternatively, if you do not know how many events you will be importing, you may want to enable incremental handling.
     * In this mode, after each {@link ImportResult#SUCCESS successful import} all prior deferred events will be checked again.
     *
     * @param incremental {@code true} to enable incremental handling of deferred events.
     */
    public void setIncremental(boolean incremental)
    {
        this.incremental = incremental;
    }

    /**
     * Sets a consumer for handling successful CoT event imports.
     *
     * @param onImportSuccess Consumer to act on valid events.
     */
    public void setOnImportSuccess(@Nullable Consumer<CotEvent> onImportSuccess)
    {
        this.onImportSuccess = onImportSuccess;
    }

    /**
     * Sets a consumer for handling failed CoT event imports.
     *
     * @param onImportFailure Consumer to act on failed events.
     */
    public void setOnImportFailure(@Nullable Consumer<CotEvent> onImportFailure)
    {
        this.onImportFailure = onImportFailure;
    }

    /**
     * Import a CoT message from text.
     *
     * @param cotMessage Text of CoT event to process.
     *
     * @return Import result indicating success/failure/deferred status of import.
     */
    @Nonnull
    public CompletableFuture<ImportResult> parseAndImportCot(@Nullable String cotMessage)
    {
        // Skip for empty input.
        if ((cotMessage == null || cotMessage.isEmpty())) return CompletableFuture.completedFuture(ImportResult.IGNORE);

        // Some CoT files contain a Byte-order-mark.
        //  EF BB BF    - UTF-8
        //  FF FE       - UTF-16, little endian
        //  FE FF       - UTF-16, big endian
        //  FF FE 00 00 - UTF-32, little endian
        //  00 00 FE FF - UTF-32, big-endian
        // We're going to assume that the string value contains parsable XML and the
        // header can be safely ignored.
        cotMessage = cotMessage.substring(Math.max(0, cotMessage.indexOf('<')));

        CotEvent event = CotEvent.parse(cotMessage);
        return importCot(event);
    }

    /**
     * Import a CoT message.
     *
     * @param event CoT event to process.
     *
     * @return Import result indicating success/failure/deferred status of import.
     */
    @Nonnull
    private CompletableFuture<ImportResult> importCot(@Nonnull CotEvent event)
    {
        if (event.isValid())
        {
            return cotEventReceiver.cotEventReceived(event).thenApply(result -> {
                switch (result)
                {
                    case SUCCESS ->
                    {
                        if (onImportSuccess != null) onImportSuccess.accept(event);
                        if (incremental) handleDeferredEvents(false);
                    }
                    case FAILURE ->
                    {
                        if (onImportFailure != null) onImportFailure.accept(event);
                    }
                    case DEFERRED -> deferredEvents.add(event);
                }
                return result;
            });
        }

        // Invalid events will be treated as failures.
        if (onImportFailure != null) onImportFailure.accept(event);
        return CompletableFuture.completedFuture(ImportResult.FAILURE);
    }

    /**
     * Import any remaining CoT messages that were {@link ImportResult#DEFERRED} on initial import.
     *
     * @return Future of all currently remaining deferred events being imported. The result of each import can be any value.
     * After completion, you can check {@link #hasDeferredEvents()} to see if there are any remaining values that were not successfully imported.
     */
    @Nonnull
    public CompletableFuture<Void> importDeferredEvents()
    {
        if (!incremental) return handleDeferredEvents(true);

        // If we are in incremental mode, this isn't supposed to be used.
        // We can supply a completed future in this case.
        return CompletableFuture.completedFuture(null);
    }

    /**
     * Deferred events are {@link CotEvent} items that when imported yielded {@link ImportResult#DEFERRED}.
     * Simply put, they could not be imported because they depend on another {@link CotEvent} that has not yet been imported.
     *
     * @return {@code true} when there are remaining deferred events which have not yet been imported.
     */
    public boolean hasDeferredEvents()
    {
        return !deferredEvents.isEmpty();
    }

    /**
     * Takes the snapshot of existing deferred event list and attempts to reimport each deferred event.
     *
     * @param untilCompletion {@code true} to continue re-iterating over deferred events until no more actionable items remain.
     *
     * @return Future of all currently remaining deferred events being imported. The result of each import can be any value.
     * After completion, you can check {@link #hasDeferredEvents()} to see if there are any remaining values that were not successfully imported.
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    private CompletableFuture<Void> handleDeferredEvents(boolean untilCompletion)
    {
        // Snapshot the current list and clear the original.
        // This way the re-importation won't duplicate items in the list.
        final List<CotEvent> copy = new ArrayList<>(deferredEvents);
        deferredEvents.clear();

        // Create a combined future detailing the completion of all event imports.
        final CompletableFuture<ImportResult>[] futures = new CompletableFuture[copy.size()];
        for (int i = 0; i < copy.size(); i++) futures[i] = importCot(copy.get(i));
        CompletableFuture<Void> allImportsFuture = CompletableFuture.allOf(futures);
        if (untilCompletion)
        {
            // When all imports are completed, see if we still have remaining events to handle.
            // If so, map the future to the another round of handling deferred events.
            allImportsFuture = allImportsFuture.thenApply(unused -> {
                // Count the number of deferred imports, and successes.
                int deferredCount = 0;
                int successCount = 0;
                for (CompletableFuture<ImportResult> future : futures)
                {
                    // All the futures are complete, so we can do 'getNow' without risks.
                    ImportResult result = future.getNow(null);
                    if (result == ImportResult.DEFERRED)
                    {
                        deferredCount++;
                    } else if (result == ImportResult.SUCCESS)
                    {
                        successCount++;
                    }
                }

                // If we had deferred events and other items were acted on, we may be able to import those deferred events
                // with another round of importing.
                if (deferredCount > 0 && successCount > 0) return handleDeferredEvents(true).join();

                // If there were no deferred event imports, or no successful event imports then there's
                // nothing to gain from another round of importing. So we will not continue.
                return null;
            });
        }
        return allImportsFuture;
    }
}
