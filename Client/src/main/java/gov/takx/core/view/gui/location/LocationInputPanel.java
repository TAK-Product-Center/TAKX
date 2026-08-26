package gov.takx.core.view.gui.location;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.ui.location.ILocationInputListener;
import gov.takx.api.ui.location.ILocationInputPanel;
import gov.takx.platform.ui.location.MarkerIconPanel;
import jakarta.annotation.Nonnull;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * User interface for providing a location.
 *
 * @since 3.0
 */
// TODO KMB: When this is actually implemented, several improvements should be made over the existing implementation:
//  - Add tooltips to each of the fields to indicate the type of value expected and the valid range
//  - Add support for allowing the user to paste a valid location into the first text field on each tab and the location
//    will be parsed and the text fields populated correctly
//  - Add a red border around text fields when they are invalid (route planner plugin has code we can copy)
public class LocationInputPanel extends JPanel implements ILocationInputPanel
{
    private final List<ILocationInputListener> listeners = new CopyOnWriteArrayList<>();

    public LocationInputPanel()
    {
        initComponents();
    }

    @Override
    public void addListener(@Nonnull ILocationInputListener listener)
    {
        listeners.add(listener);
    }

    @Override
    public void removeListener(@Nonnull ILocationInputListener listener)
    {
        listeners.remove(listener);
    }

    @Override
    public void setLocation(@Nonnull ILocation location, CoordinateSystem coordinateSystem)
    {
        // TODO: Populate all the location fields and display the appropriate tab based on the specified coordinate system
    }

    @Override
    public void setShowMarkerIcons(boolean showMarkerIcons)
    {
        markerIconPanel.setVisible(showMarkerIcons);
    }

    private void autoFillFromMapButtonActionPerformed()
    {
        // TODO: Autofill all the location fields from the coordinates of the center of the map
    }

    private void elevationDtedButtonActionPerformed()
    {
        // TODO: Populate elevation from DTED (note: the button will need to be enabled if DTED is present)
    }

    private void clearButtonActionPerformed()
    {
        // TODO: Clear all location fields
    }

    private void copyButtonActionPerformed()
    {
        // TODO: Copy location onto clipboard
    }

    private void okButtonActionPerformed()
    {
        // TODO: Notify listeners of the selected location
//        final URI markerIconUri = markerIconPanel.getSelectedMarkerIcon();
//        listeners.forEach(listener -> listener.onLocationSelected(location, coordinateSystem, markerIconUri));
    }

    private void cancelButtonActionPerformed()
    {
        listeners.forEach(ILocationInputListener::onCancel);
    }

