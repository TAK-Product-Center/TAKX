package gov.tak.api.comms.takserver;

import gov.tak.api.comms.takserver.mission.data.AssociatedResource;
import jakarta.annotation.Nonnull;

import java.nio.file.Path;

/**
 * Listener to handle responses to data package downloads within {@link IDataPackageManager}.
 *
 * @since 3.5
 */
public interface IDataPackageDownloadListener
{
    /**
     * Called when a data package download from a TAK Server completes.
     *
     * @param connection The TAK Server connection hosting the data package.
     * @param resource Information about the data package resource.
     * @param localPath The local file path to the downloaded data package.
     */
    void onPackageDownloadSuccess(@Nonnull ITakServerConnection connection, @Nonnull AssociatedResource resource,
                                  @Nonnull Path localPath);

    /**
     * Called when a data package download from a TAK Server fails.
     *
     * @param connection The TAK Server connection that was attempted to be downloaded from.
     * @param error The exception thrown handling data package downloading.
     */
    void onPackageDownloadError(@Nonnull ITakServerConnection connection, @Nonnull Throwable error);
}