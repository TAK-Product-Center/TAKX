package gov.takx.core.persist;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.Sets;
import com.koloboke.collect.set.IntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.persist.IRdmBatchQueue;
import gov.takx.dataservices.utils.DynamicStructureDBUtils;
import gov.takx.platform.utils.TimeBatchedQueue;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Timed batch queue to handle RDM persistence operations.
 *
 * @since 1.48
 */
@Singleton
public class RdmBatchQueue
        extends TimeBatchedQueue<RdmBatchQueue.LatchedBatch, RdmBatchQueue.RDMStorage>
        implements IRdmBatchQueue
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Allow for up to 1 second delay before processing the batch, but limit the size to 10,000 messages
    // so that massive data ingests (like file imports) don't create batches that are too big to handle efficiently.
    private static final int BATCH_INITIAL_DELAY = 250; // 0.25 seconds
    private static final int BATCH_MAX_WAIT = 1_000; // 1 second
    private static final int BATCH_MAX_SIZE = 10_000;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Inject
    private MessagePersistHelper messagePersistHelper;

    @Inject
    private RdmBatchExecutorService batchExecutorService;

    @Inject
    private DynamicStructureDBUtils dynamicStructureDBUtils;

    /**
     * Set of batches that are waiting to be persisted.
     */
    private final Set<LatchedBatch> pendingBatchSet = Collections.synchronizedSet(Sets.newIdentityHashSet());

    /**
     * Construct with configured timeouts and a max size.
     */
    RdmBatchQueue()
    {
        super("RDM Batch Persist", BATCH_INITIAL_DELAY, BATCH_MAX_WAIT, BATCH_MAX_SIZE, LatchedBatch::new);
    }

    /**
     * Overridden so we can use a special executor service just for processing RDM batches.
     *
     * @return Our RDM batch executor service
     */
    @Override
    protected ExecutorService getExecutorService()
    {
        return batchExecutorService;
    }

    /**
     * A new pending batch has been kicked off.  Add it to our set of pending batches that we know are not yet
     * in the DB.
     *
     * @param work pending batch
     * @since 2.18
     */
    @Override
    protected void onPendingBatch(LatchedBatch work)
    {
        pendingBatchSet.add(work);
    }

    /**
     * A pending batch has been persisted.  Remove it from our set of pending.
     *
     * @param work pending batch
     * @since 2.18
     */
    @Override
    protected void onBatchDone(LatchedBatch work)
    {
        pendingBatchSet.remove(work);
    }

    /**
     * Add an RDM to the queue.
     *
     * @param rdm     The RDM to add
     * @param unsaved Pass {@code true} if this RDM has never been persisted before
     */
    @Override
    public void addToQueue(IPrePersistRaptorDataMessage rdm, boolean unsaved)
    {
        rdm.setBatchLatch(batch.batchCompleteLatch);

        super.add(new RDMStorage(rdm, unsaved));
    }

    @Override
    public void performUnderBatchQueueOperationsLock(Runnable runnable)
    {
        lockBatch();
        try
        {
            runnable.run();
        } finally
        {
            unlockBatch();
        }
    }

    @Override
    public void waitOnPersistingBatches() throws InterruptedException
    {
        processNow(false); // Ensure any partial batch is queued

        LatchedBatch[] batches = pendingBatchSet.toArray(new LatchedBatch[0]);
        for (LatchedBatch batch : batches)
        {
            batch.batchCompleteLatch.await();
        }
    }

    /**
     * Overridden to update the outstanding work size when queued and when completed.
     * <p>
     * {@inheritDoc}
     *
     * @since 2.14
     */
    @Override
    protected void queueBatch(LatchedBatch work)
    {
        final int size = work.size();

        batchExecutorService.addRDMBatch(size);

        batchExecutorService.execute(() -> {
            runBatch(work);
            batchExecutorService.completeRDMBatch(size);
        });
    }

    @Override
    protected void setThreadName()
    {
        // Do nothing, the thread name generated by the thread pool is sufficient
    }

    @Override
    protected void runBatch(LatchedBatch batch)
    {
        final boolean debugEnabled = logger.isDebugEnabled();

        if (debugEnabled) logger.debug("Run RDM batch: size = {}", batch.size());

        try
        {
            final long start = debugEnabled ? System.nanoTime() : 0;

            if (Thread.interrupted()) return;

            persistBatch(batch);

            if (debugEnabled)
            {
                final long duration = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
                final double avg = duration / (double) batch.size();
                logger.debug(String.format("Persist batch of %d took %d ms, avg =%.2f", batch.size(), duration, avg));
            }
        } finally
        {
            batch.release(); // Now release our latch indicating that the batch is complete
            onBatchDone(batch);    // need to call this implicitly because we kick off batches from here.
        }
    }

    @Override
    public boolean isRdmIdInBatch(int rdmId)
    {
        return batch.containsId(rdmId);
    }

    @Override
    public List<IRaptorDataMessage> getUnsavedRDMsInBatch()
    {
        final List<IRaptorDataMessage> unsaved = new ArrayList<>(batch.size());

        batch.stream().filter(s -> s.unsaved).map(s -> s.message).forEach(unsaved::add);

        return unsaved;
    }

    /**
     * Save (insert or update) all the RDMs in a batch to persistence.
     *
     * @param batch Batch to process
     */
    private void persistBatch(LatchedBatch batch)
    {
        final IPersistService persistService = persistServiceProvider.getPersistService();
        final boolean supportsBulkInsertion = persistService.databaseSupportsBulkInsertion();

        if (Thread.interrupted()) return;

        // If the database supports bulk insertion, then use that method instead of our normal JDBC batching
        if (supportsBulkInsertion)
        {
            performBulkBatchInsert(batch, persistService);
        } else
        {
            performNormalBatchInsert(batch, persistService);
        }

        // If there are updates in the batch, then perform them
        if (!batch.updateStorage.isEmpty())
        {
            persistService.doWork(connection -> {
                // Perform an UPDATE on an existing messages
                try (PreparedStatement stmt = connection.prepareStatement(MessagePersistHelper.RDM_UPDATE_SQL))
                {
                    batch.getUpdates().forEach(rdm -> {
                        try
                        {
                            messagePersistHelper.bindUpdateRDMParams(stmt, rdm);
                            stmt.addBatch();
                        } catch (SQLException e)
                        {
                            logger.error("Failed to update RDM in batch: {}", rdm, e);

                            // We can't throw a checked exception here, so convert it to a RuntimeException
                            throw new RuntimeException("RDM Batch Failed", e);
                        }
                    });

                    stmt.executeBatch();
                    connection.commit();
                } catch (Exception e)
                {
                    logger.error("Failed to persist RDM batch", e);
                    connection.rollback();
                }
            });
        }
    }

    /**
     * Insert the given batch of RDMs into the DB using the bulk insert method.
     *
     * @param batch          Batch to process
     * @param persistService Persist service to use
     * @since 2.14
     */
    private void performBulkBatchInsert(LatchedBatch batch, IPersistService persistService)
    {
        try
        {
            // First, do all the core data (to the MapPoint table)
            persistService.bulkInsertRDMs(batch.getInsertions());

            // Then collection all the RDMs according to their structure
            final ArrayListMultimap<IStructure, IPrePersistRaptorDataMessage> rdmsByStructure = ArrayListMultimap.create();
            batch.getInsertions().forEach(rdm -> rdmsByStructure.put(rdm.getStructure(), rdm));

            // And go through and bulk persist each structure's RDMs
            for (IStructure structure : rdmsByStructure.keySet())
            {
                // Make sure the extended data table has been created before using it
                dynamicStructureDBUtils.ensureMappedInDb(structure);

                persistService.bulkInsertRDMsExtendedData(structure, rdmsByStructure.get(structure));
            }
        } catch (Exception e)
        {
            logger.error("Failed to perform BULK insert of RDMs", e);
        }
    }

    /**
     * Perform a "normal" JDBC batch insertion of the RDMs.
     *
     * @param batch          Batch to process
     * @param persistService Persist service to use
     * @since 2.14 (signature changed in 2.38)
     */
    private void performNormalBatchInsert(LatchedBatch batch, IPersistService persistService)
    {
        final ArrayListMultimap<IStructure, ? extends IRaptorDataMessage> rdmsByStructure = dynamicStructureDBUtils.collectRdmsByStructure(batch.getInsertions());

        final boolean success = persistService.doReturningWork(connection -> {
            // Perform an INSERT on each RDM
            try (PreparedStatement stmt = connection.prepareStatement(MessagePersistHelper.RDM_INSERT_SQL))
            {
                for (IRaptorDataMessage rdm : rdmsByStructure.values())
                {
                    try
                    {
                        messagePersistHelper.bindInsertRDMParams(stmt, rdm);
                        stmt.addBatch();
                    } catch (SQLException e)
                    {
                        logger.error("Failed to insert RDM in batch: {}", rdm);
                        throw e;
                    }
                }

                stmt.executeBatch();
                connection.commit();
                return true;
            } catch (Exception e)
            {
                logger.error("RDM Batch Failed", e);
                connection.rollback();
                return false;
            }
        });

        // And then all the extended data for those same RDMs
        if (success)
        {
            dynamicStructureDBUtils.batchPersistExtendedData(rdmsByStructure);
        }
    }

    /**
     * Holder class to track RDM objects that need to be persisted.
     * <p/>
     * The hashCode and equals methods are strictly based on the RDM Id.  First, we only need one record of a
     * persistence request for an RDM.  Second, we need a way to quickly determine if an RDM Id is in the batch.
     */
    public static final class RDMStorage
    {
        final int rdmId;
        final long uuidMsb;
        final long uuidLsb;
        final IPrePersistRaptorDataMessage message;
        final boolean unsaved;

        public RDMStorage(IPrePersistRaptorDataMessage message, boolean unsaved)
        {
            this.message = message;
            rdmId = message.getIdAsInt();
            uuidMsb = message.getUuidMsb();
            uuidLsb = message.getUuidLsb();
            this.unsaved = unsaved;
        }

        /**
         * Equality is strictly driven by RDM Id.
         */
        @Override
        public boolean equals(Object o)
        {
            return this == o || !(o == null || getClass() != o.getClass()) && rdmId == ((RDMStorage) o).rdmId;
        }

        /**
         * @return rdmId
         */
        @Override
        public int hashCode()
        {
            return rdmId;
        }
    }

    /**
     * Batch collection that:
     * <ul>
     * <li>includes a CountDownLatch to be used for external synchronization on the persistence of the batch contents</li>
     * <li>Provides easy inclusion testing of both RDM Id and UUID</li>
     * <p/>
     * The latch allows external code to determine when the batch containing an RDM has been persisted.
     * The latch is created and initialized to 1 at construction.  Later, when the
     * batch is processed, it is released.  Any consuming code that wants to wait until the batch containing
     * an RDM is persisted just needs to park in a call to {@link java.util.concurrent.CountDownLatch#await()}.
     * This latch is set on each RDM as it is added to the batch, and the latch is shared by all RDMs in the batch.
     *
     * @since 1.49
     */
    @PseudoBean
    protected static final class LatchedBatch extends AbstractCollection<RDMStorage>
    {
        private final CountDownLatch batchCompleteLatch = new CountDownLatch(1); // Simple gate
        private final List<RDMStorage> insertStorage = new ArrayList<>((int) (BATCH_MAX_SIZE * 1.1)); // Avoid any runtime expansion
        private final List<RDMStorage> updateStorage = new ArrayList<>((BATCH_MAX_SIZE / 10)); // Expect a lot fewer updates
        private final IntSet ids = HashIntSets.newUpdatableSet(BATCH_MAX_SIZE);

        /**
         * Release all waiting threads.  Since out latch was initialized to 1, a single call to countDown() will
         * release anyone waiting on the latch.
         */
        void release()
        {
            batchCompleteLatch.countDown(); // release the hounds

            // And dump our internal storage
            insertStorage.forEach(storage -> storage.message.setBatchLatch(null));
            insertStorage.clear();

            updateStorage.forEach(storage -> storage.message.setBatchLatch(null));
            updateStorage.clear();

            ids.clear();
        }

        private boolean containsId(int rdmId)
        {
            return ids.contains(rdmId);
        }

        /**
         * @return An iterable over the RDMs that need to be inserted (are unsaved)
         */
        public Iterable<IPrePersistRaptorDataMessage> getInsertions()
        {
            return () -> new RdmIterator(insertStorage);
        }

        /**
         * @return An iterable over the RDMs that need to be updated (have been persisted previously)
         */
        public Iterable<IPrePersistRaptorDataMessage> getUpdates()
        {
            return () -> new RdmIterator(updateStorage);
        }

        @Override
        public Iterator<RDMStorage> iterator()
        {
            return Iterators.concat(insertStorage.iterator(), updateStorage.iterator());
        }

        @Override
        public int size()
        {
            return insertStorage.size() + updateStorage.size();
        }

        @Override
        public boolean add(RDMStorage rdmStorage)
        {
            final boolean changed = ids.add(rdmStorage.rdmId);

            if (rdmStorage.unsaved)
            {
                insertStorage.add(rdmStorage);
            } else
            {
                updateStorage.add(rdmStorage);
            }

            return changed;
        }

        /**
         * Custom iterator implementation to return the RDM contained within an RDMStorage object.
         */
        private static class RdmIterator implements Iterator<IPrePersistRaptorDataMessage>
        {
            final List<RDMStorage> storage;
            final int size;
            int index = 0;

            public RdmIterator(List<RDMStorage> storage)
            {
                this.storage = storage;
                size = storage.size();
            }

            @Override
            public boolean hasNext()
            {
                return index < size;
            }

            @Override
            public IPrePersistRaptorDataMessage next()
            {
                if (index >= size) throw new NoSuchElementException();

                return storage.get(index++).message;
            }
        }
    }
}
