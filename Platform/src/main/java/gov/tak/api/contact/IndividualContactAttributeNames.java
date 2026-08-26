package gov.tak.api.contact;

/**
 * Defines the set of {@link gov.tak.api.util.AttributeSet} keys used to maintain {@link IIndividualContact} state. The
 * attributes can be retrieved using {@link IIndividualContact#getAttributes()}. Generally, these attributes should
 * <em>only</em> be modified by the core and {@link gov.tak.api.chat.protocol.IChatProtocolHandler}s. Otherwise,
 * modifying these attributes may have unexpected effects.
 *
 * @since 5.1
 */
public class IndividualContactAttributeNames
{
    /**
     * Key for a boolean value used to indicate if a {@link IIndividualContact}'s state will be updated manually
     * rather than automatically by the core. Typically, this will only be used by
     * {@link gov.tak.api.chat.protocol.IChatProtocolHandler}s that create their own contacts.
     */
    public static final String STATE_MANAGED_MANUALLY = "State Managed Manually";

    /**
     * Key for the most recent message time in milliseconds.
     */
    public static final String MOST_RECENT_MESSAGE_TIME = "Most Recent Message Time";

    /**
     * Key for the contact's role.
     */
    public static final String ROLE = "Role";

    /**
     * Key for the contact's stale time, in milliseconds. This is the time at which a contact will be considered
     * inactive if no new messages are received from the contact. This core uses this value to determine a contact's
     * current state. The value is typically updated when new contact messages are received.
     */
    public static final String STALE_TIME = "Stale Time";

    /**
     * Key for the contact's current state. The value will correspond to the value returned by
     * {@link IndividualContactState#toString()}}.
     *
     * @see IndividualContactState
     */
    public static final String STATE = "State";

    /**
     * Key for the contact's team.
     */
    public static final String TEAM = "Team";

    /**
     * A contact's optional configured streaming endpoint id.
     *
     * @see gov.tak.api.comms.commo.ICommonCommoManager#configKnownEndpointForContact(String, String, String)
     * @since 5.7
     */
    public static final String ENDPOINT_ID = "Endpoint Id";
}
