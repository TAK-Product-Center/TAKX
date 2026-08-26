package gov.takx.core.view.gui.cachecleaner;

/**
 */
public interface CacheCleanerTaskListener
{
    /**
     * Notify the listener that cache cleaning has begun.
     */
    public void cacheCleanerTaskStarted();

    /**
     * Notify the listener that cache cleaning has completed.
     *
     * @param success {@code true} on if cleaning was successful or {@code false} otherwise.
     */
    public void cacheCleanerTaskCompleted(boolean success);
}
