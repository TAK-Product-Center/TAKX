package gov.takx.api.event.globemessage;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.globe.IGlobeDelegate;
import jakarta.annotation.Nullable;

/**
 * Event fired to hide any currently displayed message on the globe from a {@link ShowGlobeMessageEvent}.
 *
 * @since 3.0
 */
public class HideGlobeMessageEvent implements PlatformEvent
{
    @Nullable
    private final IGlobeDelegate globe;

    /**
     * Constructor have the event target all globes.
     */
    public HideGlobeMessageEvent()
    {
        this(null);
    }

    /**
     * Constructor to have the event target a specific globe.
     *
     * @param globe Globe on which to hide a displayed message, pass {@code null} to hide on all globes
     * @since 5.0
     */
    public HideGlobeMessageEvent(@Nullable IGlobeDelegate globe)
    {
        this.globe = globe;
    }

    /**
     * @return Globe on which to hide a displayed message, {@code null} to hide on all globes
     * @since 5.0
     */
    @Nullable
    public IGlobeDelegate getGlobe()
    {
        return globe;
    }
}
