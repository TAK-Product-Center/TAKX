package gov.takx.dataservices.persist;

import gov.takx.api.annotation.Internal;
import gov.takx.api.mapobject.IDeletedMapObject;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.tracks.IConnectingLineDefinition;
import gov.takx.api.tracks.ITrackStyleDefinition;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Transient;

/**
 * Entity used to track deleted map objects.  Since the actual cleanup of all the data associated with a MapObject
 * can take a long time, and may be spread across multiple project open/close cycles, we have to track all the
 * MapObjects that have been deleted.
 *
 * @since 1.34
 */
@Internal
@Entity
public class DeletedMapObject implements IDeletedMapObject
{
    @SuppressWarnings("unused")
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(optional = false, targetEntity = MapEntity.class)
    private IMapEntity mapEntity;

    private int mapObjectId;
    private long timeOfDeletion;

    private int trackStyleDefinitionId;
    private int connectingLineDefinitionId;

    /**
     * Is this a "partial" delete, meaning we're just deleting RDMs earlier than the timeOfDeletion and not
     * the whole MapObject.
     */
    private boolean partialDelete;

    /**
     * The MapObject's unique key.
     *
     * @since 2.38 (was transient, now made persistent)
     */
    private String mapObjectKey;

    /**
     * Reference to the MapObject being deleted.  This speeds up the background deletion since it can avoid
     * a query for the MO when this is available.
     */
    @Transient
    private IMapObject mapObject;

    /**
     * Construct a DeletedMapObject representing the given MapObject.  The time of deletion will be "now."
     *
     * @param mapObject The MapObject being deleted
     */
    public DeletedMapObject(IMapObject mapObject)
    {
        this(mapObject, System.currentTimeMillis(), false);
    }

    /**
     * Construct a DMO on the given MapObject.
     *
     * @param mapObject      MapObject being operated upon
     * @param timeOfDeletion The time at which the delete is effective
     * @param partialDelete  {@code true} if we are only deleting RDM data, {@code false} if we're deleting the
     *                       entire MapObject and all associated data
     */
    public DeletedMapObject(IMapObject mapObject, long timeOfDeletion, boolean partialDelete)
    {
        this.timeOfDeletion = timeOfDeletion;
        this.partialDelete = partialDelete;

        this.mapObject = mapObject;
        mapObjectId = mapObject.getId();
        mapObjectKey = mapObject.getKey();

        if (mapObject instanceof IIconMapObject imo)
        {
            final ITrackStyleDefinition tsd = imo.getTrackStyleDefinition();
            trackStyleDefinitionId = tsd != null ? tsd.getId() : 0;

            final IConnectingLineDefinition cld = imo.getConnectingLineDefinition();
            connectingLineDefinitionId = cld != null ? cld.getId() : 0;
        }

        mapEntity = mapObject.getMapEntity();
    }

    @Override
    public Integer getId()
    {
        return id;
    }

    @Override
    public String getMapObjectKey()
    {
        return mapObjectKey;
    }

    @Override
    public IMapEntity getMapEntity()
    {
        return mapEntity;
    }

    @Override
    public IMapObject getMapObject()
    {
        return mapObject;
    }

    @Override
    public int getMapObjectId()
    {
        return mapObjectId;
    }

    @Override
    public int getTrackStyleDefinitionId()
    {
        return trackStyleDefinitionId;
    }

    @Override
    public int getConnectingLineDefinitionId()
    {
        return connectingLineDefinitionId;
    }

    @Override
    public long getTimeOfDeletion()
    {
        return timeOfDeletion;
    }

    @Override
    public void updateTimeOfDeletion(long timeOfDeletion)
    {
        this.timeOfDeletion = Math.max(this.timeOfDeletion, timeOfDeletion);
    }

    @Override
    public boolean isPartialDelete()
    {
        return partialDelete;
    }

    @Override
    public String toString()
    {
        return "DeletedMapObject{" +
                "mapEntity=" + mapEntity +
                ", mapObjectKey=" + mapObjectKey +
                ", mapObjectId=" + mapObjectId +
                ", timeOfDeletion=" + timeOfDeletion +
                ", partial=" + partialDelete +
                '}';
    }

    @SuppressWarnings("unused") // From Hibernate
    protected DeletedMapObject()
    {
    }
}
