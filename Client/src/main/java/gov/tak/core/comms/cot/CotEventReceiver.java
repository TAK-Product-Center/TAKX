package gov.tak.core.comms.cot;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.Uninterruptibles;
import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.comms.commo.ICotEventListener;
import gov.tak.api.cot.ICotEventReceiver;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ImportResult;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.globe.IExtendedGlobeManager;
import gov.takx.commons.file.FileUtils;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

import static gov.takx.core.project.ServiceOrdering.START_GenericCoreService;
import static gov.takx.core.project.ServiceOrdering.STOP_GenericCoreService;

/**
 * Listener to dispatch all Cursor on Target Events received over Common Commo.
 * <p>
 * The Common Commo library generates new threads (apparently one per message) to deliver messages to listeners.
 * If we allow those threads to directly process the incoming message, it hammers the persistence subsystem,
 * and leads to DB connection starvation.
 * <p>
 * So, instead of direct processing, we simply drop the messages on a queue and then have a single background
 * thread pull and process them.  That minimizes persistence overhead since only a single thread is now performing
 * the operations.
 *
 * @since 3.0
 */
@ApplicationScoped
public class CotEventReceiver implements ICotEventReceiver, ICotEventListener
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Set of UIDs we ignore
    private static final Set<String> OUR_UIDS = new HashSet<>();

    private final CotImporterManager cotImporterManager;
    private final String ourUid;
    private volatile boolean processing;

    private EventConsumer eventConsumer; // Thread to consume and process queued events

    // Use an unbounded queue
    private final LinkedBlockingDeque<QueueEntry> eventQueue = new LinkedBlockingDeque<>();

    private final CompletableFuture<ImportResult> ignoredFuture = CompletableFuture.completedFuture(ImportResult.IGNORE);

    @Inject
    CotEventReceiver(@NodeUUID UUID nodeUuid, CotImporterManager cotImporterManager)
    {
        this.cotImporterManager = cotImporterManager;
        ourUid = nodeUuid.toString();

        // Special UID types to filter out
        OUR_UIDS.add(ourUid + ".SPI"); // Sensor Point of Interest
        OUR_UIDS.add(ourUid + ".PLRF"); // Pocket Laser Range Finder

        logger.debug("our UIDs: {}", OUR_UIDS);
    }

    /**
     * Start this service.
     */
    private void startService(@Observes @Priority(START_GenericCoreService) StartProjectServices event,
                              ICommonCommoManager commoManager,
                              IExtendedGlobeManager globeManager)
    {
        // Can't process CoT messages until the Globe service is ready
        globeManager.getPrimaryGlobeDelegate().doWhenReady(this::startProcessing);

        commoManager.addCotEventListener(this);
    }

    /**
     * Stop this service, tear down all resources.
     */
    private void stopService(@Observes @Priority(STOP_GenericCoreService) StopProjectServices event,
                             ICommonCommoManager commoManager)
    {
        commoManager.removeCotEventListener(this);

        processing = false;
        eventQueue.clear();

        if (eventConsumer != null)
        {
            eventConsumer.interrupt();
            Uninterruptibles.joinUninterruptibly(eventConsumer, Duration.ofSeconds(1));
            eventConsumer = null;
        }
    }

    /**
     * Start processing messages.
     */
    private void startProcessing()
    {
        processing = true;

        eventConsumer = new EventConsumer();
        eventConsumer.start();
    }

    /**
     * @return the number of events in the queue
     */
    @VisibleForTesting
    int getEventQueueSize()
    {
        return eventQueue.size();
    }

    @Override
    public CompletableFuture<ImportResult> cotEventReceived(CotEvent event)
    {
        logger.debug("Event: p={} : {}", processing, event);

        final String uid = event.getUID();

        // Filter out messages with an empty UID or any of "our" UIDs
        if (StringUtils.isEmpty(uid) || ourUid.equals(uid) || OUR_UIDS.stream().anyMatch(uid::startsWith))
        {
            if (logger.isDebugEnabled()) logger.debug("Skip event type='{}' uid='{}'", event.getType(), uid);
            return ignoredFuture;
        } else
        {
            // Since we use the UID as the name of a directory (for attachments in VFS), we need to sanitize it so that
            // no form of file path manipulation attack can access arbitrary locations on this computer.
            event.setUID(FileUtils.sanitizeForFilename(event.getUID()));

            final QueueEntry queueEntry = new QueueEntry(event);

            if (!eventQueue.offer(queueEntry))
            {
                logger.warn("Dropped event: {}", event);
                queueEntry.future.completeExceptionally(new IllegalStateException("CoT event queue is full"));
            }

            return queueEntry.future;
        }
    }

    /**
     * Thread to pull CoT events off the queue and process them.
     */
    private class EventConsumer extends Thread
    {
        @Override
        public void run()
        {
            currentThread().setName("CoT Event Dispatcher");

            while (processing)
            {
                QueueEntry queueEntry = null;
                try
                {
                    queueEntry = eventQueue.take();
                    queueEntry.future.complete(cotImporterManager.importEvent(queueEntry.event));

                } catch (InterruptedException ignore)
                {
                    // Expected
                } catch (Throwable e)
                {
                    if (queueEntry != null)
                    {
                        logger.error("Failed to process CoT event: {}", queueEntry.event, e);

                        queueEntry.future.completeExceptionally(e);
                    }
                }
            }
        }
    }

    /**
     * Simple holder for the CoT event and the future for its processing.
     *
     * @since 3.1
     */
    private static final class QueueEntry
    {
        private final CotEvent event;
        private final CompletableFuture<ImportResult> future = new CompletableFuture<>();

        private QueueEntry(CotEvent event)
        {
            this.event = event;
        }
    }
}
