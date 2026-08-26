package gov.tak.core.chat.protocol.geochat;

/**
 * Contact used to represent the top-level Teams group.
 *
 * @since 3.5
 */
public class GeoChatTeamsContact extends AGeoChatGroupContact
{
    public static final String TEAMS_GROUP_NAME = "Teams";
    public static final String TEAMS_GROUP_UID = "TeamGroups";

    public GeoChatTeamsContact()
    {
        super(TEAMS_GROUP_UID, TEAMS_GROUP_NAME);
    }
}