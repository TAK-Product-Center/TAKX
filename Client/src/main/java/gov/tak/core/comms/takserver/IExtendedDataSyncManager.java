package gov.tak.core.comms.takserver;

import gov.tak.api.comms.takserver.IDataSyncManager;
import jakarta.annotation.Nonnull;

/**
 * Extended {@link IDataSyncManager} for internal client use only.
 *
 * @since 5.3
 */
public interface IExtendedDataSyncManager extends IDataSyncManager
{
    /**
     * Adds a listener to be notified of restoration events.
     *
     * @param listener Listener to add.
     */
    void addDataSyncRestorationListener(@Nonnull IDataSyncRestorationListener listener);

    /**
     * @param listener Listener to remove.
     */
    void removeDataSyncRestorationListener(@Nonnull IDataSyncRestorationListener listener);
}
