package gov.takx.core.view.gui.preferences.network;

import com.google.common.annotations.VisibleForTesting;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.comms.commo.ConnectionConfiguration;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.commons.swing.ActionPopupFactory;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
import java.util.function.Consumer;
import java.util.function.Function;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * Displays a network input or output configuration.
 * 
 * @since 5.0
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class NetworkInputOutputPanel extends JPanel
{
    private static final ImageIcon DELETE_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "delete.png");

    @Inject
    private ICoreExecutorService executorService;

    private final ConnectionConfiguration configuration;
    private final PreferenceEntry preferenceEntry;
    private final String type;
    private final Consumer<NetworkInputOutputPanel> deleteConsumer;

    /**
     * @param preferenceEntry The parent preference entry that is the parent of the configuration this panel will represent
     * @param configurationNode The network input or output {@link PreferencesNode} that this panel will represent
     * @param type The network type (i.e. "input" or "output") for use in error messages
     * @param deleteConsumer Called when the user deletes this configuration
     * @param duplicateDescriptionChecker Function that takes a description String and returns {@code true} if the description is already in use
     */
    public NetworkInputOutputPanel(@Nonnull PreferenceEntry preferenceEntry, @Nonnull PreferencesNode configurationNode,
                                   @Nonnull String type, @Nonnull Consumer<NetworkInputOutputPanel> deleteConsumer,
                                   @Nonnull Function<String, Boolean> duplicateDescriptionChecker, boolean udpOnly)
    {
        this.preferenceEntry = preferenceEntry;
        this.type = type;
        this.deleteConsumer = deleteConsumer;
        configuration = new ConnectionConfiguration(configurationNode);

        initComponents();

        deleteButton.setIcon(DELETE_ICON);

        setValuesFromPreference();

        editingPanel.setListeners(this::okay, this::cancel, duplicateDescriptionChecker);
        editingPanel.setVisible(false);
        if (udpOnly) editingPanel.setUdpOnly();
    }

    /**
     * @return The {@link PreferencesNode} represented by this panel
     */
    @Nonnull
    public PreferencesNode getNode()
    {
        return configuration;
    }

    /**
     * Called when the user edits the configuration and clicks "Okay".
     */
    @VisibleForTesting
    void okay()
    {
        final String description = editingPanel.getDescription();
        final String uri = editingPanel.getUri();

        // update local UI elements
        descriptionLabel.setText(description);
        uriLabel.setText(uri);

        // Update the preference node values

        // By default, the node name is used as the description.
        if (description.equals(configuration.name()))
        {
            configuration.clearDisplayName(); // They are the same, so remove the display name entry
        } else
        {
            configuration.setDisplayName(description);
        }

        configuration.setEnabled(enableCheckBox.isSelected());
        configuration.setUri(URI.create(uri));

        // todo: TAKX-3350 only fire if changes are detected
        // Use executor service to get the update logic off the UI thread
        executorService.execute(() -> {
            Thread.currentThread().setName("Update " + type + " configuration");
            preferenceEntry.getPreferenceManager().savePreferences();
            preferenceEntry.firePreferenceEntryChangeEvent();
        });

        editingPanel.setVisible(false);
    }

    /**
     * Called when the user edits the configuration but clicks "Cancel". 
     */
    @VisibleForTesting
    void cancel()
    {
        editingPanel.setFromConfiguration(configuration);
        editingPanel.setVisible(false);
    }

    /**
     * Opens the editing panel when the configuration description is clicked.
     */
    private void descriptionPanelMouseClicked(MouseEvent e)
    {
        if (e.getButton() == MouseEvent.BUTTON1) editingPanel.setVisible(true);
    }

    /**
     * Sets the description, tracking fields and editing panel values to those stored in the 
     * {@link ConnectionConfiguration}.
     */
    private void setValuesFromPreference()
    {
        enableCheckBox.setSelected(configuration.isEnabled());
        descriptionLabel.setText(configuration.getDisplayName());
        uriLabel.setText(configuration.getUri().toString());

        editingPanel.setFromConfiguration(configuration);
    }

    private void delete()
    {
        // Confirm the deletion before processing it.
        ActionPopupFactory.yesNo("Are you sure you want to delete this " + type + "?", deleteButton,
                () -> deleteConsumer.accept(this)).show();
    }

    private void enableCheckboxActionPerformed()
    {
        configuration.setEnabled(enableCheckBox.isSelected());

        // Use executor service to get the update logic off the UI thread
        executorService.execute(() -> {
            Thread.currentThread().setName("Update " + type + " configuration");
            preferenceEntry.getPreferenceManager().savePreferences();
            preferenceEntry.firePreferenceEntryChangeEvent();
        });
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var descriptionPanel = new JPanel();
        descriptionLabel = new JLabel();
        uriLabel = new JLabel();
        var enableAndDeletePanel = new JPanel();
        enableCheckBox = new JCheckBox();
        deleteButton = new JButton();
        editingPanel = new NetworkInputOutputEditPanel();
        var separator1 = new JSeparator();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "[default,200dlu]:grow, $lcgap, default",
                "default, $ugap, 2*(default)"));

        //======== descriptionPanel ========
        {
            descriptionPanel.setName("descriptionPanel");
            descriptionPanel.addMouseListener(new MouseAdapter()
            {
                @Override
                public void mouseClicked(MouseEvent e)
                {
                    descriptionPanelMouseClicked(e);
                }
            });
            descriptionPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, default"));

            //---- descriptionLabel ----
            descriptionLabel.setText("Description");
            descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(descriptionLabel.getFont().getSize() + 2f));
            descriptionLabel.setName("descriptionLabel");
            descriptionPanel.add(descriptionLabel, cc.xy(1, 1));

            //---- uriLabel ----
            uriLabel.setText("uri");
            uriLabel.setName("uriLabel");
            descriptionPanel.add(uriLabel, cc.xy(1, 3));
        }
        add(descriptionPanel, cc.xy(1, 1));

        //======== enableAndDeletePanel ========
        {
            enableAndDeletePanel.setName("enableAndDeletePanel");
            enableAndDeletePanel.setLayout(new FormLayout(
                    "center:default, $ugap, center:default",
                    "default"));

            //---- enableCheckBox ----
            enableCheckBox.setName("enableCheckBox");
            enableCheckBox.addActionListener(e -> enableCheckboxActionPerformed());
            enableAndDeletePanel.add(enableCheckBox, cc.xy(1, 1));

            //---- deleteButton ----
            deleteButton.setName("deleteButton");
            deleteButton.addActionListener(e -> delete());
            enableAndDeletePanel.add(deleteButton, cc.xy(3, 1));
        }
        add(enableAndDeletePanel, cc.xy(3, 1));

        //---- editingPanel ----
        editingPanel.setName("editingPanel");
        add(editingPanel, cc.xywh(1, 3, 3, 1));

        //---- separator1 ----
        separator1.setName("separator1");
        add(separator1, cc.xywh(1, 4, 3, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    JLabel descriptionLabel;
    JLabel uriLabel;
    JCheckBox enableCheckBox;
    private JButton deleteButton;
    NetworkInputOutputEditPanel editingPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
