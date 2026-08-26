package gov.takx.dataservices.persist.upgraders;

import gov.takx.dataservices.persist.MapObject;

/**
 * Upgrade from DB version 33 to 34.  Changes include:
 * <ul>
 *     <li>Add the {@code fieldOfView} text column to the {@link MapObject} table</li>
 * </ul>
 * @since 5.8
 */
public class UpgradeToV34 extends AUpgrader
{
    private static final String FOV_CONFIGURATION = "fieldOfView";

    @Override
    public void doUpgrade() throws UpgraderException
    {
        // Because of a previous bug, this upgrader can get run on a project that already contains this column,
        // so check for existence first.
        final String tableName = MapObject.class.getSimpleName();
        if (!columnExists(tableName, FOV_CONFIGURATION)) addColumn(tableName, FOV_CONFIGURATION, "text");
    }
}
