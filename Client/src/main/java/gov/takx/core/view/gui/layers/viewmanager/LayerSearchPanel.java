package gov.takx.core.view.gui.layers.viewmanager;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.commons.swing.KeyActionHandler;
import gov.takx.commons.swing.PlaceholderTextField;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import java.awt.Color;

import static gov.takx.commons.swing.KeyActionHandler.ESCAPE_KEY;

/**
 */
public class LayerSearchPanel extends JPanel
{
    private final LayerViewPanel layerManagerPanel;

    public LayerSearchPanel(LayerViewPanel layerManagerPanel)
    {
        this.layerManagerPanel = layerManagerPanel;
        initComponents();
        searchEnabledLabel.setIcon(LayerViewUtils.ACTIVE_FILTER_ICON);
        setSearchDisplayEnabled(false);
        clearButton.setToolTipText("Clear search query");

        // Add a single 'Esc'-to-clear key listener to this panel and its children.
        new KeyActionHandler(this, "SearchPanel.clear", ESCAPE_KEY, JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, this::clear)
                .install();
    }

    /**
     * Clear the search query filter.
     */
    public void clear()
    {
        setSearchDisplayEnabled(false);
        queryField.setText("");
    }

    /**
     * Apply the current query string to the layer panels.
     */
    private void updateQuery()
    {
        String query = queryField.getText();
        setSearchDisplayEnabled(!query.isEmpty());
        layerManagerPanel.setFilterString(query);
    }

    /**
     * If enabled, show an active filter icon, as well as red active text.  If disabled, show a darker inactive filter
     * icon and dark grey inactive text.
     */
    private void setSearchDisplayEnabled(boolean enabled)
    {
        searchEnabledLabel.setIcon(
                enabled ? LayerViewUtils.ACTIVE_FILTER_ICON : LayerViewUtils.INACTIVE_FILTER_ICON);
        searchEnabledLabel.setText(enabled ? LayerViewUtils.SEARCH_ENABLED : LayerViewUtils.SEARCH_DISABLED);
    }

    private void queryFieldCaretUpdate()
    {
        updateQuery();
    }

    private void clearButtonActionPerformed()
    {
        clear();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel1 = new JPanel();
        var label1 = new JLabel();
        queryField = new PlaceholderTextField();
        clearButton = new JButton();
        var panel2 = new JPanel();
        searchEnabledLabel = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, $lgap, default"));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default, $lcgap, default:grow, $lcgap, default",
                    "default"));

            //---- label1 ----
            label1.setText("Layer Filter: ");
            label1.setName("label1");
            panel1.add(label1, cc.xy(1, 1));

            //---- queryField ----
            queryField.setPlaceholderText("Enter search query...");
            queryField.setName("queryField");
            queryField.addCaretListener(e -> queryFieldCaretUpdate());
            panel1.add(queryField, cc.xy(3, 1));

            //---- clearButton ----
            clearButton.setText("Clear");
            clearButton.setName("clearButton");
            clearButton.addActionListener(e -> clearButtonActionPerformed());
            panel1.add(clearButton, cc.xy(5, 1));
        }
        add(panel1, cc.xy(1, 1));

        //======== panel2 ========
        {
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "default",
                    "default"));

            //---- searchEnabledLabel ----
            searchEnabledLabel.setText("Layer Search Filter Enabled");
            searchEnabledLabel.setForeground(Color.green);
            searchEnabledLabel.setName("searchEnabledLabel");
            panel2.add(searchEnabledLabel, cc.xy(1, 1));
        }
        add(panel2, cc.xy(1, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private PlaceholderTextField queryField;
    private JButton clearButton;
    private JLabel searchEnabledLabel;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
