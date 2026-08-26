package gov.takx.dataservices.persist.upgraders;

import gov.takx.dataservices.persist.AMedia;

/**
 * Upgrade from DB version 30 to 31.  Changes include:
 * <ul>
 *     <li>Add the {@code connectionEntry} text column to the {@link AMedia} table</li>
 * </ul>
 * @since 5.3
 */
public class UpgradeToV31 extends AUpgrader
{
    @Override
    public void doUpgrade() throws UpgraderException
    {
        addColumn(AMedia.class.getSimpleName(), "connectionEntry", "text");
    }
}
