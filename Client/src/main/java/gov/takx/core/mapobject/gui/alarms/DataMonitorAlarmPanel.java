package gov.takx.core.mapobject.gui.alarms;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.mapobject.FieldType;
import gov.takx.api.messages.DataStructureBuilder;
import gov.takx.api.messages.IDataField;
import gov.takx.api.messages.IDataStructure;
import gov.takx.api.messages.IRaptorDataStructure;
import gov.takx.api.query.ComparisonType;
import gov.takx.api.query.CoreField;
import gov.takx.commons.constants.State;
import gov.takx.commons.utils.StringUtils;
import gov.takx.core.view.gui.GUIUtils;
import gov.takx.dataservices.persist.CustomDataMonitor;
import gov.takx.dataservices.persist.CustomDataMonitor.AlarmType;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;

import java.awt.event.ItemEvent;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

/**
 * View panel for {@link CustomDataMonitor} configuration.
 * Allows operator to enter all possible values that can be set through the CustomDataMonitor.
 *
 * @see CustomDataMonitor
 */
public class DataMonitorAlarmPanel extends AAlarmPanel
{
    private static final ComparisonType[] COMPARISON_TYPES = {
            ComparisonType.EQ, ComparisonType.NEQ, ComparisonType.GT, ComparisonType.GTE, ComparisonType.LT, ComparisonType.LTE
    };

    private static final List<CoreField> SUPPORTED_CORE_FIELDS = Arrays.asList(
            CoreField.latitude, CoreField.longitude, CoreField.elevation,
            CoreField.speed, CoreField.heading, CoreField.pitch, CoreField.roll,
            CoreField.hasMotion);

    private final DefaultComboBoxModel<String> fieldComboBoxModel = new DefaultComboBoxModel<>();
    private final Collection<IRaptorDataStructure> structures;
    private IRaptorDataStructure selectedStructure;
    private IDataField selectedStructureField;
    private CoreField selectedCoreField;

    /**
     * Initialize custom alarm panel.
     *
     * @param dataMonitor Data monitor we're processing
     * @param structures  possible structures for alarms
     */
    public DataMonitorAlarmPanel(CustomDataMonitor dataMonitor, Collection<IRaptorDataStructure> structures)
    {
        super(dataMonitor);

        if (dataMonitor.getAlarmType() != AlarmType.DataMonitor)
        {
            throw new IllegalArgumentException("'customDataMonitor' must be of type \"" + AlarmType.DataMonitor + "\"");
        }

        initComponents();

        //noinspection AssignmentOrReturnOfFieldWithMutableType - we're given a fresh collection
        this.structures = structures;

        GUIUtils.addSingleActionDocumentListener(displayNameTextField, e -> setDirty());
        GUIUtils.addSingleActionDocumentListener(conditionalValueTextField, e -> setDirty());

        final String[] messageTypes = structures.stream()
                .sorted()
                .map(IDataStructure::getMessageType)
                .filter(messageType -> !DataStructureBuilder.LOCATION_MESSAGE_TYPE.equals(messageType))
                .toArray(String[]::new);

        dataStructureComboBox.setModel(new DefaultComboBoxModel<>(messageTypes));

        if (messageTypes.length > 0)
        {
            dataStructureComboBox.setSelectedIndex(0);
            selectDataStructure(0);
        }

        conditionalComboBox.setModel(new DefaultComboBoxModel<>(COMPARISON_TYPES));
        conditionalComboBox.setSelectedIndex(2);

        levelComboBox.setModel(new DefaultComboBoxModel<>(ALARM_STATES));
        levelComboBox.setSelectedIndex(1);
        levelComboBox.setRenderer(new LevelComboBoxRenderer());

        defaultStateComboBox.setModel(new DefaultComboBoxModel<>(DEFAULT_STATES));
        defaultStateComboBox.setSelectedIndex(0);
        defaultStateComboBox.setRenderer(new LevelComboBoxRenderer());

        installDataMonitorState();
    }

