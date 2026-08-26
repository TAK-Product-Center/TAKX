package gov.takx.api.persist;

import gov.takx.api.media.IMedia;

import java.util.List;

/**
 */
public interface IMediaPersist
{
    /**
     * Save a media object
     *
     * @param media Media object to save
     */
    public void persist(IMedia media);

    /**
     * Query for all the media objects in the project
     *
     * @return list of Media objects
     */
    public List<IMedia> queryAll();

    /**
     * Delete the specified media from the database
     *
     * @param media Media to delete
     */
    public void delete(IMedia media);
}
