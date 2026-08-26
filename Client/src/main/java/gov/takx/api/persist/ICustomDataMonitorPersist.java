package gov.takx.api.persist;

import gov.takx.dataservices.persist.CustomDataMonitor;

import java.util.List;

/**
 * Interface for a persist service extension for persist functions on a CustomDataMonitor
 *
 */
public interface ICustomDataMonitorPersist
{
    /**
     * Persist a custom data monitor to the database
     *
     * @param cdm CustomDataMonitor
     */
    void persistDataMonitor(CustomDataMonitor cdm);

    /**
     * Query database for custom data monitors for a particular map object key
     *
     * @param key map object key
     * @return list of custom data monitors for a given map object key
     */
    List<CustomDataMonitor> query(String key);

    /**
     * @return ALL custom data monitors in the database.
     * @since 2.36
     */
    List<CustomDataMonitor> queryAll();

    /**
     * Delete a custom data monitor
     *
     * @param cdm CustomDataMonitor
     */
    void deleteDataMonitor(CustomDataMonitor cdm);
}
