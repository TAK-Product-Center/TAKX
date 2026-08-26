package gov.takx.api.dataportal;

import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IMapObjectOfflineMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;

/**
 * Default implementation of the {@link IRaptorDataListener} interface, for convenience.
 * This listener accepts data for all map object, and provides empty implementations
 * of callback methods.
 *
 */
public class DefaultRaptorDataListener implements IRaptorDataListener
{
    /**
     * {@inheritDoc}
     * <br>
     * Default matches all map objects.
     * @return true
     */
    @Override
    public boolean matchMapObject(IMapObject mapObject)
    {
        return true;
    }

    /**
     * {@inheritDoc}
     * <br>
     * Empty.
     */
    @Override
    public void onMapObjectDefined(IMapEntity message)
    {
    }

    /**
     * {@inheritDoc}
     * <br>
     * Empty.
     */
    @Override
    public void onMapObjectAdded(IMapObject mapObject, IRaptorDataPortalController controller)
    {
    }

    /**
     * {@inheritDoc}
     * <br>
     * Empty.
     */
    @Override
    public void onDataMessage(String topic, IRaptorDataMessage msg, IRaptorDataStructure struct)
    {
    }

    /**
     * {@inheritDoc}
     * <br>
     * Empty.
     */
    @Override
    public void onDisconnect(IRaptorDataPortalController controller)
    {
    }

    /**
     * {@inheritDoc}
     * <br>
     * Empty.
     */
    @Override
    public void onMapObjectOffline(IMapObjectOfflineMessage msg, IRaptorDataPortalController controller, boolean wipeHistoricalData)
    {
    }
}
