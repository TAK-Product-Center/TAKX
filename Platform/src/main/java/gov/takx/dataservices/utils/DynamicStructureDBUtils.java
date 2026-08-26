package gov.takx.dataservices.utils;

import com.google.common.collect.ArrayListMultimap;
import com.koloboke.collect.map.IntObjMap;
import com.koloboke.collect.map.hash.HashIntObjMaps;
import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IDataType;
import gov.takx.api.messages.IExtendedDataStructure;
import gov.takx.api.messages.IIndexDefinition;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.commons.utils.StringUtils;
import gov.takx.dataservices.upgrade.ColumnDefinition;
import gov.takx.dataservices.upgrade.CreateIndexStatement;
import gov.takx.dataservices.upgrade.CreateTableStatement;
import gov.takx.dataservices.upgrade.UpgradeStatement;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLDataException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

/**
 * Shared utility methods for working with the Database and our storage of dynamic structure data.  This includes
 * methods to load and store the extended data for an RDM.
 *
 * @since 1.22
 */
@Internal
@ApplicationScoped
public class DynamicStructureDBUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int RDM_ID_BATCH_SZ = 1000; // How many ids to place in a batch

    // Track structures that have been mapped into the DB
    private static final BitSet structuresMappedInDatabase = new BitSet(512);

    private final DynamicStructures dynamicStructures;
    private final IPersistServiceProvider persistServiceProvider;

    @Inject
    DynamicStructureDBUtils(DynamicStructures dynamicStructures, IPersistServiceProvider persistServiceProvider)
    {
        this.dynamicStructures = dynamicStructures;
        this.persistServiceProvider = persistServiceProvider;
    }

    /**
     * Generate the list of statements required to create the table that will hold the data for a structure.
     *
     * @param structure Structure of interest
     * @return List of SQL statements to create the table
     */
    public List<UpgradeStatement> buildCreateStatement(IStructure structure)
    {
        final String tableName = structure.getExtendedDataClassName();

        logger.debug("Creating table {}", tableName);

        final List<UpgradeStatement> statementList = new ArrayList<>();

        final CreateTableStatement stmt = new CreateTableStatement(tableName);

        // Add a field for a key that relates to the MapPoint table. This is the primary key, no auto-generation for these tables
        stmt.addColumnDefinition(new ColumnDefinition("mappoint_id", IDataType.INT).setPrimary());

        for (IDataField f : structure.getFields())
        {
            stmt.addColumnDefinition(new ColumnDefinition(f.getNeutralizedFieldName(), f.getFieldType(), f.getFieldLength()));
        }
        statementList.add(stmt);

        // Address the custom indexes
        for (IIndexDefinition indexDefinition : structure.getIndexes())
        {
            CreateIndexStatement indexStatement = new CreateIndexStatement(tableName, null, indexDefinition.isUnique());

            for (String columnName : indexDefinition.getColumnNames())
            {
                indexStatement.addColumn(structure.getField(columnName).getNeutralizedFieldName());
            }
            statementList.add(indexStatement);
        }

        return statementList;
    }

    /**
     * Read the extended field values for a given message from persistence.
     *
     * @param session The DB session to use
     * @param rxm     The structured message to process
     * @return Extended data object for the structure defining the message
     */
    public IExtendedDataStructure loadExtendedData(Session session, final IRaptorMessage rxm)
    {
        final String sql = dynamicStructures.getSelectSql(rxm.getStructure()) + " WHERE mappoint_id = ?";

        // Run the query in the Hibernate session
        return session.doReturningWork(conn -> {
            try (PreparedStatement statement = prepareLoadStatement(conn, sql, rxm.getIdAsInt());
                 ResultSet results = statement.executeQuery())
            {
                final IExtendedDataStructure extendedData = results.next()
                        ? loadSingleExtendedData(rxm, results)
                        : rxm.getStructure().makeExtendedDataObject();

                conn.commit(); // Don't leave the connection hanging

                return extendedData;
            }
        });
    }

    private PreparedStatement prepareLoadStatement(Connection conn, String sql, int id) throws SQLException
    {
        final PreparedStatement statement = conn.prepareStatement(sql);
        statement.setInt(1, id);
        return statement;
    }

    /**
     * Load the extended data for a collection of RDMs in bulk.  This will minimize the round-trips to the database.
     *
     * @param session  DB Session to use
     * @param messages Messages for which to load the extended data
     */
    public void bulkLoadExtendedData(Session session, List<IRaptorDataMessage> messages)
    {
        final ArrayListMultimap<IStructure, ? extends IRaptorDataMessage> rdmsByStructure = collectRdmsByStructure(messages);

        // Now go through and bulk load each structure's RDMs
        for (IStructure structure : rdmsByStructure.keySet())
        {
            bulkLoadExtendedDataForStructure(session, structure, rdmsByStructure.get(structure));
        }
    }

    /**
     * Organize a collection of RDMs into lists according to their defining structure.
     *
     * @param messages RDMs to organize
     * @return list multi-map of organized RDMs, keyed by structure
     */
    public ArrayListMultimap<IStructure, ? extends IRaptorDataMessage> collectRdmsByStructure(Iterable<? extends IRaptorDataMessage> messages)
    {
        final ArrayListMultimap<IStructure, IRaptorDataMessage> messagesPerStructure = ArrayListMultimap.create();
        messages.forEach(rdm -> messagesPerStructure.put(rdm.getStructure(), rdm));
        return messagesPerStructure;
    }

    /**
     * Bulk load data for the collection of RDMs that share a common structure, and thus a common DB table.
     *
     * @param session   The DB session to use
     * @param structure Defining structure
     * @param rdms      Messages for which to load the extended data
     */
    private void bulkLoadExtendedDataForStructure(Session session, IStructure structure, final List<? extends IRaptorDataMessage> rdms)
    {
        // All RDMs in this collection share the same structure, and we know the collection is not empty
        final String tableName = structure.getExtendedDataClassName();

        if (logger.isDebugEnabled()) logger.debug("Loading {} data for {} RDMs", structure, rdms.size());

        final String baseSelect = dynamicStructures.getSelectSql(structure);

        // See if we are selecting a large portion of the table contents.  If so, then just pull the entire
        // table in instead of using IN clause batches.

        final int size = rdms.size();
        final int tableCount = getCountForTable(session, tableName);

        if (size > tableCount / 2) // If over 50%
        {
            final IntObjMap<IRaptorDataMessage> idMap = HashIntObjMaps.newUpdatableMap(size);
            for (IRaptorDataMessage rdm : rdms)
            {
                idMap.put(rdm.getIdAsInt(), rdm);
            }

            executeBulkLoad(session, baseSelect, idMap);
        } else
        {
            // Work on the RDMs in batches.  We're going to use an IN clause, and performance degrades if the
            // number of values used is too large, so we limit it to RDM_ID_BATCH_SZ.
            //
            // As we collect the batch, also build a map from Id to RDM so that we can find the RDM for which a
            // returned data set is destined.

            final String sqlFormat = baseSelect + " WHERE mappoint_id IN (%s)";
            int index = 0;

            while (index < size)
            {
                final int thisBatchSize = Math.min(RDM_ID_BATCH_SZ, size - index);
                final int[] idBatch = new int[thisBatchSize];
                final IntObjMap<IRaptorDataMessage> idMap = HashIntObjMaps.newUpdatableMap(RDM_ID_BATCH_SZ);
                int count = 0;

                while (count < thisBatchSize)
                {
                    final IRaptorDataMessage rdm = rdms.get(index++);
                    final int id = rdm.getIdAsInt();
                    idBatch[count++] = id;
                    idMap.put(id, rdm);
                }

                final String batchSql = String.format(sqlFormat, StringUtils.join(idBatch, ",", null));

                executeBulkLoad(session, batchSql, idMap);
            }
        }
    }

    /**
     * Get the number of rows in a given table.
     *
     * @param session   Session to use
     * @param tableName Table to count
     * @return count from table
     */
    private int getCountForTable(Session session, final String tableName)
    {
        return session.doReturningWork(conn -> {
            final String countSql = "SELECT COUNT(*) FROM " + tableName;

            try (PreparedStatement statement = conn.prepareStatement(countSql);
                 ResultSet results = statement.executeQuery())
            {
                results.next();
                return results.getInt(1);
            }
        });
    }

    private void executeBulkLoad(Session session, final String sql, final IntObjMap<IRaptorDataMessage> idMap)
    {
        logger.debug("Execute SQL: {}", sql);

        session.doWork(conn -> {
            // Each result row is the extended data for a single RDM

            try (PreparedStatement statement = conn.prepareStatement(sql);
                 ResultSet results = statement.executeQuery())
            {
                while (results.next())
                {
                    final int id = results.getInt("mappoint_id");
                    final IRaptorDataMessage rdm = idMap.get(id);
                    if (rdm == null) continue;

                    logger.trace("Load data for RDM Id: {}", id);

                    rdm.setDataObject(loadSingleExtendedData(rdm, results));
                }

                conn.commit();
            } catch (Exception e)
            {
                logger.error("Failed to bulk load extended data", e);
            }
        });
    }

    /**
     * Load the extended data for a message from the given results set.
     *
     * @param rxm     Message being processed
     * @param results Result set to process
     * @return Extended data object
     * @throws IllegalStateException If a data field has an unknown type
     */
    private IExtendedDataStructure loadSingleExtendedData(IRaptorMessage rxm, ResultSet results)
            throws IllegalStateException
    {
        final IExtendedDataStructure dataObject = rxm.getStructure().makeExtendedDataObject();

        if (dataObject != null) dataObject.loadFieldValues(results);

        return dataObject;
    }

    /**
     * Write all the extended field values for a given message into persistence.
     *
     * @param connection The JDBC connection to use
     * @param rxm        The structured message to process
     */
    public void persistExtendedData(Connection connection, final IRaptorMessage rxm)
    {
        final String insertSQL = dynamicStructures.getInsertSql(rxm.getStructure());

        try (PreparedStatement statement = connection.prepareStatement(insertSQL))
        {
            rxm.getDataObject().bindFieldValues(statement, rxm.getIdAsInt());

            statement.executeUpdate();
            connection.commit(); // Always commit work to avoid "Idle in Transaction" issues
        } catch (SQLDataException e)
        {
            logger.error(e.getMessage());
        } catch (SQLException e)
        {
            logger.error("Failed to handle statement: {}", insertSQL);
            logger.error("Message was: {}", rxm, e);
        }
    }

    /**
     * Persist the extended data for a collection of RDMs as a JDBC batch.
     * This will minimize the round-trips to the database.
     *
     * @param rdmsByStructure Messages for which to persist the extended data, organized by structure
     * @since 2.2 (renamed in 2.38)
     */
    public void batchPersistExtendedData(ArrayListMultimap<IStructure, ? extends IRaptorDataMessage> rdmsByStructure)
    {
        // Ensure that all the structures used by these RDMs are mapped into the DB
        rdmsByStructure.keySet().forEach(this::ensureMappedInDb);

        persistServiceProvider.getPersistService().doWork(connection -> {
            // Now go through and batch persist each structure's RDMs
            try
            {
                rdmsByStructure.asMap().forEach((structure, rdms) -> batchPersistExtendedDataForStructure(connection, structure, rdms));

                connection.commit();
            } catch (Exception e)
            {
                logger.error("Failed to persist RDM data", e);
                connection.rollback();
            }
        });
    }

    /**
     * Batch persist data for the collection of RDMs that share a common structure, and thus a common DB table.
     *
     * @param connection DB connection to use
     * @param structure  Defining structure
     * @param rdms       Messages for which to persist the extended data
     */
    private void batchPersistExtendedDataForStructure(Connection connection, IStructure structure, Collection<? extends IRaptorDataMessage> rdms)
    {
        // All messages in this collection share the same structure, and we know the collection is not empty
        final String insertSQL = dynamicStructures.getInsertSql(structure);

        if (logger.isDebugEnabled())
        {
            logger.debug(String.format("Persist %s data for %d messages", structure, rdms.size()));
        }

        try (PreparedStatement statement = connection.prepareStatement(insertSQL))
        {
            for (IRaptorMessage message : rdms)
            {
                message.getDataObject().bindFieldValues(statement, message.getIdAsInt());

                statement.addBatch();
                statement.clearParameters();
            }

            statement.executeBatch();
        } catch (SQLDataException e)
        {
            logger.error(e.getMessage());
        } catch (SQLException e)
        {
            logger.error("Failed to handle statement: {}", insertSQL, e);
        }
    }

    /**
     * Ensure that the DB table that backs the extended data for the given structure has been created.
     *
     * @param structure Structure of interest
     * @since 2.37
     */
    public void ensureMappedInDb(IStructure structure)
    {
        // Get it mapped in this session
        final Integer structureId = structure.getId();

        synchronized (structuresMappedInDatabase)
        {
            if (!structuresMappedInDatabase.get(structureId))
            {
                logger.debug("Mapping structure {}", structure);

                persistServiceProvider.getPersistService().mapStructure(structure);
                structuresMappedInDatabase.set(structureId);
            }
        }
    }

    /**
     * Clear the set of structures that have been mapped into the DB for this project session.
     *
     * @since 2.37
     */
    public void clear()
    {
        structuresMappedInDatabase.clear();
    }
}
