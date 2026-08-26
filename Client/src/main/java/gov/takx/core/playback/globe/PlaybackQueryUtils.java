package gov.takx.core.playback.globe;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.CoreField;
import gov.takx.api.query.FieldSelector;
import gov.takx.api.query.InvalidFieldSelectorException;
import gov.takx.core.messagequery.ExtendedQueryService;
import jakarta.annotation.Nonnull;

import java.sql.Timestamp;
import java.util.List;

/**
 * Utility methods to perform a query for RDMs for devices in playback.
 * @since 5.7
 */
public class PlaybackQueryUtils
{
    private static final int MAX_MESSAGES = 100_000;

    /**
     * Query for RDM data associated with a given {@code mapObject}.  Only renderable RDMs are included, and no data
     * before the epoch is included.
     * 
     * @param mapObject Map Object of interest
     * @param coreFields Fields to be selected by the query
     * @return List of query results, according to {@code coreFields}
     */
    @Nonnull
    public static List<Object[]> query(@Nonnull IMapObject mapObject, CoreField... coreFields) throws ConflictingConstraintException, InvalidFieldSelectorException
    {
        final var queryService = new ExtendedQueryService();
        final var constraints = queryService.createEmptyConstraints();
        constraints.setMapObject(mapObject); // Constrain to this MO
        constraints.setMaxMessageCount(MAX_MESSAGES);

        // Only include data that can render
        constraints.addFieldConstraint(CoreField.hasLocation, ComparisonType.EQ, Boolean.TRUE);
        constraints.addFieldConstraint(CoreField.isRenderable, ComparisonType.EQ, Boolean.TRUE);
        constraints.addFieldConstraint(CoreField.isManualLocation, ComparisonType.EQ, Boolean.FALSE);

        // So as not to screw with the timescale, we are going to limit payback to tracks that
        // have a non-zero time.  The probable cause of a <=0 time is that the data came from
        // a file import.  This issue arose from CORE-1825 from data points imported from a
        // TRK file with time values containing year = 1899.
        constraints.addFieldConstraint(CoreField.relevantTime, ComparisonType.GT, new Timestamp(0));

        return queryService.queryForFields(constraints, new FieldSelector().addCoreFields(coreFields));
    }

    private PlaybackQueryUtils()
    {
    }
}
