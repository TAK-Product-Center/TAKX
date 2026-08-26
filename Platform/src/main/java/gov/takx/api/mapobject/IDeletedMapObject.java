package gov.takx.api.mapobject;

import gov.takx.api.annotation.Internal;
import gov.takx.api.annotation.NoPluginImplementations;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.persist.IPersistable;

/**
 * Contract for the entities used to track deleted map objects.  Since the actual cleanup of all the data
 * associated with a MapObject can take a long time, and may be spread across multiple project open/close cycles,
 * we have to track all the MapObjects that have been deleted.
 *
 * @since 1.34
 */
@Internal
@NoPluginImplementations
public interface IDeletedMapObject extends IPersistable
{
    /**
     * @return The MapEntity defining the Map Object
     */
    IMapEntity getMapEntity();

    /**
     * @return Map Object being deleted, unavailable ({@code null}) upon project re-open
     * @since 2.29
     */
    IMapObject getMapObject();

    /**
     * @return the id of the MapObject
     */
    int getMapObjectId();

    /**
     * @return The time at which the MapObject was deleted
     */
    long getTimeOfDeletion();

    /**
     * Set the time of deletion to the greater of the current time and the given time.
     *
     * @param timeOfDeletion Possibly newer time of deletion
     */
    void updateTimeOfDeletion(long timeOfDeletion);

    /**
     * @return {@code true} if this is a record of a partial delete (just RDM data and not everything),
     * {@code false} if this is a complete MapObject delete
     */
    boolean isPartialDelete();

    /**
     * @return Unique key of MapObject
     */
    String getMapObjectKey();

    /**
     * @return The id of the related TrackStyleDefinition object, 0 if none
     * @since 2.38
     */
    int getTrackStyleDefinitionId();

    /**
     * @return The id of the related ConnectingLineDefinition object, 0 if none
     * @since 2.38
     */
    int getConnectingLineDefinitionId();
}
