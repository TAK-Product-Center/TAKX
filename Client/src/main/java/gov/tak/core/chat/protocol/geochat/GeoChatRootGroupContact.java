package gov.tak.core.chat.protocol.geochat;

/**
 * Default contact representing the "Groups" group.
 *
 * @since 3.0
 */
public class GeoChatRootGroupContact extends AGeoChatGroupContact
{
    public static final String GROUPS_ROOT_UID = "UserGroups";
    public static final String GROUPS_ROOT_DISPLAY_NAME = "Groups";

    /**
     * @param clientUid The UID of the client
     * @param clientName The name of the client
     */
    public GeoChatRootGroupContact(String clientUid, String clientName)
    {
        super(GROUPS_ROOT_UID, GROUPS_ROOT_DISPLAY_NAME, clientUid, clientName);
    }
}
