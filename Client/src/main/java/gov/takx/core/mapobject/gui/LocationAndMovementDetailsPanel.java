package gov.takx.core.mapobject.gui;

import com.atakmap.coremap.maps.coords.NorthReference;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.util.StandardAttributeNames;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.core.view.gui.drifter.EditDrifterConfigurationDialog;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nullable;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import java.awt.Color;
import java.util.List;

/**
 * A panel that displays and updates location, speed, and heading information for an {@link IMapObject}.
 * 
 * @since 5.8
 */
public class LocationAndMovementDetailsPanel extends JPanel implements IResourceCleaner
{
    private static final java.util.List<String> LISTENER_PROPERTIES = List.of(IMapObject.LOCATION, IMapObject.SPEED, IMapObject.HEADING, IMapObject.DRIFTER);
    private static final Color DRIFTER_ACTIVE_COLOR = Color.GREEN;
    private static final Color DRIFTER_PAUSED_COLOR = Color.YELLOW;

    private final IPreferenceMonitor<CoordinateSystem> coordinateMonitor = UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::updateValues);
    private final IPreferenceMonitor<SpeedUnits> speedMonitor = UserPreferenceEntry.DISPLAY_SPEED_UNITS.newMonitor(SpeedUnits.class, this::updateValues);
    private final IPreferenceMonitor<BearingUnits> bearingMonitor = UserPreferenceEntry.DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, this::updateValues);
    private final IPreferenceMonitor<NorthReference> northMonitor = UserPreferenceEntry.DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, this::updateValues);

    private final ListenerManager listenerManager = new ListenerManager();
    private IMapObject mapObject;
    private boolean empty;

    /**
     * Construct a new empty panel
     */
    public LocationAndMovementDetailsPanel()
    {
        initComponents();
    }

    /**
     * @return the source map object, or {@code null} if one has not yet been set
     */
    @Nullable
    public IMapObject getMapObject()
    {
        return mapObject;
    }

    /**
     * @param mapObject the source map object, or {@code null} to clear the value and hide all derived values
     */
    public void setMapObject(@Nullable IMapObject mapObject)
    {
        if (this.mapObject == mapObject) return;
        if (this.mapObject != null) listenerManager.unregister(this.mapObject);
        this.mapObject = mapObject;
        if (this.mapObject != null) listenerManager.addPropertyChangeListener(this.mapObject, LISTENER_PROPERTIES, this::updateValues);
        updateValues(null);
    }

    @Override
    public void releaseResources()
    {
        listenerManager.unregisterAll();
        speedMonitor.dispose();
        bearingMonitor.dispose();
        northMonitor.dispose();
        coordinateMonitor.dispose();
    }

    /**
     * @return {@code true} if there is no map object, or it does not contain a valid most recent communication time
     */
    public boolean isEmpty()
    {
        return empty;
    }

    @InvokeOnEDT
    private void updateValues(Object ignored)
    {
        var speedText = "";
        var headingText = "";
        var locationText = "";
        var drifterText = "";
        Color drifterForeground = null;

        if (mapObject != null)
        {
            var speed = mapObject.getSpeedInMps();
            if (Double.isNaN(speed)) speed = mapObject.getAttributeSet().getDoubleAttribute(StandardAttributeNames.EST_SPEED, Double.NaN);
            if (!Double.isNaN(speed)) speedText = speedMonitor.getCurrentValue().format(speed, 0);

            var heading = mapObject.getHeading();
            if (Double.isNaN(heading)) heading = mapObject.getAttributeSet().getDoubleAttribute(StandardAttributeNames.EST_COURSE, Double.NaN);
            if (!Double.isNaN(heading)) headingText = UserPreferenceEntry.DISPLAY_BEARING_UNIT.getValueAs(BearingUnits.class).format(mapObject.getLocation(), 0, heading);

            var location = mapObject.getLocation();
            if (location.isValid()) locationText = CoordinateSystem.getLocationString(location, UserPreferenceEntry.COORDINATE_SYSTEM.getValueAs(CoordinateSystem.class), false);

            var drifter = mapObject.getDrifterConfiguration();
            if (drifter != null)
            {
                if (drifter.paused())
                {
                    drifterText = "Paused";
                    drifterForeground = DRIFTER_PAUSED_COLOR;
                } else
                {
                    drifterForeground = DRIFTER_ACTIVE_COLOR;
                    drifterText = "Active";
                }
            } else
            {
                drifterText = "Inactive";
            }
        }

        empty = speedText.isEmpty() && headingText.isEmpty();
        locationField.setText(locationText);
        headingField.setText(headingText);
        speedField.setText(speedText);
        drifterField.setText(drifterText);
        drifterField.setForeground(drifterForeground);
    }

    private void editDrifterAction()
    {
        EditDrifterConfigurationDialog.show(mapObject);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var locationLabel = new JLabel();
        locationField = new JLabel();
        var speedLabel = new JLabel();
        speedField = new JLabel();
        var headingLabel = new JLabel();
        headingField = new JLabel();
        var drifterLabel = new JLabel();
        var drifterPanel = new JPanel();
        var drifterButtonPanel = new JPanel();
        var openParen = new JLabel();
        var editDrifterButton = new JButton();
        var closeParen = new JLabel();
        drifterField = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default, $lcgap, default:grow",
                "3*(default, $lgap), default"));

        //---- locationLabel ----
        locationLabel.setText("Location:");
        locationLabel.setName("locationLabel");
        add(locationLabel, cc.xy(1, 1));

        //---- locationField ----
        locationField.setName("locationField");
        add(locationField, cc.xy(3, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- speedLabel ----
        speedLabel.setText("Speed:");
        speedLabel.setName("speedLabel");
        add(speedLabel, cc.xy(1, 3));

        //---- speedField ----
        speedField.setName("speedField");
        add(speedField, cc.xy(3, 3, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- headingLabel ----
        headingLabel.setText("Heading:");
        headingLabel.setName("headingLabel");
        add(headingLabel, cc.xy(1, 5));

        //---- headingField ----
        headingField.setName("headingField");
        add(headingField, cc.xy(3, 5, CellConstraints.RIGHT, CellConstraints.DEFAULT));

        //---- drifterLabel ----
        drifterLabel.setText("Drifter:");
        drifterLabel.setName("drifterLabel");
        add(drifterLabel, cc.xy(1, 7));

        //======== drifterPanel ========
        {
            drifterPanel.setName("drifterPanel");
            drifterPanel.setLayout(new FormLayout(
                    "default, $ugap, default",
                    "default"));

            //======== drifterButtonPanel ========
            {
                drifterButtonPanel.setName("drifterButtonPanel");
                drifterButtonPanel.setLayout(new FormLayout(
                        "3*(default)",
                        "default"));

                //---- openParen ----
                openParen.setText("(");
                openParen.setName("openParen");
                drifterButtonPanel.add(openParen, cc.xy(1, 1, CellConstraints.RIGHT, CellConstraints.DEFAULT));

                //---- editDrifterButton ----
                editDrifterButton.setText("Edit");
                editDrifterButton.setBorderPainted(false);
                editDrifterButton.setBorder(new CompoundBorder(
                        new LineBorder(UIManager.getColor("Button.startBorderColor"), 1, true),
                        new EmptyBorder(1, 2, 1, 2)));
                editDrifterButton.setBackground(null);
                editDrifterButton.setName("editDrifterButton");
                editDrifterButton.addActionListener(e -> editDrifterAction());
                drifterButtonPanel.add(editDrifterButton, cc.xy(2, 1));

                //---- closeParen ----
                closeParen.setText(")");
                closeParen.setName("closeParen");
                drifterButtonPanel.add(closeParen, cc.xy(3, 1));
            }
            drifterPanel.add(drifterButtonPanel, cc.xy(1, 1));

            //---- drifterField ----
            drifterField.setName("drifterField");
            drifterPanel.add(drifterField, cc.xy(3, 1));
        }
        add(drifterPanel, cc.xy(3, 7, CellConstraints.RIGHT, CellConstraints.DEFAULT));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel locationField;
    private JLabel speedField;
    private JLabel headingField;
    private JLabel drifterField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
