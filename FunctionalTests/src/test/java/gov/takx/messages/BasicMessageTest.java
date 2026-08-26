package gov.takx.messages;

import gov.takx.api.dataportal.DefaultRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.eventlog.IRaptorEventLogManager;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.AMapObjectDataMonitor;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IMapObjectOfflineMessage;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.plugin.datamonitor.DataMonitorEvent;
import gov.takx.api.plugin.datamonitor.DataMonitorEventType;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.IQueryService;
import gov.takx.commons.constants.State;
import gov.takx.commons.utils.UUIDUtils;
import gov.takx.dataservices.persist.RaptorDataStructure;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.mapobject.TestMapObjectControllerListener;
import gov.takx.test.mock.mapobject.TestMapObjectManagerListener;
import gov.takx.test.mock.mapobject.TestMapObjectPropertyChangeListener;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import gov.takx.test.mock.plugin.device.TestDevicePluginController;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.beans.PropertyChangeEvent;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Basic integration test to validate that an RDM goes through the core properly.
 *
 * @since 3.4 (largely rewritten to have independent test methods)
 */
public class BasicMessageTest extends AIntegrationTest
{
    private static final String UNIT_ID = UUIDUtils.generateType1UUID().toString();
    private static final String MAP_OBJECT_KEY = KeyUtils.getUniqueKey(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID);

    private final TestMapObjectManagerListener managerListener = new TestMapObjectManagerListener();
    private final RaptorDataListener raptorDataListener = new RaptorDataListener();

    private TestDevicePlugin plugin;
    private IPrePersistRaptorDataMessage currentRDM;
    private long messageStartTime;
    private long messageEndTime;
    private int expectedEventCount = 0;
    private int totalMessageCount = 0;
    private State expectedState = State.Unknown;
    private int expectedPCECount = 0;

    private IRaptorDataPortal dataPortal;
    private IDeviceGateway deviceGateway;
    private IRaptorEventLogManager eventLogManager;
    private IMapObjectManager mapObjectManager;

