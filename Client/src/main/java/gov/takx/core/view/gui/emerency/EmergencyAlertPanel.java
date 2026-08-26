package gov.takx.core.view.gui.emerency;

import com.formdev.flatlaf.FlatClientProperties;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.emergency.EmergencyAlertType;
import gov.takx.api.emergency.IEmergencyAlertManager;
import gov.takx.core.view.gui.ATitledPanel;
import jakarta.inject.Inject;

import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import java.util.List;

/**
 * Panel allowing the user to configure and send an emergency alert.
 * @since 5.7
 */
@PseudoBean
public class EmergencyAlertPanel extends ATitledPanel
{
    @Inject
    private IEmergencyAlertManager emergencyAlertManager;

    private boolean alertActive;

    public EmergencyAlertPanel()
    {
        super("Emergency Beacon");

        initComponents();

        final var alertTypeModel = new DefaultComboBoxModel<>(EmergencyAlertType.values());
        alertTypeModel.removeElement(EmergencyAlertType.Cancel); // Only include "active" types
        emergencyTypeComboBox.setModel(alertTypeModel);

        List.of(button1Off, button1Locked, button2Off, button2Locked)
                .forEach(b -> b.putClientProperty(FlatClientProperties.BUTTON_TYPE, FlatClientProperties.BUTTON_TYPE_BORDERLESS));
    }

    /**
     * When either "Off" button is pressed, set both off buttons selected. If an alert is active, cancel it.
     */
    private void unlock()
    {
        button1Off.setSelected(true);
        button2Off.setSelected(true);

        if (alertActive)
        {
            emergencyAlertManager.cancelAlert(getAlertType());
            alertActive = false;
        }
    }

    /**
     * If both lock buttons are selected, and we're not already active, broadcast the alert.
     */
    private void alertIfLocked()
    {
        if (!alertActive && button1Locked.isSelected() && button2Locked.isSelected())
        {
            emergencyAlertManager.broadcastAlert(getAlertType(), messageTextField.getText());
            alertActive = true;
        }
    }

    private EmergencyAlertType getAlertType()
    {
        return emergencyTypeComboBox.getItemAt(emergencyTypeComboBox.getSelectedIndex());
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var label1 = new JLabel();
        var panel1 = new JPanel();
        button1Off = new JToggleButton();
        button1Locked = new JToggleButton();
        var panel2 = new JPanel();
        button2Off = new JToggleButton();
        button2Locked = new JToggleButton();
        var label2 = new JLabel();
        emergencyTypeComboBox = new JComboBox<>();
        var label3 = new JLabel();
        messageTextField = new JTextField();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setBorder(Borders.createEmptyBorder("2dlu, 2dlu, 0dlu, 2dlu"));
        setName("this");
        setLayout(new FormLayout(
                "default",
                "6*(default, $lgap), default"));

        //---- label1 ----
        label1.setText("Lock both switches to initiate emergency beacon");
        label1.setName("label1");
        add(label1, cc.xy(1, 1));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "2*(default)",
                    "default"));

            //---- button1Off ----
            button1Off.setText("Off");
            button1Off.setFocusable(false);
            button1Off.setSelected(true);
            button1Off.setName("button1Off");
            button1Off.addActionListener(e -> unlock());
            panel1.add(button1Off, cc.xy(1, 1));

            //---- button1Locked ----
            button1Locked.setText("LOCKED");
            button1Locked.setFocusable(false);
            button1Locked.setName("button1Locked");
            button1Locked.addActionListener(e -> alertIfLocked());
            panel1.add(button1Locked, cc.xy(2, 1));
        }
        add(panel1, cc.xy(1, 3));

        //======== panel2 ========
        {
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "2*(default)",
                    "default"));

            //---- button2Off ----
            button2Off.setText("Off");
            button2Off.setFocusable(false);
            button2Off.setSelected(true);
            button2Off.setName("button2Off");
            button2Off.addActionListener(e -> unlock());
            panel2.add(button2Off, cc.xy(1, 1));

            //---- button2Locked ----
            button2Locked.setText("LOCKED");
            button2Locked.setFocusable(false);
            button2Locked.setName("button2Locked");
            button2Locked.addActionListener(e -> alertIfLocked());
            panel2.add(button2Locked, cc.xy(2, 1));
        }
        add(panel2, cc.xy(1, 5));

        //---- label2 ----
        label2.setText("Select the type of emergency");
        label2.setName("label2");
        add(label2, cc.xy(1, 7));

        //---- emergencyTypeComboBox ----
        emergencyTypeComboBox.setName("emergencyTypeComboBox");
        add(emergencyTypeComboBox, cc.xy(1, 9));

        //---- label3 ----
        label3.setText("Add an optional message");
        label3.setName("label3");
        add(label3, cc.xy(1, 11));

        //---- messageTextField ----
        messageTextField.setName("messageTextField");
        add(messageTextField, cc.xy(1, 13));

        //---- buttonGroup1 ----
        var buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(button1Off);
        buttonGroup1.add(button1Locked);

        //---- buttonGroup2 ----
        var buttonGroup2 = new ButtonGroup();
        buttonGroup2.add(button2Off);
        buttonGroup2.add(button2Locked);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JToggleButton button1Off;
    private JToggleButton button1Locked;
    private JToggleButton button2Off;
    private JToggleButton button2Locked;
    private JComboBox<EmergencyAlertType> emergencyTypeComboBox;
    private JTextField messageTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
