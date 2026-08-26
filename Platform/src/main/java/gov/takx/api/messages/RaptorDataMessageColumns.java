package gov.takx.api.messages;

import gov.takx.api.annotation.Internal;
import org.apache.commons.lang3.StringUtils;

/**
 * Constants declaring the columns that make up the RDM database table.  Preferably, this wouldn't exist, but
 * in order for the SqlGenerator to properly construct final SQL, it needs access to the list of RDM columns,
 * and the original location of said list was in Client, and thus inaccessible to classes in Platform.
 * <p>
 * This class and the constants it defines are for internal use only, and they may change or go away at any time.
 *
 * @since 2.14
 */
@Internal
public class RaptorDataMessageColumns
{
    public static final String ALL_RDM_COLUMNS =
            "id, mapObject_id, structure_id, uuid_msb, uuid_lsb, elevation," +
                    " has3dOrientation, hasElevation, hasHeading, hasLocation, hasMotion, hasSpeed, hasTime, heading," +
                    " isManualLocation, isRenderable, latitude, longitude, overrideTrackStyleId, pitch," +
                    " receiveTime, relevantTime, roll, sourceName, sourceType, speed, time, trackStyleId, isImported";

    public static final String[] ALL_RDM_COLUMNS_ARRAY = ALL_RDM_COLUMNS.split(",\\s*");

    /**
     * A series of "?, ?, "... (value placeholders) for each RDM Column for use in generating insert SQL
     */
    public static final String ALL_RDM_VALUE_MARKERS = StringUtils.repeat("?", ", ", ALL_RDM_COLUMNS_ARRAY.length);

    /**
     * Create a list of all column names adding a table alias (such as "rdm.") as a prefix.
     *
     * @param alias Alias to prefix to each column name
     * @return aliased column list
     */
    public static String getAllRdmColumnsWithAlias(String alias)
    {
        return alias + StringUtils.join(ALL_RDM_COLUMNS_ARRAY, ", " + alias);
    }
}
