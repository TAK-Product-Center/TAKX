package gov.takx.core.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IDataType;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IStructure;
import gov.takx.api.messages.RaptorDataMessageColumns;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.api.persist.IRaptorDataMessageFactory;
import gov.takx.commons.constants.CommServiceType;
import gov.takx.dataservices.persist.RdmIdGenerator;
import gov.takx.dataservices.upgrade.ColumnDefinition;
import gov.takx.dataservices.upgrade.CreateForeignKeyStatement;
import gov.takx.dataservices.upgrade.CreateIndexStatement;
import gov.takx.dataservices.upgrade.CreateTableStatement;
import gov.takx.dataservices.upgrade.UpgradeStatement;
import gov.takx.dataservices.utils.DynamicStructureDBUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Static helper methods for persisting (insert and update) RDMs.
 *
 * @since 2.0.0
 */
@SuppressWarnings("SameParameterValue")
@Internal
@ApplicationScoped
public class MessagePersistHelper
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * The SQL to use to insert a new RDM.  Use this to create a {@link PreparedStatement} for an insert.
     */
    public static final String RDM_INSERT_SQL =
            "insert into MapPoint (" + RaptorDataMessageColumns.ALL_RDM_COLUMNS + ")" +
                    " values (" + RaptorDataMessageColumns.ALL_RDM_VALUE_MARKERS + ")";

    /**
     * The SQL to use to update an existing RDM.  Use this to create a {@link PreparedStatement} for an update.
     */
    public static final String RDM_UPDATE_SQL = "UPDATE MapPoint SET overrideTrackStyleId=?, trackStyleId=? WHERE id = ?";

    private static final int ID_COLUMN = 1;

    /**
     * Shared RDM Id generator.
     *
     * @since 2.29 (moved here from PersistService)
     */
    private static final RdmIdGenerator RDM_ID_GENERATOR = new RdmIdGenerator();

    @Inject
    private IMapObjectPersist mapObjectPersist;

    @Inject
    private IRaptorDataMessageFactory raptorDataMessageFactory;

    @Inject
    private DynamicStructureDBUtils dynamicStructureDBUtils;

    public List<UpgradeStatement> buildRDMCreateStatements()
    {
        final List<UpgradeStatement> statements = new ArrayList<>();

        final CreateTableStatement stmt = makeCreateTableStatement("MapPoint");

        stmt.addColumnDefinition(new ColumnDefinition("elevation", IDataType.DOUBLE))
                .addColumnDefinition(new ColumnDefinition("has3dOrientation", IDataType.BOOLEAN, true))
                .addColumnDefinition(new ColumnDefinition("hasElevation", IDataType.BOOLEAN, true))
                .addColumnDefinition(new ColumnDefinition("hasHeading", IDataType.BOOLEAN, true))
                .addColumnDefinition(new ColumnDefinition("hasLocation", IDataType.BOOLEAN, true))
                .addColumnDefinition(new ColumnDefinition("hasMotion", IDataType.BOOLEAN))
                .addColumnDefinition(new ColumnDefinition("hasSpeed", IDataType.BOOLEAN, true))
                .addColumnDefinition(new ColumnDefinition("hasTime", IDataType.BOOLEAN, true))
                .addColumnDefinition(new ColumnDefinition("heading", IDataType.DOUBLE))
                .addColumnDefinition(new ColumnDefinition("isManualLocation", IDataType.BOOLEAN, true))
                .addColumnDefinition(new ColumnDefinition("isRenderable", IDataType.BOOLEAN, true))
                .addColumnDefinition(new ColumnDefinition("latitude", IDataType.DOUBLE))
                .addColumnDefinition(new ColumnDefinition("longitude", IDataType.DOUBLE))
                .addColumnDefinition(new ColumnDefinition("overrideTrackStyleId", IDataType.INT))
                .addColumnDefinition(new ColumnDefinition("pitch", IDataType.DOUBLE))
                .addColumnDefinition(new ColumnDefinition("priority", IDataType.INT))
                .addColumnDefinition(new ColumnDefinition("receiveTime", IDataType.LONG, true))
                .addColumnDefinition(new ColumnDefinition("relevantTime", IDataType.DATE, true))
                .addColumnDefinition(new ColumnDefinition("roll", IDataType.DOUBLE))
                .addColumnDefinition(new ColumnDefinition("sourceName", IDataType.STRING))
                .addColumnDefinition(new ColumnDefinition("sourceType", IDataType.INT))
                .addColumnDefinition(new ColumnDefinition("speed", IDataType.DOUBLE))
                .addColumnDefinition(new ColumnDefinition("time", IDataType.LONG))
                .addColumnDefinition(new ColumnDefinition("trackStyleId", IDataType.INT))
                .addColumnDefinition(new ColumnDefinition("mapObject_id", IDataType.INT, true))
                .addColumnDefinition(new ColumnDefinition("structure_id", IDataType.INT, true))
                .addColumnDefinition(new ColumnDefinition("isImported", IDataType.BOOLEAN, true));

        statements.add(stmt);

        // add indices

        statements.add(new CreateIndexStatement("MapPoint", "idx_mo_id", false, "mapObject_id"));
        statements.add(new CreateIndexStatement("MapPoint", "idx_structure_id", false, "structure_id"));
        statements.add(new CreateIndexStatement("MapPoint", "idx_uuid", true, "uuid_msb", "uuid_lsb"));
        statements.add(new CreateIndexStatement("MapPoint", "idx_latitude", false, "latitude"));
        statements.add(new CreateIndexStatement("MapPoint", "idx_longitude", false, "longitude"));
        statements.add(new CreateIndexStatement("MapPoint", "idx_time", false, "time"));
        statements.add(new CreateIndexStatement("MapPoint", "idx_receive_time", false, "receiveTime"));

        // add foreign key constraints

        statements.add(new CreateForeignKeyStatement("FK_RDM_Structure", "MapPoint", "structure_id", "Structure", "id"));

        return statements;
    }

    /**
     * Make the CreateTableStatement for the given table name and add the primary key "id" column definition.
     *
     * @param tableName Name of the table to create
     * @return CreateTableStatement
     * @since 2.14
     */
    private CreateTableStatement makeCreateTableStatement(String tableName)
    {
        final CreateTableStatement stmt = new CreateTableStatement(tableName);

        // Use this as the primary key, we use a custom generator
        stmt.addColumnDefinition(new ColumnDefinition("id", IDataType.INT).setPrimary());

        stmt.addColumnDefinition(new ColumnDefinition("uuid_msb", IDataType.LONG))
                .addColumnDefinition(new ColumnDefinition("uuid_lsb", IDataType.LONG));

        return stmt;
    }

    /**
     * Initialize the state of the RDM Id generator.
     *
     * @param session The session to use to perform the SQL operations
     * @since 2.29
     */
    public void initializeIdGenerators(Session session)
    {
        RDM_ID_GENERATOR.initialize(session);
    }

    /**
     * @return The next RDM Id to use
     * @since 2.29
     */
    public int generateRdmId()
    {
        return RDM_ID_GENERATOR.generateId();
    }

    /**
     * Clear the RDM Id generator.
     *
     * @since 2.29
     */
    public void clearIdGenerators()
    {
        RDM_ID_GENERATOR.clear();
    }

    /**
     * Persist (insert or update) an RDM.
     *
     * @param connection DB Connection
     * @param rdm        RDM to persist
     * @param insert     Pass {@code true} to insert, {@code false} to update the RDM
     */
    public void persistRDM(Connection connection, IRaptorDataMessage rdm, boolean insert) throws SQLException
    {
        dynamicStructureDBUtils.ensureMappedInDb(rdm.getStructure());

        if (insert)
        {
            try (PreparedStatement stmt = connection.prepareStatement(RDM_INSERT_SQL))
            {
                bindInsertRDMParams(stmt, rdm);

                logger.debug("persist RDM: {}", stmt);

                stmt.execute();
                dynamicStructureDBUtils.persistExtendedData(connection, rdm); // And the extended data
                connection.commit();
            } catch (Exception e)
            {
                logger.error("Failed to persist RDM: {}", rdm, e);

                // In this case, we might have been persisting an RDM and failed on the extended data
                // This leaves us with a core RDM that has been given an ID, but we still failed to persist
                // the whole thing.  So, reset the ID so callers are not confused about this.  We need to
                // reset the ID since some code paths will eat this exception and the ultimate caller only
                // has the presence of the ID to determine if the RDM was persisted or not.

                rdm.resetId();
                connection.rollback();
            }
        } else
        {
            try (PreparedStatement stmt = connection.prepareStatement(RDM_UPDATE_SQL))
            {
                bindUpdateRDMParams(stmt, rdm);

                logger.debug("Update RDM: {}", stmt);

                stmt.execute();
                connection.commit();
            } catch (Exception e)
            {
                logger.error("Failed to update RDM: {}", rdm, e);
                connection.rollback();
            }
        }
    }

    /**
     * Bind the parameters for updating an RDM.
     *
     * @param s   The statement into which to bind parameters
     * @param rdm The RDM from which to extract values to bind
     */
    public void bindUpdateRDMParams(PreparedStatement s, IRaptorDataMessage rdm) throws SQLException
    {
        int param = 1;
        param = setIntOrNull(s, param, rdm.getOverrideTrackStyleDefinitionId());
        param = setIntOrNull(s, param, rdm.getTrackStyleDefinitionId());
        s.setInt(param, rdm.getIdAsInt());
    }

    /**
     * Bind the parameters for inserting a new RDM.
     *
     * @param s   The statement into which to bind parameters
     * @param rdm The RDM from which to extract values to bind
     */
    public void bindInsertRDMParams(PreparedStatement s, IRaptorDataMessage rdm) throws SQLException
    {
        final CommServiceType sourceType = rdm.getSourceType();
        final Boolean hasMotion = rdm.isMotionSet() ? rdm.hasMotion() : null;

        int param = 1;

        s.setInt(param++, rdm.getIdAsInt());
        s.setInt(param++, rdm.getMapObject().getId());
        s.setInt(param++, rdm.getStructure().getId());
        param = setUuid(s, param, rdm.getUUID());

        s.setDouble(param++, rdm.getElevation());
        s.setBoolean(param++, rdm.has3dOrientation());
        s.setBoolean(param++, rdm.hasElevation());
        s.setBoolean(param++, rdm.hasHeading());
        s.setBoolean(param++, rdm.hasLocation());
        param = setBooleanOrNull(s, param, hasMotion);
        s.setBoolean(param++, rdm.hasSpeed());
        s.setBoolean(param++, rdm.hasTime());
        s.setDouble(param++, rdm.getHeading());
        s.setBoolean(param++, rdm.isManuallyGeneratedLocation());
        s.setBoolean(param++, rdm.isRenderable());
        s.setDouble(param++, rdm.getLatitude());
        s.setDouble(param++, rdm.getLongitude());
        param = setIntOrNull(s, param, rdm.getOverrideTrackStyleDefinitionId());
        s.setDouble(param++, rdm.getPitch());
        s.setLong(param++, rdm.getReceiveTime());
        s.setTimestamp(param++, new java.sql.Timestamp(rdm.getRelevantTime()));
        s.setDouble(param++, rdm.getRoll());
        s.setString(param++, rdm.getSourceName());
        if (sourceType != null)
        {
            s.setInt(param++, sourceType.ordinal());
        } else
        {
            s.setNull(param++, Types.INTEGER);
        }
        s.setDouble(param++, rdm.getSpeedInMps());
        s.setLong(param++, rdm.getTime());
        setIntOrNull(s, param++, rdm.getTrackStyleDefinitionId());
        s.setBoolean(param, rdm.isImported());
    }

    /**
     * Given a result set (that used the ALL_COLUMNS select list), reify all the resulting rows into RDM instances.
     *
     * @param resultSet The result set from the query execution
     * @return List of RDMs
     */
    public List<IRaptorDataMessage> reifyRDMs(ResultSet resultSet) throws SQLException
    {
        List<IRaptorDataMessage> rdms = new ArrayList<>();

        while (resultSet.next())
        {
            rdms.add(reifyRDM(resultSet));
        }

        return rdms;
    }

    /**
     * Given a ResultSet positioned at a row of data assumed to have been selected using the ALL_RDM_COLUMNS
     * select list, reify an RDM with the values in the current row.
     *
     * @param resultSet ResultSet position to row of data to process
     * @return Reified RDM
     */
    public IRaptorDataMessage reifyRDM(ResultSet resultSet) throws SQLException
    {
        int column = 2; // Skip 'id' column

        // NOTE: You MUST process the columns in the order defined in ALL_RDM_COLUMNS

        // Grab all the data we need in order to create the RDM (need MO and Structure)
        final int moId = resultSet.getInt(column++);

        final IMapObject mapObject = mapObjectPersist.findMapObjectById(moId);

        final int structureId = resultSet.getInt(column++);
        final IStructure structure = mapObjectPersist.findStructureById(structureId);

        final IPrePersistRaptorDataMessage rdm = raptorDataMessageFactory.createRaptorDataMessage(structure, mapObject);

        final UUID uuid = getUuidOrNull(resultSet, column);
        column += 2; // UUIDs use 2 columns

        if (uuid != null) rdm.setUUID(uuid);

        final double elevation = resultSet.getDouble(column++);

        final boolean has3dOrientation = resultSet.getBoolean(column++);
        final boolean hasElevation = resultSet.getBoolean(column++);
        final boolean hasHeading = resultSet.getBoolean(column++);
        final boolean hasLocation = resultSet.getBoolean(column++);

        final boolean hasMotion = resultSet.getBoolean(column++);
        if (!resultSet.wasNull()) rdm.setHasMotion(hasMotion);

        final boolean hasSpeed = resultSet.getBoolean(column++);
        final boolean hasTime = resultSet.getBoolean(column++);

        final double heading = resultSet.getDouble(column++);

        final boolean isManualLocation = resultSet.getBoolean(column++);
        rdm.setManuallyGeneratedLocation(isManualLocation);

        final boolean isRenderable = resultSet.getBoolean(column++);
        rdm.setRenderable(isRenderable);

        final double latitude = resultSet.getDouble(column++);
        final double longitude = resultSet.getDouble(column++);

        final int overrideTsdId = resultSet.getInt(column++);
        if (!resultSet.wasNull()) rdm.setOverrideTrackStyleDefinitionId(overrideTsdId);

        final double pitch = resultSet.getDouble(column++);

        final long receiveTime = resultSet.getLong(column++);
        rdm.setReceiveTime(receiveTime);

        column += 1; // Skip relevant time (we don't keep it in memory)

        final double roll = resultSet.getDouble(column++);

        final String sourceName = resultSet.getString(column++);
        if (!resultSet.wasNull()) rdm.setSourceName(sourceName);

        final int sourceTypeOrd = resultSet.getInt(column++);
        if (!resultSet.wasNull()) rdm.setSourceType(CommServiceType.values()[sourceTypeOrd]);

        if (hasSpeed)
        {
            rdm.setSpeedInMps(resultSet.getDouble(column));
        }
        column++;

        if (hasTime)
        {
            rdm.setTime(resultSet.getLong(column));
        }
        column++;

        final int tsdId = resultSet.getInt(column++);
        if (!resultSet.wasNull()) rdm.setTrackStyleDefinitionId(tsdId);

        //noinspection UnusedAssignment
        if (resultSet.getBoolean(column++)) rdm.setImported(true);

        // Final pieces that need flags and values to compose
        if (hasLocation)
        {
            if (hasElevation)
            {
                rdm.setLocation(latitude, longitude, elevation);
            } else
            {
                rdm.setLocation(latitude, longitude);
            }
        }

        if (hasHeading)
        {
            if (has3dOrientation)
            {
                rdm.setOrientation(heading, pitch, roll);
            } else
            {
                rdm.setHeading(heading);
            }
        }

        // Set id after all the fields have been set, so that fields checking for ID do not reach back into the
        // database when creating RDMs from persistence (likely to cause an invalid cursor error).
        final int id = resultSet.getInt(ID_COLUMN);
        rdm.setId(id);

        return rdm;
    }

    /**
     * Set an INT parameter to the value given, set as NULL if the value is {@code null}.
     *
     * @param s     The statement into which to bind parameters
     * @param param The current parameter index
     * @param value The value to bind at that index
     * @return the next parameter index
     */
    private int setIntOrNull(PreparedStatement s, int param, Integer value) throws SQLException
    {
        if (value != null)
        {
            s.setInt(param++, value);
        } else
        {
            s.setNull(param++, Types.INTEGER);
        }

        return param;
    }

    /**
     * Set an BOOLEAN parameter to the value given, set as NULL if the value is {@code null}.
     *
     * @param s     The statement into which to bind parameters
     * @param param The current parameter index
     * @param value The value to bind at that index
     * @return the next parameter index
     */
    private int setBooleanOrNull(PreparedStatement s, int param, Boolean value) throws SQLException
    {
        if (value != null)
        {
            s.setBoolean(param++, value);
        } else
        {
            s.setNull(param++, Types.BOOLEAN);
        }

        return param;
    }

    /**
     * Set an LONG parameter to the value given, set as NULL if the value is {@code null}.
     *
     * @param s     The statement into which to bind parameters
     * @param param The current parameter index
     * @param value The value to bind at that index
     * @return the next parameter index
     */
    private int setLongOrNull(PreparedStatement s, int param, Long value) throws SQLException
    {
        if (value != null)
        {
            s.setLong(param++, value);
        } else
        {
            s.setNull(param++, Types.BIGINT);
        }

        return param;
    }

    /**
     * Set a UUID parameter to the value given, bind 0's if the value is {@code null}.
     *
     * @param s     The statement into which to bind parameters
     * @param param The current parameter index
     * @param uuid  The UUID to bind at that index
     * @return the next parameter index
     */
    private int setUuid(PreparedStatement s, int param, UUID uuid) throws SQLException
    {
        s.setLong(param++, uuid == null ? 0 : uuid.getMostSignificantBits());
        s.setLong(param++, uuid == null ? 0 : uuid.getLeastSignificantBits());

        return param;
    }

    private UUID getUuidOrNull(ResultSet resultSet, int column) throws SQLException
    {
        final long msb = resultSet.getLong(column);

        if (!resultSet.wasNull() || msb != 0)
        {
            final long lsb = resultSet.getLong(column + 1);
            return new UUID(msb, lsb);
        }

        return null;
    }
}
