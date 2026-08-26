package gov.takx.core.persist;

import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.persist.IRdmBatchQueue;
import gov.takx.api.persist.ISmartTrackPersist;
import gov.takx.api.query.param.HQLParameter;
import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.IExtendedSmartTrackManager;
import gov.takx.api.tracks.ITrackStyleDefinition;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.utils.TimeBatchedQueue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Persistence helper methods for working with smart track objects.
 *
 */
@ApplicationScoped
public class SmartTrackPersist implements ISmartTrackPersist
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int TSD_BATCH_HANDLER_DELAY = 250; // 0.25 seconds
    private static final int TSD_BATCH_HANDLER_MAX_WAIT = 1000; // 1 second
    private final TsdAssignmentQueue tsdAssignmentQueue = new TsdAssignmentQueue(); // Batch queue for TSD assignments

    // Determine if we are batching TSD Assignment operations (tie it to RDM Batching)
    @Inject
    @ProgramPreference(PreferenceEntry.RDM_BATCHING)
    private boolean doTSDBatch;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Inject
    private IRdmBatchQueue rdmBatchQueue;

    @Inject
    private IExtendedSmartTrackManager smartTrackManager;

    @Override
    public ITrackStyleDefinition getTrackStyleDefinition(int id)
    {
        String hql = "from TrackStyleDefinition " +
                " where id = :id";

        return persistServiceProvider.getPersistService().queryUnique(hql, new HQLParameter("id", id));
    }

    @Override
    public void persistTrackStyleDefinition(ITrackStyleDefinition trackStyleDefinition)
    {
        persistServiceProvider.getPersistService().persistEntity(trackStyleDefinition);
    }

    @Nonnull
    @Override
    public List<ITrackStyleDefinition> queryAllTrackStyleDefinitions()
    {
        String hql = "from TrackStyleDefinition";

        List<ITrackStyleDefinition> results = persistServiceProvider.getPersistService().query(hql);

        if (logger.isDebugEnabled())
        {
            logger.debug("Query: {}", hql);
            logger.debug("    results size: {}", results.size());
        }

        return results;
    }

    @Nonnull
    @Override
    public List<ITrackStyleDefinition> queryTrackStylesFamily(@Nonnull String family)
    {
        String hql = "from TrackStyleDefinition where family=:family";

        List<ITrackStyleDefinition> results = persistServiceProvider.getPersistService().query(hql,
                new HQLParameter("family", family));

        if (logger.isDebugEnabled())
        {
            logger.debug("Query: {}", hql);
            logger.debug("    results size: {}", results.size());
        }

        return results;
    }

    @Override
    public ITrackStyleDefinition queryTrackStyleByUniqueKey(@Nonnull String uniqueKey)
    {
        String hql = "from TrackStyleDefinition " +
                " where uniqueKey = :uniqueKey";

        return persistServiceProvider.getPersistService().queryUnique(hql,
                new HQLParameter("uniqueKey", uniqueKey));
    }

    @Override
    public void deleteTrackStyleDefinition(ITrackStyleDefinition trackStyleDefinition)
    {
        persistServiceProvider.getPersistService().deleteEntity(trackStyleDefinition);
    }

    @Override
    public void persistConnectingLineDefinition(IConnectingLineDefinition connectingLineDefinition)
    {
        persistServiceProvider.getPersistService().persistEntity(connectingLineDefinition);
    }

    @Nonnull
    @Override
    public List<IConnectingLineDefinition> queryAllConnectingLineDefinitions()
    {
        String hql = "from ConnectingLineDefinition";

        List<IConnectingLineDefinition> results = persistServiceProvider.getPersistService().query(hql);

        if (logger.isDebugEnabled())
        {
            logger.debug("Query: {}", hql);
            logger.debug("    results size: {}", results.size());
        }

        return results;
    }

    @Nonnull
    @Override
    public List<IConnectingLineDefinition> queryConnectingLinesByFamily(@Nonnull String family)
    {
        String hql = "from ConnectingLineDefinition where family=:family";

        List<IConnectingLineDefinition> results = persistServiceProvider.getPersistService().query(hql,
                new HQLParameter("family", family));

        if (logger.isDebugEnabled())
        {
            logger.debug("Query: {}", hql);
            logger.debug("    results size: {}", results.size());
        }

        return results;
    }

    @Override
    public IConnectingLineDefinition queryConnectingLineByUniqueKey(@Nonnull String uniqueKey)
    {
        String hql = "from ConnectingLineDefinition " +
                " where uniqueKey = :uniqueKey";

        return persistServiceProvider.getPersistService().queryUnique(hql,
                new HQLParameter("uniqueKey", uniqueKey));
    }

    @Override
    public void deleteConnectingLineDefinition(IConnectingLineDefinition connectingLineDefinition)
    {
        persistServiceProvider.getPersistService().deleteEntity(connectingLineDefinition);
    }

    @Override
    public void updateRDMTSD(int rdmId, Integer tsdId)
    {
        if (doTSDBatch)
        {
            // If the RDM is waiting in a batch, then we're done since the RDM has already been updated (which is why
            // this method was called).  Otherwise, it is already persisted, and we need to perform a SQL update.

            rdmBatchQueue.performUnderBatchQueueOperationsLock(() -> {
                if (!rdmBatchQueue.isRdmIdInBatch(rdmId))
                {
                    // Ok, it wasn't in the batch, so queue it for SQL update
                    tsdAssignmentQueue.add(new TsdAssignment(rdmId, tsdId));
                }
            });
        } else
        {
            // Do it immediately
            applyTsdIdBatch(tsdId, Collections.singletonList(rdmId));
        }
    }

    @Override
    public void batchUpdateRDMTSD(Integer tsdId, @Nonnull Collection<Integer> rdmIds)
    {
        // Need to find any RDMs that are not already persisted and handle them separately.
        // Taking care to lock the batch persistence operations while we do these tests.

        rdmBatchQueue.performUnderBatchQueueOperationsLock(() -> {
            final List<IRaptorDataMessage> unsavedInBatch = rdmBatchQueue.getUnsavedRDMsInBatch();
            final Map<Integer, IRaptorDataMessage> unsavedMsgMap = new HashMap<>(unsavedInBatch.size());
            final List<Integer> idsForSQL = new ArrayList<>(unsavedInBatch.size());

            if (unsavedInBatch.isEmpty())
            {
                applyTsdIdBatch(tsdId, rdmIds); // Optimize for empty case, all given rdmIds will be used
            } else
            {
                // In almost all cases, the number of messages in the batch is smaller than the number being assigned
                // in the batch.  So, we optimize for that case and create a map from Id to RDM in the unsaved set.

                for (IRaptorDataMessage rdm : unsavedInBatch)
                {
                    unsavedMsgMap.put(rdm.getId(), rdm);
                }

                // Now go through and update the unsaved ones directly in memory.  The others (already persisted),
                // we'll need to handle via a SQL operation.

                for (int rdmId : rdmIds)
                {
                    final IRaptorDataMessage rdm = unsavedMsgMap.get(rdmId);

                    if (rdm != null)
                    {
                        // Since it's unsaved, just update in memory
                        rdm.setTrackStyleDefinitionId(tsdId); // Need new API method
                    } else
                    {
                        idsForSQL.add(rdmId);
                    }
                }

                applyTsdIdBatch(tsdId, idsForSQL);
            }
        });
    }

    /**
     * Set trackStyleId of a collection of RDMs by id.
     *
     * @param tsdId  TSD Id to assign to RDMs, may be {@code null}
     * @param rdmIds Collection of RDM Ids to apply the TSD Id to
     * @since 1.48
     */
    private void applyTsdIdBatch(@Nullable Integer tsdId, @Nonnull Collection<Integer> rdmIds)
    {
        final IPersistService persistService = persistServiceProvider.getPersistService();

        final String hql = "update MapPoint set trackStyleId = " + (tsdId == null ? "NULL" : tsdId) + " where id IN ";

        processInBatches(rdmIds, ids -> {
            final String finalSql = hql + "(" + StringUtils.join(ids) + ")"; // Add IN clause to end
            persistService.executeNativeUpdateSQL(finalSql); // Execute the SQL for this batch
        });
    }

    @Override
    public void updateOverrideRDMTSD(int rdmId, Integer tsdId)
    {
        String hql = "update MapPoint set overrideTrackStyleId = " + (tsdId == null ? "NULL" : tsdId) + " where id = " + rdmId;
        persistServiceProvider.getPersistService().executeNativeUpdateSQL(hql);
    }

    @Override
    public ITrackStyleDefinition getActiveTSDforRDM(Integer rdmId)
    {
        IPersistService ps = persistServiceProvider.getPersistService();

        // Start by getting the two possible TSD ids from the RDM
        String hql = "select rdm.trackStyleId, rdm.overrideTrackStyleId from RaptorDataMessage rdm where rdm.id = :rdmId";
        Object[] tsdIds = ps.queryUniqueFromRdm(hql, new HQLParameter("rdmId", rdmId));

        Integer tsdId = null;

        if (tsdIds != null)
        {
            Integer permTsdId = (Integer) tsdIds[0];
            Integer overrideTsdId = (Integer) tsdIds[1];

            tsdId = overrideTsdId != null ? overrideTsdId : permTsdId;
        }

        // And return the proper instance - use our shared instances, not a fresh one from persistence
        return (tsdId != null) ? smartTrackManager.getTrackStyleDefinitionByID(tsdId) : null;
    }

    @Override
    public ITrackStyleDefinition getSharedTrackStyleDefinition(Integer tsdId)
    {
        return smartTrackManager.getTrackStyleDefinitionByID(tsdId);
    }

    @Override
    public List<Object[]> queryRDMTSDMapObjectIds()
    {
        String hql = "select distinct rdm.mapObject_id, rdm.trackStyleId" + " from RaptorDataMessage rdm";

        List<Object[]> results = persistServiceProvider.getPersistService().queryFromRdm(hql);

        if (logger.isDebugEnabled())
        {
            logger.debug("Query: {}", hql);
            logger.debug("    results size: {}", results.size());
        }

        return results;
    }

    @Override
    public List<Object[]> queryRDMTSDAssignments(@Nonnull List<Integer> rdmIds)
    {
        final IPersistService persistService = persistServiceProvider.getPersistService();

        final String hql = "select rdm.id, rdm.mapObject_id, rdm.trackStyleId, rdm.overrideTrackStyleId" +
                " from RaptorDataMessage rdm" +
                " where rdm.id IN (:rdmIds)";

        final List<Object[]> results = new ArrayList<>(rdmIds.size());

        processInBatches(rdmIds, ids -> results.addAll(persistService.queryFromRdm(hql, new HQLParameter("rdmIds", ids))));

        return results;
    }

    /**
     * When performing SQL operations that may contain large IN clauses, it is best to process them in batches to
     * avoid performance issues.
     * <p>
     * We can't just fire off a select with N thousand ids in the "in" clause - it performs
     * like crap.  So, we batch them up.  More selects with smaller in clauses is much faster.
     *
     * @param rdmIds       Collection of RDM Ids to process
     * @param batchHandler The handler to process the batch of ids
     * @since 2.37
     */
    private void processInBatches(@Nonnull Collection<Integer> rdmIds, @Nonnull Consumer<Collection<Integer>> batchHandler)
    {
        final int size = rdmIds.size();
        final int BATCH_SIZE = 500;
        final List<Integer> ids = new ArrayList<>(BATCH_SIZE);
        int index = 0;
        final Iterator<Integer> iterator = rdmIds.iterator();

        while (index < size)
        {
            ids.clear();

            int batchEnd = index + BATCH_SIZE < size ? BATCH_SIZE : size - index;

            for (int i = 0; i < batchEnd; i++)
            {
                ids.add(iterator.next());
            }

            batchHandler.accept(ids);

            index += batchEnd;
        }
    }

    /**
     * Timed queue to handle TSD Assignment batch persist operations.
     *
     * @since 1.48
     */
    private final class TsdAssignmentQueue extends TimeBatchedQueue<List<TsdAssignment>, TsdAssignment>
    {
        TsdAssignmentQueue()
        {
            super("TSD Batch Handler", TSD_BATCH_HANDLER_DELAY, TSD_BATCH_HANDLER_MAX_WAIT, ArrayList::new);
        }

        @Override
        protected void runBatch(List<TsdAssignment> batch)
        {
            // Convert the collection of individual assignments into a set of batch update operations, one
            // per unique TSD Id.
            final Map<Integer, List<Integer>> tsdMap = new HashMap<>();

            for (TsdAssignment assignment : batch)
            {
                // Create on demand
                List<Integer> rdmIdList = tsdMap.computeIfAbsent(assignment.tsdId, k -> new ArrayList<>());

                rdmIdList.add(assignment.rdmId);
            }

            // Now, go back through the map and process each entry as a batch update operation
            for (Map.Entry<Integer, List<Integer>> entry : tsdMap.entrySet())
            {
                applyTsdIdBatch(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Simple class to track TSD assignments.
     */
    private static final class TsdAssignment
    {
        final int rdmId;
        final Integer tsdId;

        TsdAssignment(int rdmId, Integer tsdId)
        {
            this.rdmId = rdmId;
            this.tsdId = tsdId;
        }

        @Override
        public String toString()
        {
            return "TsdAssignment{" +
                    "rdmId=" + rdmId +
                    ", tsdId=" + tsdId +
                    '}';
        }
    }
}
