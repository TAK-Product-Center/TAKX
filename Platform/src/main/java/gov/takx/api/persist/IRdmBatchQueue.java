package gov.takx.api.persist;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;

import java.util.List;

/**
 * Contract for RDM Batch queue service.
 *
 * @since 2.24
 */
@InjectableService
@NoPluginImplementations
public interface IRdmBatchQueue
{
    /**
     * Add an RDM to the queue. For internal use only.
     *
     * @param rdm     The RDM to add
     * @param unsaved Pass {@code true} if this RDM has never been persisted before
     */
    @Internal
    void addToQueue(IPrePersistRaptorDataMessage rdm, boolean unsaved);

    /**
     * Execute the given logic under the auspices of a lock on the batch queue.  This prevents any additions or
     * processing of the current batch queue.
     * <p>
     * Use this method with care, and avoid any long running operations.
     *
     * @param runnable Runnable to execute
     */
    void performUnderBatchQueueOperationsLock(Runnable runnable);

    /**
     * Wait on all outstanding batches to be fully persisted.  This is a blocking operation.  If there
     * is no batch currently being processed, this method will just pass through.
     *
     * @throws InterruptedException interruption to the wait operation
     */
    void waitOnPersistingBatches() throws InterruptedException;

    /**
     * Determine if the given RDM Id is in the batch of RDMs awaiting persistence.
     * <p/>
     * In order to obtain consistent results, you should have acquired the batch lock, using
     * {@link #performUnderBatchQueueOperationsLock(Runnable)} prior to calling this method.
     *
     * @param rdmId ID of RDM of interest
     * @return {@code true} If the RDM is in the batch of pending persistence operations, {@code false} otherwise
     * @see #performUnderBatchQueueOperationsLock(Runnable)
     */
    boolean isRdmIdInBatch(int rdmId);

    /**
     * Get a list of all the unsaved RDMs that are currently in the pending batch.  This may be empty if none are
     * waiting, or if persistence batching is not enabled.  Only RDM instances that have yet to be persisted for the
     * first time are returned, previously persisted RDMs are excluded.
     * <p/>
     * In order to obtain consistent results, you should have acquired the batch lock, using
     * {@link #performUnderBatchQueueOperationsLock(Runnable)} prior to calling this method.
     *
     * @return List of RDMs awaiting persistence
     * @see #performUnderBatchQueueOperationsLock(Runnable)
     */
    List<IRaptorDataMessage> getUnsavedRDMsInBatch();
}
