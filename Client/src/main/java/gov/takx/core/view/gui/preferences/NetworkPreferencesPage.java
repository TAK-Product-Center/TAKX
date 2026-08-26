package gov.takx.core.view.gui.preferences;

import gov.takx.core.view.gui.preferences.network.HostAndIpPanel;
import gov.takx.core.view.gui.preferences.network.MeshEncryptionDialog;
import gov.takx.core.view.gui.preferences.network.NetworkInputsOutputsListPanel;
import gov.takx.core.view.gui.takserver.TakServerPanel;

import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_ADD_DEST_UID;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_DEFAULT_INPUTS_NODE;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_DEFAULT_OUTPUTS_NODE;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_ENABLE_ADDRESS_REUSE;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_MULTICAST_TTL;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_TAK_SERVER_CERT_ENROLLMENT_API_PORT;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_TAK_SERVER_SECURE_API_PORT;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_TAK_SERVER_UNSECURE_API_PORT;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_TAK_SERVER_USE_SYSTEM_CERT_AUTHORITIES;
import static gov.takx.platform.preferences.PreferenceEntry.COMMON_COMMO_TCP_CONNECTION_TIMEOUT;

/**
 * Preferences page for network related settings, including Commo ports and TAKServer connections.
 * @since 5.4
 */
public class NetworkPreferencesPage extends APreferencesPage
{
    NetworkPreferencesPage()
    {
        super("Network Preferences");

        addCustomTile(new HostAndIpPanel());

        addSectionHeader("Server Connections");
        addPageLink("Manage Server Connections", "Manage connections to TAK Server", ManageServersPage::new);
        addEditor(COMMON_COMMO_TAK_SERVER_SECURE_API_PORT, PreferencesComponentFactory.makePortNumberEditor());
        addEditor(COMMON_COMMO_TAK_SERVER_UNSECURE_API_PORT, PreferencesComponentFactory.makePortNumberEditor());
        addEditor(COMMON_COMMO_TAK_SERVER_CERT_ENROLLMENT_API_PORT, PreferencesComponentFactory.makePortNumberEditor());
        addCheckboxEditor(COMMON_COMMO_TAK_SERVER_USE_SYSTEM_CERT_AUTHORITIES);

        addSectionHeader("Input/Output Management");
        addAction(null, "Configure AES-256 Mesh Encryption", "Allows for management of AES-256 encryption for mesh network traffic",
                () -> new MeshEncryptionDialog().setVisible(true));

        addPageLink("Manage Inputs", "Add and remove inputs (Advanced)", ManageInputsPage::new);
        addPageLink("Manage Outputs", "Add and remove outputs (Advanced)", ManageOutputsPage::new);
        addPageLink("Manage Network Interfaces", "Specify which Network Interfaces to use (Advanced)", ManageNetworkInterfacesPage::new);

        addSectionHeader("Network Tweaks");
        addCheckboxEditor(COMMON_COMMO_ENABLE_ADDRESS_REUSE);
        addEditor(COMMON_COMMO_MULTICAST_TTL, PreferencesComponentFactory.makeIntSpinnerEditor(1, 255, 1));
        addEditor(COMMON_COMMO_TCP_CONNECTION_TIMEOUT, PreferencesComponentFactory.makeIntSpinnerEditor(1, 600, 1));
        addCheckboxEditor(COMMON_COMMO_ADD_DEST_UID);
    }

    private static class ManageServersPage extends APreferencesPage
    {
        public ManageServersPage()
        {
            super("Manage Server Connections");
            addCustomTile(new TakServerPanel());
        }
    }

    private static class ManageInputsPage extends APreferencesPage
    {
        public ManageInputsPage()
        {
            super("Manage Inputs");
            addCustomTile(new NetworkInputsOutputsListPanel("Input",
                    COMMON_COMMO_DEFAULT_INPUTS_NODE, false));
        }
    }

    private static class ManageOutputsPage extends APreferencesPage
    {
        public ManageOutputsPage()
        {
            super("Manage Outputs");
            addCustomTile(new NetworkInputsOutputsListPanel("Output",
                    COMMON_COMMO_DEFAULT_OUTPUTS_NODE, true));
        }
    }
}
