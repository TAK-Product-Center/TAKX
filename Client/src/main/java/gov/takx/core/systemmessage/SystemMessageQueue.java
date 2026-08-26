package gov.takx.core.systemmessage;

import gov.takx.api.systemmessage.SystemMessage;
import gov.takx.platform.utils.TimeBatchedQueue;

import java.util.Collection;

/**
 * Timed queue to batch up new messages.  The internal batch is limited to the maximum configured number of
 * messages to avoid excessive memory consumption if messages arrive at a fast rate.
 *
 * @since 2.30
 */
class SystemMessageQueue extends TimeBatchedQueue<Collection<SystemMessage>, SystemMessage>
{
    private final ThreadLocal<Boolean> guard = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private final SystemMessagesView systemMessagesView;

    /**
     * @param maxVisibleSystemMessages The size to limit the evicting queue.
     */
    SystemMessageQueue(SystemMessagesView systemMessagesView, int maxVisibleSystemMessages)
    {
        super("System Message View Updater", 500, 2_000, () -> new CircularMessageCollection(maxVisibleSystemMessages));

        this.systemMessagesView = systemMessagesView;
    }

    /**
     * Override this method to ensure that we don't cause infinite recursion in trying to process a message
     * that leads to adding another message.  Use a thread local so we only prevent recursion, and don't interfere
     * with other threads calling this same method at the same time.
     *
     * @param entry Entry to add to batch
     */
    @Override
    public void add(SystemMessage entry)
    {
        if (!guard.get())
        {
            guard.set(Boolean.TRUE);
            super.add(entry);
            guard.set(Boolean.FALSE);
        }
    }

    @Override
    protected void runBatch(Collection<SystemMessage> batch)
    {
        systemMessagesView.addNewMessages(batch);
    }
}
