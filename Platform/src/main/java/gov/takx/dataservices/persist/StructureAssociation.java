package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.messages.IDataType;
import gov.takx.dataservices.upgrade.ColumnDefinition;
import gov.takx.dataservices.upgrade.CreatePrimaryKeyStatement;
import gov.takx.dataservices.upgrade.CreateTableStatement;
import gov.takx.dataservices.upgrade.UpgradeStatement;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * Holds the association between a MapEntity and a Structure.  This is used to maintain the N:M relationship
 * between Map Entities and Structures.
 *
 * @since 2.29
 */
@Internal
public class StructureAssociation
{
    private static final String MAPENTITY_ID = "mapentity_id";
    private static final String STRUCTURE_ID = "structure_id";
    private static final String COLUMNS = MAPENTITY_ID + ", " + STRUCTURE_ID;

    /**
     * SQL statement to use to query all the rows of the table.
     *
     * @see #StructureAssociation(ResultSet)
     * @since 3.2
     */
    public static final String LOAD_SQL = "select " + COLUMNS + " from StructureAssociation";

    private final int mapEntityId; // The Id of the Map Entity
    private final int structureId; // The Id of the Structure

    /**
     * Construct an association.
     */
    public StructureAssociation(int mapEntityId, int structureId)
    {
        this.mapEntityId = mapEntityId;
        this.structureId = structureId;
    }

    /**
     * Construct from the given result set, which must have used {@link #COLUMNS} in the select statement.
     *
     * @param resultSet Properly positioned result set to extract our entity data
     * @throws SQLException if column value retrieval fails
     * @since 3.2
     */
    public StructureAssociation(ResultSet resultSet) throws SQLException
    {
        mapEntityId = resultSet.getInt(1);
        structureId = resultSet.getInt(2);
    }

    public int getMapEntityId()
    {
        return mapEntityId;
    }

    public int getStructureId()
    {
        return structureId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        StructureAssociation that = (StructureAssociation) o;

        return mapEntityId == that.mapEntityId && structureId == that.structureId;
    }

    @Override
    public int hashCode()
    {
        int result = mapEntityId;
        result = 31 * result + structureId;
        return result;
    }

    /**
     * @return The SQL insert statement to persist this association
     * @since 3.2
     */
    public String getInsertSql()
    {
        return "insert into StructureAssociation(" + COLUMNS + ") values (" + mapEntityId + ", " + structureId + ")";
    }

    /**
     * @return Upgrade statements needed to create the table for our entity
     */
    public static List<UpgradeStatement> buildTableDefinition()
    {
        final CreateTableStatement stmt = new CreateTableStatement(StructureAssociation.class.getSimpleName())
                .addColumnDefinition(new ColumnDefinition(MAPENTITY_ID, IDataType.INT))
                .addColumnDefinition(new ColumnDefinition(STRUCTURE_ID, IDataType.INT))
                .withPrimaryKey(new CreatePrimaryKeyStatement(MAPENTITY_ID, STRUCTURE_ID));

        return List.of(stmt);
    }

    @Override
    public String toString()
    {
        return '[' + mapEntityId + " : " + structureId + ']';
    }
}