    @Override
    protected void installDataMonitorState()
    {
        final String displayName = customDataMonitor.getDisplayName();
        displayNameTextField.setText(displayName == null ? "" : displayName);

        final String structureName = customDataMonitor.getStructureName();
        if (structureName != null)
        {
            dataStructureComboBox.setSelectedItem(structureName);
            int index = dataStructureComboBox.getSelectedIndex();
            selectDataStructure(index);
        }

        final String fieldName = customDataMonitor.getFieldName();
        if (fieldName != null)
        {
            fieldComboBox.setSelectedItem(fieldName);
            selectedCoreField = CoreField.forName(fieldName);
            selectedStructureField = selectedStructure.getField(fieldName);
        }

        final State alarmState = customDataMonitor.getAlarmState();
        if (alarmState != null) levelComboBox.setSelectedItem(alarmState);

        final State defaultState = customDataMonitor.getDefaultState();
        if (defaultState != null) defaultStateComboBox.setSelectedItem(defaultState);

        final ComparisonType comparisonType = customDataMonitor.getComparisonType();
        if (comparisonType != null) conditionalComboBox.setSelectedItem(comparisonType);

        final String threshold = customDataMonitor.getThreshold();
        conditionalValueTextField.setText(threshold == null ? "" : threshold);

        clearableCheckBox.setSelected(customDataMonitor.isClearable());
        ackCheckBox.setSelected(customDataMonitor.isAckReq());
        holdStateCheckBox.setSelected(customDataMonitor.isHoldState());
        showValueCheckBox.setSelected(customDataMonitor.isShowValue());
    }

    @Override
    public String validateContents()
    {
        String errorString = ""; // Start with an empty String, if it stays empty then everything was valid
        final String displayNameText = displayNameTextField.getText();

        if (StringUtils.isEmpty(displayNameText))
        {
            errorString += "\t-Please enter a Name for the alarm.\n";
        }

        final String conditionalValueText = conditionalValueTextField.getText();
        if (StringUtils.isEmpty(conditionalValueText))
        {
            errorString += "\t-Please enter a Condition Threshold Value for the alarm.\n";
        }
        if (getFieldType() == null)
        {
            errorString += "\t-Please select a valid Alarm Field.\n";
        }
        if (dataStructureComboBox.getSelectedIndex() == -1)
        {
            errorString += "\t-Please select a Message Type for the alarm.\n";
        }
        if (fieldComboBox.getSelectedIndex() == -1)
        {
            errorString += "\t-Please select a Field Type for the alarm.\n";
        }

        if (errorString.isEmpty())
        {
            return null;
        } else
        {
            if (StringUtils.isEmpty(displayNameText))
            {
                return "Data Monitor Alarm Error(s): \n" + errorString + "\n";
            } else
            {
                return displayNameText + " Alarm Error(s): \n" + errorString + "\n";
            }
        }
    }

    @Override
    public void applyAction()
    {
        customDataMonitor.setDataMonitorAlarmValues(getDisplayName(), getStructureName(), getFieldName(),
                getComparisonType(), getComparisonValue(), getAlarmState(), getFieldType(), getDefaultState(),
                isClearable(), isAckRequired(), isShowValue(), isHoldState());
    }

