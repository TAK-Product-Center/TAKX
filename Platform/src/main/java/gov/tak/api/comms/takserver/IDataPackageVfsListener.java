package gov.tak.api.comms.takserver;

import gov.tak.platform.datapackage.content.DataPackageContents;
import jakarta.annotation.Nonnull;

import java.io.File;

/**
 * Listener to handle updates to locally kept data packages within {@link IDataPackageManager}.
 *
 * @since 3.5
 */
public interface IDataPackageVfsListener
{
    /**
     * Called when a new data package is added to the package manager's VFS.
     *
     * @param packageFile File path to data package added.
     * @param contents Contents of the data package.
     */
    void onPackageAdded(@Nonnull File packageFile, @Nonnull DataPackageContents contents);

    /**
     * Called when an existing data package is updated within the package manager's VFS.
     *
     * @param packageFile File path to data package updated.
     * @param contents Updated contents of the data package.
     */
    void onPackageUpdated(@Nonnull File packageFile, @Nonnull DataPackageContents contents);

    /**
     * Called when a data package is removed from the package manager's VFS.
     *
     * @param packageFile File path to data package removed.
     */
    void onPackageRemoved(@Nonnull File packageFile);
}
