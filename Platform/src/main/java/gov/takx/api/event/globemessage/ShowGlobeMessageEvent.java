package gov.takx.api.event.globemessage;

import gov.takx.api.event.PlatformEvent;
import gov.takx.api.globe.IGlobeDelegate;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Event fired to display a message on one or all globes.
 *
 * @since 3.0
 */
public class ShowGlobeMessageEvent implements PlatformEvent
{
    @Nonnull
    private final String messageText;

    @Nullable
    private final IGlobeDelegate globe;

    /**
     * Constructor to display a message on all globes.
     *
     * @param messageText The text to display in the message
     */
    public ShowGlobeMessageEvent(@Nonnull String messageText)
    {
        this(messageText, null);
    }

    /**
     * Constructor to display a message on a specific globe.
     *
     * @param messageText The text to display in the message
     * @param globe Globe on which to display the message, pass {@code null} to display on all globes
     * @since 5.0
     */
    public ShowGlobeMessageEvent(@Nonnull String messageText, @Nullable IGlobeDelegate globe)
    {
        this.messageText = messageText;
        this.globe = globe;
    }

    /**
     * @return Globe on which to display the message, {@code null} to display on all globes
     * @since 5.0
     */
    @Nullable
    public IGlobeDelegate getGlobe()
    {
        return globe;
    }

    @Nonnull
    public String getMessageText()
    {
        return messageText;
    }
}
