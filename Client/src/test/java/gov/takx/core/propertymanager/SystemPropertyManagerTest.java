package gov.takx.core.propertymanager;

import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.gateway.IDeviceGateway;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.plugin.gateway.IGatewayController;
import gov.takx.api.preferences.IPreferenceEntry;
import gov.takx.api.preferences.IPreferenceEntryChangeListener;
import gov.takx.api.preferences.IUserPreferenceManager;
import gov.takx.api.propertymanager.ISystemPropertyChangeListener;
import gov.takx.api.propertymanager.ISystemPropertyManager;
import gov.takx.api.propertymanager.SystemPropertyChangeEvent;
import gov.takx.api.propertymanager.SystemPropertyType;
import gov.takx.commons.media.AudioAlert;
import gov.takx.platform.KeyUtils;
import gov.takx.platform.preferences.ProjectPreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.extension.DependsOn;
import gov.takx.test.extension.DependsOnOrdering;
import gov.takx.test.mock.plugin.device.TestDevicePlugin;
import gov.takx.test.utils.SwingTestUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.IOException;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests the System Property Manager
 *
 */
@SuppressWarnings("removal")
@TestMethodOrder(DependsOnOrdering.class)
public class SystemPropertyManagerTest extends AIntegrationTest
{
    private TestDevicePlugin plugin;
    private static final String UNIT_ID_1 = "SYSTEM_PROPERTY_MANAGER_TEST_UNIT_5150";
    private final String mapObjectKey1 = KeyUtils.getUniqueKey(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID_1);
    private IGatewayController gc1;
    private IMapObjectManager mom;

    private final SystemPropertyManagerListener spml1 = new SystemPropertyManagerListener();
    private final SystemPropertyManagerListener spml2 = new SystemPropertyManagerListener();

    public SystemPropertyManagerTest()
    {
        setPluginsToRegister(TestDevicePlugin.class);
    }

    /**
     * Reboot the core - bring our plugin online afterward.
     */
    @Override
    protected void reboot() throws IOException
    {
        super.reboot();
        bringPluginOnline();
    }

    /**
     * Send newly generated RDM using both plugins.  The plugin's Gateway Controller must have been initialized before
     * calling this method.
     */
    private void sendMessages()
    {
        gc1.processParserMessage(plugin.generateMessage(UNIT_ID_1));
    }

    /**
     * Initialize the plugin instance (bring it online).
     */
    @Test
    @Disabled// TODO KMB: Fix test (TAKX-1333)
    public void bringPluginOnline()
    {
        plugin = getPluginInstance(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME);

        mom = getBean(IMapObjectManager.class);
        final IDeviceGateway gateway = getBean(IDeviceGateway.class);
        gc1 = gateway.getController(TestDevicePlugin.FAMILY, TestDevicePlugin.DEVICE_NAME, UNIT_ID_1);
    }

    @Test
    @DependsOn("bringPluginOnline")
    @Disabled // TODO KMB: Fix test (TAKX-1333)
    public void doTest()
    {
        sendMessages();

        IMapObjectController moc1 = mom.getMapObjectController(mapObjectKey1);
        //should only be 1 map object
        assertThat(mom.getMapObjectControllers()).withFailMessage("MOC count").hasSize(1);
        assertThat(moc1).withFailMessage("MOC exists").isNotNull();
        assertThat(moc1.getMapObject().getUnitID()).withFailMessage("Unit ID equal").isEqualTo(UNIT_ID_1);
        ISystemPropertyManager manager = moc1.getSystemPropertyManager();
        manager.addSystemPropertyChangeListener(spml1);

        //Test changing preferences
        final IUserPreferenceManager userPreferenceManager = getBean(IUserPreferenceManager.class);
        final Set<IPreferenceEntry> changesSeen = new HashSet<>();
        final IPreferenceEntryChangeListener changeListener = change -> changesSeen.add(change.getPreferenceEntry());

        userPreferenceManager.addPreferenceEntryChangeListener(changeListener);

        UserPreferenceEntry.AUDIO_ALERT.put(AudioAlert.ALERTS_OFF.getName());
        UserPreferenceEntry.COORDINATE_SYSTEM.put(CoordinateSystem.DECIMAL_DEGREES.getName());
        ProjectPreferenceEntry.WMS_OFFLINE_MODE.put(false);

        // The SystemPropertyManager delivers events on the EDT, so invoke a no-op invokeAndWait to ensure it has run.
        SwingTestUtils.waitForEdtEvents();

        userPreferenceManager.removePreferenceEntryChangeListener(changeListener);

        assertThat(manager.isWMSNetworkOnline()).withFailMessage("Failed global is wms online test").isTrue();

        assertThat(changesSeen).withFailMessage("Incorrect number of changes").hasSize(3);
        assertThat(changesSeen).containsOnly(UserPreferenceEntry.AUDIO_ALERT, UserPreferenceEntry.COORDINATE_SYSTEM);

        assertThatIllegalArgumentException().isThrownBy(() -> manager.addSystemPropertyChangeListener(null));
        assertThatIllegalArgumentException().isThrownBy(() -> manager.removeSystemPropertyChangeListener(null));

        // Remove these listeners so that even if the tests below them fail, they don't remain registered
        assertThat(manager.removeSystemPropertyChangeListener(spml2)).withFailMessage("Failed remove non added listener test").isFalse();
        assertThat(manager.removeSystemPropertyChangeListener(spml1)).withFailMessage("Failed remove added listener test").isTrue();

        assertThat(manager.getCoordinateSystem().getName()).withFailMessage("Failed get coordinate system test").isEqualTo(CoordinateSystem.DECIMAL_DEGREES.getName());
        assertThat(manager.getAudioAlertLevel().getName()).withFailMessage("Failed get audio threshold test").isEqualTo(AudioAlert.ALERTS_OFF.getName());

        assertThat(spml1.receivedAllPropertyTypes()).withFailMessage("Failed to receive all property types, missed: " + spml1.getMissedTypes()).isTrue();
    }

    /**
     * Test listener for receiving all property types
     */
    private static final class SystemPropertyManagerListener implements ISystemPropertyChangeListener
    {
        // Make a set containing all the property types
        private static final Set<SystemPropertyType> allPropertyTypes = EnumSet.allOf(SystemPropertyType.class);

        private final Set<SystemPropertyType> receivedPropertyTypes = EnumSet.noneOf(SystemPropertyType.class);

        @Override
        public void onSystemPropertyChange(SystemPropertyChangeEvent event)
        {
            receivedPropertyTypes.add(event.getPropertyType());
        }

        /**
         * Check if all property types were caught
         *
         * @return true if all the property types were seen
         */
        public boolean receivedAllPropertyTypes()
        {
            return receivedPropertyTypes.containsAll(allPropertyTypes);
        }

        /**
         * @return String listing all the property types NOT seen during the test
         */
        public String getMissedTypes()
        {
            final Set<SystemPropertyType> missingTypes = EnumSet.allOf(SystemPropertyType.class);
            missingTypes.removeAll(receivedPropertyTypes);

            return String.valueOf(missingTypes);
        }
    }
}
