package gov.takx.dataservices.persist.upgraders;

import gov.takx.dataservices.persist.MapObject;

/**
 * Upgrade from DB version 32 to 33.  Changes include:
 * <ul>
 *     <li>Add the {@code drifterConfiguration} text column to the {@link MapObject} table</li>
 * </ul>
 * @since 5.6
 */
public class UpgradeToV33 extends AUpgrader
{
    private static final String DRIFTER_CONFIGURATION = "drifterConfiguration";

    @Override
    public void doUpgrade() throws UpgraderException
    {
        // Because of a previous bug, this upgrader can get run on a project that already contains this column,
        // so check for existence first.
        final String tableName = MapObject.class.getSimpleName();
        if (!columnExists(tableName, DRIFTER_CONFIGURATION)) addColumn(tableName, DRIFTER_CONFIGURATION, "text");
    }
}
