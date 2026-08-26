package gov.takx.core.view.gui.maps.terraindiscovery.controller.calculator;

import gov.takx.core.view.gui.maps.terraindiscovery.controller.ITerrainDiscoveryController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 */
public class TerrainDiscoveryThreadPool
{
    private final ITerrainDiscoveryController controller;
    private final ExecutorService executor;
    private final AtomicInteger taskCount;
    private final AtomicInteger tasksComplete;
    private final AtomicInteger batchID;

    public TerrainDiscoveryThreadPool(ITerrainDiscoveryController controller)
    {
        this.controller = controller;
        int numThreads = Math.min(3, Runtime.getRuntime().availableProcessors());
        executor = Executors.newFixedThreadPool(numThreads);
        batchID = new AtomicInteger(0);
        taskCount = new AtomicInteger(0);
        tasksComplete = new AtomicInteger(0);
    }

    /**
     * Submit a list of tasks to complete.  Cancels any currently running tasks and clears the progress bar before
     * starting.
     */
    public synchronized List<Future> submitTasks(List<TerrainDiscoveryTask> tasks)
    {
        clear();
        taskCount.set(0);
        tasksComplete.set(0);
        controller.setProgressStarted();
        taskCount.addAndGet(tasks.size());
        int id = batchID.get();
        List<Future> futures = new ArrayList<>();
        for (TerrainDiscoveryTask task : tasks)
        {
            task.setOwner(this, id);
            futures.add(executor.submit(task));
        }
        return futures;
    }

    /**
     * The the fraction of complete tasks (a value between 0 and 1).
     */
    public double getProgress()
    {
        return tasksComplete.get() / ((double) taskCount.get());
    }

    /**
     * Clear any currently running tasks and hide the progress bar.
     */
    public synchronized void clear()
    {
        // All currently scheduled tasks are canceled when the batch ID is changed.
        batchID.incrementAndGet();
        controller.setProgressStopped();
    }

    /**
     * Shut this thread pool down.  Once this is called, it can no longer be used.
     */
    public void dispose()
    {
        clear();
        executor.shutdownNow();
    }

    public synchronized void setProgressText(String text)
    {
        controller.setProgressText(text);
    }

    public synchronized void incrementTasksComplete(int id)
    {
        if (id == batchID.get())
        {
            int complete = tasksComplete.incrementAndGet();

            if (complete % 20 == 0)
            {
                controller.setProgress(getProgress());
            }

            if (complete >= taskCount.get())
            {
                controller.setProgressStopped();
                controller.calculationComplete();
            }
        }
    }

    public int getBatchID()
    {
        return batchID.get();
    }
}
