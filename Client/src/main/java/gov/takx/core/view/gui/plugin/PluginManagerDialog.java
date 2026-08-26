package gov.takx.core.view.gui.plugin;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.plugin.IMetadata;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IPluginDescriptor;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.registry.IPluginInspector;
import gov.takx.platform.ui.AStandardDialog;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ItemEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.lang.invoke.MethodHandles;
import java.util.Optional;
import java.util.Set;

/**
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PluginManagerDialog extends AStandardDialog
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NOT_PROVIDED = "<Not Provided>";

    private IMetadata activeMetadata = null;

    private final DefaultComboBoxModel<String> metaFieldModel = new DefaultComboBoxModel<>();
    private PluginTableModel tableModel;

    @Inject
    private IPluginInspector pluginInspector;

    public static void showInfo()
    {
        new PluginManagerDialog().showDialog();
    }

    private void showDialog()
    {
        setVisible(true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public PluginManagerDialog()
    {
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        tableModel = new PluginTableModel(pluginInspector.getPluginDefinitions());

        // Push the table model into the UI and display the complete list of known plugins, active or otherwise
        tablePlugins.setModel(tableModel);

        // Hide the family column by default
        tablePlugins.getColumnExt(PluginTableModel.Columns.Family.ordinal()).setVisible(false);

        comboBoxMetadataKey.setModel(metaFieldModel);

        resetMetadataFields();
    }

    private void okButtonActionPerformed()
    {
        setVisible(false);
        dispose();
    }

    private void resetMetadataFields()
    {
        activeMetadata = null;
        metaFieldModel.removeAllElements();
        labelName.setText(NOT_PROVIDED);
        labelDescription.setText(NOT_PROVIDED);
        labelVendor.setText(NOT_PROVIDED);
        labelCopyright.setText(NOT_PROVIDED);
        textPaneMetaData.setText("");
    }

    private void populateDescriptorData(IPluginDescriptor descriptor)
    {
        logger.debug("Populating meta data");

        String name = Optional.ofNullable(descriptor.getName()).orElse(NOT_PROVIDED);
        labelName.setText(name);

        String description = Optional.ofNullable(descriptor.getDescription()).orElse(NOT_PROVIDED);
        labelDescription.setText(description);

        String vendor = Optional.ofNullable(descriptor.getVendor()).orElse(NOT_PROVIDED);
        labelVendor.setText(vendor);

        String copyright = Optional.ofNullable(descriptor.getCopyrightNotice()).orElse(NOT_PROVIDED);
        labelCopyright.setText(copyright);
    }

    private void populateMetaData(IMetadata metadata)
    {
        if (metadata == null)
        {
            return;
        }

        metaFieldModel.removeAllElements();

        final Set<String> metadataKeys = metadata.getMetadataKeys();
        if (metadataKeys != null)
        {
            metadataKeys.forEach(metaFieldModel::addElement);
        }
    }

    private void comboBoxMetadataKeyItemStateChanged(ItemEvent e)
    {
        if (null != activeMetadata && e.getStateChange() == ItemEvent.SELECTED)
        {
            String key = comboBoxMetadataKey.getSelectedItem().toString();
            textPaneMetaData.setText(activeMetadata.getMetadataValue(key));
        }
    }

    private void tablePluginsMouseClicked()
    {
        resetMetadataFields();

        int selectedRow = tablePlugins.convertRowIndexToModel(tablePlugins.getSelectedRow());
        if (selectedRow >= 0)
        {
            refreshMetadata(selectedRow);
        }
    }

    private void refreshMetadata(int selectedRow)
    {
        final IPluginDefinition<? extends IRaptorPlugin> pluginDefinition = tableModel.getPluginDefinition(selectedRow);

        if (pluginDefinition != null)
        {
            populateDescriptorData(pluginDefinition);

            activeMetadata = pluginDefinition.getInstance();
            populateMetaData(activeMetadata);
        }
    }

    private void cancelButtonActionPerformed()
    {
        dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var scrollPane6 = new JScrollPane();
        tablePlugins = new JXTable();
        var panelMetadata = new JPanel();
        var label1 = new JLabel();
        labelName = new JLabel();
        var label2 = new JLabel();
        labelDescription = new JLabel();
        var label3 = new JLabel();
        labelVendor = new JLabel();
        var label4 = new JLabel();
        labelCopyright = new JLabel();
        var label5 = new JLabel();
        comboBoxMetadataKey = new JComboBox<>();
        var scrollPane4 = new JScrollPane();
        textPaneMetaData = new JTextPane();
        var buttonBar = new JPanel();
        var okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Plugin Manager");
        setModal(true);
        setName("this");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setBorder(Borders.DIALOG_BORDER);
            dialogPane.setName("dialogPane");
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setName("contentPanel");
                contentPanel.setLayout(new FormLayout(
                        "default:grow, $lcgap, default:grow(0.5)",
                        "fill:default:grow"));

                //======== scrollPane6 ========
                {
                    scrollPane6.setName("scrollPane6");

                    //---- tablePlugins ----
                    tablePlugins.setColumnControlVisible(true);
                    tablePlugins.setAutoStartEditOnKeyStroke(false);
                    tablePlugins.setEditable(false);
                    tablePlugins.setFocusable(false);
                    tablePlugins.setRequestFocusEnabled(false);
                    tablePlugins.setPreferredScrollableViewportSize(new Dimension(100, 100));
                    tablePlugins.setName("tablePlugins");
                    tablePlugins.addMouseListener(new MouseAdapter()
                    {
                        @Override
                        public void mouseClicked(MouseEvent e)
                        {
                            tablePluginsMouseClicked();
                        }
                    });
                    scrollPane6.setViewportView(tablePlugins);
                }
                contentPanel.add(scrollPane6, cc.xy(1, 1));

                //======== panelMetadata ========
                {
                    panelMetadata.setName("panelMetadata");
                    panelMetadata.setLayout(new FormLayout(
                            "default:grow",
                            "10*(default, $lgap), fill:default:grow"));

                    //---- label1 ----
                    label1.setText("Name:");
                    label1.setName("label1");
                    panelMetadata.add(label1, cc.xy(1, 1));

                    //---- labelName ----
                    labelName.setName("labelName");
                    panelMetadata.add(labelName, cc.xy(1, 3));

                    //---- label2 ----
                    label2.setText("Description:");
                    label2.setName("label2");
                    panelMetadata.add(label2, cc.xy(1, 5));

                    //---- labelDescription ----
                    labelDescription.setName("labelDescription");
                    panelMetadata.add(labelDescription, cc.xy(1, 7));

                    //---- label3 ----
                    label3.setText("Vendor:");
                    label3.setName("label3");
                    panelMetadata.add(label3, cc.xy(1, 9));

                    //---- labelVendor ----
                    labelVendor.setName("labelVendor");
                    panelMetadata.add(labelVendor, cc.xy(1, 11));

                    //---- label4 ----
                    label4.setText("Copyright:");
                    label4.setName("label4");
                    panelMetadata.add(label4, cc.xy(1, 13));

                    //---- labelCopyright ----
                    labelCopyright.setName("labelCopyright");
                    panelMetadata.add(labelCopyright, cc.xy(1, 15));

                    //---- label5 ----
                    label5.setText("Metadata Fields:");
                    label5.setName("label5");
                    panelMetadata.add(label5, cc.xy(1, 17));

                    //---- comboBoxMetadataKey ----
                    comboBoxMetadataKey.setName("comboBoxMetadataKey");
                    comboBoxMetadataKey.addItemListener(e -> comboBoxMetadataKeyItemStateChanged(e));
                    panelMetadata.add(comboBoxMetadataKey, cc.xy(1, 19));

                    //======== scrollPane4 ========
                    {
                        scrollPane4.setName("scrollPane4");

                        //---- textPaneMetaData ----
                        textPaneMetaData.setName("textPaneMetaData");
                        scrollPane4.setViewportView(textPaneMetaData);
                    }
                    panelMetadata.add(scrollPane4, cc.xy(1, 21));
                }
                contentPanel.add(panelMetadata, cc.xy(3, 1));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, default, $lcgap, default",
                        "pref"));

                //---- okButton ----
                okButton.setText("OK");
                okButton.setName("okButton");
                okButton.addActionListener(e -> okButtonActionPerformed());
                buttonBar.add(okButton, cc.xy(2, 1));

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.setName("cancelButton");
                cancelButton.addActionListener(e -> cancelButtonActionPerformed());
                buttonBar.add(cancelButton, cc.xy(4, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
// JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JXTable tablePlugins;
    private JLabel labelName;
    private JLabel labelDescription;
    private JLabel labelVendor;
    private JLabel labelCopyright;
    private JComboBox<String> comboBoxMetadataKey;
    private JTextPane textPaneMetaData;
// JFormDesigner - End of variables declaration  //GEN-END:variables
}
