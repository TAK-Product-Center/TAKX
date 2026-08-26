package gov.takx.core.mapobject.gui.alarms;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.shapes.IShape;
import gov.takx.commons.constants.State;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.dataservices.persist.CustomDataMonitor;
import gov.takx.dataservices.persist.CustomDataMonitor.AlarmType;
import gov.takx.dataservices.persist.CustomDataMonitor.GeofenceAlarmTrigger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;

import java.util.List;
import java.util.stream.Stream;

/**
 * View panel for {@link CustomDataMonitor} Geofence configuration. Allows operator to enter all possible
 * values that can be set through the CustomDataMonitor.
 *
 * @see CustomDataMonitor
 * @since 1.49
 */
public class GeofenceAlarmPanel extends AAlarmPanel
{
    public GeofenceAlarmPanel(CustomDataMonitor customDataMonitor)
    {
        super(customDataMonitor);

        if (customDataMonitor.getAlarmType() != AlarmType.Geofence)
        {
            throw new IllegalArgumentException("'customDataMonitor' must be of type \"" + AlarmType.Geofence + "\"");
        }

        initComponents();

        final List<IShape> shapeList = customDataMonitor.getSelectableValues();

        final ShapeWrapper[] shapesForModel = Stream.concat(
                Stream.of(new ShapeWrapper(null)),
                shapeList.stream().map(ShapeWrapper::new).sorted())
                .toArray(ShapeWrapper[]::new);

        shapeComboBox.setModel(new DefaultComboBoxModel<>(shapesForModel));

        GUIUtils.addSingleActionDocumentListener(textFieldAlarmName, e -> setDirty());

        levelComboBox.setModel(new DefaultComboBoxModel<>(ALARM_STATES));
        levelComboBox.setSelectedIndex(1);
        levelComboBox.setRenderer(new LevelComboBoxRenderer());

        defaultStateComboBox.setModel(new DefaultComboBoxModel<>(DEFAULT_STATES));
        defaultStateComboBox.setSelectedIndex(0);
        defaultStateComboBox.setRenderer(new LevelComboBoxRenderer());

        alarmTriggerModeComboBox.setModel(new DefaultComboBoxModel<>(GeofenceAlarmTrigger.values()));

        installDataMonitorState();
    }

    @Override
    protected void installDataMonitorState()
    {
        final String displayName = customDataMonitor.getDisplayName();
        textFieldAlarmName.setText(displayName == null ? "" : displayName);

        final State alarmState = customDataMonitor.getAlarmState();
        if (alarmState != null) levelComboBox.setSelectedItem(alarmState);

        final State defaultState = customDataMonitor.getDefaultState();
        if (defaultState != null) defaultStateComboBox.setSelectedItem(defaultState);

        clearableCheckBox.setSelected(customDataMonitor.isClearable());
        ackCheckBox.setSelected(customDataMonitor.isAckReq());
        holdStateCheckBox.setSelected(customDataMonitor.isHoldState());

        shapeComboBox.setSelectedItem(new ShapeWrapper(customDataMonitor.getGeofenceShape()));
        alarmTriggerModeComboBox.setSelectedItem(customDataMonitor.getGeofenceAlarmTrigger());
    }

    @Override
    public String validateContents()
    {
        String errorString = ""; // Start with an empty String, if it stays empty then everything was valid
        final String alarmNameText = textFieldAlarmName.getText();

        if (StringUtils.isEmpty(alarmNameText))
        {
            errorString += "\t-Please enter a Name for the alarm.\n";
        }
        if (shapeComboBox.getSelectedIndex() < 1)
        {
            // The user needs to select a shape from the drop down menu
            errorString += "\t-Please select a Geofence shape.\n";
        }

        if (errorString.isEmpty())
        {
            return null;
        } else
        {
            return StringUtils.isEmpty(alarmNameText)
                    ? "Geofence Alarm Error(s): \n" + errorString + "\n"
                    : alarmNameText + " Alarm Error(s): \n" + errorString + "\n";
        }
    }

