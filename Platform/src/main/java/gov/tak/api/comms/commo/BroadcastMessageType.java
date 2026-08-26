package gov.tak.api.comms.commo;

import com.atakmap.commoncommo.CoTMessageType;

/**
 * Enum to describe broadcast message types.
 *
 * @since 3.0
 */
public enum BroadcastMessageType
{
    /**
     * All types of CoT messages
     */
    All(CoTMessageType.SITUATIONAL_AWARENESS, CoTMessageType.CHAT),

    /**
     * CoT message(s) containing a Chat message
     */
    Chat(CoTMessageType.CHAT),

    /**
     * CoT message(s) containing a message with situational awareness information such as team member markers, shapes, etc.
     */
    SituationalAwareness(CoTMessageType.SITUATIONAL_AWARENESS);

    private final CoTMessageType[] coTMessageTypes;

    /**
     * @param coTMessageTypes Array of {@link CoTMessageType} associated with this broadcast message type
     */
    BroadcastMessageType(CoTMessageType... coTMessageTypes)
    {
        this.coTMessageTypes = coTMessageTypes;
    }

    /**
     * @return The array of {@link CoTMessageType} associated with this broadcast message type
     */
    public CoTMessageType[] getCoTMessageTypes()
    {
        return coTMessageTypes.clone();
    }
}