    @Override
    public String getDisplayName()
    {
        return displayNameTextField.getText();
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

    private boolean isShowValue()
    {
        return showValueCheckBox.isSelected();
    }

    private String getStructureName()
    {
        return (String) dataStructureComboBox.getSelectedItem();
    }

    private String getFieldName()
    {
        return (String) fieldComboBox.getSelectedItem();
    }

    private FieldType getFieldType()
    {
        final int fieldType = selectedCoreField != null
                ? selectedCoreField.getFieldType()
                : selectedStructureField != null ? selectedStructureField.getFieldType() : -1;

        return fieldType == -1 ? null : FieldType.getFieldType(fieldType);
    }

    private ComparisonType getComparisonType()
    {
        return (ComparisonType) conditionalComboBox.getSelectedItem();
    }

    private String getComparisonValue()
    {
        return conditionalValueTextField.getText();
    }

    private IRaptorDataStructure findStructure(String messageType)
    {
        IRaptorDataStructure match = null;

        for (IRaptorDataStructure structure : structures)
        {
            if (structure.getMessageType().equals(messageType))
            {
                match = structure;
                break;
            }
        }

        return match;
    }

    private void selectDataStructure(int index)
    {
        String name = dataStructureComboBox.getItemAt(index);
        IRaptorDataStructure structure = findStructure(name);
        if (structure != null)
        {
            selectedStructure = structure;
            populateFieldComboBox(structure.getFields());

            String rdsDetails = selectedStructure
                    .getStructure()
                    .getMetadata()
                    .getMetadataValue(IRaptorDataStructure.METADATA_STRUCTURE_DETAILS);

            if (StringUtils.isEmpty(rdsDetails))
            {
                descriptionTextPane.setText("None given");
            } else
            {
                descriptionTextPane.setText(rdsDetails);
            }
            descriptionTextPane.setToolTipText("Description of Message Type (not editable) - " + descriptionTextPane.getText());
        }
    }

    @InvokeOnEDT
    private void populateFieldComboBox(final Collection<IDataField> fields)
    {
        // We're creating two groups of field names:
        //  - all the core fields, sorted by name
        //  - all the structure fields, sorted as DataFields (which includes the ordinal position)

        final Stream<String> coreFieldNames = SUPPORTED_CORE_FIELDS.stream()
                .map(CoreField::name)
                .sorted(String.CASE_INSENSITIVE_ORDER);

        final Stream<String> structureFields = fields.stream()
                .sorted()
                .filter(field -> field.getFieldType() != IDataField.DATE) // No support for Date fields yet
                .map(IDataField::getFieldName);

        fieldComboBoxModel.removeAllElements();
        Stream.concat(coreFieldNames, structureFields).forEach(fieldComboBoxModel::addElement);

        fieldComboBox.setModel(fieldComboBoxModel);

        if (fieldComboBoxModel.getSize() > 0)
        {
            fieldComboBox.setSelectedIndex(0);
        }

        String fieldName = (String) fieldComboBox.getSelectedItem();
        selectedCoreField = CoreField.forName(fieldName);
        selectedStructureField = selectedStructure.getField(fieldName);

        updateConditionalComboBox();
    }

    private void updateConditionalComboBox()
    {
        final FieldType fieldType = getFieldType();

        if (fieldType == FieldType.STRING)
        {
            final Object selectedItem = conditionalComboBox.getSelectedItem();

            if (selectedItem != ComparisonType.EQ && selectedItem != ComparisonType.NEQ)
            {
                conditionalComboBox.setSelectedItem(ComparisonType.EQ);
            }
        }
    }

    private void dataStructureComboBoxItemStateChanged(ItemEvent e)
    {
        String name = (String) e.getItem();
        IRaptorDataStructure structure = findStructure(name);
        if (structure != selectedStructure)
        {
            int index = dataStructureComboBox.getSelectedIndex();
            selectDataStructure(index);
        }
        setDirty();
    }

    private void conditionalComboBoxItemStateChanged()
    {
        updateConditionalComboBox();
        setDirty();
    }

    private void fieldComboBoxItemStateChanged(ItemEvent e)
    {
        if (e.getStateChange() == ItemEvent.SELECTED)
        {
            String fieldName = (String) e.getItem();
            selectedCoreField = CoreField.forName(fieldName);
            selectedStructureField = selectedStructure.getField(fieldName);

            updateConditionalComboBox();
        }
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

    private void showValueCheckBoxItemStateChanged()
    {
        setDirty();
    }

    private void levelComboBoxItemStateChanged()
    {
        setDirty();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var panel1 = new JPanel();
        var label8 = new JLabel();
        displayNameTextField = new JTextField();
        var label1 = new JLabel();
        dataStructureComboBox = new JComboBox<>();
        var label2 = new JLabel();
        var label5 = new JLabel();
        var scrollPane1 = new JScrollPane();
        descriptionTextPane = new JTextPane();
        var label3 = new JLabel();
        fieldComboBox = new JComboBox<>();
        var label6 = new JLabel();
        var panel5 = new JPanel();
        conditionalComboBox = new JComboBox<>();
        conditionalValueTextField = new JTextField();
        var label4 = new JLabel();
        levelComboBox = new JComboBox<>();
        var label7 = new JLabel();
        defaultStateComboBox = new JComboBox<>();
        clearableCheckBox = new JCheckBox();
        holdStateCheckBox = new JCheckBox();
        ackCheckBox = new JCheckBox();
        showValueCheckBox = new JCheckBox();
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
                    "default, $lcgap, min:grow",
                    "2*(default, $lgap), 2*(fill:default), 4*($lgap, default)"));

            //---- label8 ----
            label8.setText("Alarm Name:");
            label8.setName("label8");
            panel1.add(label8, cc.xy(1, 1));

            //---- displayNameTextField ----
            displayNameTextField.setToolTipText("Name for custom alarm");
            displayNameTextField.setName("displayNameTextField");
            panel1.add(displayNameTextField, cc.xy(3, 1));

            //---- label1 ----
            label1.setText("Message Type:");
            label1.setName("label1");
            panel1.add(label1, cc.xy(1, 3));

            //---- dataStructureComboBox ----
            dataStructureComboBox.setToolTipText("Message type used to create the custom alarm");
            dataStructureComboBox.setName("dataStructureComboBox");
            dataStructureComboBox.addItemListener(e -> dataStructureComboBoxItemStateChanged(e));
            panel1.add(dataStructureComboBox, cc.xy(3, 3));

            //---- label2 ----
            label2.setText("Message");
            label2.setName("label2");
            panel1.add(label2, cc.xy(1, 5, CellConstraints.DEFAULT, CellConstraints.BOTTOM));

            //---- label5 ----
            label5.setText("Description:");
            label5.setName("label5");
            panel1.add(label5, cc.xy(1, 6, CellConstraints.DEFAULT, CellConstraints.TOP));

            //======== scrollPane1 ========
            {
                scrollPane1.setName("scrollPane1");

                //---- descriptionTextPane ----
                descriptionTextPane.setEditable(false);
                descriptionTextPane.setEnabled(false);
                descriptionTextPane.setToolTipText("Description of Message Type (not editable)");
                descriptionTextPane.setName("descriptionTextPane");
                scrollPane1.setViewportView(descriptionTextPane);
            }
            panel1.add(scrollPane1, cc.xywh(3, 5, 1, 2));

            //---- label3 ----
            label3.setText("Alarm Field:");
            label3.setName("label3");
            panel1.add(label3, cc.xy(1, 8));

            //---- fieldComboBox ----
            fieldComboBox.setToolTipText("Field in the message used to create the custom alarm");
            fieldComboBox.setName("fieldComboBox");
            fieldComboBox.addItemListener(e -> fieldComboBoxItemStateChanged(e));
            panel1.add(fieldComboBox, cc.xy(3, 8));

            //---- label6 ----
            label6.setText("Condition:");
            label6.setName("label6");
            panel1.add(label6, cc.xy(1, 10));

            //======== panel5 ========
            {
                panel5.setName("panel5");
                panel5.setLayout(new FormLayout(
                        "default, $lcgap, default:grow",
                        "default"));

                //---- conditionalComboBox ----
                conditionalComboBox.setToolTipText("Condition");
                conditionalComboBox.setName("conditionalComboBox");
                conditionalComboBox.addItemListener(e -> conditionalComboBoxItemStateChanged());
                panel5.add(conditionalComboBox, cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

                //---- conditionalValueTextField ----
                conditionalValueTextField.setToolTipText("Threshold value");
                conditionalValueTextField.setName("conditionalValueTextField");
                panel5.add(conditionalValueTextField, cc.xy(3, 1));
            }
            panel1.add(panel5, cc.xy(3, 10));

            //---- label4 ----
            label4.setText("Alarm Type:");
            label4.setName("label4");
            panel1.add(label4, cc.xy(1, 12));

            //---- levelComboBox ----
            levelComboBox.setToolTipText("Severity of alarm when satisfying alarm condition");
            levelComboBox.setName("levelComboBox");
            levelComboBox.addItemListener(e -> levelComboBoxItemStateChanged());
            panel1.add(levelComboBox, cc.xy(3, 12));

            //---- label7 ----
            label7.setText("Default State:");
            label7.setName("label7");
            panel1.add(label7, cc.xy(1, 14));

            //---- defaultStateComboBox ----
            defaultStateComboBox.setToolTipText("State to display when NOT alarming");
            defaultStateComboBox.setName("defaultStateComboBox");
            defaultStateComboBox.addItemListener(e -> defaultStateComboBoxItemStateChanged());
            panel1.add(defaultStateComboBox, cc.xy(3, 14));
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
        holdStateCheckBox.setName("holdStateCheckBox");
        holdStateCheckBox.addItemListener(e -> holdStateCheckBoxItemStateChanged());
        add(holdStateCheckBox, cc.xy(3, 3));

        //---- ackCheckBox ----
        ackCheckBox.setText("Require ACK");
        ackCheckBox.setToolTipText("Require the operator to acknowledge the alarm before it can be completely cleared");
        ackCheckBox.setName("ackCheckBox");
        ackCheckBox.addItemListener(e -> ackCheckBoxItemStateChanged());
        add(ackCheckBox, cc.xy(1, 5));

        //---- showValueCheckBox ----
        showValueCheckBox.setText("Show Value");
        showValueCheckBox.setToolTipText("Show current value of the field in the alarm status panel (i.e. battery voltage)");
        showValueCheckBox.setName("showValueCheckBox");
        showValueCheckBox.addItemListener(e -> showValueCheckBoxItemStateChanged());
        add(showValueCheckBox, cc.xy(3, 5));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTextField displayNameTextField;
    private JComboBox<String> dataStructureComboBox;
    private JTextPane descriptionTextPane;
    private JComboBox<String> fieldComboBox;
    private JComboBox<ComparisonType> conditionalComboBox;
    private JTextField conditionalValueTextField;
    private JComboBox<State> levelComboBox;
    private JComboBox<State> defaultStateComboBox;
    private JCheckBox clearableCheckBox;
    private JCheckBox holdStateCheckBox;
    private JCheckBox ackCheckBox;
    private JCheckBox showValueCheckBox;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
