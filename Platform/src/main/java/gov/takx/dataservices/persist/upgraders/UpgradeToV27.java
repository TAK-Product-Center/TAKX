package gov.takx.dataservices.persist.upgraders;

/**
 * Upgrade from DB version 26 to 27.  Changes include:
 * <ul>
 *     <li>Adds chatlinksjson column to chatmessage table</li>
 * </ul>
 * @since 5.2
 */
public class UpgradeToV27 extends AUpgrader
{
    @Override
    public void doUpgrade() throws UpgraderException
    {
        addColumn("chatmessage", "chatlinksjson", "text");
    }
}
