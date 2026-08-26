package gov.takx.api.ui.event;

import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IApplicationGlobe;
import gov.takx.api.globe.IExtendedGlobeManager;
import jakarta.inject.Inject;

/**
 * Event fired when a secondary globe is created.
 *
 * @since 1.14
 */
@PseudoBean
public class GlobeCreatedEvent
{
    private final int globeId;

    @Inject
    private IExtendedGlobeManager globeManager;

    /**
     * Construct an event for the given globe.
     *
     * @param globeId ID of the newly created globe
     */
    public GlobeCreatedEvent(int globeId)
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

    /**
     * Utility method to return the globe that was just created.
     *
     * @return New globe instance
     */
    public IApplicationGlobe getGlobe()
    {
        return globeManager.getGlobeDelegate(globeId);
    }
}
