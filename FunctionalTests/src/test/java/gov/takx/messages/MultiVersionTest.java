package gov.takx.messages;

import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.dataportal.DefaultRaptorDataListener;
import gov.takx.api.dataportal.IRaptorDataPortal;
import gov.takx.api.dataportal.IRaptorDataPortalController;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IPrePersistRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.messages.IStructure;
import gov.takx.api.plugin.device.IDataStructureFactory;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.ConflictingConstraintException;
import gov.takx.api.query.IQueryConstraints;
import gov.takx.api.query.IQueryService;
import gov.takx.api.query.NoSuchFieldException;
import gov.takx.commons.constants.State;
import gov.takx.core.dataportal.DataPortalController;
import gov.takx.dataservices.persist.MapEntity;
import gov.takx.dataservices.persist.RaptorDataStructure;
import gov.takx.platform.KeyUtils;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.mock.mapobject.TestMapObjectManagerListener;
import gov.takx.test.mock.plugin.device.ATestDevicePlugin;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import gov.takx.test.mock.plugin.device.TestDevicePlugin2;
import gov.takx.test.mock.plugin.device.TestDevicePlugin3;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Integration test to verify that changes in message structure is handled correctly.
 *
 * @since 3.4 (largely rewritten to have independent test methods)
 */
public class MultiVersionTest extends AIntegrationTest
{
    private final TestMapObjectManagerListener managerListener = new TestMapObjectManagerListener();
    private final RaptorDataListener raptorDataListener = new RaptorDataListener();

    private IRaptorDataPortal dataPortal;
    private IDeviceGateway deviceGateway;
    private IMapObjectManager mapObjectManager;

    private int expectedEventCount = 0;

    private final PluginInfo pluginInfo1 = new PluginInfo();
    private final PluginInfo pluginInfo2 = new PluginInfo();
    private final PluginInfo pluginInfo3 = new PluginInfo();

    /**
     * Construct with a registered plugin so we can use it to send messages.
     */
    public MultiVersionTest()
    {
        setPluginsToRegister(TestDevicePlugin.class, TestDevicePlugin2.class, TestDevicePlugin3.class);
    }

