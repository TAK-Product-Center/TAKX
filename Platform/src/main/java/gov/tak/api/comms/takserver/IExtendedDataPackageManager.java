package gov.tak.api.comms.takserver;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.commons.file.FileUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Extended {@link IDataPackageManager} for internal client use only.
 *
 * @since 3.5
 */
@Internal
@NoPluginImplementations
public interface IExtendedDataPackageManager extends IDataPackageManager
{
    /**
     * Copy the given file into our VFS store.  Return the created file, whose name may have been made unique
     * by {@link FileUtils#getUniqueFileName(File, File)}.
     *
     * @param packageFile Data package file to copy
     *
     * @return Copied file in VFS (with potentially different name)
     *
     * @throws IOException If file copy fails.
     */
    @Nonnull
    File copyPackageToVFS(@Nonnull File packageFile) throws IOException;

    /**
     * Clears the peer data package transfer log.
     *
     * @since 5.0
     */
    void clearTransferLog();

    /**
     * @return List of data package transfers between peers.
     *
     * @since 5.0
     */
    @Nonnull
    List<TransferLogItem> getTransferLog();

    /**
     * Adds a listener that's notified when the {@link #getTransferLog() transfer log} is updated.
     *
     * @param listener Listener to add.
     *
     * @since 5.0
     */
    void addTransferListener(@Nonnull ITransferListener listener);

    /**
     * Creates a temporary data package that will be deleted when the program exists. The name does not need to be
     * unique. The data package's manifest will include parameters that indicate the package should be auto-imported and
     * then deleted.
     *
     * @param name Name of the data package to create. Does not need to include the '.zip' extension.
     * @return File location of created data package.
     * @throws IOException When the given name is blank, or creating the file failed for some other reason.
     * @since 5.2
     */
    @Nonnull
    File createNewTempDataPackage(@Nonnull String name) throws IOException;

    /**
     * Outlines a listener for updates to the transfer log.
     *
     * @since 5.0
     */
    interface ITransferListener
    {
        /**
         * Called when a new item is added to the transfer log.
         *
         * @param item Item added to the transfer log.
         */
        void onNewTransfer(@Nonnull TransferLogItem item);

        /**
         * Called when the transfer log is cleared.
         */
        void onTransfersCleared();
    }

    /**
     * Wrapper of a single transfer.
     * The {@code to} or {@code from} is {@code null}, indicating which party we are in the transaction.
     *
     * @param packageFile Path to package file.
     * @param from The origin of the file. {@code null} if we are the sender.
     * @param to The contact receiving the file. {@code null} if we are the receiver.
     *
     * @since 5.0
     */
    record TransferLogItem(@Nonnull File packageFile, @Nullable String from, @Nullable String to)
    {
        /**
         * @return The callsign of the other person involved in the transfer.
         */
        @Nonnull
        public String getOtherCallsign()
        {
            return from != null ? from : to != null ? to : "UNKNOWN";
        }

        /**
         * @return {@code true} when this entry represents receiving a package from somebody else.
         * {@code false} when this entry represents sending a package to somebody else.
         */
        public boolean isReceive()
        {
            return from != null;
        }
    }
}
