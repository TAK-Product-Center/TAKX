package gov.tak.platform.events;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.event.Message;
import gov.nasa.worldwind.event.MessageListener;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A specialized Nasa World Wind Model that allows any MessageListener that isn't a Layer to tap into the Message stream
 *
 * @since 3.3.0
 */
public class MessageTapModel extends BasicModel
{
    private final ConcurrentLinkedQueue<MessageListener> messageListeners = new ConcurrentLinkedQueue<>();

    @Override
    public void onMessage(Message msg)
    {
        super.onMessage(msg);
        for (MessageListener listener : messageListeners)
        {
            listener.onMessage(msg);
        }
    }

    /**
     * Add a message listener to model
     *
     * @param listener the message listener
     */
    public void addMessageListener(MessageListener listener)
    {
        // synchronize is to make the contains check and add call atomic and mutually exclusive with remove
        synchronized (messageListeners)
        {
            if (!messageListeners.contains(listener)) messageListeners.add(listener);
        }
    }

    /**
     * Remove a message listener from the model
     *
     * @param listener the message listener
     */
    public void removeMessageListener(MessageListener listener)
    {
        synchronized (messageListeners)
        {
            messageListeners.remove(listener);
        }
    }
}
