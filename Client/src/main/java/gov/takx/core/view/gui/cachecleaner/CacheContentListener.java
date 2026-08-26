package gov.takx.core.view.gui.cachecleaner;

import java.io.File;

/**
 */
public interface CacheContentListener
{
    /**
     * Notify the listener that an update on a cache location has begun.
     *
     * @param location a cache location.
     */
    public void cacheUpdateStarted(File location);

    /**
     * Notify the listener that the update on a cache location has completed.
     *
     * @param location a cache location.
     */
    public void cacheUpdateCompleted(File location);
}
