package gov.takx.api.ui.event;

/**
 * Event fired when a secondary globe is removed.
 *
 * @since 1.14
 */
public class GlobeRemovedEvent
{
    private final int globeId;

    /**
     * Construct an event for the given globe.
     *
     * @param globeId ID of the newly created globe
     */
    public GlobeRemovedEvent(int globeId)
    {
        this.globeId = globeId;
    }

    /**
     * @return The Id of the globe that was created
     */
    public int getGlobeId()
    {
        return globeId;
    }
}
