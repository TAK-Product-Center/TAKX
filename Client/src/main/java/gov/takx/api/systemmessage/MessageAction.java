package gov.takx.api.systemmessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * A MessageAction is a callback handler allowing an arbitrary action to be associated with a {@link SystemMessage}.
 * Actions have a text message (for use in displaying to the user) and a runnable to execute when the user invokes
 * the action.
 *
 * @since 1.43
 */
public class MessageAction
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String message;
    private final Runnable runnable;

    /**
     * Construct a message action.
     *
     * @param message  Message to display
     * @param runnable Runnable to execute when the action is invoked
     */
    public MessageAction(String message, Runnable runnable)
    {
        this.message = message;
        this.runnable = runnable;
    }

    /**
     * @return Message for this action
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Invoke the action runnable.
     */
    public void invoke()
    {
        try
        {
            runnable.run();
        } catch (Exception e)
        {
            logger.error("Failed to execute action: {}", message, e);
        }
    }
}
