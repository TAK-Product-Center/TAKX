package gov.takx.api.ui.event;

/**
 * Event fired when the rendering data model has changed, this typically forces a re-rendering of the scene.
 *
 * @since 1.14
 */
public class RenderingModelChangedEvent
{
    private String reason;

    /**
     * Construct an event with the given reason.
     *
     * @param reason Simple description of why the globe is being made dirty.
     */
    public RenderingModelChangedEvent(String reason)
    {
        this.reason = reason;
    }

    /**
     * @return The event reason
     */
    public String getReason()
    {
        return reason;
    }

    public String toString()
    {
        return "RenderingModelChangedEvent[reason=" + reason + "]";
    }
}