    /**
     * Construct with a registered plugin so we can use it to send messages.
     */
    public BasicMessageTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);
    }

    /**
     * Register ourselves as a listener on the data portal.
     */
    @Override
    protected void preLoadState()
    {
        deviceGateway = getBean(IDeviceGateway.class);
        mapObjectManager = getBean(IMapObjectManager.class);
        eventLogManager = getBean(IRaptorEventLogManager.class);
        dataPortal = getBean(IRaptorDataPortal.class);

        dataPortal.addListener(raptorDataListener); // Track portal operation

        plugin = getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME);
    }

    @Override
    protected void postProjectOpen()
    {
        mapObjectManager.addMapObjectManagerListener(managerListener); // Track map object additions
    }

    @Override
    protected void onTeardown()
    {
        dataPortal.removeListener(raptorDataListener);
        mapObjectManager.removeMapObjectManagerListener(managerListener);
    }

    /**
     * Send a newly generated RDM using the plugin. The plugin's Gateway Controller must have been initialized before
     * calling this method. Record the time boundaries around the message processing.
     */
    private void sendMessage()
    {
        messageStartTime = System.currentTimeMillis();
        currentRDM = plugin.generateMessage(UNIT_ID);
        deviceGateway.getController(plugin.getFamily(), plugin.getType(), UNIT_ID).processParserMessage(currentRDM);
        messageEndTime = System.currentTimeMillis();
        totalMessageCount += 1;
    }

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();

        totalMessageCount = 0;
        managerListener.reset();
    }

    @Test
    public void sendInitialMessages()
    {
        sendMessage();
        expectedEventCount = 1; // Online event

        // Now validate that the system has the proper state after plugin comes online
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(1);
        checkEventLogEntryCount(expectedEventCount);

        final IMapObjectController mapObjectController = mapObjectManager.getMapObjectController(MAP_OBJECT_KEY);
        assertThat(mapObjectController).isNotNull();
        final IMapObject mapObject = mapObjectController.getMapObject();
        assertThat(mapObject).isNotNull();

        // Validate the data structures
        validateMessageStructures(RaptorDataStructure.convertToStructureCollection(mapObjectController.getDataStructures()));

        assertThat(mapObject).isSameAs(managerListener.getLastMapObjectAdded());
        assertThat(managerListener.getMapObjectsAddedCount()).isEqualTo(1);

        assertThat(mapObject.getKey()).isEqualTo(MAP_OBJECT_KEY);
        assertThat(mapObject.getType()).isEqualTo(plugin.getType());
        assertThat(mapObject.getUnitID()).isEqualTo(UNIT_ID);
        assertThat(mapObject.getHighestState()).isEqualTo(State.Unknown);

        assertThat(mapObject).isInstanceOf(IIconMapObject.class);
        IIconMapObject iconMapObject = (IIconMapObject) mapObject;
        assertThat(iconMapObject.isContained()).isFalse();
        assertThat(iconMapObject.getParentContainer()).isNull();
        assertThat(iconMapObject.getContainedMapObjects()).hasSize(0);

        IRaptorDataStructure cmoStruct = mapObject.getRaptorDataStructure(TestDevicePlugin.STRUCTURE_NAME);
        assertThat(cmoStruct).isNotNull();

        IRaptorDataStructure struct2 = plugin.getDataStructureFactory().getDataStructure(TestDevicePlugin.STRUCTURE_NAME);
        assertThat(struct2).isNotNull();

        // Now send another message and verify a few more things
        sendMessage();

        long mostRecentCommunicationTime = mapObject.getMostRecentCommunicationTime();
        assertThat(mostRecentCommunicationTime).isGreaterThanOrEqualTo(messageStartTime).isLessThanOrEqualTo(messageEndTime);
        assertThat(mapObject.getLocation()).isEqualTo(currentRDM.getLocation());
        assertThat(mapObject.getKey()).isEqualTo(MAP_OBJECT_KEY);
        assertThat(mapObject.getType()).isEqualTo(currentRDM.getType());
        assertThat(mapObject.getUnitID()).isEqualTo(currentRDM.getUnitID());
        assertThat(mapObject.getHighestState()).isEqualTo(State.Unknown);
        assertThat(mapObject).isSameAs(mapObjectController.getMapObject());
        assertThat(managerListener.getMapObjectsAddedCount()).isEqualTo(1);
        assertThat(mapObject.getLocation()).isEqualTo(currentRDM.getLocation());

        assertThat(getMessageCount()).isEqualTo(totalMessageCount);
    }

    @Test
    public void testMapObjectControllerListener()
    {
        sendMessage();
        expectedEventCount = 1; // Online event
        final IMapObjectController mapObjectController = mapObjectManager.getMapObjectController(MAP_OBJECT_KEY);
        final IMapObject mapObject = mapObjectController.getMapObject();

        final TestMapObjectControllerListener controllerListener = new TestMapObjectControllerListener();
        mapObjectController.addMapObjectControllerListener(controllerListener);

        sendMessage();
        assertThat(controllerListener.getReceivedMessagesCount()).isEqualTo(1);
        verifyReceivedRdm(controllerListener.getLastRdmReceived());
        assertThat(mapObject.getLocation()).isEqualTo(currentRDM.getLocation());

        sendMessage();
        assertThat(controllerListener.getReceivedMessagesCount()).isEqualTo(2);
        verifyReceivedRdm(controllerListener.getLastRdmReceived());
        assertThat(mapObject.getLocation()).isEqualTo(currentRDM.getLocation());

        mapObjectController.removeMapObjectControllerListener(controllerListener);

        sendMessage();
        assertThat(controllerListener.getReceivedMessagesCount()).isEqualTo(2); // No more after unregistering
        assertThat(mapObject.getLocation()).isEqualTo(currentRDM.getLocation());

        assertThat(getMessageCount()).isEqualTo(totalMessageCount);
    }

    /**
     * Verifies values in received RDM.
     *
     * @param receivedRdm The RDM received by the map object controller listener
     * @since 3.4
     */
    private void verifyReceivedRdm(IRaptorDataMessage receivedRdm)
    {
        assertThat(receivedRdm).isSameAs(currentRDM);

        // Validate custom field values
        assertThat(receivedRdm.getBoolean(TestDevicePlugin.ALARM)).isEqualTo(TestDevicePlugin.ALARM_DEFAULT);
        assertThat(receivedRdm.getInt(TestDevicePlugin.STATUS)).isEqualTo(TestDevicePlugin.STATUS_DEFAULT);
        assertThat(receivedRdm.getDouble(TestDevicePlugin.BATTERY)).isEqualTo(TestDevicePlugin.BATTERY_DEFAULT);
    }

    @Test
    public void testMapObjectPropertyChangeListener()
    {
        sendMessage();
        expectedEventCount = 1; // Online event
        final IMapObject mapObject = mapObjectManager.getMapObject(MAP_OBJECT_KEY);

        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        mapObject.addPropertyChangeListener(IMapObject.LOCATION, listener);

        // Verify first message received
        sendMessage();
        List<PropertyChangeEvent> locationEvents = listener.getEventsForEventType(IMapObject.LOCATION);
        assertThat(locationEvents).hasSize(1);
        assertThat(locationEvents.get(0).getNewValue()).isEqualTo(currentRDM.getLocation());

        listener.clearEvents();

        // Verify second message received
        sendMessage();
        locationEvents = listener.getEventsForEventType(IMapObject.LOCATION);
        assertThat(locationEvents).hasSize(1);
        assertThat(locationEvents.get(0).getNewValue()).isEqualTo(currentRDM.getLocation());

        listener.clearEvents();

        // Verify no messages received after unregistering
        mapObject.removePropertyChangeListener(IMapObject.LOCATION, listener);
        sendMessage();
        locationEvents = listener.getEventsForEventType(IMapObject.LOCATION);
        assertThat(locationEvents).hasSize(0);

        assertThat(getMessageCount()).isEqualTo(totalMessageCount);
    }

    /**
     * Validate that data monitors properly track message values and update the state of the CMO.
     */
    @Test
    public void testDataMonitors()
    {
        sendMessage();
        expectedEventCount = 1; // Online event
        final IMapObjectController mapObjectController = mapObjectManager.getMapObjectController(MAP_OBJECT_KEY);
        final IMapObject mapObject = mapObjectController.getMapObject();

        // Make the ALARM monitor "hold until cleared"
        AlarmDataMonitor alarmDataMonitor = new AlarmDataMonitor(mapObjectController);
        mapObjectController.addDataMonitor(alarmDataMonitor);
        mapObjectController.addDataMonitor(new BatteryDataMonitor(mapObjectController));
        mapObjectController.addDataMonitor(new StatusDataMonitor(mapObjectController));

        // Now send messages with appropriate field values and test the resulting state

        TestMapObjectPropertyChangeListener listener = new TestMapObjectPropertyChangeListener();
        mapObject.addPropertyChangeListener(IMapObject.HIGHEST_STATE, listener); // Track property changes

        // ALARM

        //increment the expected event count for the map object state change
        expectedEventCount += 1;
        plugin.setAlarmValue(true);

        sendMessageAndValidateDataMonitors(listener, State.Critical, true, true);

        plugin.setAlarmValue(false);    // DM is holdUntilCleared, so shouldn't change here
        sendMessageAndValidateDataMonitors(listener, State.Critical, false, false);

        // Clear the alarm.  This will cause a PCE on the map object and a state event.
        expectedState = State.Active;

        //alarm clearing causes an event to be logged so increment the expected event count
        expectedEventCount += 1;

        alarmDataMonitor.onDataMonitorEvent(new DataMonitorEvent(this, DataMonitorEventType.AlarmClear));
        validateDataMonitors(listener, true, true);

        // NOTE: Property change and event occurred above when clear() was called, so no counts will change here
        // ALSO: we get active here since the BATTERY monitors default is Active, which is > Inactive
        plugin.setAlarmValue(false);
        sendMessageAndValidateDataMonitors(listener, State.Active, false, false);

        // BATTERY

        plugin.setBatteryValue(7.0);    // Test exclusive boundary
        sendMessageAndValidateDataMonitors(listener, State.Warning, true, true);

        plugin.setBatteryValue(6.9);
        sendMessageAndValidateDataMonitors(listener, State.Critical, true, true);

        plugin.setBatteryValue(8.0);    // Test exclusive boundary
        sendMessageAndValidateDataMonitors(listener, State.Active, true, true);

        // STATUS

        // Should be no event and no property change since state remains active
        plugin.setStatusValue(1);
        sendMessageAndValidateDataMonitors(listener, State.Active, false, false);

        plugin.setStatusValue(2);
        sendMessageAndValidateDataMonitors(listener, State.Warning, true, true);

        plugin.setStatusValue(4);
        sendMessageAndValidateDataMonitors(listener, State.Critical, true, true);

        plugin.setStatusValue(1);
        sendMessageAndValidateDataMonitors(listener, State.Active, true, true);

        expectedPCECount = 0;
        listener.clearEvents();
        mapObject.removePropertyChangeListener(IMapObject.HIGHEST_STATE, listener);

        // Should be no more PCEs since we unregistered, but should be one more event
        plugin.setStatusValue(3);
        sendMessageAndValidateDataMonitors(listener, State.Warning, false, true);

        // Get this back to Active so future messages will not generate events
        plugin.setStatusValue(1);
        sendMessageAndValidateDataMonitors(listener, State.Active, false, true);

        assertThat(getMessageCount()).isEqualTo(totalMessageCount);
    }

    /**
     * Send a message and validate that the counters and states are correct.
     *
     * @param listener      The listener for property change events from the map object
     * @param expectedState The expected state after the message has been sent
     * @param bumpPCE       Increment the {@code expectedPCECount} if true
     * @param bumpEvent     Increment the {@code expectedEventCount} if true
     */
    private void sendMessageAndValidateDataMonitors(TestMapObjectPropertyChangeListener listener, State expectedState, boolean bumpPCE, boolean bumpEvent)
    {
        this.expectedState = expectedState;

        sendMessage();

        validateDataMonitors(listener, bumpPCE, bumpEvent);
    }

    /**
     * Validate that the counters and states are correct.
     *
     * @param listener  The listener for property change events from the map object
     * @param bumpPCE   Increment the {@code expectedPCECount} if true
     * @param bumpEvent Increment the {@code expectedEventCount} if true
     */
    private void validateDataMonitors(TestMapObjectPropertyChangeListener listener, final boolean bumpPCE, final boolean bumpEvent)
    {
        if (bumpPCE) expectedPCECount += 1;
        if (bumpEvent) expectedEventCount += 1;

        assertThat(listener.getCountForEventType(IMapObject.HIGHEST_STATE)).isEqualTo(expectedPCECount);

        final var mapObject = mapObjectManager.getMapObject(MAP_OBJECT_KEY);
        assertThat(mapObject.getHighestState()).isEqualTo(expectedState);
        checkEventLogEntryCount(expectedEventCount);
    }

    @Test
    public void testMapObjectDeletion()
    {
        sendMessage();
        expectedEventCount = 1; // Online event
        final IMapObject mapObject = mapObjectManager.getMapObject(MAP_OBJECT_KEY);

        TestDevicePluginController pluginController = (TestDevicePluginController) plugin.getDeviceController(mapObject.getUnitID());

        mapObjectManager.deleteMapObject(mapObject);

        assertThat(managerListener.getLastMapObjectRemoved()).isSameAs(mapObject);
        assertThat(managerListener.getMapObjectsRemovedCount()).isEqualTo(1);
        checkEventLogEntryCount(0);
        assertThat(pluginController.isDeleteCalled()).isTrue();
        assertThat(plugin.isDeleteCalled()).isTrue();
    }

    /**
     * Validate that the message structures for the plugin is as we expect.
     *
     * @param structures Collection of structures to inspect
     */
    private void validateMessageStructures(Collection<IStructure> structures)
    {
        boolean sawPluginStructure = false;
        boolean sawPluginStructureShared = false;
        boolean sawManualLocationStructure = false;

        for (IStructure rds : structures)
        {
            final String messageType = rds.getMessageType();
            switch (messageType)
            {
                case TestDevicePlugin.STRUCTURE_NAME -> sawPluginStructure = true;
                case DataStructureBuilder.LOCATION_MESSAGE_TYPE -> sawManualLocationStructure = true;
                case TestDevicePlugin.STRUCTURE_NAME_SHARED -> sawPluginStructureShared = true;
                default -> fail("Unknown structure: " + messageType);
            }
        }

        assertThat(sawPluginStructure).isTrue();
        assertThat(sawPluginStructureShared).isTrue();
        assertThat(sawManualLocationStructure).isTrue();
    }

    /**
     * @return the number of messages that have been persisted so far
     */
    private long getMessageCount()
    {
        final IMapObjectController mapObjectController = mapObjectManager.getMapObjectController(MAP_OBJECT_KEY);
        IQueryService qs = mapObjectController.getQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();
        return qs.count(qc);
    }

    @SuppressWarnings("AssignmentToSuperclassField")
    private class AlarmDataMonitor extends AMapObjectDataMonitor
    {
        private final IMapObjectController mapObjectController;

        public AlarmDataMonitor(IMapObjectController mapObjectController)
        {
            super(mapObjectController.getFamily(), mapObjectController.getType());
            this.mapObjectController = mapObjectController;
            displayName = TestDevicePlugin.ALARM;
            clearable = true;
            holdHighestAlarmState = true;
            trackHighestAlarmState = true;
            defaultState = State.Active;
            currentState = State.Active;
        }

        @Override
        public void analyze(@Nonnull IRaptorDataMessage rdm)
        {
            String alarm = TestDevicePlugin.ALARM;
            if (rdm.hasField(alarm))
            {
                boolean value = (Boolean) rdm.getField(alarm);
                State currentState = getCurrentState();
                State newState = value ? State.Critical : State.Active;
                setState(newState);

                // Alarm data monitor state has changed, so log a data monitor state change event and increment our expected event log count
                if (currentState != newState)
                {
                    eventLogManager.logDataMonitorStateChange(new Date(), mapObjectController.getMapObject(), this, newState, "[" + getDisplayName() + "] " + newState + " from " + currentState);
                    expectedEventCount += 1;
                }
            }
        }
    }

    @SuppressWarnings("AssignmentToSuperclassField")
    private class BatteryDataMonitor extends AMapObjectDataMonitor
    {
        private final IMapObjectController mapObjectController;

        public BatteryDataMonitor(IMapObjectController mapObjectController)
        {
            super(mapObjectController.getFamily(), mapObjectController.getType());
            this.mapObjectController = mapObjectController;
            displayName = TestDevicePlugin.BATTERY;
            clearable = true;
            holdHighestAlarmState = false;
            trackHighestAlarmState = false;
            defaultState = State.Active;
            currentState = State.Active;
        }

        @Override
        public void analyze(@Nonnull IRaptorDataMessage rdm)
        {
            String battery = TestDevicePlugin.BATTERY;
            if (rdm.hasField(battery))
            {
                double value = (Double) rdm.getField(battery);
                State currentState = getCurrentState();
                State newState;
                if (value < 7.0)
                {
                    newState = State.Critical;
                    setState(newState);
                } else if (value < 8.0)
                {
                    newState = State.Warning;
                    setState(newState);
                } else
                {
                    newState = State.Active;
                    setState(newState);
                }

                //Battery data monitor state has changed, so log a data monitor state change event and increment our expected event log count
                if (newState != currentState && mapObjectController != null)
                {
                    eventLogManager.logDataMonitorStateChange(new Date(), mapObjectController.getMapObject(), this, newState, "[" + getDisplayName() + "] " + newState + " from " + currentState);
                    expectedEventCount += 1;
                }
            }
        }
    }

    @SuppressWarnings("AssignmentToSuperclassField")
    private class StatusDataMonitor extends AMapObjectDataMonitor
    {
        private final IMapObjectController mapObjectController;

        public StatusDataMonitor(IMapObjectController mapObjectController)
        {
            super(mapObjectController.getFamily(), mapObjectController.getType());
            this.mapObjectController = mapObjectController;
            displayName = TestDevicePlugin.STATUS;
            defaultState = State.Active;
            currentState = State.Active;
            clearable = true;
            trackHighestAlarmState = false;
        }

        @Override
        public void analyze(@Nonnull IRaptorDataMessage rdm)
        {
            String status = TestDevicePlugin.STATUS;
            if (rdm.hasField(status))
            {
                int value = (Integer) rdm.getField(status);
                State currentState = getCurrentState();
                State newState;
                if (value < 2)
                {
                    newState = State.Active;
                    setState(State.Active);
                } else if (value < 4)
                {
                    newState = State.Warning;
                    setState(State.Warning);
                } else
                {
                    newState = State.Critical;
                    setState(State.Critical);
                }

                //Status data monitor state has changed, so log a data monitor state change event and increment our expected event log count
                if (newState != currentState && mapObjectController != null)
                {
                    eventLogManager.logDataMonitorStateChange(new Date(), mapObjectController.getMapObject(), this, newState, "[" + getDisplayName() + "] " + newState + " from " + currentState);
                    expectedEventCount += 1;
                }
            }
        }
    }

    /**
     * Listener to the data portal for RDMs.
     */
    private class RaptorDataListener extends DefaultRaptorDataListener
    {
        @Override
        public boolean matchMapObject(IMapObject mapObject)
        {
            // Should only get called for our test plugin
            assertThat(mapObject.getType()).isEqualTo(plugin.getType());
            assertThat(mapObject.getUnitID()).isEqualTo(UNIT_ID);
            return true;  // Accept all objects
        }

        @Override
        public void onMapObjectAdded(IMapObject mapObject, IRaptorDataPortalController controller)
        {
            assertThat(mapObject.getType()).isEqualTo(plugin.getType());
            assertThat(mapObject.getUnitID()).isEqualTo(UNIT_ID);
        }

        @Override
        public void onDataMessage(String topic, IRaptorDataMessage msg, IRaptorDataStructure struct)
        {
            assertThat(topic).isEqualTo(MAP_OBJECT_KEY);
            assertThat(msg).isSameAs(currentRDM);
        }

        @Override
        public void onMapObjectOffline(IMapObjectOfflineMessage msg, IRaptorDataPortalController controller, boolean wipeHistoricalData)
        {
            assertThat(msg.getMapObjectKey()).isEqualTo(MAP_OBJECT_KEY);
            assertThat(msg.getType()).isEqualTo(plugin.getType());
            assertThat(msg.getUnitID()).isEqualTo(UNIT_ID);
        }
    }
}
