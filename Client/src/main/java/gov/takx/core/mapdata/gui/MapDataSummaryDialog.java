package gov.takx.core.mapdata.gui;

import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.messages.IRaptorDataMessage;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.core.mapdata.RaptorDataMessageTableModel;
import gov.takx.core.mapdata.RaptorDataMessageTableModel.Columns;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.AStandardDialog;
import gov.takx.platform.ui.devicelog.ElevationStringValue;
import gov.takx.platform.ui.devicelog.SpeedStringValue;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.renderer.DefaultTableRenderer;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;

/**
 * Dialog to present a list of {@link IRaptorDataMessage}s selected from a geographic region.
 */
public class MapDataSummaryDialog extends AStandardDialog
{
    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor =
            UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::coordinateSystemChanged);
    private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor =
            UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, this::altitudeUnitsChanged);
    private final IPreferenceMonitor<SpeedUnits> speedUnitsMonitor =
            UserPreferenceEntry.DISPLAY_SPEED_UNITS.newMonitor(SpeedUnits.class, this::speedUnitsChanged);

    private final RaptorDataMessageTableModel tableModel;
    private final SpeedStringValue speedStringValue;
    private final ElevationStringValue elevationStringValue;

    public MapDataSummaryDialog(Frame frame, RaptorDataMessageTableModel tableModel)
    {
        super(frame);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.tableModel = tableModel;
        initComponents();

        messageCountLabel.setText(" Showing " + tableModel.getMessageCount() + " of " + tableModel.getPossibleMessageCount() + " messages");

        mapDataTable.setModel(tableModel);

        mapDataTable.getColumnExt(Columns.Type.toString()).setVisible(false);
        mapDataTable.getColumnExt(Columns.ID.toString()).setVisible(false);
        mapDataTable.getColumnExt(Columns.Heading.toString()).setVisible(false);
        mapDataTable.getColumnExt(Columns.Speed.toString()).setVisible(false);
        mapDataTable.getColumnExt(Columns.Latitude.toString()).setVisible(false);
        mapDataTable.getColumnExt(Columns.Longitude.toString()).setVisible(false);
        mapDataTable.getColumnExt(Columns.Elevation.toString()).setVisible(false);
        mapDataTable.getColumnExt(Columns.Received.toString()).setVisible(false);

        mapDataTable.toggleSortOrder(Columns.Time.toString());

        // Initialize the user's coordinate system preference
        coordinateSystemChanged(coordinateSystemMonitor.getCurrentValue());

        // Get the current UoM
        final AltitudeUnits altitudeUnits = altitudeUnitsMonitor.getCurrentValue();
        final SpeedUnits speedUnits = speedUnitsMonitor.getCurrentValue();

        // Update the table model's initial UoM.
        tableModel.updateAltitudeUnits(altitudeUnits);

        // Render the speed and elevation according to the current UoM
        speedStringValue = new SpeedStringValue(speedUnits);
        DefaultTableRenderer speedRenderer = new DefaultTableRenderer(speedStringValue, JLabel.RIGHT);
        mapDataTable.getColumnExt(Columns.Speed.toString()).setCellRenderer(speedRenderer);

        elevationStringValue = new ElevationStringValue(altitudeUnits);
        DefaultTableRenderer elevationRenderer = new DefaultTableRenderer(elevationStringValue, JLabel.RIGHT);
        mapDataTable.getColumnExt(Columns.Elevation.toString()).setCellRenderer(elevationRenderer);

        mapDataTable.packAll();
        pack();
    }

    @InvokeOnEDT
    private void altitudeUnitsChanged(@Nonnull AltitudeUnits altitudeUnits)
    {
        // Update all renderers with the current UoM.
        elevationStringValue.updateUnitsOfMeasure(altitudeUnits);
        tableModel.updateAltitudeUnits(altitudeUnits);

        // Refresh.
        repaint();
    }

    @InvokeOnEDT
    private void speedUnitsChanged(@Nonnull SpeedUnits speedUnits)
    {
        speedStringValue.updateUnitsOfMeasure(speedUnits);
        repaint();
    }

    private void closeButtonActionPerformed()
    {
        dispose();
    }

    /**
     * Update our labels and table content based on the current coordinate system.
     */
    @InvokeOnEDT
    private void coordinateSystemChanged(CoordinateSystem coordinateSystem)
    {
        String searchArea =
                " Min: " + CoordinateSystem.getLocationString(tableModel.getMinCorner(), coordinateSystem) +
                        "       Max: " + CoordinateSystem.getLocationString(tableModel.getMaxCorner(), coordinateSystem);

        trackSearchAreaLabel.setText(searchArea);

        tableModel.fireTableDataChanged();  // Force a repaint of the table contents (with new coord system)
    }

    @Override
    public void dispose()
    {
        // Stop listening for changes.
        coordinateSystemMonitor.dispose();
        altitudeUnitsMonitor.dispose();
        speedUnitsMonitor.dispose();

        super.dispose();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        var dialogPane = new JPanel();
        var contentPanel = new JPanel();
        var panel1 = new JPanel();
        trackSearchAreaLabel = new JLabel();
        messageCountLabel = new JLabel();
        var mapDataScrollPane = new JScrollPane();
        mapDataTable = new JXTable();
        var buttonBar = new JPanel();
        var closeButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setTitle("Track Properties");
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
                        "default:grow",
                        "default, $rgap, default:grow"));

                //======== panel1 ========
                {
                    panel1.setBorder(new TitledBorder(null, "Search Area", TitledBorder.LEADING, TitledBorder.TOP));
                    panel1.setName("panel1");
                    panel1.setLayout(new FormLayout(
                            "default:grow",
                            "default, $lgap, default"));

                    //---- trackSearchAreaLabel ----
                    trackSearchAreaLabel.setText("Unknown");
                    trackSearchAreaLabel.setName("trackSearchAreaLabel");
                    panel1.add(trackSearchAreaLabel, cc.xy(1, 1));

                    //---- messageCountLabel ----
                    messageCountLabel.setText("counts");
                    messageCountLabel.setName("messageCountLabel");
                    panel1.add(messageCountLabel, cc.xy(1, 3));
                }
                contentPanel.add(panel1, cc.xy(1, 1));

                //======== mapDataScrollPane ========
                {
                    mapDataScrollPane.setName("mapDataScrollPane");

                    //---- mapDataTable ----
                    mapDataTable.setEditable(false);
                    mapDataTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
                    mapDataTable.setColumnControlVisible(true);
                    mapDataTable.setHorizontalScrollEnabled(true);
                    mapDataTable.setColumnSelectionAllowed(true);
                    mapDataTable.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
                    mapDataTable.setPreferredScrollableViewportSize(new Dimension(200, 100));
                    mapDataTable.setName("mapDataTable");
                    mapDataScrollPane.setViewportView(mapDataTable);
                }
                contentPanel.add(mapDataScrollPane, cc.xy(1, 3));
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setBorder(Borders.BUTTON_BAR_GAP_BORDER);
                buttonBar.setName("buttonBar");
                buttonBar.setLayout(new FormLayout(
                        "$glue, $button",
                        "pref"));

                //---- closeButton ----
                closeButton.setText("Close");
                closeButton.setName("closeButton");
                closeButton.addActionListener(e -> closeButtonActionPerformed());
                buttonBar.add(closeButton, cc.xy(2, 1));
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel trackSearchAreaLabel;
    private JLabel messageCountLabel;
    private JXTable mapDataTable;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
