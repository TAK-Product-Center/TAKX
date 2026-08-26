package gov.takx.core.view.gui.preferences;

import gov.tak.core.comms.commo.INetworkDeviceManager;
import gov.tak.core.comms.commo.NetworkDevice;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.security.EncodingUtils;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JCheckBox;
import javax.swing.JPanel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_NETWORK_INTERFACES_NODE;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_USE_ALL_NETWORK_INTERFACES;

/**
 * Preferences page managing the list of network interfaces that are to be used for communications.
 * 
 * @since 5.4
 */
@PseudoBean
class ManageNetworkInterfacesPage extends APreferencesPage
{
    @Inject
    private INetworkDeviceManager networkDeviceManager;

    private final List<JPanel> devicePanels = new ArrayList<>();

    ManageNetworkInterfacesPage()
    {
        super("Manage Network Interfaces");
        addCheckboxEditor(COMMON_COMMO_USE_ALL_NETWORK_INTERFACES, this::updatePanelVisibility);
    }

    @PostConstruct
    private void postConstruct()
    {
        final var node = COMMON_COMMO_NETWORK_INTERFACES_NODE.node();

        // Add a panel for each network device
        networkDeviceManager.getNetworkDevices().stream()
                .sorted(Comparator.comparing(NetworkDevice::getDisplayName))
                .forEach(device -> addDevicePanel(device, node));

        updatePanelVisibility(COMMON_COMMO_USE_ALL_NETWORK_INTERFACES.getBooleanValue());
    }

    /**
     * Add a panel for a single network device.
     */
    private void addDevicePanel(@Nonnull NetworkDevice device, @Nonnull PreferencesNode node)
    {
        final var checkBox = new JCheckBox();
        checkBox.setSelected(node.getBoolean(device.getDisplayName(), true));
        checkBox.addItemListener(e -> setDeviceEnabled(device, checkBox.isSelected()));

        final var devicePanel = addCustomTile(device.getDisplayName(),
                "MAC Address: " + EncodingUtils.bytesToHex(device.getMacAddress()),
                checkBox, checkBox::doClick);
        devicePanels.add(devicePanel);
    }

    /**
     * @param useAll Pass {@code true} if all interfaces are to be used, this results in the individual interface
     *               panels being hidden.  If {@code false}, the interface panels will be shown.
     */
    private void updatePanelVisibility(boolean useAll)
    {
        devicePanels.forEach(p -> p.setVisible(!useAll));
        repaint();
    }

    /**
     * Enable/disable a given network device.
     */
    private void setDeviceEnabled(@Nonnull NetworkDevice device, boolean enabled)
    {
        final var node = COMMON_COMMO_NETWORK_INTERFACES_NODE.node();
        node.put(device.getDisplayName(), enabled);

        // Notify listeners that things have changed
        doInBackground(() -> {
            COMMON_COMMO_NETWORK_INTERFACES_NODE.getPreferenceManager().savePreferences();
            COMMON_COMMO_NETWORK_INTERFACES_NODE.firePreferenceEntryChangeEvent();
        });
    }
}
