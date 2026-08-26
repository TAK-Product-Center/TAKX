package gov.takx.dataservices.persist.upgraders;

import gov.takx.dataservices.persist.MapObject;

/**
 * Upgrade from DB version 28 to 29.  Changes include:
 * <ul>
 *     <li>Add the {@code geoFenceJson} text column to the {@code MapObject} table</li>
 * </ul>
 * @since 5.3
 */
public class UpgradeToV29 extends AUpgrader
{
    @Override
    public void doUpgrade() throws UpgraderException
    {
        addColumn(MapObject.class.getSimpleName(), "geoFenceJson", "text");
    }
}
