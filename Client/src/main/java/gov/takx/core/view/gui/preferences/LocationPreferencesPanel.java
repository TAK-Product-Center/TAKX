package gov.takx.core.view.gui.preferences;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.location.ILocationProvider;
import gov.tak.api.location.ILocationProviderConfiguration;
import gov.tak.api.location.ILocationProviderManager;
import gov.takx.api.annotation.PseudoBean;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;
import java.awt.Font;

/**
 * Panel to allow the user to select and configure a Location Provider.
 *
 * @since 3.0
 */
@PseudoBean
public class LocationPreferencesPanel extends JPanel
{
    @Inject
    private ILocationProviderManager locationProviderManager;

    public LocationPreferencesPanel()
    {
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        // Put all the known location providers in the combo box model
        providerComboBox.setModel(new DefaultComboBoxModel<>(
                locationProviderManager.getLocationProviders().stream()
                        .map(ProviderAndConfig::new)
                        .toArray(ProviderAndConfig[]::new)));

        // Select the active one
        providerComboBox.setSelectedItem(new ProviderAndConfig(locationProviderManager.getActiveLocationProvider()));
    }

    /**
     * Update UI state and install the selected provider if it is properly configured.
     * @since 5.4
     */
    private void updateAndInstallIfConfigured()
    {
        final var providerAndConfig = getSelectedProvider();
        final var selectedProvider = providerAndConfig.provider;
        final var configuration = providerAndConfig.configuration;
        final boolean configurable = selectedProvider.isConfigurable();

        // Since changes should be "immediately" applied, we need to determine if the selected provider can be
        // installed.  It must either not require a configuration, or we must have a non-empty configuration.
        final boolean canInstall = !configurable || !configuration.isEmpty();

        if (canInstall)
        {
            selectedProvider.configure(configuration);
            locationProviderManager.setActiveLocationProvider(selectedProvider.getUniqueKey());
        }

        btnConfigure.setEnabled(configurable);
        lblConfig.setVisible(!canInstall);
        repaint();
    }

    /**
     * User selected a location provider.  Show configuration label and enable button as appropriate.
     */
    private void providerComboBoxActionPerformed()
    {
        if (getSelectedProvider() != null) updateAndInstallIfConfigured();
    }

    /**
     * @return The currently selected location provider
     */
    private ProviderAndConfig getSelectedProvider()
    {
        return (ProviderAndConfig) providerComboBox.getSelectedItem();
    }

    /**
     * Launch the configuration UI for the selected provider.
     */
    private void btnConfigureActionPerformed()
    {
        final var providerAndConfig = getSelectedProvider();

        final var configuration = providerAndConfig.provider.showConfiguration(providerAndConfig.configuration);
        final boolean gotConfiguration = configuration != null;

        // Install new configuration to model
        if (gotConfiguration)
        {
            providerAndConfig.configuration = configuration;
            updateAndInstallIfConfigured();
        }
    }

    /**
     * Tuple class to hold a LocationProvider and an associated Configuration.  Equality is based on the provider,
     * the configuration is not considered.
     */
    private static final class ProviderAndConfig
    {
        private final ILocationProvider provider;
        private ILocationProviderConfiguration configuration;

        private ProviderAndConfig(ILocationProvider provider)
        {
            this.provider = provider;
            configuration = provider.getConfiguration();
        }

        @Override
        public String toString()
        {
            return provider.isConfigurable()
                    ? provider.getDisplayName() + " [" + configuration.getDisplayableString() + "]"
                    : provider.getDisplayName();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            return provider == ((ProviderAndConfig) o).provider;
        }

        @Override
        public int hashCode()
        {
            return provider.hashCode();
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var label1 = new JLabel();
        providerComboBox = new JComboBox<>();
        btnConfigure = new JButton();
        lblConfig = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.DLU2_BORDER);
        setName("this");
        setLayout(new FormLayout(
                "default:grow, $lcgap, $button",
                "2*(default, $lgap), default"));

        //---- label1 ----
        label1.setText("Select a Location Provider");
        label1.setFont(label1.getFont().deriveFont(label1.getFont().getStyle() | Font.BOLD, label1.getFont().getSize() + 1f));
        label1.setName("label1");
        add(label1, cc.xywh(1, 1, 3, 1));

        //---- providerComboBox ----
        providerComboBox.setName("providerComboBox");
        providerComboBox.addActionListener(e -> providerComboBoxActionPerformed());
        add(providerComboBox, cc.xy(1, 3));

        //---- btnConfigure ----
        btnConfigure.setText("Configure...");
        btnConfigure.setName("btnConfigure");
        btnConfigure.addActionListener(e -> btnConfigureActionPerformed());
        add(btnConfigure, cc.xy(3, 3));

        //---- lblConfig ----
        lblConfig.setText("This location provider requires configuration");
        lblConfig.setForeground(Color.green);
        lblConfig.setName("lblConfig");
        add(lblConfig, cc.xy(1, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox<ProviderAndConfig> providerComboBox;
    private JButton btnConfigure;
    private JLabel lblConfig;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