    @Override
    public void applyAction()
    {
        customDataMonitor.setGeofenceAlarmValues(getDisplayName(), getAlarmState(), getDefaultState(),
                isClearable(), isAckRequired(), isHoldState(), getSelectedGeofenceShape(), getAlarmTriggerMode());
    }

    @Override
    public String getDisplayName()
    {
        return textFieldAlarmName.getText();
    }

    private State getAlarmState()
    {
        return (State) levelComboBox.getSelectedItem();
    }

    private State getDefaultState()
    {
        return (State) defaultStateComboBox.getSelectedItem();
    }

    private boolean isClearable()
    {
        return clearableCheckBox.isSelected();
    }

    private boolean isAckRequired()
    {
        return ackCheckBox.isSelected();
    }

    private boolean isHoldState()
    {
        return holdStateCheckBox.isSelected();
    }

    private IShape getSelectedGeofenceShape()
    {
        final ShapeWrapper wrapper = (ShapeWrapper) shapeComboBox.getSelectedItem();

        return wrapper != null ? wrapper.shape : null;
    }

    private GeofenceAlarmTrigger getAlarmTriggerMode()
    {
        final int selectedIndex = alarmTriggerModeComboBox.getSelectedIndex();

        return selectedIndex < 0
                ? GeofenceAlarmTrigger.ENTERING // Default to entering
                : alarmTriggerModeComboBox.getItemAt(selectedIndex);
    }

    private void shapeComboBoxItemStateChanged()
    {
        setDirty();
    }

    private void alarmTriggerModeComboBoxItemStateChanged()
    {
        setDirty();
    }

    private void defaultStateComboBoxItemStateChanged()
    {
        setDirty();
    }

    private void clearableCheckBoxItemStateChanged()
    {
        setDirty();
    }

    private void holdStateCheckBoxItemStateChanged()
    {
        setDirty();
    }

    private void ackCheckBoxItemStateChanged()
    {
        setDirty();
    }

    private void levelComboBoxItemStateChanged()
    {
        setDirty();
    }

    private static class ShapeWrapper implements Comparable<ShapeWrapper>
    {
        public final IShape shape;

        public ShapeWrapper(IShape shape)
        {
            this.shape = shape;
        }

        @Override
        public String toString()
        {
            return shape != null ? shape.getName() : "None";
        }

        public String getKey()
        {
            return shape != null ? shape.getKey() : "null";
        }

        public String getName()
        {
            return shape != null ? shape.getName() : "";
        }

        @Override
        public int compareTo(ShapeWrapper o)
        {
            return getName().compareTo(o.getName());
        }

        @Override
        public int hashCode()
        {
            return getKey().hashCode();
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            return getKey().equals(((ShapeWrapper) o).getKey());
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel1 = new JPanel();
        var labelAlarmName = new JLabel();
        textFieldAlarmName = new JTextField();
        var selectedGeofenceLabel = new JLabel();
        shapeComboBox = new JComboBox<>();
        var insideModeLabel = new JLabel();
        var panel3 = new JPanel();
        alarmTriggerModeComboBox = new JComboBox<>();
        var labelAlarmType = new JLabel();
        levelComboBox = new JComboBox<>();
        var label7 = new JLabel();
        defaultStateComboBox = new JComboBox<>();
        clearableCheckBox = new JCheckBox();
        holdStateCheckBox = new JCheckBox();
        ackCheckBox = new JCheckBox();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "2*(default, $lgap), default"));

