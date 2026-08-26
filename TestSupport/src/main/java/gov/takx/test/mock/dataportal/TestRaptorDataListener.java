package gov.takx.test.mock.dataportal;

import gov.takx.api.dataportal.DefaultRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.messages.IMapEntity;
import gov.takx.api.messages.IMapObjectOfflineMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import jakarta.annotation.Nullable;

import java.util.function.Function;

/**
 * Listener for notifications of received Raptor Data Messages (RDMs).
 *
 * @since 3.4
 */
@SuppressWarnings("unused") // full implementation provided for test support
public class TestRaptorDataListener extends DefaultRaptorDataListener
{
    private int mapObjectsDefinedCount = 0;
    private int mapObjectsAddedCount = 0;
    private int dataMessagesReceivedCount = 0;
    private int disconnectCount = 0;
    private int mapObjectOfflineCount = 0;
    private IMapEntity lastMapObjectDefined;
    private IMapObject lastMapObjectAdded;
    private IRaptorDataPortalController lastControllerAdded;
    private String lastTopicReceived;
    private IRaptorDataMessage lastMessageReceived;
    private IRaptorDataStructure lastStructureReceived;
    private IRaptorDataPortalController lastControllerDisconnected;
    private IMapObjectOfflineMessage lastOfflineMessage;
    private IRaptorDataPortalController lastControllerOffline;

    private final Function<IMapObject, Boolean> matchFunction;

    public TestRaptorDataListener()
    {
        this(null);
    }

    /**
     * Constructor.
     *
     * @param matchFunction Function to use as a filter in {@link #matchMapObject(IMapObject)}. If none provided, the
     *                      method will always return true to match all map objects.
     */
    public TestRaptorDataListener(@Nullable Function<IMapObject, Boolean> matchFunction)
    {
        this.matchFunction = matchFunction;
    }

    /**
     * Uses the matchFunction (if one was set) to determine if a map object matches. If not, true is always returned.
     *
     * @param mapObject The map object to match
     * @return The result of the matchFunction (if one was set) or true.
     */
    @Override
    public boolean matchMapObject(IMapObject mapObject)
    {
        if (matchFunction != null) return matchFunction.apply(mapObject);
        return true;
    }

    @Override
    public void onMapObjectDefined(IMapEntity mapEntity)
    {
        mapObjectsDefinedCount++;
        lastMapObjectDefined = mapEntity;
    }

    @Override
    public void onMapObjectAdded(IMapObject mapObject, IRaptorDataPortalController controller)
    {
        mapObjectsAddedCount++;
        lastMapObjectAdded = mapObject;
        lastControllerAdded = controller;
    }

    @Override
    public void onDataMessage(String topic, IRaptorDataMessage message, IRaptorDataStructure structure)
    {
        dataMessagesReceivedCount++;
        lastTopicReceived = topic;
        lastMessageReceived = message;
        lastStructureReceived = structure;
    }

    @Override
    public void onDisconnect(IRaptorDataPortalController controller)
    {
        disconnectCount++;
        lastControllerDisconnected = controller;
    }

    @Override
    public void onMapObjectOffline(IMapObjectOfflineMessage message, IRaptorDataPortalController controller, boolean wipeHistoricalData)
    {
        mapObjectOfflineCount++;
        lastOfflineMessage = message;
        lastControllerOffline = controller;
    }

    /**
     * Resets state of listener.
     */
    public void reset()
    {
        mapObjectsDefinedCount = 0;
        mapObjectsAddedCount = 0;
        dataMessagesReceivedCount = 0;
        disconnectCount = 0;
        mapObjectOfflineCount = 0;
        lastMapObjectDefined = null;
        lastMapObjectAdded = null;
        lastControllerAdded = null;
        lastTopicReceived = null;
        lastMessageReceived = null;
        lastStructureReceived = null;
        lastControllerDisconnected = null;
        lastOfflineMessage = null;
        lastControllerOffline = null;
    }

    /**
     * @return The number of map objects defined with this listener since it was created or reset.
     */
    public int getMapObjectsDefinedCount()
    {
        return mapObjectsDefinedCount;
    }

    /**
     * @return The number of map objects added with this listener since it was created or reset.
     */
    public int getMapObjectsAddedCount()
    {
        return mapObjectsAddedCount;
    }

    /**
     * @return The number of data messages received by this listener since it was created or reset.
     */
    public int getDataMessagesReceivedCount()
    {
        return dataMessagesReceivedCount;
    }

    /**
     * @return The number of map objects disconnected with this listener since it was created or reset.
     */
    public int getDisconnectCount()
    {
        return disconnectCount;
    }

    /**
     * @return The number of map objects offline with this listener since it was created or reset.
     */
    public int getMapObjectOfflineCount()
    {
        return mapObjectOfflineCount;
    }

    /**
     * @return The last map object defined that was sent to this listener.
     */
    public IMapEntity getLastMapObjectDefined()
    {
        return lastMapObjectDefined;
    }

    /**
     * @return The last map object added that was sent to this listener.
     */
    public IMapObject getLastMapObjectAdded()
    {
        return lastMapObjectAdded;
    }

    /**
     * @return The last controller added that was sent to this listener.
     */
    public IRaptorDataPortalController getLastControllerAdded()
    {
        return lastControllerAdded;
    }

    /**
     * @return The last topic received by this listener.
     */
    public String getLastTopicReceived()
    {
        return lastTopicReceived;
    }

    /**
     * @return The last data message received by this listener.
     */
    public IRaptorDataMessage getLastMessageReceived()
    {
        return lastMessageReceived;
    }

    /**
     * @return The last structure received by this listener.
     */
    public IRaptorDataStructure getLastStructureReceived()
    {
        return lastStructureReceived;
    }

    /**
     * @return The last controller disconnected that was sent to this listener.
     */
    public IRaptorDataPortalController getLastControllerDisconnected()
    {
        return lastControllerDisconnected;
    }

    /**
     * @return The last offline message that was sent to this listener.
     */
    public IMapObjectOfflineMessage getLastOfflineMessage()
    {
        return lastOfflineMessage;
    }

    /**
     * @return The last offline controller that was sent to this listener.
     */
    public IRaptorDataPortalController getLastControllerOffline()
    {
        return lastControllerOffline;
    }
}