    @SuppressWarnings("DuplicatedCode")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        coordinateTypeTabPane = new JTabbedPane();
        mgrsPanel = new JPanel();
        var locationMgrsLabel = new JLabel();
        mgrsGzdTextField = new JTextField();
        mgrsSquareIdTextField = new JTextField();
        mgrsEastingTextField = new JTextField();
        mgrsNorthingTextField = new JTextField();
        var autoFillFromMapMgrsButton = new JButton();
        decimalDegreesPanel = new JPanel();
        var locationDegreesLabel = new JLabel();
        degreesLatTextField = new JTextField();
        degreesLongTextField = new JTextField();
        var autoFillFromMapDegreesButton = new JButton();
        dmPanel = new JPanel();
        var dmLatitudeLabel = new JLabel();
        dmDegreesLatTextField = new JTextField();
        var dmDegreesLatLabel = new JLabel();
        dmMinutesLatTextField = new JTextField();
        var dmMinutesLatLabel = new JLabel();
        var autoFillFromMapDmButton = new JButton();
        var dmLongitudeLabel = new JLabel();
        dmDegreesLongTextField = new JTextField();
        var dmDegreesLongLabel = new JLabel();
        dmMinutesLongTextField = new JTextField();
        var dmMinutesLongLabel = new JLabel();
        dmsPanel = new JPanel();
        var dmsLatitudeLabel = new JLabel();
        dmsDegreesLatTextField = new JTextField();
        var dmsDegreesLatLabel = new JLabel();
        dmsMinutesLatTextField = new JTextField();
        var dmsMinutesLatLabel = new JLabel();
        dmsSecondsLatTextField = new JTextField();
        var dmsSecondsLatLabel = new JLabel();
        var autoFillFromMapDmsButton = new JButton();
        var dmsLongitudeLabel = new JLabel();
        dmsDegreesLongTextField = new JTextField();
        var dmsDegreesLongLabel = new JLabel();
        dmsMinutesLongTextField = new JTextField();
        var dmsMinutesLongLabel = new JLabel();
        dmsSecondsLongTextField = new JTextField();
        var dmsSecondsLongLabel = new JLabel();
        var elevationLabel = new JLabel();
        elevationTextField = new JTextField();
        var ftMslLabel = new JLabel();
        elevationDtedButton = new JButton();
        markerIconPanel = new MarkerIconPanel();
        var clearButton = new JButton();
        var copyButton = new JButton();
        var buttonPanel = new JPanel();
        okButton = new JButton();
        var cancelButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow, $lcgap, default, 2*($lcgap, $button)",
                "4*(default, $lgap), default"));

        //======== coordinateTypeTabPane ========
        {
            coordinateTypeTabPane.setName("coordinateTypeTabPane");

            //======== mgrsPanel ========
            {
                mgrsPanel.setName("mgrsPanel");
                mgrsPanel.setLayout(new FormLayout(
                        "default, 4*($lcgap, default:grow), $lcgap, default",
                        "default"));

                //---- locationMgrsLabel ----
                locationMgrsLabel.setText("Location");
                locationMgrsLabel.setName("locationMgrsLabel");
                mgrsPanel.add(locationMgrsLabel, cc.xy(1, 1));

                //---- mgrsGzdTextField ----
                mgrsGzdTextField.setName("mgrsGzdTextField");
                mgrsPanel.add(mgrsGzdTextField, cc.xy(3, 1));

                //---- mgrsSquareIdTextField ----
                mgrsSquareIdTextField.setName("mgrsSquareIdTextField");
                mgrsPanel.add(mgrsSquareIdTextField, cc.xy(5, 1));

                //---- mgrsEastingTextField ----
                mgrsEastingTextField.setName("mgrsEastingTextField");
                mgrsPanel.add(mgrsEastingTextField, cc.xy(7, 1));

                //---- mgrsNorthingTextField ----
                mgrsNorthingTextField.setName("mgrsNorthingTextField");
                mgrsPanel.add(mgrsNorthingTextField, cc.xy(9, 1));

                //---- autoFillFromMapMgrsButton ----
                autoFillFromMapMgrsButton.setName("autoFillFromMapMgrsButton");
                autoFillFromMapMgrsButton.addActionListener(e -> autoFillFromMapButtonActionPerformed());
                mgrsPanel.add(autoFillFromMapMgrsButton, cc.xy(11, 1));
            }
            coordinateTypeTabPane.addTab("MGRS", mgrsPanel);

            //======== decimalDegreesPanel ========
            {
                decimalDegreesPanel.setName("decimalDegreesPanel");
                decimalDegreesPanel.setLayout(new FormLayout(
                        "default, 2*($lcgap, default:grow), $lcgap, default",
                        "default"));

                //---- locationDegreesLabel ----
                locationDegreesLabel.setText("Location");
                locationDegreesLabel.setName("locationDegreesLabel");
                decimalDegreesPanel.add(locationDegreesLabel, cc.xy(1, 1));

                //---- degreesLatTextField ----
                degreesLatTextField.setName("degreesLatTextField");
                decimalDegreesPanel.add(degreesLatTextField, cc.xy(3, 1));

                //---- degreesLongTextField ----
                degreesLongTextField.setName("degreesLongTextField");
                decimalDegreesPanel.add(degreesLongTextField, cc.xy(5, 1));

                //---- autoFillFromMapDegreesButton ----
                autoFillFromMapDegreesButton.setName("autoFillFromMapDegreesButton");
                autoFillFromMapDegreesButton.addActionListener(e -> autoFillFromMapButtonActionPerformed());
                decimalDegreesPanel.add(autoFillFromMapDegreesButton, cc.xy(7, 1));
            }
            coordinateTypeTabPane.addTab("Deg", decimalDegreesPanel);

            //======== dmPanel ========
            {
                dmPanel.setName("dmPanel");
                dmPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, default, $lcgap, default:grow, default, $lcgap, default",
                        "default, $lgap, default"));

                //---- dmLatitudeLabel ----
                dmLatitudeLabel.setText("Latitude");
                dmLatitudeLabel.setName("dmLatitudeLabel");
                dmPanel.add(dmLatitudeLabel, cc.xy(1, 1));

                //---- dmDegreesLatTextField ----
                dmDegreesLatTextField.setName("dmDegreesLatTextField");
                dmPanel.add(dmDegreesLatTextField, cc.xy(3, 1));

                //---- dmDegreesLatLabel ----
                dmDegreesLatLabel.setText("\u00b0");
                dmDegreesLatLabel.setName("dmDegreesLatLabel");
                dmPanel.add(dmDegreesLatLabel, cc.xy(4, 1));

                //---- dmMinutesLatTextField ----
                dmMinutesLatTextField.setName("dmMinutesLatTextField");
                dmPanel.add(dmMinutesLatTextField, cc.xy(6, 1));

                //---- dmMinutesLatLabel ----
                dmMinutesLatLabel.setText("'");
                dmMinutesLatLabel.setName("dmMinutesLatLabel");
                dmPanel.add(dmMinutesLatLabel, cc.xy(7, 1));

                //---- autoFillFromMapDmButton ----
                autoFillFromMapDmButton.setName("autoFillFromMapDmButton");
                autoFillFromMapDmButton.addActionListener(e -> autoFillFromMapButtonActionPerformed());
                dmPanel.add(autoFillFromMapDmButton, cc.xy(9, 1));

                //---- dmLongitudeLabel ----
                dmLongitudeLabel.setText("Longitude");
                dmLongitudeLabel.setName("dmLongitudeLabel");
                dmPanel.add(dmLongitudeLabel, cc.xy(1, 3));

                //---- dmDegreesLongTextField ----
                dmDegreesLongTextField.setName("dmDegreesLongTextField");
                dmPanel.add(dmDegreesLongTextField, cc.xy(3, 3));

                //---- dmDegreesLongLabel ----
                dmDegreesLongLabel.setText("\u00b0");
                dmDegreesLongLabel.setName("dmDegreesLongLabel");
                dmPanel.add(dmDegreesLongLabel, cc.xy(4, 3));

                //---- dmMinutesLongTextField ----
                dmMinutesLongTextField.setName("dmMinutesLongTextField");
                dmPanel.add(dmMinutesLongTextField, cc.xy(6, 3));

                //---- dmMinutesLongLabel ----
                dmMinutesLongLabel.setText("'");
                dmMinutesLongLabel.setName("dmMinutesLongLabel");
                dmPanel.add(dmMinutesLongLabel, cc.xy(7, 3));
            }
            coordinateTypeTabPane.addTab("DM", dmPanel);

            //======== dmsPanel ========
            {
                dmsPanel.setName("dmsPanel");
                dmsPanel.setLayout(new FormLayout(
                        "default, $lcgap, default:grow, default, $lcgap, default:grow, default, $lcgap, default:grow, 2*($lcgap, default)",
                        "default, $lgap, default"));

                //---- dmsLatitudeLabel ----
                dmsLatitudeLabel.setText("Latitude");
                dmsLatitudeLabel.setName("dmsLatitudeLabel");
                dmsPanel.add(dmsLatitudeLabel, cc.xy(1, 1));

                //---- dmsDegreesLatTextField ----
                dmsDegreesLatTextField.setName("dmsDegreesLatTextField");
                dmsPanel.add(dmsDegreesLatTextField, cc.xy(3, 1));

                //---- dmsDegreesLatLabel ----
                dmsDegreesLatLabel.setText("\u00b0");
                dmsDegreesLatLabel.setName("dmsDegreesLatLabel");
                dmsPanel.add(dmsDegreesLatLabel, cc.xy(4, 1));

                //---- dmsMinutesLatTextField ----
                dmsMinutesLatTextField.setName("dmsMinutesLatTextField");
                dmsPanel.add(dmsMinutesLatTextField, cc.xy(6, 1));

                //---- dmsMinutesLatLabel ----
                dmsMinutesLatLabel.setText("'");
                dmsMinutesLatLabel.setName("dmsMinutesLatLabel");
                dmsPanel.add(dmsMinutesLatLabel, cc.xy(7, 1));

                //---- dmsSecondsLatTextField ----
                dmsSecondsLatTextField.setName("dmsSecondsLatTextField");
                dmsPanel.add(dmsSecondsLatTextField, cc.xy(9, 1));

                //---- dmsSecondsLatLabel ----
                dmsSecondsLatLabel.setText("\"");
                dmsSecondsLatLabel.setName("dmsSecondsLatLabel");
                dmsPanel.add(dmsSecondsLatLabel, cc.xy(11, 1));

                //---- autoFillFromMapDmsButton ----
                autoFillFromMapDmsButton.setName("autoFillFromMapDmsButton");
                autoFillFromMapDmsButton.addActionListener(e -> autoFillFromMapButtonActionPerformed());
                dmsPanel.add(autoFillFromMapDmsButton, cc.xy(13, 1));

                //---- dmsLongitudeLabel ----
                dmsLongitudeLabel.setText("Longitude");
                dmsLongitudeLabel.setName("dmsLongitudeLabel");
                dmsPanel.add(dmsLongitudeLabel, cc.xy(1, 3));

                //---- dmsDegreesLongTextField ----
                dmsDegreesLongTextField.setName("dmsDegreesLongTextField");
                dmsPanel.add(dmsDegreesLongTextField, cc.xy(3, 3));

                //---- dmsDegreesLongLabel ----
                dmsDegreesLongLabel.setText("\u00b0");
                dmsDegreesLongLabel.setName("dmsDegreesLongLabel");
                dmsPanel.add(dmsDegreesLongLabel, cc.xy(4, 3));

                //---- dmsMinutesLongTextField ----
                dmsMinutesLongTextField.setName("dmsMinutesLongTextField");
                dmsPanel.add(dmsMinutesLongTextField, cc.xy(6, 3));

                //---- dmsMinutesLongLabel ----
                dmsMinutesLongLabel.setText("'");
                dmsMinutesLongLabel.setName("dmsMinutesLongLabel");
                dmsPanel.add(dmsMinutesLongLabel, cc.xy(7, 3));

                //---- dmsSecondsLongTextField ----
                dmsSecondsLongTextField.setName("dmsSecondsLongTextField");
                dmsPanel.add(dmsSecondsLongTextField, cc.xy(9, 3));

                //---- dmsSecondsLongLabel ----
                dmsSecondsLongLabel.setText("\"");
                dmsSecondsLongLabel.setName("dmsSecondsLongLabel");
                dmsPanel.add(dmsSecondsLongLabel, cc.xy(11, 3));
            }
            coordinateTypeTabPane.addTab("DMS", dmsPanel);
        }
        add(coordinateTypeTabPane, cc.xywh(1, 1, 9, 1));

        //---- elevationLabel ----
        elevationLabel.setText("Elevation");
        elevationLabel.setName("elevationLabel");
        add(elevationLabel, cc.xy(1, 3));

        //---- elevationTextField ----
        elevationTextField.setName("elevationTextField");
        add(elevationTextField, cc.xy(3, 3));

        //---- ftMslLabel ----
        ftMslLabel.setText("ft MSL");
        ftMslLabel.setName("ftMslLabel");
        add(ftMslLabel, cc.xy(5, 3));

        //---- elevationDtedButton ----
        elevationDtedButton.setText("Pull from DTED");
        elevationDtedButton.setEnabled(false);
        elevationDtedButton.setName("elevationDtedButton");
        elevationDtedButton.addActionListener(e -> elevationDtedButtonActionPerformed());
        add(elevationDtedButton, cc.xywh(7, 3, 3, 1));

        //---- markerIconPanel ----
        markerIconPanel.setVisible(false);
        markerIconPanel.setName("markerIconPanel");
        add(markerIconPanel, cc.xywh(1, 5, 5, 1));

        //---- clearButton ----
        clearButton.setName("clearButton");
        clearButton.addActionListener(e -> clearButtonActionPerformed());
        add(clearButton, cc.xy(7, 5));

        //---- copyButton ----
        copyButton.setName("copyButton");
        copyButton.addActionListener(e -> copyButtonActionPerformed());
        add(copyButton, cc.xy(9, 5));

        //======== buttonPanel ========
        {
            buttonPanel.setName("buttonPanel");
            buttonPanel.setLayout(new FormLayout(
                    "default:grow, $lcgap, default:grow",
                    "default"));

            //---- okButton ----
            okButton.setText("OK");
            okButton.setEnabled(false);
            okButton.setName("okButton");
            okButton.addActionListener(e -> okButtonActionPerformed());
            buttonPanel.add(okButton, cc.xy(1, 1));

            //---- cancelButton ----
            cancelButton.setText("Cancel");
            cancelButton.setName("cancelButton");
            cancelButton.addActionListener(e -> cancelButtonActionPerformed());
            buttonPanel.add(cancelButton, cc.xy(3, 1));
        }
        add(buttonPanel, cc.xywh(1, 7, 9, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JTabbedPane coordinateTypeTabPane;
    private JPanel mgrsPanel;
    private JTextField mgrsGzdTextField;
    private JTextField mgrsSquareIdTextField;
    private JTextField mgrsEastingTextField;
    private JTextField mgrsNorthingTextField;
    private JPanel decimalDegreesPanel;
    private JTextField degreesLatTextField;
    private JTextField degreesLongTextField;
    private JPanel dmPanel;
    private JTextField dmDegreesLatTextField;
    private JTextField dmMinutesLatTextField;
    private JTextField dmDegreesLongTextField;
    private JTextField dmMinutesLongTextField;
    private JPanel dmsPanel;
    private JTextField dmsDegreesLatTextField;
    private JTextField dmsMinutesLatTextField;
    private JTextField dmsSecondsLatTextField;
    private JTextField dmsDegreesLongTextField;
    private JTextField dmsMinutesLongTextField;
    private JTextField dmsSecondsLongTextField;
    private JTextField elevationTextField;
    private JButton elevationDtedButton;
    private MarkerIconPanel markerIconPanel;
    private JButton okButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
