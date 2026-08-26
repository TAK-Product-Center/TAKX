package gov.tak.api.comms.takserver;

import gov.tak.api.comms.takserver.mission.data.AssociatedResource;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Listener to handle responses to data package query responses within {@link IDataPackageManager}.
 *
 * @since 3.5
 */
public interface IDataPackageQueryListener
{
    /**
     * Called when a data package query on a TAK Server yields results.
     *
     * @param connection The TAK Server connection hosting the data packages.
     * @param results The list of resources detailing available data packages.
     */
    void onPackageListResponse(@Nonnull ITakServerConnection connection, @Nonnull List<AssociatedResource> results);

    /**
     * Called when a data package query on a TAK Server fails.
     *
     * @param connection The TAK Server connection that was attempted to be queried.
     * @param error The exception thrown handling data package lookups.
     */
    void onPackageListError(@Nonnull ITakServerConnection connection, @Nonnull Throwable error);
}