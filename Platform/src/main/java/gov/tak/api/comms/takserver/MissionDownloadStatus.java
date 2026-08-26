package gov.tak.api.comms.takserver;

import gov.tak.api.comms.takserver.mission.data.Mission;

/**
 * Download progress of a {@link Mission} and its associated {@link Mission#contents()}.
 *
 * @since 5.0
 */
public enum MissionDownloadStatus
{
    /**
     * Mission not downloaded.
     */
    NOT_DOWNLOADED,
    /**
     * Mission model being downloaded.
     */
    MODEL_DOWNLOADING,
    /**
     * Mission's {@link Mission#contents()} being downloaded.
     */
    ASSOCIATED_RESOURCE_DOWNLOADING,
    /**
     * Generic status for when we've done something that implies the mission will download something or change in the near future
     * and thus should not be considered as {@link #COMPLETED}.
     */
    PENDING_CHANGE,
    /**
     * Mission model and {@link Mission#contents()} both downloaded.
     */
    COMPLETED;

    /**
     * Check if a download status indicates it is not fully completed yet, but only if initialized.
     *
     * @return {@code true} when the status is beyond {@link #NOT_DOWNLOADED} but before {@link #COMPLETED}.
     */
    public boolean isInProgress()
    {
        int ordinal = ordinal();
        return ordinal >= MODEL_DOWNLOADING.ordinal() && ordinal < COMPLETED.ordinal();
    }

    /**
     * @return {@code true} if the status is anything beyond {@link #NOT_DOWNLOADED}.
     */
    public boolean isAtLeastPartiallyDownloaded()
    {
        return ordinal() > 0;
    }

    /**
     * @return {@code true} if the status is {@link #PENDING_CHANGE} or {@link #COMPLETED}.
     * @since 5.3
     */
    public boolean isDownloadedOrPendingChange()
    {
        return ordinal() >= PENDING_CHANGE.ordinal();
    }
}
