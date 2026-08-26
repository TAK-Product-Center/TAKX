package gov.takx.platform.utils;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.event.mapobject.MapObjectRemoved;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.plugin.device.IDevicePluginDefinition;
import gov.takx.commons.swing.AlwaysOnTopJOptionPane;
import gov.takx.platform.KeyUtils;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Helper class to deal with finding unused Unit Ids and querying the user for Unit Ids.
 *
 * @since 2.29
 */
@InjectableService
@ApplicationScoped
public class UnitIdHelper
{
    private final IMapObjectManager mapObjectManager;

    // This map is used for generating "friendly" unit Ids for map objects that use UUIDs as unit Ids.
    private final Map<String, Integer> nextUnitId = new ConcurrentHashMap<>();

    @Inject
    UnitIdHelper(IMapObjectManager mapObjectManager)
    {
        this.mapObjectManager = mapObjectManager;
    }

    /**
     * Prompt the user for a Unit ID.  Keep prompting them until they enter a valid (unused) Id, or they cancel out.
     *
     * @param definition The plugin definition defining te map object to create
     * @return Id or {@code null} if they canceled
     */
    public String getUserEnteredUnitId(IDevicePluginDefinition definition)
    {
        while (true)
        {
            String id = AlwaysOnTopJOptionPane.showInputDialog("Please enter a Unit ID", "Create New " + definition.getType());

            if (id == null) break;          // They hit cancel

            id = id.trim();
            if (id.isEmpty())
            {
                AlwaysOnTopJOptionPane.showErrorDialog("The Unit ID must be non-empty.", "Error");
                continue;
            }

            if (id.length() > 255)
            {
                AlwaysOnTopJOptionPane.showErrorDialog("The Unit ID must be 1 to 255 characters in length", "Error");
                continue;
            }

            String key = KeyUtils.getUniqueKey(definition, id);

            if (!mapObjectManager.keyExists(key))
            {
                return id;
            } else
            {
                AlwaysOnTopJOptionPane.showErrorDialog("This Unit ID is already being used.\nPlease choose another one.", "Error");
            }
        }
        return null;
    }

    /**
     * Get the next "friendly" unit ID to use for a given map object family and type.  We generate a simple
     * integer for the user to see instead of a UUID.  However, that means that there's no simple way to
     * tell if a unit ID has already been used except by testing each one against all the existing map objects.
     *
     * @param family Family of object
     * @param type   type of object
     * @return unit ID to use
     */
    public synchronized int getNextFriendlyUnitId(String family, String type)
    {
        int unitId = getNextFriendlyUnitIdNoUpdate(family, type);

        nextUnitId.put(KeyUtils.getUniqueKey(family, type), unitId); // Update the map with the new value

        return unitId;
    }

    /**
     * Get the next "friendly" unit ID to use for a given map object family and type.  We generate a simple
     * integer for the user to see instead of a UUID.  However, that means that there's no simple way to
     * tell if a unit ID has already been used except by testing each one against all the existing map objects.
     * <p>
     * The "NoUpdate" here means that the internal counter for the given family/type will NOT be updated after
     * determining the next available id.  Thus, this method can be sequentially called multiple times and it will
     * return the same id each time (assuming no interleaved MO creations or calls to
     * {@link #getNextFriendlyUnitId(String, String)}).
     *
     * @param family Family of object
     * @param type   type of object
     * @return unit ID to use
     * @since 2.33
     */
    public synchronized int getNextFriendlyUnitIdNoUpdate(String family, String type)
    {
        final String counterKey = KeyUtils.getUniqueKey(family, type);

        int unitId = nextUnitId.getOrDefault(counterKey, 0) + 1;

        while (isUnitIdOrNameUsed(family, type, unitId)) unitId++;

        return unitId;
    }

    /**
     * Ensure that the id isn't already in use.  There's no easy way to do this except to loop over every
     * map object and see if one already has that unit id or a name of type + " " + unitId.
     *
     * @param family Family of object
     * @param type   type of object
     * @param unitId Unit Id to test
     * @return {@code true} if the unit Id or name are already in use, {@code false} if not
     * @since 2.33
     */
    private boolean isUnitIdOrNameUsed(String family, String type, int unitId)
    {
        final String name = type + " " + unitId;
        final String key = KeyUtils.getUniqueKey(family, type, String.valueOf(unitId));

        for (IMapObjectController moc : mapObjectManager.getMapObjectControllers())
        {
            final IMapObject mapObject = moc.getMapObject();

            if (mapObject.getKey().equals(key) || mapObject.getName().equals(name)) return true;
        }
        return false;
    }

    /**
     * Get the next available numeric Unit Id for the given family and type.
     *
     * @param family         MO Family
     * @param mapObjectType  MO Type
     * @param startingUnitId Initial unit Id to try
     * @return String unit Id
     */
    public String getNextUnitID(String family, String mapObjectType, int startingUnitId)
    {
        final String mapObjectKey = KeyUtils.getUniqueKey(family, mapObjectType, Integer.toString(startingUnitId));

        // Check to see if this key is already in use

        return mapObjectManager.keyExists(mapObjectKey)
                ? getNextUnitID(family, mapObjectType, ++startingUnitId)
                : ((Integer) startingUnitId).toString();
    }

    private void onMapObjectDeleted(@Observes MapObjectRemoved event)
    {
        updateNextFriendlyUnitId(event.getMapObject());
    }

    /**
     * Updates the next ID to be used by Map Objects when one gets deleted.
     *
     * @param mapObject Map Object being deleted
     */
    private synchronized void updateNextFriendlyUnitId(IMapObject mapObject)
    {
        final String key = KeyUtils.getUniqueKey(mapObject.getFamily(), mapObject.getType());

        // Get last unit id put in map
        Integer mapId = nextUnitId.get(key);
        if (mapId != null)
        {
            try
            {
                // Get object unit id from name
                int objectId = Integer.parseInt(mapObject.getName().replaceAll("^.*\\D", ""));

                // If equal, update the map with new value
                if (objectId == mapId)
                {
                    nextUnitId.put(key, --mapId);
                }
            } catch (Exception e)
            {
                //Silent catch. We don't need to do anything
            }
        }
    }

    /**
     * Clear out our ids when closing.
     */
    private void cleanup(@SuppressWarnings("unused") @Observes ProjectClosing event)
    {
        nextUnitId.clear();
    }
}
