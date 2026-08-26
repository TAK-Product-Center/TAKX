package gov.takx.core.persist;

import com.koloboke.collect.IntCursor;
import com.koloboke.collect.set.IntSet;
import com.koloboke.collect.set.hash.HashIntSets;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IDeletedMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.persist.IPersistService;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.persist.IPluginStatePersist;
import gov.takx.platform.preferences.PreferenceEntry;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.function.Consumer;

/**
 * This class is responsible for handling the deletion of all the artifacts associated with a Map Object in the
 * background and to ensure it completes regardless of a project being closed and then opened any number of times.
 * <p/>
 * The {@link BackgroundDeleteManager} is responsible for recreating instances of this class to carry on any
 * incomplete work from a previous project session.
 *
 * @see BackgroundDeleteManager
 * @since 1.34
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
class BackgroundDeleter
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final int dmoId;
    private final int mapObjectId;
    private final String mapObjectKey;
    private final int trackStyleDefinitionId;
    private final int connectingLineDefinitionId;
    private final boolean partialDelete;
    private final long timeOfDeletion;
    private final int mapEntityId;

    private IPersistService ps;
    private IntSet rdmIds;

    private volatile boolean running = true;

    @Inject
    private IPluginStatePersist pluginStatePersist;

    @Inject
    private IMapObjectPersist mapObjectPersist;

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Inject
    @ProgramPreference(PreferenceEntry.ENABLE_EVENT_LOG)
    private boolean eventLogEnabled;

    /**
     * The stages of our deletion process.
     *
     * @since 2.38
     */
    private enum DeletionStage
    {
        LoadMessageIds(BackgroundDeleter::loadMessageIds, true),
        TsdCld(BackgroundDeleter::handleTsdAndCld, false),
        EventLog(BackgroundDeleter::handleEventLogs, false),
        ExtendedData(BackgroundDeleter::handleExtendedData, true),
        Messages(BackgroundDeleter::handleMessages, true),
        PluginState(BackgroundDeleter::handlePluginState, false);

        private final Consumer<BackgroundDeleter> action;
        private final boolean doIfPartialDelete;

        /**
         * @param action            Action (method) to invoke when processing this stage
         * @param doIfPartialDelete Pass {@code true} if this action should be performed even on partial deletes
         */
        DeletionStage(Consumer<BackgroundDeleter> action, boolean doIfPartialDelete)
        {
            this.action = action;
            this.doIfPartialDelete = doIfPartialDelete;
        }
    }

    /**
     * Construct a BG deleter on the given DeletedMapObject.
     *
     * @param dmo DeletedMapObject record to work on
     */
    BackgroundDeleter(IDeletedMapObject dmo)
    {
        // Since we will run on a different thread than the one that created the DMO, pull out all the data
        // to avoid having to re-query it during execution.

        dmoId = dmo.getId();
        mapObjectId = dmo.getMapObjectId();
        mapObjectKey = dmo.getMapObjectKey();
        trackStyleDefinitionId = dmo.getTrackStyleDefinitionId();
        connectingLineDefinitionId = dmo.getConnectingLineDefinitionId();
        partialDelete = dmo.isPartialDelete();
        timeOfDeletion = dmo.getTimeOfDeletion();
        mapEntityId = dmo.getMapEntity().getId();
    }

    /**
     * @return Key of MO we're deleting
     * @since 2.36
     */
    String getMapObjectKey()
    {
        return mapObjectKey;
    }

    /**
     * Perform the delete operations.
     */
    protected void delete()
    {
        if (!running) return; // In case we are stopped before we really get started

        try
        {
            // Now we can cache the PS reference since we are now on the real execution thread.
            ps = persistServiceProvider.getPersistService();

            // Iterate the deletion stages and perform the associated action

            for (DeletionStage stage : DeletionStage.values())
            {
                if (!running) return;

                if (!partialDelete || stage.doIfPartialDelete)
                {
                    stage.action.accept(this);
                }
            }

            // Work is complete, so we can delete the DMO
            logger.debug("Work is done, deleting DMO");
            ps.executeNativeUpdateSQL("DELETE from DeletedMapObject where id = " + dmoId);
        } catch (Exception e)
        {
            // If we are no longer "running" then the exception is likely due to the session or persist service
            // being pulled out from under us.  So, only report exceptions when running == true

            if (running)
            {
                // Report exception, then rethrow it
                logger.error("Failed to delete MapObject data", e);
                throw e;
            }
        }
    }

    /**
     * Tell this task to stop.
     */
    void stop()
    {
        running = false;
        logger.debug("Stopped");
    }

    /**
     * Query all the Ids of all the RDMs associated with this MO.
     */
    private void loadMessageIds()
    {
        rdmIds = HashIntSets.newImmutableSet(getIds("MapPoint"));
    }

    /**
     * Get the list of ids to be deleted on the given table.  If this is a partial delete, then we select only
     * those messages that are prior to the timeOfDeletion.
     *
     * @param tableName Name of table to process
     * @return List of ids of rows to be deleted
     * @since 2.14
     */
    private List<Integer> getIds(String tableName)
    {
        // If it is a partial delete, then restrict the ids to those received at or before the timeOfDeletion
        final String where = partialDelete
                ? "mapobject_id = " + mapObjectId + " and receiveTime <= " + timeOfDeletion
                : "mapobject_id = " + mapObjectId;

        final String sql = "select id from " + tableName + " where " + where;

        return ps.querySQL(sql);
    }

    /**
     * Delete the TSD and CLD for the MO if they are no longer referenced.
     *
     * @since 2.28
     */
    private void handleTsdAndCld()
    {
        deleteIfNotReferencedFromMapObject("trackStyleDefinition_id", trackStyleDefinitionId, "TrackStyleDefinition");
        deleteIfNotReferencedFromMapObject("connectingLineDefinition_id", connectingLineDefinitionId, "ConnectingLineDefinition");
    }

    /**
     * Delete the given entity row if the count of rows in IconMapObject where the specified column is
     * equal to the given id is 0 (i.e., where the entity is not referenced).
     *
     * @param column Column to count (on IconMapObject)
     * @param id     Id of Object whose references are to be counted and deleted if 0
     * @param table  The table from which to delete the entity if no longer referenced
     * @since 2.38 (originally 2.29, signature change)
     */
    private void deleteIfNotReferencedFromMapObject(String column, int id, String table)
    {
        if (id <= 0) return; // Nothing to do

        final long count = ps.countSQL("MapObject", column + " = " + id);

        if (count == 0)
        {
            ps.executeNativeUpdateSQL("delete from " + table + " where id = " + id);
        }
    }

    /**
     * Delete all the event log entries for this MO.
     */
    private void handleEventLogs()
    {
        if (!eventLogEnabled) return; // Don't erase event log entries if the event log is disabled

        ps.executeNativeUpdateSQL("delete from RaptorEventLog where mapObjectId = " + mapObjectId);
    }

    /**
     * Delete any plugin state associated with this MO.
     */
    private void handlePluginState()
    {
        pluginStatePersist.deleteState(mapObjectKey);
    }

    /**
     * Delete all the extended data for all the RDMs associated with this MapObject.
     */
    private void handleExtendedData()
    {
        // Loop through each structure and delete the extended data in that table
        final IMapEntity mapEntity = mapObjectPersist.findMapEntityById(mapEntityId);

        for (IStructure struct : mapEntity.getDataStructures())
        {
            if (!running) return;

            final String tableName = struct.getExtendedDataClassName();

            // If this structure was never used, the table might not exist
            if (ps.tableExists(tableName))
            {
                final String sql = "delete from " + tableName + " where mappoint_id in (%s)";
                logger.debug("Extended: {}", sql);

                new SqlBatcher(rdmIds, sql).runBatches();
            }
        }
    }

    /**
     * Delete all the RDMs associated with this MapObject.
     */
    private void handleMessages()
    {
        new SqlBatcher(rdmIds, "delete from MapPoint where id in (%s)").runBatches();
    }

    @Override
    public String toString()
    {
        return "BackgroundDeleter: " + mapObjectKey;
    }

    /**
     * Our operations can be very long running if we tried to process all the message related objects at one time.
     * So, we batch them up and check for cancellation in between each batch.  Our batches are constructed from
     * a supplied SQL statement and message Ids from a master bit set of all the messages to process.
     */
    private final class SqlBatcher
    {
        private static final int BATCH_SZ = 250;

        private final IntCursor idCursor;
        private final String sql;

        private final StringBuilder idBuffer = new StringBuilder();

        /**
         * Construct a batcher on the given SQL, which must include a single %s for substitution of the batch of
         * message ids.
         *
         * @param ids The ids to process
         * @param sql SQL statement
         */
        SqlBatcher(IntSet ids, String sql)
        {
            idCursor = ids.cursor();
            this.sql = sql;
        }

        /**
         * Run the SQL in batches.
         */
        void runBatches()
        {
            String idBatch;

            while (running && (idBatch = nextBatch()) != null)
            {
                final String batchSql = String.format(sql, idBatch);

                logger.debug("Batch SQL: {}", batchSql);

                ps.executeNativeUpdateSQL(batchSql);
            }
        }

        /**
         * Fill a batch of message ids from the id cursor.
         *
         * @return {@code null} if we're done, otherwise a string containing a comma-separated list of the message Ids
         */
        private String nextBatch()
        {
            final StringBuilder idBuffer = this.idBuffer; // Local variables for faster access
            final IntCursor idCursor = this.idCursor;

            idBuffer.setLength(0); // Clear it out
            int count = 0;

            while (count < BATCH_SZ && idCursor.moveNext())
            {
                if (count > 0) idBuffer.append(',');
                idBuffer.append(idCursor.elem());

                count += 1;
            }

            return idBuffer.isEmpty() ? null : idBuffer.toString();
        }
    }
}
