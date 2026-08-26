package gov.takx.core.systemmessage;

import gov.takx.api.logging.SystemLogLevel;
import gov.takx.api.systemmessage.ISystemMessageManager;
import gov.takx.api.systemmessage.ISystemMessageManagerListener;
import gov.takx.api.systemmessage.SystemMessage;
import gov.takx.logging.SystemMessageAppender;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The system message manager.  This class handles the receipt of {@link SystemMessage System Messages}.
 * System messages are anything that the developer believes should be shown to the user.  Currently, they
 * are generated from two primary sources:
 * <ol>
 * <li>
 * from the logging system that are at or above level {@link SystemLogLevel#SYSTEM_WARN} and records them.
 * The {@link SystemMessageAppender} handles converting logging events into system messages.
 * </li>
 * <li>
 * Any direct call to {@link #addMessage(SystemMessage)}.  Almost all managed background tasks will
 * generate a system message to inform the user of their completion.
 * </li>
 * </ol>
 * <p/>
 * The highest level recorded and the count of outstanding messages is maintained until reset,
 * using {@link #resetOutstandingCount()}.
 * <p/>
 * Interested parties can be informed of new messages and count changes by registering as a listener via
 * {@link #addSystemMessageManagerListener(ISystemMessageManagerListener)}.
 * <p/>
 * Be very careful to not log anything above INFO level within this class or you will get the pleasure of dealing
 * with some infinite recursion and stack overflows.
 *
 * @see SystemMessageAppender
 * @since 1.42
 */
@ApplicationScoped
public class SystemMessageManager implements ISystemMessageManager
{
    private final Queue<SystemMessage> pendingMessages = new ConcurrentLinkedQueue<>();

    private boolean collectPendingMessages = true;
    private final AtomicInteger outstandingCount = new AtomicInteger(0);
    private volatile SystemLogLevel highestOutstandingLevel = SystemLogLevel.SYSTEM_ALL;
    private final ConcurrentLinkedQueue<ISystemMessageManagerListener> listeners = new ConcurrentLinkedQueue<>();

    /**
     * Force the early startup of this bean.
     * @since 3.4 (converted from using @EagerStartup at the class level)
     */
    private void eagerStartup(@Observes jakarta.enterprise.event.Startup ignored)
    {
    }

    @Override
    public void info(@Nonnull String message)
    {
        addMessage(new SystemMessage(SystemLogLevel.SYSTEM_INFO, message));
    }

    @Override
    public void info(@Nonnull String message, Throwable exception)
    {
        addMessage(new SystemMessage(SystemLogLevel.SYSTEM_INFO, message, exception));
    }

    @Override
    public void warn(@Nonnull String message)
    {
        addMessage(new SystemMessage(SystemLogLevel.SYSTEM_WARN, message));
    }

    @Override
    public void warn(@Nonnull String message, Throwable exception)
    {
        addMessage(new SystemMessage(SystemLogLevel.SYSTEM_WARN, message, exception));
    }

    @Override
    public void error(@Nonnull String message)
    {
        addMessage(new SystemMessage(SystemLogLevel.SYSTEM_ERROR, message));
    }

    @Override
    public void error(@Nonnull String message, Throwable exception)
    {
        addMessage(new SystemMessage(SystemLogLevel.SYSTEM_ERROR, message, exception));
    }

    @Override
    public void addMessage(SystemMessage message)
    {
        if (collectPendingMessages) pendingMessages.add(message);

        updateHighestOutstandingLevel(message.getLevel());
        outstandingCount.incrementAndGet();
        fireCountChanged();

        fireNewMessage(message); // And notify all listeners
    }

    @Override
    public void resetOutstandingCount()
    {
        highestOutstandingLevel = SystemLogLevel.SYSTEM_ALL;
        outstandingCount.set(0);

        fireCountChanged();
    }

    @Override
    public SystemLogLevel getHighestOutstandingLevel()
    {
        return highestOutstandingLevel;
    }

    @Override
    public int getOutstandingCount()
    {
        return outstandingCount.get();
    }

    @Override
    public List<SystemMessage> takePendingMessages()
    {
        final ArrayList<SystemMessage> messages = new ArrayList<>(pendingMessages);
        pendingMessages.clear();
        collectPendingMessages = false;
        return messages;
    }

    /**
     * Set the highest outstanding level.
     *
     * @param level New highest level
     */
    private void updateHighestOutstandingLevel(SystemLogLevel level)
    {
        if (level.isMoreSpecificThan(highestOutstandingLevel))
        {
            highestOutstandingLevel = level;
        }
    }

    /**
     * Fire a new message event to all listeners.
     *
     * @param message New message
     */
    private void fireNewMessage(SystemMessage message)
    {
        for (ISystemMessageManagerListener listener : listeners)
        {
            listener.newSystemMessage(message);
        }
    }

    /**
     * Fire a count changed event to all listeners.
     */
    private void fireCountChanged()
    {
        listeners.forEach(ISystemMessageManagerListener::outstandingCountChanged);
    }

    @Override
    public void addSystemMessageManagerListener(ISystemMessageManagerListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeSystemMessageManagerListener(ISystemMessageManagerListener listener)
    {
        listeners.remove(listener);
    }
}
