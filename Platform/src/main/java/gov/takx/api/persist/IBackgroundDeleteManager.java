package gov.takx.api.persist;

import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.mapobject.IDeletedMapObject;

/**
 * Contract for manager handling background delete operations.
 *
 * @since 1.34
 */
@NoPluginImplementations
public interface IBackgroundDeleteManager
{
    /**
     * Add a new deleted map object to be processed.
     *
     * @param dmo Deleted map object to add to our processing pool
     */
    void add(IDeletedMapObject dmo);

    /**
     * Suspend all active background delete operations.  Typically because the project is about to close.
     */
    void suspendWork();

    /**
     * If the given Map Object key is queued for deletion, then block the calling thread until that deletion
     * is complete.  If the given MO key is not queued for deletion, return immediately.
     *
     * @param mapObjectKey Key of Map Object of interest
     * @since 2.36
     */
    void waitForDeletion(String mapObjectKey);
}
