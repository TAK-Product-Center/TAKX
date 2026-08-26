package gov.takx.core.persist;

import gov.takx.api.annotation.PlatformUnderTest;
import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.mapobject.IDeletedMapObject;
import gov.takx.api.persist.IBackgroundDeleteManager;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.core.project.ServiceOrdering;
import gov.takx.dataservices.persist.DeletedMapObject;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * This class manages the background delete operations that need to re-start each time a project is opened, until
 * they finally complete.
 * <p>
 * Note, don't try to cache a reference to the PersistService here since we are accessed from multiple threads and
 * we need to be sure to use the PS belonging to the executing thread at all times.
 *
 * @since 1.34
 */
@ApplicationScoped
public class BackgroundDeleteManager implements IBackgroundDeleteManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final int WORKER_COUNT = 2; // Number of worker threads to spawn

    private final List<Worker> workers = new ArrayList<>();
    private final BlockingQueue<BackgroundDeleter> workQueue = new LinkedBlockingQueue<>();
    private final Set<String> mapObjectKeysPendingDelete = ConcurrentHashMap.newKeySet();

    private volatile boolean running = true;

    private final boolean platformUnderTest;
    private final IPersistServiceProvider persistServiceProvider;
    private final ICoreExecutorService executorService;

    @Inject
    BackgroundDeleteManager(IPersistServiceProvider persistServiceProvider, ICoreExecutorService executorService,
                            @PlatformUnderTest boolean platformUnderTest)
    {
        this.persistServiceProvider = persistServiceProvider;
        this.executorService = executorService;
        this.platformUnderTest = platformUnderTest;
    }

    /**
     * Start our service.
     */
    private void startService(@Observes @Priority(ServiceOrdering.START_GenericCoreService) StartProjectServices ignored)
    {
        running = true; // mark the manager to start running

        // If we are under test, don't bother to crank up the worker threads since we're never going to queue work

        if (!platformUnderTest)
        {
            startWorkers();
            executorService.submit(this::resumeWork);
        }
    }

    /**
     * Stop our service.
     */
    private void stopService(@Observes @Priority(ServiceOrdering.STOP_GenericCoreService) StopProjectServices ignored)
    {
        suspendWork();
    }

    @Override
    public void add(IDeletedMapObject dmo)
    {
        if (!running) return;

        logger.debug("Add: {}", dmo);

        queueWork(dmo);
    }

    @Override
    public void suspendWork()
    {
        logger.debug("Suspending work");

        running = false; // mark the manager to stop running

        stopWorkers();

        workQueue.clear(); // And clear the queue of any pending work
    }

    @Override
    public void waitForDeletion(String mapObjectKey)
    {
        // Keep the happy path lock free
        if (!mapObjectKeysPendingDelete.contains(mapObjectKey)) return;

        boolean interrupted = false;
        try
        {
            synchronized (mapObjectKeysPendingDelete)
            {
                while (mapObjectKeysPendingDelete.contains(mapObjectKey))
                {
                    try
                    {
                        mapObjectKeysPendingDelete.wait();
                    } catch (InterruptedException e)
                    {
                        interrupted = true;
                    }
                }
            }
        } finally
        {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    /**
     * Queue the handling of an in progress delete.
     *
     * @param dmo deletion to work
     */
    private void queueWork(IDeletedMapObject dmo)
    {
        logger.debug("Queue work: {}", dmo);

        mapObjectKeysPendingDelete.add(dmo.getMapObjectKey());

        final BackgroundDeleter deleter = new BackgroundDeleter(dmo);

        // For now, we don't want to actually crank up any background deleter threads while we're running
        // the unit tests.  So, perform the deletion immediately.

        if (platformUnderTest)
        {
            runDeleter(deleter);
        } else
        {
            workQueue.add(deleter);
        }
    }

    /**
     * Query any unfinished work and queue it up for completion.
     */
    private void resumeWork()
    {
        if (!running) return; // Quick shortcut if we've already been shutdown (like in unit tests)

        Thread.currentThread().setName("BackgroundDeleteManager.resumeWork");

        // We need to find all the background delete operations that have not completed, and restart them.
        final List<DeletedMapObject> deletionsInProgress =
                persistServiceProvider.getPersistService().query("from DeletedMapObject");

        if (logger.isDebugEnabled()) logger.debug("DIP count: {}", deletionsInProgress.size());

        deletionsInProgress.forEach(this::queueWork);   // Queue it up to keep working
    }

    /**
     * Start all our worker threads.
     */
    private void startWorkers()
    {
        for (int i = 0; i < WORKER_COUNT; i++)
        {
            final Worker worker = new Worker();
            workers.add(worker);
            worker.start();
        }
    }

    /**
     * Stop all our worker threads.
     */
    private void stopWorkers()
    {
        workers.forEach(Worker::stopWork);
        workers.clear();
    }

    /**
     * Run a deleter cleanly, reporting any errors.
     */
    private void runDeleter(BackgroundDeleter deleter)
    {
        try
        {
            deleter.delete();
        } catch (Exception e)
        {
            logger.error("Background deletion failed: {}", deleter);
        } finally
        {
            // Even in the face of a failure, we want to remove this key from the pending set.  If we didn't, and some
            // future thread tries to create a new MO with this same key, then it would block forever in waitForDeletion(),
            // when it is called by the MapObjectManager.

            synchronized (mapObjectKeysPendingDelete)
            {
                mapObjectKeysPendingDelete.remove(deleter.getMapObjectKey());
                mapObjectKeysPendingDelete.notifyAll();
            }
        }
    }

    /**
     * Worker thread to pull elements of the work queue and execute them.
     *
     * @since 2.28
     */
    private class Worker extends Thread
    {
        private volatile BackgroundDeleter deleter;

        @Override
        public void run()
        {
            while (running)
            {
                try
                {
                    setName("BackgroundDeleter: idle");

                    deleter = workQueue.take(); // Block until work becomes available
//                    final boolean empty = workQueue.peek() == null;

                    setName(deleter.toString());

                    runDeleter(deleter);

//                    if (empty && workQueue.peek() == null) logger.warn("DONE");
                } catch (InterruptedException e)
                {
                    // ignore
                } finally
                {
                    deleter = null;
                }
            }
        }

        /**
         * Stop our work.  If we are currently processing a deleter, then tell it to stop.
         */
        void stopWork()
        {
            if (deleter != null) deleter.stop();

            interrupt(); // And interrupt our wait
        }
    }
}