    /**
     * Register ourself as a listener on the data portal.
     */
    @Override
    protected void preLoadState()
    {
        mapObjectManager = getBean(IMapObjectManager.class);
        deviceGateway = getBean(IDeviceGateway.class);
        dataPortal = getBean(IRaptorDataPortal.class);

        pluginInfo1.setPlugin(getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME));
        pluginInfo2.setPlugin(getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin2.DEVICE_NAME));
        pluginInfo3.setPlugin(getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin3.DEVICE_NAME));

        dataPortal.addListener(raptorDataListener); // Track portal operation
    }

    @Override
    protected void postProjectOpen()
    {
        mapObjectManager.addMapObjectManagerListener(managerListener); // Track map object additions
    }

    @BeforeEach
    public void setupBeforeMethod()
    {
        // Set gateway controllers.
        pluginInfo1.generateGatewayController(deviceGateway);
        pluginInfo2.generateGatewayController(deviceGateway);
        pluginInfo3.generateGatewayController(deviceGateway);

        sendParserMessage(pluginInfo1);
        sendDeviceMessage(pluginInfo2);

        expectedEventCount = 2;    // Online event

        // Now validate that the system has the proper state after plugin comes online
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(2);
        checkEventLogEntryCount(expectedEventCount);

        pluginInfo1.mapObjectController = mapObjectManager.getMapObjectController(pluginInfo1.key);
        assertThat(pluginInfo1.mapObjectController).isNotNull();

        pluginInfo2.mapObjectController = mapObjectManager.getMapObjectController(pluginInfo2.key);
        assertThat(pluginInfo2.mapObjectController).isNotNull();
    }

    @Override
    protected void reboot() throws IOException
    {
        super.reboot();

        // The MOCs need to be refreshed or else they'll have a reference to the old PersistService.
        pluginInfo1.mapObjectController = mapObjectManager.getMapObjectController(pluginInfo1.key);
        pluginInfo2.mapObjectController = mapObjectManager.getMapObjectController(pluginInfo2.key);
        pluginInfo3.mapObjectController = mapObjectManager.getMapObjectController(pluginInfo3.key);

        // The gateway controllers need to be refreshed because the delegate is remove during shutdown
        pluginInfo1.generateGatewayController(deviceGateway);
        pluginInfo2.generateGatewayController(deviceGateway);
        pluginInfo3.generateGatewayController(deviceGateway);
    }

    @AfterEach
    public void cleanupAfterMethod()
    {
        deleteAllMapObjects();

        pluginInfo1.reset();
        pluginInfo2.reset();
        pluginInfo3.reset();

        // May be null if this test was skipped
        if (dataPortal != null) dataPortal.removeListener(raptorDataListener);
        if (mapObjectManager != null) mapObjectManager.removeMapObjectManagerListener(managerListener);
    }

    /**
     * Send a newly generated RDM using the plugin.  The plugin's Gateway Controller must have been initialized before
     * calling this method.  Record the time boundaries around the message processing.
     */
    private void sendParserMessage(PluginInfo pluginInfo)
    {
        pluginInfo.startMessage(() -> pluginInfo.plugin.generateMessage());
        pluginInfo.gatewayController.processParserMessage(pluginInfo.currentMessage);
        pluginInfo.endMessage(pluginInfo, pluginInfo);
    }

    /**
     * Send a newly generated RDM using the shared structure of the plugin.
     * Record the time boundaries around the message processing.
     */
    private void sendSharedStructureParserMessage(PluginInfo pluginInfo)
    {
        pluginInfo.startMessage(() -> pluginInfo.plugin.generateSharedMessage());
        pluginInfo.gatewayController.processParserMessage(pluginInfo.currentMessage);
        pluginInfo.endMessage(pluginInfo, pluginInfo);
    }

    private void sendPluginControllerMessageAcrossBoundary(PluginInfo source, PluginInfo dest) throws IllegalAccessException
    {
        ATestDevicePlugin sourcePlugin = source.plugin;
        ATestDevicePlugin destPlugin = dest.plugin;

        source.startMessage(() -> sourcePlugin.generateSharedMessage(destPlugin.getFamily(), destPlugin.getType()));
        source.mapObjectController.sendToDevice(source.currentMessage, true, "");
        source.endMessage(source, dest);
    }

    private void sendGatewayControllerMessageAcrossBoundary(PluginInfo source, PluginInfo dest)
    {
        ATestDevicePlugin sourcePlugin = source.plugin;
        ATestDevicePlugin destPlugin = dest.plugin;

        source.startMessage(() -> sourcePlugin.generateSharedMessage(destPlugin.getFamily(), destPlugin.getType()));
        source.gatewayController.processParserMessage(source.currentMessage);
        source.endMessage(source, dest);
    }

    /**
     * Send a newly generated RDM using the plugin2.  The plugin's Gateway Controller must have been initialized before
     * calling this method.
     */
    private void sendDeviceMessage(PluginInfo pluginInfo)
    {
        pluginInfo.startMessage(() -> pluginInfo.plugin.generateMessage());
        pluginInfo.gatewayController.processDeviceMessage(pluginInfo.currentMessage);
        pluginInfo.endMessage(pluginInfo, pluginInfo);
    }

    /**
     * Send a newly generated RDM.  The plugin's Gateway Controller must have been initialized before
     * calling this method.
     */
    private void sendSharedStructureDeviceMessage(PluginInfo pluginInfo)
    {
        pluginInfo.startMessage(() -> pluginInfo.plugin.generateSharedMessage());
        pluginInfo.gatewayController.processDeviceMessage(pluginInfo.currentMessage);
        pluginInfo.endMessage(pluginInfo, pluginInfo);
    }

    @Test
    public void sendInitialMessages()
    {
        // Validate the data structures
        validateMessageStructures(RaptorDataStructure.convertToStructureCollection(pluginInfo1.mapObjectController.getDataStructures()));

        assertThat(managerListener.getMapObjectsAddedCount()).isEqualTo(2);

        final IMapObject plugin1MapObject = pluginInfo1.mapObjectController.getMapObject();
        validatePlugin(plugin1MapObject, pluginInfo1, TestDevicePlugin.STRUCTURE_NAME);

        final IMapObject plugin2MapObject = pluginInfo2.mapObjectController.getMapObject();
        validatePlugin(plugin2MapObject, pluginInfo2, TestDevicePlugin2.STRUCTURE_NAME);

        assertThat(plugin1MapObject.getRaptorDataStructure(TestDevicePlugin.STRUCTURE_NAME_SHARED).getStructure())
                .isSameAs(plugin2MapObject.getRaptorDataStructure(TestDevicePlugin.STRUCTURE_NAME_SHARED).getStructure());

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(2); // still 2
        checkEventLogEntryCount(expectedEventCount); // still 2
        assertThat(pluginInfo1.mapObjectController).isSameAs(mapObjectManager.getMapObjectController(pluginInfo1.key));

        long mostRecentCommunicationTime = plugin1MapObject.getMostRecentCommunicationTime();
        assertThat(mostRecentCommunicationTime).isGreaterThanOrEqualTo(pluginInfo1.sendStartTime).isLessThanOrEqualTo(pluginInfo1.sendEndTime);

        assertThat(plugin1MapObject.getLocation()).isEqualTo(pluginInfo1.currentMessage.getLocation());
        assertThat(plugin1MapObject.getKey()).isEqualTo(pluginInfo1.key);
        assertThat(plugin1MapObject.getType()).isEqualTo(pluginInfo1.currentMessage.getType());
        assertThat(plugin1MapObject.getUnitID()).isEqualTo(pluginInfo1.currentMessage.getUnitID());
        assertThat(plugin1MapObject.getHighestState()).isEqualTo(State.Unknown);

        // Now send another message and verify a few more things
        sendParserMessage(pluginInfo1);

        mostRecentCommunicationTime = plugin1MapObject.getMostRecentCommunicationTime();
        assertThat(mostRecentCommunicationTime).isGreaterThanOrEqualTo(pluginInfo1.sendStartTime).isLessThanOrEqualTo(pluginInfo1.sendEndTime);
        assertThat(plugin1MapObject).isSameAs(pluginInfo1.mapObjectController.getMapObject());

        // Now send some shared messages
        sendSharedStructureParserMessage(pluginInfo1);
        sendSharedStructureDeviceMessage(pluginInfo2);

        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(2); // still 2
        assertThat(plugin1MapObject.getLocation()).isEqualTo(pluginInfo1.currentMessage.getLocation());

        assertThat(getMessageCount(pluginInfo1)).isEqualTo(pluginInfo1.totalMessageCount);
        assertThat(pluginInfo1.currentMessage.getStructure()).isEqualTo(pluginInfo2.currentMessage.getStructure());
    }

    /**
     * Validate a Map Object's configuration.
     *
     * @since 3.0
     */
    private void validatePlugin(IMapObject mapObject, PluginInfo pluginInfo, String structureName)
    {
        assertThat(mapObject).isInstanceOf(IIconMapObject.class);
        final IIconMapObject iconMapObject = (IIconMapObject) mapObject;

        assertThat(iconMapObject.getKey()).isEqualTo(pluginInfo.key);
        assertThat(iconMapObject.getType()).isEqualTo(pluginInfo.plugin.getType());
        assertThat(iconMapObject.isContained()).isFalse();
        assertThat(iconMapObject.getParentContainer()).isNull();
        assertThat(iconMapObject.getContainedMapObjects()).hasSize(0);
        assertThat(mapObject.getHighestState()).isEqualTo(State.Unknown);
        assertThat(mapObject.getRaptorDataStructure(structureName)).isNotNull();

        final IDataStructureFactory factory = pluginInfo.plugin.getDataStructureFactory();
        assertThat(factory.getDataStructure(structureName)).isNotNull();
        assertThat(factory.getDataStructure(TestDevicePlugin.STRUCTURE_NAME_SHARED)).isNotNull();
    }

    @Test
    public void sendRoutedMessage() throws IllegalAccessException
    {
        sendPluginControllerMessageAcrossBoundary(pluginInfo1, pluginInfo3);

        pluginInfo3.mapObjectController = mapObjectManager.getMapObjectController(pluginInfo3.key);
        assertThat(pluginInfo3.mapObjectController).isNotNull();
        assertThat(mapObjectManager.getMapObjectControllers()).hasSize(3);
        assertThat(getMessageCount(pluginInfo3)).isEqualTo(1);

        sendGatewayControllerMessageAcrossBoundary(pluginInfo1, pluginInfo3);
        assertThat(getMessageCount(pluginInfo3)).isEqualTo(2);
    }

    @Test
    @SuppressLogging({DataPortalController.class, MapEntity.class})
    public void testStructureChange() throws IOException, ConflictingConstraintException, NoSuchFieldException
    {
        shutdown();
        pluginInfo1.plugin.initStructures2();
        pluginInfo2.plugin.initStructures1();
        reboot();

        sendParserMessage(pluginInfo1);

        IQueryService qs = pluginInfo1.mapObjectController.getQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();

        // 2 status messages were sent in setupBeforeMethod() with the first status structure.
        // 1 status message was sent with the second status structure.
        // If the query is spanning multiple structure versions, as it should, we should get 3 results.
        qc.addFieldConstraint(TestDevicePlugin.STRUCTURE_NAME, TestDevicePlugin.STATUS, ComparisonType.EQ, TestDevicePlugin.STATUS_DEFAULT);

        assertThat(qs.count(qc)).isEqualTo((long) pluginInfo1.structureMessageCount.get(TestDevicePlugin.STRUCTURE_NAME));
    }

    @Test
    @SuppressLogging(MapEntity.class)
    public void testUpgradePlugin() throws IOException, ConflictingConstraintException, NoSuchFieldException
    {
        shutdown();
        pluginInfo1.plugin.initStructures2();
        pluginInfo2.plugin.initStructures1();
        reboot();

        sendParserMessage(pluginInfo1);

        shutdown();
        pluginInfo1.plugin.initStructures3();
        pluginInfo2.plugin.initStructures1();
        reboot();

        sendParserMessage(pluginInfo1);

        IQueryService qs = pluginInfo1.mapObjectController.getQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();

        // 2 status messages were sent in setupBeforeMethod() with the first status structure.
        // 1 status message was sent above with the second status structure.
        // 1 status message was sent above
        // If the query is spanning multiple structure versions, as it should, we should get 3 results.
        qc.addFieldConstraint(TestDevicePlugin.STRUCTURE_NAME, TestDevicePlugin.STATUS, ComparisonType.EQ, TestDevicePlugin.STATUS_DEFAULT);

        assertThat(qs.count(qc)).isEqualTo((long) pluginInfo1.structureMessageCount.get(TestDevicePlugin.STRUCTURE_NAME));
    }

    @Test
    @SuppressLogging(MapEntity.class)
    public void testDowngradePlugin() throws ConflictingConstraintException, NoSuchFieldException, IOException
    {
        shutdown();
        pluginInfo1.plugin.initStructures2();
        pluginInfo2.plugin.initStructures1();
        reboot();

        sendParserMessage(pluginInfo1);

        shutdown();
        pluginInfo1.plugin.initStructures3();
        pluginInfo2.plugin.initStructures1();
        reboot();

        sendParserMessage(pluginInfo1);

        shutdown();
        pluginInfo1.plugin.initStructures1();
        pluginInfo2.plugin.initStructures1();
        reboot();

        sendParserMessage(pluginInfo1);

        IQueryService qs = pluginInfo1.mapObjectController.getQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();

        // 2 status messages were sent in setupBeforeMethod() with the first status structure.
        // 1 status message was sent in testUpgradePlugin() with the second status structure.
        // 1 status message was sent in testUpgradePlugin()
        // 1 status message was sent above
        qc.addFieldConstraint(TestDevicePlugin.STRUCTURE_NAME, TestDevicePlugin.STATUS, ComparisonType.EQ, TestDevicePlugin.STATUS_DEFAULT);

        assertThat(qs.count(qc)).isEqualTo((long) pluginInfo1.structureMessageCount.get(TestDevicePlugin.STRUCTURE_NAME));
    }

    /**
     * Validate that the message structures for the plugin are as we expect.
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
    private long getMessageCount(PluginInfo pluginInfo)
    {
        IQueryService qs = pluginInfo.mapObjectController.getQueryService();
        IQueryConstraints qc = qs.createEmptyConstraints();
        return qs.count(qc);
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
            if (mapObject.getType().equals(pluginInfo1.plugin.getType()))
            {
                assertThat(mapObject.getKey()).withFailMessage("Keys equal").isEqualTo(pluginInfo1.key);
            }
            return true;  // Accept all objects
        }

        @Override
        public void onMapObjectAdded(IMapObject mapObject, IRaptorDataPortalController controller)
        {
            if (mapObject.getType().equals(pluginInfo1.plugin.getType()))
            {
                assertThat(mapObject.getKey()).withFailMessage("Keys equal").isEqualTo(pluginInfo1.key);
            }
        }

        @Override
        public void onDataMessage(String topic, IRaptorDataMessage msg, IRaptorDataStructure struct)
        {
            assertThat(struct).isNotNull();
            assertThat(msg.getStructure()).isNotNull();
        }
    }

    private static class PluginInfo
    {
        ATestDevicePlugin plugin;
        String key;
        IMapObjectController mapObjectController;
        IGatewayController gatewayController;
        IPrePersistRaptorDataMessage currentMessage;

        long sendStartTime;
        long sendEndTime;
        final Map<String, Integer> structureMessageCount = new HashMap<>();
        int totalMessageCount;

        void setPlugin(ATestDevicePlugin plugin)
        {
            this.plugin = plugin;
            key = KeyUtils.getUniqueKey(plugin.getFamily(), plugin.getType(), ATestDevicePlugin.DEFAULT_UNIT_ID);
        }

        public void generateGatewayController(IDeviceGateway deviceGateway)
        {
            gatewayController = deviceGateway.getController(key, false);
        }

        /**
         * Record the start of processing for the given message.
         */
        void startMessage(Supplier<IPrePersistRaptorDataMessage> messageSupplier)
        {
            sendStartTime = System.currentTimeMillis();
            currentMessage = messageSupplier.get();
        }

        /**
         * Record the end of processing a message, update the counts on the destination using the source message type.
         */
        void endMessage(PluginInfo source, PluginInfo dest)
        {
            sendEndTime = System.currentTimeMillis();
            dest.incrementMessageCount(source.currentMessage.getMessageType());
        }

        void incrementMessageCount(String structure)
        {
            structureMessageCount.merge(structure, 1, Integer::sum);
            totalMessageCount++;
        }

        /**
         * Resets message state.
         */
        void reset()
        {
            structureMessageCount.clear();
            totalMessageCount = 0;
            currentMessage = null;
        }
    }
}
