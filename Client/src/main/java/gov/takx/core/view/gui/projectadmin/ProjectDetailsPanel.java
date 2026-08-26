package gov.takx.core.view.gui.projectadmin;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.project.IProjectMapObjectMetadata;
import gov.takx.api.project.IProjectMetadata;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXDatePicker;
import org.jdesktop.swingx.JXEditorPane;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;

import java.util.Date;

/**
 * Panel to show the details from the project metadata.
 *
 * @since 3.0
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public class ProjectDetailsPanel extends JPanel
{
    private static final String ZERO_SIZE = "0 (0 mb)";

    private final DefaultListModel<String> mapObjectListModel = new DefaultListModel<>();
    private boolean editable = true;

    public ProjectDetailsPanel()
    {
        initComponents();

        mapObjectList.setModel(mapObjectListModel);
    }

    /**
     * Install the values from the given project metadata into this form.
     *
     * @param pmd Project metadata to install
     */
    public void setProjectDetails(@Nullable IProjectMetadata pmd)
    {
        if (pmd == null)
        {
            clear();
            return;
        }

        final long startDate = pmd.getStartDate();
        startDateField.setDate(startDate != 0 ? new Date(startDate) : null);
        startDateField.setEnabled(editable);

        final long endDate = pmd.getEndDate();
        endDateField.setDate(endDate != 0 ? new Date(endDate) : null);
        endDateField.setEnabled(editable);

        descriptionTextField.setText(valueOrDefault(pmd.getDescription(), "No description has been set."));
        locationTextField.setText(valueOrDefault(pmd.getLocation(), "No location has been set."));
        notesTextField.setText(valueOrDefault(pmd.getNotes(), "No notes have been set."));
        sizeLabel.setText(valueOrDefault(pmd.getSize(), ZERO_SIZE));
        vfsLabel.setText(valueOrDefault(pmd.getVfsSize(), ZERO_SIZE));

        // Install the Map Object display strings
        mapObjectListModel.clear();

        pmd.getMapObjects().stream()
                .map(IProjectMapObjectMetadata::getDisplayText)
                .sorted()
                .forEach(mapObjectListModel::addElement);
    }

    /**
     * This enables or disables all the text/date entry fields in this panel.
     *
     * @param editable whether the fields should be enabled for editing
     */
    public void enableEditing(boolean editable)
    {
        this.editable = editable;
        startDateField.setEditable(editable);
        endDateField.setEditable(editable);
        descriptionTextField.setEditable(editable);
        locationTextField.setEditable(editable);
        notesTextField.setEditable(editable);
    }

    /**
     * Clear all the fields.
     */
    public void clear()
    {
        mapObjectListModel.clear();

        descriptionTextField.setText("");
        locationTextField.setText("");
        notesTextField.setText("");
        sizeLabel.setText(ZERO_SIZE);
        vfsLabel.setText(ZERO_SIZE);

        startDateField.setDate(null);
        startDateField.setEnabled(false);
        endDateField.setDate(null);
        endDateField.setEnabled(false);
    }

    /**
     * @return Text value of the location field
     */
    public String getLocationText()
    {
        return locationTextField.getText();
    }

    /**
     * @return Text value of the notes field
     */
    public String getNotesText()
    {
        return notesTextField.getText();
    }

    /**
     * @return Text value of the description field
     */
    public String getDescriptionText()
    {
        return descriptionTextField.getText();
    }

    /**
     * @return Text value of the size label
     */
    public String getSizeText()
    {
        return sizeLabel.getText();
    }

    /**
     * @return Text value of the VFS size label
     */
    public String getVfsText()
    {
        return vfsLabel.getText();
    }

    /**
     * @return Start time value, millis since the epoch
     */
    public long getStartTime()
    {
        final Date date = startDateField.getDate();
        return date == null ? System.currentTimeMillis() : date.getTime();
    }

    /**
     * @return End time value, millis since the epoch
     */
    public long getEndTime()
    {
        final Date date = endDateField.getDate();
        return date == null ? System.currentTimeMillis() : date.getTime();
    }

    /**
     * @param value        Value to check
     * @param defaultValue Default value to use
     * @return the given {@code value} if it is not {@code null}/empty, otherwise the {@code defaultValue}.
     */
    private String valueOrDefault(String value, String defaultValue)
    {
        return StringUtils.isEmpty(value) ? defaultValue : value;
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var tabbedPane = new JTabbedPane();
        var panel5 = new JPanel();
        var datesPanel = new JPanel();
        var label19 = new JLabel();
        startDateField = new JXDatePicker();
        var label20 = new JLabel();
        endDateField = new JXDatePicker();
        var sizesPanel = new JPanel();
        var label24 = new JLabel();
        sizeLabel = new JLabel();
        var label25 = new JLabel();
        vfsLabel = new JLabel();
        var mapObjectPanel = new JPanel();
        var scrollPane1 = new JScrollPane();
        mapObjectList = new JList<>();
        var scrollPane15 = new JScrollPane();
        descriptionTextField = new JXEditorPane();
        var scrollPane12 = new JScrollPane();
        locationTextField = new JXEditorPane();
        var scrollPane14 = new JScrollPane();
        notesTextField = new JXEditorPane();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default"));

        //======== tabbedPane ========
        {
            tabbedPane.setName("tabbedPane");

            //======== panel5 ========
            {
                panel5.setName("panel5");
                panel5.setLayout(new FormLayout(
                        "default:grow, $lcgap, default:grow",
                        "default, $lgap, default"));

                //======== datesPanel ========
                {
                    datesPanel.setName("datesPanel");
                    datesPanel.setLayout(new FormLayout(
                            "default, $lcgap, default:grow",
                            "default, $lgap, fill:default"));

                    //---- label19 ----
                    label19.setText(" Start Date");
                    label19.setName("label19");
                    datesPanel.add(label19, cc.xy(1, 1, CellConstraints.CENTER, CellConstraints.CENTER));

                    //---- startDateField ----
                    startDateField.setName("startDateField");
                    datesPanel.add(startDateField, cc.xy(3, 1));

                    //---- label20 ----
                    label20.setText("Stop Date");
                    label20.setName("label20");
                    datesPanel.add(label20, cc.xy(1, 3, CellConstraints.CENTER, CellConstraints.CENTER));

                    //---- endDateField ----
                    endDateField.setName("endDateField");
                    datesPanel.add(endDateField, cc.xy(3, 3));
                }
                panel5.add(datesPanel, cc.xy(1, 1));

                //======== sizesPanel ========
                {
                    sizesPanel.setName("sizesPanel");
                    sizesPanel.setLayout(new FormLayout(
                            "$ugap, right:default, $lcgap, left:pref:grow",
                            "default, $lgap, default"));

                    //---- label24 ----
                    label24.setText("Project Size:");
                    label24.setName("label24");
                    sizesPanel.add(label24, cc.xy(2, 1));

                    //---- sizeLabel ----
                    sizeLabel.setText("0.0 mb");
                    sizeLabel.setBorder(null);
                    sizeLabel.setName("sizeLabel");
                    sizesPanel.add(sizeLabel, cc.xy(4, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));

                    //---- label25 ----
                    label25.setText("VFS Size:");
                    label25.setHorizontalAlignment(SwingConstants.LEFT);
                    label25.setName("label25");
                    sizesPanel.add(label25, cc.xy(2, 3));

                    //---- vfsLabel ----
                    vfsLabel.setText("0 (0.0 mb)");
                    vfsLabel.setName("vfsLabel");
                    sizesPanel.add(vfsLabel, cc.xy(4, 3));
                }
                panel5.add(sizesPanel, cc.xy(3, 1));
            }
            tabbedPane.addTab("General", panel5);

            //======== mapObjectPanel ========
            {
                mapObjectPanel.setName("mapObjectPanel");
                mapObjectPanel.setLayout(new FormLayout(
                        "default:grow",
                        "fill:default:grow"));

                //======== scrollPane1 ========
                {
                    scrollPane1.setName("scrollPane1");

                    //---- mapObjectList ----
                    mapObjectList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                    mapObjectList.setName("mapObjectList");
                    scrollPane1.setViewportView(mapObjectList);
                }
                mapObjectPanel.add(scrollPane1, cc.xy(1, 1));
            }
            tabbedPane.addTab("Map Objects", mapObjectPanel);

            //======== scrollPane15 ========
            {
                scrollPane15.setOpaque(false);
                scrollPane15.setName("scrollPane15");

                //---- descriptionTextField ----
                descriptionTextField.setName("descriptionTextField");
                scrollPane15.setViewportView(descriptionTextField);
            }
            tabbedPane.addTab("Description", scrollPane15);

            //======== scrollPane12 ========
            {
                scrollPane12.setOpaque(false);
                scrollPane12.setName("scrollPane12");

                //---- locationTextField ----
                locationTextField.setName("locationTextField");
                scrollPane12.setViewportView(locationTextField);
            }
            tabbedPane.addTab("Location", scrollPane12);

            //======== scrollPane14 ========
            {
                scrollPane14.setOpaque(false);
                scrollPane14.setName("scrollPane14");

                //---- notesTextField ----
                notesTextField.setName("notesTextField");
                scrollPane14.setViewportView(notesTextField);
            }
            tabbedPane.addTab("Notes", scrollPane14);
        }
        add(tabbedPane, cc.xy(1, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JXDatePicker startDateField;
    private JXDatePicker endDateField;
    private JLabel sizeLabel;
    private JLabel vfsLabel;
    private JList<String> mapObjectList;
    private JXEditorPane descriptionTextField;
    private JXEditorPane locationTextField;
    private JXEditorPane notesTextField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
