package gov.takx.dataservices.persist.upgraders;

import gov.takx.dataservices.persist.DeletedMapObject;

/**
 * Upgrade from DB version 31 to 32.  Changes include:
 * <ul>
 *     <li>Add an index on "markerUid" to the {@code Track} table</li>
 *     <li>Drop the "inProgress" column from the {@link DeletedMapObject} table</li>
 * </ul>
 * @since 5.5.1
 */
public class UpgradeToV32 extends AUpgrader
{
    @Override
    public void doUpgrade() throws UpgraderException
    {
        addIndex("Track", "marker_uid", "markerUid");
        dropColumn(DeletedMapObject.class.getSimpleName(), "inProgress");
    }
}
