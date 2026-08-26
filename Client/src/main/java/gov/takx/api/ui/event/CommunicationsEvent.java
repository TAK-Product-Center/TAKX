package gov.takx.api.ui.event;

import jakarta.annotation.Nonnull;

/**
 * Event fired whenever inbound or outbound communications traffic is detected by ICommIn and ICommOut, respectively.
 *
 */
public class CommunicationsEvent
{
    /**
     * Communications source direction, either incoming data or outgoing data.
     */
    public enum CommDirection
    {
        CommIn,
        CommOut
    }

    private final String sourceName;
    private final CommDirection direction;

    /**
     * Constructor.
     *
     * @param sourceName Source name where the communications event took place
     * @param direction Direction of the activity
     */
    public CommunicationsEvent(@Nonnull String sourceName, @Nonnull CommDirection direction)
    {
        this.sourceName = sourceName;
        this.direction = direction;
    }

    /**
     * @return CommDirection
     */
    public CommDirection getCommDirection()
    {
        return direction;
    }

    /**
     * @return The source name where the communications took place
     */
    public String getSourceName()
    {
        return sourceName;
    }

    @Override
    public String toString()
    {
        return sourceName + " / " + direction;
    }
}
