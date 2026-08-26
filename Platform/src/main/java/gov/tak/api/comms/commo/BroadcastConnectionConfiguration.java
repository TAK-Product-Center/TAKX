package gov.tak.api.comms.commo;

import com.atakmap.commoncommo.CoTMessageType;
import gov.takx.platform.preferences.PreferencesNode;

/**
 * Class to describe a broadcast connection configuration to be used with the Common Commo library to send CoT messages
 * to other TAK clients on local network
 *
 * @since 3.0
 */
public class BroadcastConnectionConfiguration extends ConnectionConfiguration
{
    private final static String MESSAGE_TYPE = "messageType";

    private BroadcastMessageType broadcastMessageType;

    /**
     * Create a Broadcast Connection Configuration from a Preference Node.
     *
     * @param node preference node
     */
    public BroadcastConnectionConfiguration(PreferencesNode node)
    {
        super(node);

        broadcastMessageType = BroadcastMessageType.valueOf(node.get(MESSAGE_TYPE, "All"));
    }

    public BroadcastMessageType getBroadcastMessageType()
    {
        return broadcastMessageType;
    }

    public void setBroadcastMessageType(BroadcastMessageType broadcastMessageType)
    {
        this.broadcastMessageType = broadcastMessageType;
        put(MESSAGE_TYPE, broadcastMessageType.name());
    }

    /**
     * @return The array of {@link CoTMessageType}s associated with the broadcast message type of this configuration.
     */
    public CoTMessageType[] getCoTMessageTypes()
    {
        return broadcastMessageType.getCoTMessageTypes();
    }
}
