package gov.takx.platform.io.panels;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObjectManager;
import gov.takx.api.plugin.IPluginDefinition;
import gov.takx.api.plugin.IRaptorPlugin;
import gov.takx.api.registry.IDevicePluginRegistry;
import gov.takx.commons.wizard.IWizardModel;
import gov.takx.platform.KeyUtils;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import java.awt.Color;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;

/**
 * Wizard panel that prompts for the type of device that created the
 * data to be imported. The user is asked to enter a map object type,
 * version, message type, and unit ID. The panel sets the following
 * properties in the wizard properties map:
 * <p/>
 * <pre>
 *   CommonWizardSettings.MAP_OBJECT_FAMILY
 *   CommonWizardSettings.MAP_OBJECT_TYPE
 *   CommonWizardSettings.MAP_OBJECT_VERSION
 *   CommonWizardSettings.MESSAGE_TYPE
 *   CommonWizardSettings.UNIT_ID
 * </pre>
 *
 */
@PseudoBean
public class ImportInfoPanel extends JPanel
{
    private static final String DEFAULT_FAMILY = KeyUtils.DEFAULT_FAMILY;

    private final List<IPluginDefinition<? extends IRaptorPlugin>> plugins = new ArrayList<>();
    private boolean flag = false;

    private final IWizardModel model;

    @Inject
    private IDevicePluginRegistry devicePluginRegistry;

    @Inject
    private IMapObjectManager mapObjectManager;

    /**
     * Create the panel.
     *
     * @param model wizard model
     * @since 3.0 (delegate parameter removed)
     */
    public ImportInfoPanel(IWizardModel model)
    {
        this.model = model;
        initComponents();
    }

    @PostConstruct
    private void postConstruct()
    {
        plugins.addAll(devicePluginRegistry.getPluginDefinitions());
        plugins.sort((a, b) -> a.getType().compareToIgnoreCase(b.getType()));

        List<String> deviceTypes = new ArrayList<>();
        deviceTypes.add("Generic Device");
        plugins.forEach(pd -> deviceTypes.add(pd.getType()));

        deviceComboBox.setModel(new DefaultComboBoxModel<>(deviceTypes.toArray(new String[0])));
        deviceComboBox.setSelectedItem("Generic Device");

        initialize();
    }

    public void validateContents()
    {
        String family = textFieldFamily.getText();
        String type = typeField.getText();
        String version = versionField.getText();
        String unitId = idField.getText();

        model.setNextButtonEnabled(
                !family.isEmpty()
                        && !type.isEmpty()
                        && !version.isEmpty()
                        && !unitId.isEmpty());

        // Determine if a map object with this ID already exists.
        final String uniqueKey = KeyUtils.getUniqueKey(family, type, unitId);
        boolean alreadyExists = mapObjectManager.getMapObjectController(uniqueKey) != null;

        alreadyExistsLabel1.setVisible(alreadyExists);
        alreadyExistsLabel2.setVisible(alreadyExists);
    }

    public void updateModel()
    {
        model.setProperty(CommonWizardSettings.MAP_OBJECT_FAMILY, textFieldFamily.getText());
        model.setProperty(CommonWizardSettings.MAP_OBJECT_TYPE, typeField.getText());
        model.setProperty(CommonWizardSettings.UNIT_ID, idField.getText());
    }

    private void initialize()
    {
        String family = model.getStringProperty(CommonWizardSettings.MAP_OBJECT_FAMILY);
        String type = model.getStringProperty(CommonWizardSettings.MAP_OBJECT_TYPE);
        String unitId = model.getStringProperty(CommonWizardSettings.UNIT_ID);

        if (family == null)
        {
            family = DEFAULT_FAMILY;
        }

        if (type == null)
        {
            String format = model.getStringProperty("format");
            if (format != null)
            {
                type = "Generic " + format.toUpperCase() + " Device";
            } else
            {
                type = "Generic Device";
            }
        }

        if (unitId == null)
        {
            unitId = mapObjectManager.getNextUnitID(family, type);
        }

        textFieldFamily.setText(family);
        typeField.setText(type);
        idField.setText(unitId);

        validateContents();
    }

    private void deviceComboBoxActionPerformed()
    {
        if (flag)
        {
            updateFields();
        }
    }

    private void updateFields()
    {
        if ("Generic Device".equalsIgnoreCase(String.valueOf(deviceComboBox.getSelectedItem())))
        {
            setGeneric();
        } else
        {
            final IPluginDefinition<? extends IRaptorPlugin> pluginDefinition = plugins.get(deviceComboBox.getSelectedIndex() - 1);
            textFieldFamily.setText(pluginDefinition.getFamily());
            typeField.setText(pluginDefinition.getType());
            versionField.setText(pluginDefinition.getVersion());
            validateContents();
        }
    }

    private void setGeneric()
    {
        initialize();
    }

    private void thisAncestorAdded()
    {
        flag = true;
    }