        //======== panel1 ========
        {
            panel1.setBorder(new TitledBorder("Alarm Parameters"));
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "4*(default, $lgap), default"));

            //---- labelAlarmName ----
            labelAlarmName.setText("Alarm Name:");
            labelAlarmName.setName("labelAlarmName");
            panel1.add(labelAlarmName, cc.xy(1, 1));

            //---- textFieldAlarmName ----
            textFieldAlarmName.setName("textFieldAlarmName");
            panel1.add(textFieldAlarmName, cc.xy(3, 1));

            //---- selectedGeofenceLabel ----
            selectedGeofenceLabel.setText("Geofence Shape: ");
            selectedGeofenceLabel.setName("selectedGeofenceLabel");
            panel1.add(selectedGeofenceLabel, cc.xy(1, 3));

            //---- shapeComboBox ----
            shapeComboBox.setName("shapeComboBox");
            shapeComboBox.addItemListener(e -> shapeComboBoxItemStateChanged());
            panel1.add(shapeComboBox, cc.xy(3, 3, CellConstraints.LEFT, CellConstraints.DEFAULT));

            //---- insideModeLabel ----
            insideModeLabel.setText("Alarm triggers on");
            insideModeLabel.setName("insideModeLabel");
            panel1.add(insideModeLabel, cc.xy(1, 5));

            //======== panel3 ========
            {
                panel3.setName("panel3");
                panel3.setLayout(new FormLayout(
                        "left:default",
                        "default"));

                //---- alarmTriggerModeComboBox ----
                alarmTriggerModeComboBox.setToolTipText("Entering: When the device enters the geofence shape        Exiting: When the device exits the geofence shape        Both: Entering OR Exiting");
                alarmTriggerModeComboBox.setName("alarmTriggerModeComboBox");
                alarmTriggerModeComboBox.addItemListener(e -> alarmTriggerModeComboBoxItemStateChanged());
                panel3.add(alarmTriggerModeComboBox, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
            }
            panel1.add(panel3, cc.xy(3, 5));

            //---- labelAlarmType ----
            labelAlarmType.setText("Alarm Type:");
            labelAlarmType.setName("labelAlarmType");
            panel1.add(labelAlarmType, cc.xy(1, 7));

            //---- levelComboBox ----
            levelComboBox.setToolTipText("Severity of alarm when satisfying alarm condition");
            levelComboBox.setName("levelComboBox");
            levelComboBox.addItemListener(e -> levelComboBoxItemStateChanged());
            panel1.add(levelComboBox, cc.xy(3, 7, CellConstraints.LEFT, CellConstraints.DEFAULT));

            //---- label7 ----
            label7.setText("Default State:");
            label7.setName("label7");
            panel1.add(label7, cc.xy(1, 9));

            //---- defaultStateComboBox ----
            defaultStateComboBox.setToolTipText("State to display when NOT alarming");
            defaultStateComboBox.setName("defaultStateComboBox");
            defaultStateComboBox.addItemListener(e -> defaultStateComboBoxItemStateChanged());
            panel1.add(defaultStateComboBox, cc.xy(3, 9, CellConstraints.LEFT, CellConstraints.DEFAULT));
        }
        add(panel1, cc.xywh(1, 1, 3, 1));

        //---- clearableCheckBox ----
        clearableCheckBox.setText("Clearable");
        clearableCheckBox.setToolTipText("Is the operator able to clear the alarm?");
        clearableCheckBox.setName("clearableCheckBox");
        clearableCheckBox.addItemListener(e -> clearableCheckBoxItemStateChanged());
        add(clearableCheckBox, cc.xy(1, 3));

        //---- holdStateCheckBox ----
        holdStateCheckBox.setText("Hold State");
        holdStateCheckBox.setToolTipText("Hold the alarm state even if the value has moved outside of the threshold");
        holdStateCheckBox.setVisible(false);
        holdStateCheckBox.setName("holdStateCheckBox");
        holdStateCheckBox.addItemListener(e -> holdStateCheckBoxItemStateChanged());
        add(holdStateCheckBox, cc.xy(3, 3));

        //---- ackCheckBox ----
        ackCheckBox.setText("Require ACK");
        ackCheckBox.setToolTipText("Require the operator to acknowledge the alarm before it can be completely cleared");
        ackCheckBox.setName("ackCheckBox");
        ackCheckBox.addItemListener(e -> ackCheckBoxItemStateChanged());
        add(ackCheckBox, cc.xy(1, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField textFieldAlarmName;
    private JComboBox<ShapeWrapper> shapeComboBox;
    private JComboBox<GeofenceAlarmTrigger> alarmTriggerModeComboBox;
    private JComboBox<State> levelComboBox;
    private JComboBox<State> defaultStateComboBox;
    private JCheckBox clearableCheckBox;
    private JCheckBox holdStateCheckBox;
    private JCheckBox ackCheckBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
