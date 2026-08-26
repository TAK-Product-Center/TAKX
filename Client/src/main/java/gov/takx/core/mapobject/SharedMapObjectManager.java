package gov.takx.core.mapobject;

import gov.takx.api.event.StartProjectServices;
import gov.takx.api.event.StopProjectServices;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.mapobject.ISharedMapObjectManager;
import gov.takx.core.project.ServiceOrdering;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the shared map object communication security manager.  This class
 * maintains the master list of authorized one-way communication paths between two map object instances.
 *
 * @since 1.3
 */
@ApplicationScoped
public class SharedMapObjectManager implements ISharedMapObjectManager
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Internal representation of a MapObject participating in a shared relationship.
     */
    private static class InternalMapObject
    {
        private final String deviceFamily;
        private final String deviceType;
        private final String deviceId;
        private final int hashCode;

        public InternalMapObject(String deviceFamily, String deviceType, String deviceId)
        {
            this.deviceFamily = deviceFamily;
            this.deviceType = deviceType;
            this.deviceId = deviceId;

            hashCode = (deviceFamily + deviceType + deviceId).hashCode();
        }

        public String getDeviceFamily()
        {
            return deviceFamily;
        }

        public String getDeviceType()
        {
            return deviceType;
        }

        public String getDeviceId()
        {
            return deviceId;
        }

        @Override
        public int hashCode()
        {
            return hashCode;
        }

        @Override
        public boolean equals(Object o)
        {
            boolean isEqual = false;
            if (o instanceof InternalMapObject)
            {
                InternalMapObject obj = (InternalMapObject) o;
                isEqual = (obj.getDeviceFamily().equals(getDeviceFamily()) &&
                        obj.getDeviceType().equals(getDeviceType()) &&
                        obj.getDeviceId().equals(getDeviceId()));
            }

            return isEqual;
        }

        @Override
        public String toString()
        {
            return deviceFamily + ":" + deviceType + ":" + deviceId;
        }
    }

    /**
     * Internal representation of a one-way MapObject relationship between a Source and a Target.
     */
    private static class MapObjectRelationship
    {
        private final InternalMapObject source;
        private final InternalMapObject target;

        public MapObjectRelationship(InternalMapObject source, InternalMapObject target)
        {
            this.source = source;
            this.target = target;
        }

        public InternalMapObject getSource()
        {
            return source;
        }

        public InternalMapObject getTarget()
        {
            return target;
        }

        @Override
        public int hashCode()
        {
            return source.hashCode * 17 + target.hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            boolean isEqual = false;
            if (o instanceof MapObjectRelationship)
            {
                MapObjectRelationship obj = (MapObjectRelationship) o;
                isEqual = (obj.getSource().equals(getSource()) && obj.getTarget().equals(getTarget()));
            }

            return isEqual;
        }

        @Override
        public String toString()
        {
            return "Source " + getSource() + " can send to target " + getTarget();
        }
    }

    private final Set<MapObjectRelationship> authorizedRelationshipSet = new CopyOnWriteArraySet<>();

    private MapObjectRelationship buildRelationship(String sourceDeviceFamily,
                                                    String sourceDeviceType,
                                                    String sourceDeviceId,
                                                    String targetDeviceFamily,
                                                    String targetDeviceType,
                                                    String targetDeviceId)
    {
        InternalMapObject source = new InternalMapObject(sourceDeviceFamily, sourceDeviceType, sourceDeviceId);
        InternalMapObject target = new InternalMapObject(targetDeviceFamily, targetDeviceType, targetDeviceId);
        MapObjectRelationship relationship = new MapObjectRelationship(source, target);

        if (logger.isDebugEnabled())
        {
            logger.debug(relationship.toString());
        }

        return relationship;
    }

    /**
     * Notified by {@link IMapObjectManager} when a map object is removed.
     * <p>
     * Need to remove the relationships which involve the map object that is being deleted, and there could be
     * more than one.
     *
     * @param event The event providing the removed map object
     * @since 2.38 - formerly clientMapObjectRemoved
     */
    private void onClientMapObjectRemoved(@Observes MapObjectRemoved event)
    {
        IMapObject mapObject = event.getMapObject();
        InternalMapObject deletedObject = new InternalMapObject(mapObject.getFamily(), mapObject.getType(), mapObject.getUnitID());

        List<MapObjectRelationship> deleteList = authorizedRelationshipSet.stream()
                .filter(relationship -> relationship.getSource().equals(deletedObject) || relationship.getTarget().equals(deletedObject))
                .collect(Collectors.toList());

        authorizedRelationshipSet.removeAll(deleteList);
    }

    @Override
    public void registerShare(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId, String targetDeviceFamily, String targetDeviceType, String targetDeviceId)
    {
        // The CopyOnWriteArraySet is best suited for reads - so it is cheaper to read and see if the relationship
        // already exists instead of instantiating 3 objects and attempting the unnecessary add() call.
        if (!isShared(sourceDeviceFamily, sourceDeviceType, sourceDeviceId, targetDeviceFamily, targetDeviceType, targetDeviceId))
        {
            MapObjectRelationship relationship =
                    buildRelationship(sourceDeviceFamily, sourceDeviceType, sourceDeviceId, targetDeviceFamily, targetDeviceType, targetDeviceId);

            logger.debug("Relationship has been registered.");

            authorizedRelationshipSet.add(relationship);
        }
    }

    @Override
    public void unregisterShare(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId, String targetDeviceFamily, String targetDeviceType, String targetDeviceId)
    {
        if (isShared(sourceDeviceFamily, sourceDeviceType, sourceDeviceId, targetDeviceFamily, targetDeviceType, targetDeviceId))
        {
            MapObjectRelationship relationship =
                    buildRelationship(sourceDeviceFamily, sourceDeviceType, sourceDeviceId, targetDeviceFamily, targetDeviceType, targetDeviceId);

            logger.debug("Relationship unregistered");

            authorizedRelationshipSet.remove(relationship);
        }
    }

    @Override
    public boolean isShared(String sourceDeviceFamily, String sourceDeviceType, String sourceDeviceId, String targetDeviceFamily, String targetDeviceType, String targetDeviceId)
    {
        MapObjectRelationship relationship =
                buildRelationship(sourceDeviceFamily, sourceDeviceType, sourceDeviceId, targetDeviceFamily, targetDeviceType, targetDeviceId);

        boolean exists = authorizedRelationshipSet.contains(relationship);
        logger.debug("Relationship Exists? {}", exists);

        return exists;
    }

    /**
     * Crank up our service on the project.
     */
    private void startService(@SuppressWarnings("UnusedParameters") @Observes @Priority(ServiceOrdering.START_SharedMapObjectManager) StartProjectServices event)
    {
    }

    /**
     * Stop our service.
     */
    private void stopService(@SuppressWarnings("UnusedParameters") @Observes @Priority(ServiceOrdering.STOP_SharedMapObjectManager) StopProjectServices event)
    {
        if (logger.isDebugEnabled()) logger.debug("Authorized Relationship Set Cleared");

        authorizedRelationshipSet.clear();
    }
}
