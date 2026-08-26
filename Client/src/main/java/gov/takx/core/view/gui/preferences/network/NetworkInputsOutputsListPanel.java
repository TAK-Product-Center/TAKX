package gov.takx.core.view.gui.preferences.network;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import java.awt.Font;
import java.util.function.Function;

/**
 * Displays a list of network input or output configurations.
 * 
 * @since 5.0
 */
public class NetworkInputsOutputsListPanel extends JPanel
{
    private final String type;
    private final PreferencesNode node;
    private final PreferenceEntry preferenceEntry;
    private final boolean udpOnly;

    /**
     * @param type            The network configuration type (i.e. Input or Output)
     * @param preferenceEntry {@link PreferenceEntry#COMMON_COMMO_DEFAULT_INPUTS_NODE} or {@link PreferenceEntry#COMMON_COMMO_DEFAULT_OUTPUTS_NODE}
     * @param udpOnly         {@code true} if this panel should only offer UDP when editing or adding items
     */
    public NetworkInputsOutputsListPanel(@Nonnull String type, @Nonnull PreferenceEntry preferenceEntry, boolean udpOnly)
    {
        this.type = type;
        node = preferenceEntry.node();
        this.preferenceEntry = preferenceEntry;
        this.udpOnly = udpOnly;

        initComponents();

        addNewPanel.setVisible(false);
        addNewEditingPanel.setListeners(this::addItem, this::cancelAddItem, node::nodeExists);
        if (udpOnly) addNewEditingPanel.setUdpOnly();

        node.childrenNames()
                .forEach(childName -> {
                    final var childNode = node.node(childName);

                    // Don't flag as a duplicate the same node we're editing
                    //noinspection DataFlowIssue - childNode can't be null
                    final Function<String, Boolean> descriptionChecker = description -> node.nodeExists(description) && !childNode.equals(node.node(description));

                    //noinspection DataFlowIssue - childNode can't be null
                    addPanel(new NetworkInputOutputPanel(preferenceEntry, childNode, type, this::deleteNode, descriptionChecker, udpOnly));
                });
    }

    /**
     * @param panel The panel to add to the list
     */
    private void addPanel(@Nonnull NetworkInputOutputPanel panel)
    {
        listPanel.add(panel);
        repaint();
    }

    /**
     * @param panel The panel to remove from the list
     */
    private void removePanel(@Nonnull NetworkInputOutputPanel panel)
    {
        listPanel.remove(panel);
        repaint();
    }

    /**
     * Delete a configuration node and remove it's associated panel.
     *
     * @param panel The panel to queue for removal
     */
    private void deleteNode(@Nonnull NetworkInputOutputPanel panel)
    {
        panel.setVisible(false);
        panel.getNode().removeNode(); // remove the preference node
        removePanel(panel); // remove the panel from UI
    }

    /**
     * Hides the "Add" button and shows the configuration editing panel
     */
    private void addItemButtonClicked()
    {
        addItemButton.setVisible(false);
        addNewPanel.setVisible(true);
    }

    /**
     * Closes the configuration editing panel and creates a {@link PreferencesNode} and panel for the new entry. If the
     * user cancels all preference changes when exiting the User Preferences dialog, the node and panel will be removed. 
     */
    private void addItem()
    {
        // Create the new preference node
        final var itemNode = node.node(addNewEditingPanel.getDescription());

        //noinspection DataFlowIssue - can't happen
        itemNode.put("uri", addNewEditingPanel.getUri());
        itemNode.put("enabled", true);

        closeNewItemPanel(); // Closing it clears the data, so do it after pulling the data from the form

        // Add an entry to the UI
        addPanel(new NetworkInputOutputPanel(preferenceEntry, itemNode, type, this::deleteNode, node::nodeExists, udpOnly));

        preferenceEntry.firePreferenceEntryChangeEvent();
    }

    private void cancelAddItem()
    {
        closeNewItemPanel();
    }

    private void closeNewItemPanel()
    {
        addNewPanel.setVisible(false);
        addNewEditingPanel.clear();
        addItemButton.setVisible(true);
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        listPanel = new JPanel();
        addItemButton = new JButton();
        addNewPanel = new JPanel();
        var addNewLabel = new JLabel();
        addNewEditingPanel = new NetworkInputOutputEditPanel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(new EmptyBorder(0, 4, 0, 4));
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, $lgap, fill:default:grow, 2*($lgap, default)"));

        //======== listPanel ========
        {
            listPanel.setName("listPanel");
            listPanel.setLayout(new VerticalLayout());
        }
        add(listPanel, cc.xy(1, 3));

        //---- addItemButton ----
        addItemButton.setText("Add Item");
        addItemButton.setName("addItemButton");
        addItemButton.addActionListener(e -> addItemButtonClicked());
        add(addItemButton, cc.xy(1, 5, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //======== addNewPanel ========
        {
            addNewPanel.setName("addNewPanel");
            addNewPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, default"));

            //---- addNewLabel ----
            addNewLabel.setText("Add New");
            addNewLabel.setFont(addNewLabel.getFont().deriveFont(addNewLabel.getFont().getStyle() | Font.BOLD, addNewLabel.getFont().getSize() + 1f));
            addNewLabel.setName("addNewLabel");
            addNewPanel.add(addNewLabel, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.DEFAULT));

            //---- addNewEditingPanel ----
            addNewEditingPanel.setName("addNewEditingPanel");
            addNewPanel.add(addNewEditingPanel, cc.xy(1, 3));
        }
        add(addNewPanel, cc.xy(1, 7));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JPanel listPanel;
    private JButton addItemButton;
    private JPanel addNewPanel;
    private NetworkInputOutputEditPanel addNewEditingPanel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
