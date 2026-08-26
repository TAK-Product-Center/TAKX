package gov.takx.api.media;

import gov.takx.api.annotation.Experimental;

/**
 * Interface to receive media manager events
 *
 * @since 2.8
 */
@SuppressWarnings("unused")
@Experimental
public interface IMediaManagerListener
{
    /**
     * Notification that media has been added to the project
     *
     * @param media The added media
     */
    void mediaAdded(IMedia media);

    /**
     * Notification that media has been removed from the project
     *
     * @param media The removed media
     */
    void mediaRemoved(IMedia media);

    /**
     * A user has requested that the media be downloaded from it's remote location so that it can be viewed/played
     *
     * @param media The media to download
     */
    void mediaDownloadRequested(IMedia media);

    /**
     * Notification that all media has been removed. Efficiency method
     *
     * @since 2.15
     */
    void mediaCleared();

    /**
     * Notification that the service has stopped
     *
     * @since 2.37
     */
    void serviceStopped();
}
