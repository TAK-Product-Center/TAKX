package gov.takx.core.mapobject.gui;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.core.mapobject.gui.primarydialog.primarypanel.FieldOfViewPanel;
import gov.takx.dataservices.shapes.FieldOfView;
import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

import java.awt.Color;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeListener;

/**
 * Panel to enable/disable sensor on a MapObject, as well as an option to open a panel to edit the sensor details.
 */
@PseudoBean
public class SensorDetailsPanel extends JPanel
{
    private final PropertyChangeListener fovChanged = e -> setSensorDetails();
    private IMapObject mapObject;
    private ActionListener sensorEditActionListener;
    private boolean updating;

    public SensorDetailsPanel()
    {
        initComponents();
        editSensorButton.setIcon(UIResources.EDIT_ICON);
    }

    /**
     * @param mapObject the map object to check for sensor/FOV details.
     */
    public void setMapObject(@Nonnull IMapObject mapObject)
    {
        if (mapObject != this.mapObject)
        {
            this.mapObject = mapObject;
            this.mapObject.addPropertyChangeListener(IMapObject.FIELD_OF_VIEW, fovChanged);
        }
        setSensorDetails();
    }

    /**
     * Initial setting of new sensor details. Used when initializing and when a new FOV is set.
     */
    private void setSensorDetails()
    {
        updating = true;
        final var fov = mapObject.getFieldOfView();
        if (fov != null)
        {
            sensorEnabledRadio.setSelected(true);
            addSensorButtonAction(fov);
            editSensorButton.setEnabled(true);
        } else
        {
            sensorDisabledRadio.setSelected(false);
            addSensorButtonAction(null);
            editSensorButton.setEnabled(false);
        }

        sensorEnabledRadio.setBackground(sensorEnabledRadio.isSelected() ? Color.blue : sensorDisabledRadio.getBackground());
        updating = false;
    }

    /**
     * Updating existing sensor details. Used when radio buttons are toggled.
     */
    private void updateSensorDetails()
    {
        if (updating) return;
        var enabledRadioSelected = sensorEnabledRadio.isSelected();
        sensorEnabledRadio.setBackground(enabledRadioSelected ? Color.blue : sensorDisabledRadio.getBackground());
        if (enabledRadioSelected)
        {
            var fov = mapObject.getFieldOfView();
            fov = fov != null ? fov : new FieldOfView();
            fov.setPoint(mapObject.getLocation());
            mapObject.setMetaBoolean(StandardAttributeNames.SENSOR_FOV, true);
            mapObject.setFieldOfView(fov);
            addSensorButtonAction(fov);
            editSensorButton.setEnabled(true);
        } else
        {
            editSensorButton.setEnabled(false);
            mapObject.setFieldOfView(null);
            if (mapObject.hasMetaValue(StandardAttributeNames.SENSOR_FOV)) mapObject.getAttributeSet().removeAttribute(StandardAttributeNames.SENSOR_FOV);
        }
    }

    private void addSensorButtonAction(@Nullable FieldOfView fov)
    {
        editSensorButton.removeActionListener(sensorEditActionListener);
        if (fov == null)
        {
            sensorEditActionListener = null;
            return;
        }
        sensorEditActionListener = FieldOfViewPanel.getInstance().createPanelActionListener(fov);
        editSensorButton.addActionListener(sensorEditActionListener);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var panel1 = new JPanel();
        var label1 = new JLabel();
        sensorDisabledRadio = new JRadioButton();
        sensorEnabledRadio = new JRadioButton();
        editSensorButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default"));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default:grow, 3*($lcgap, default)",
                    "default"));

            //---- label1 ----
            label1.setText("Sensor");
            label1.setName("label1");
            panel1.add(label1, cc.xy(1, 1));

            //---- sensorDisabledRadio ----
            sensorDisabledRadio.setText("Disabled");
            sensorDisabledRadio.setSelected(true);
            sensorDisabledRadio.setName("sensorDisabledRadio");
            sensorDisabledRadio.addItemListener(e -> updateSensorDetails());
            panel1.add(sensorDisabledRadio, cc.xy(3, 1));

            //---- sensorEnabledRadio ----
            sensorEnabledRadio.setText("Enabled");
            sensorEnabledRadio.setName("sensorEnabledRadio");
            sensorEnabledRadio.addItemListener(e -> updateSensorDetails());
            panel1.add(sensorEnabledRadio, cc.xy(5, 1));

            //---- editSensorButton ----
            editSensorButton.setName("editSensorButton");
            panel1.add(editSensorButton, cc.xy(7, 1));
        }
        add(panel1, cc.xy(1, 1));

        //---- sensorButtonGroup ----
        var sensorButtonGroup = new ButtonGroup();
        sensorButtonGroup.add(sensorDisabledRadio);
        sensorButtonGroup.add(sensorEnabledRadio);
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JRadioButton sensorDisabledRadio;
    private JRadioButton sensorEnabledRadio;
    private JButton editSensorButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
