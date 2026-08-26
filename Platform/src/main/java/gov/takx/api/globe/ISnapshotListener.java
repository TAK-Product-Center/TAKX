package gov.takx.api.globe;

import java.io.File;

/**
 * This interface will is used to listen to the completion of a snapshot creation.
 *
 * @since 1.26
 */
public interface ISnapshotListener
{
    /**
     * Called when a snapshot has been saved to a file.
     *
     * @param snapshotFile The file that contains the snapshot
     */
    public void snapshotCreated(File snapshotFile);
}