    private void advancedCheckboxActionPerformed()
    {
        advancedPanel.setVisible(advancedCheckbox.isSelected());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var label3 = new JLabel();
        var label4 = new JLabel();
        var panel1 = new JPanel();
        deviceComboBox = new JComboBox<>();
        var label5 = new JLabel();
        idField = new JTextField();
        advancedCheckbox = new JCheckBox();
        advancedPanel = new JPanel();
        var label6 = new JLabel();
        textFieldFamily = new JTextField();
        var label1 = new JLabel();
        typeField = new JTextField();
        var label2 = new JLabel();
        versionField = new JTextField();
        alreadyExistsLabel1 = new JLabel();
        alreadyExistsLabel2 = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        addAncestorListener(new AncestorListener()
        {
            @Override
            public void ancestorAdded(AncestorEvent e)
            {
                thisAncestorAdded();
            }

            @Override
            public void ancestorMoved(AncestorEvent e)
            {
            }

            @Override
            public void ancestorRemoved(AncestorEvent e)
            {
            }
        });
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "6*(default, $lgap), default"));

        //---- label3 ----
        label3.setText("Select a device type and id for this data:");
        label3.setName("label3");
        add(label3, cc.xywh(1, 1, 3, 1));

        //---- label4 ----
        label4.setText("Device:");
        label4.setName("label4");
        add(label4, cc.xy(1, 3));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default:grow",
                    "default"));

            //---- deviceComboBox ----
            deviceComboBox.setName("deviceComboBox");
            deviceComboBox.addActionListener(e -> deviceComboBoxActionPerformed());
            panel1.add(deviceComboBox, cc.xy(1, 1));
        }
        add(panel1, cc.xy(3, 3));

        //---- label5 ----
        label5.setText("Unit ID:");
        label5.setName("label5");
        add(label5, cc.xy(1, 5));

        //---- idField ----
        idField.setName("idField");
        add(idField, cc.xy(3, 5));

        //---- advancedCheckbox ----
        advancedCheckbox.setText("Advanced Options");
        advancedCheckbox.setName("advancedCheckbox");
        advancedCheckbox.addActionListener(e -> advancedCheckboxActionPerformed());
        add(advancedCheckbox, cc.xywh(1, 7, 3, 1));

        //======== advancedPanel ========
        {
            advancedPanel.setBorder(new TitledBorder("Advanced Options"));
            advancedPanel.setVisible(false);
            advancedPanel.setName("advancedPanel");
            advancedPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "2*(default, $lgap), default"));

            //---- label6 ----
            label6.setText("Family:");
            label6.setName("label6");
            advancedPanel.add(label6, cc.xy(1, 1));

            //---- textFieldFamily ----
            textFieldFamily.setText("Default Family");
            textFieldFamily.setName("textFieldFamily");
            advancedPanel.add(textFieldFamily, cc.xy(3, 1));

            //---- label1 ----
            label1.setText("Type:");
            label1.setName("label1");
            advancedPanel.add(label1, cc.xy(1, 3));

            //---- typeField ----
            typeField.setName("nameTextField");
            typeField.setText("Generic Device");
            advancedPanel.add(typeField, cc.xy(3, 3));

            //---- label2 ----
            label2.setText("Version:");
            label2.setName("label2");
            advancedPanel.add(label2, cc.xy(1, 5));

            //---- versionField ----
            versionField.setName("descriptionTextField");
            versionField.setText("1.0.0");
            advancedPanel.add(versionField, cc.xy(3, 5));
        }
        add(advancedPanel, new CellConstraints(1, 9, 3, 1, CellConstraints.DEFAULT, CellConstraints.DEFAULT, new Insets(0, 12, 0, 0)));

        //---- alreadyExistsLabel1 ----
        alreadyExistsLabel1.setText("A device already exists with this unit ID.");
        alreadyExistsLabel1.setForeground(Color.red);
        alreadyExistsLabel1.setVisible(false);
        alreadyExistsLabel1.setName("alreadyExistsLabel1");
        add(alreadyExistsLabel1, cc.xy(3, 11));

        //---- alreadyExistsLabel2 ----
        alreadyExistsLabel2.setText("New data will be added to the existing device.");
        alreadyExistsLabel2.setForeground(Color.red);
        alreadyExistsLabel2.setVisible(false);
        alreadyExistsLabel2.setName("alreadyExistsLabel2");
        add(alreadyExistsLabel2, cc.xy(3, 13));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents

        DocumentListener listener = new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                validateContents();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                validateContents();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            {
                validateContents();
            }
        };

        textFieldFamily.getDocument().addDocumentListener(listener);
        typeField.getDocument().addDocumentListener(listener);
        versionField.getDocument().addDocumentListener(listener);
        idField.getDocument().addDocumentListener(listener);
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JComboBox<String> deviceComboBox;
    private JTextField idField;
    private JCheckBox advancedCheckbox;
    private JPanel advancedPanel;
    private JTextField textFieldFamily;
    private JTextField typeField;
    private JTextField versionField;
    private JLabel alreadyExistsLabel1;
    private JLabel alreadyExistsLabel2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
