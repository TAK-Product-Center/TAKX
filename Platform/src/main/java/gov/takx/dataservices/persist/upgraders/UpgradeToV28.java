package gov.takx.dataservices.persist.upgraders;

/**
 * Upgrade from DB version 27 to 28.  Changes include:
 * <ul>
 *     <li>Converts groupcontact name column from varchar to text</li>
 * </ul>
 * @since 5.2
 */
public class UpgradeToV28 extends AUpgrader
{
    @Override
    public void doUpgrade() throws UpgraderException
    {
        changeVarCharColumnToText("groupcontact", "name");
    }
}
