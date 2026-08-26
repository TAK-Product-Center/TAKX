package gov.takx.core.mapobject.gui.primarydialog.primarypanel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMapObjectController;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.commons.swing.documents.LimitedDocument;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.location.LocationField;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import org.jdesktop.swingx.JXTextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.List;

import static gov.takx.commons.swing.ImageUtils.FEATURE_ICON_HEIGHT;
import static gov.takx.commons.swing.ImageUtils.FEATURE_ICON_WIDTH;

/**
 * @since 1.14
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class MapObjectHeaderPanel extends AMapObjectPanel
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final List<String> propertyList = Arrays.asList(IMapObject.ICON, IMapObject.LOCATION, IMapObject.NAME, IMapObject.DESCRIPTION);

    private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor =
            UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, u -> setLocation());
    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor =
            UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, u -> setLocation());

    /**
     * Construct a new header panel with details for the given map object controller.
     *
     * @param controller a map object controller to use to derive map object details
     */
    public MapObjectHeaderPanel(@Nonnull IMapObjectController controller)
    {
        super(controller);

        initComponents();
        descriptionText.setDocument(new LimitedDocument(255));

        // Use disabled text to inform the user that the values are not editable.
        // We don't want to just disable the field since that prevents selecting and copying the text.
        descriptionText.setForeground(descriptionText.getDisabledTextColor());
    }

    @PostConstruct
    private void postConstruct()
    {
        setLocation();
        setIcon();
        setDescription();

        addProperties(propertyList);
    }

    /**
     * Set the text on the label next to the description field.
     *
     * @param text Text to display
     * @since 2.37
     */
    public void setDescriptionLabelText(String text)
    {
        descriptionLabel.setText(text);
    }

    /**
     * Set the text on the label next to the location field.
     *
     * @param text Text to display
     * @since 3.0.1
     */
    @InvokeOnEDT
    public void setLocationLabelText(String text)
    {
        locationField.setLocationLabelText(text);
    }

    @Override
    @InvokeOnEDT
    public void propertyChange(final PropertyChangeEvent propertyChangeEvent)
    {
        final String propertyName = propertyChangeEvent.getPropertyName();

        switch (propertyName)
        {
            case IMapObject.ICON -> setIcon();
            case IMapObject.LOCATION -> setLocation();
            case IMapObject.DESCRIPTION, IMapObject.NAME -> setDescription();
            default -> logger.warn("Unexpected property change: {}", propertyName);
        }

        super.propertyChange(propertyChangeEvent);
    }

    @Override
    public void releaseResources()
    {
        altitudeUnitsMonitor.dispose();
        coordinateSystemMonitor.dispose();

        super.releaseResources();
        locationField.releaseResources();
    }

    /**
     * Sets the icon for the map object.
     */
    @InvokeOnEDT
    private void setIcon()
    {
        iconLabel.setIcon(ImageUtils.getScaledIcon(getMapObject().getIconImage(), FEATURE_ICON_WIDTH, FEATURE_ICON_HEIGHT));
    }

    /**
     * Sets the location of the map object .
     */
    @InvokeOnEDT
    public void setLocation()
    {
        final ILocation loc = mapObject != null ? mapObject.getLocation() : null;
        locationField.setLocationValue(loc);
    }

    /**
     * Set the description text field.
     */
    @InvokeOnEDT
    public void setDescription()
    {
        descriptionText.setText(getMapObject().getDescription());
        descriptionText.setSelectionStart(0);
        descriptionText.setSelectionEnd(0);
        nameField.setText(getMapObject().getName());
    }

    private void locationFieldChanged()
    {
        if (mapObject != null && !mapObject.getLocation().sameAs(locationField.getLocationValue())) mapObject.setLocation(locationField.getLocationValue(), true);
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents
        iconLabel = new JLabel();
        var nameLabel = new JLabel();
        nameField = new JXTextField();
        descriptionLabel = new JLabel();
        var descriptionPane = new JScrollPane();
        descriptionText = new JTextPane();
        locationField = new LocationField();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "2*(default, $lcgap), default:grow",
                "2*(default, $lgap), fill:default"));

        //---- iconLabel ----
        iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
        iconLabel.setName("iconLabel");
        add(iconLabel, cc.xywh(1, 1, 1, 3));

        //---- nameLabel ----
        nameLabel.setText("Name:");
        nameLabel.setName("nameLabel");
        add(nameLabel, cc.xy(3, 1));

        //---- nameField ----
        nameField.setName("nameField");
        add(nameField, cc.xy(5, 1));

        //---- descriptionLabel ----
        descriptionLabel.setText("Description:");
        descriptionLabel.setName("descriptionLabel");
        add(descriptionLabel, cc.xy(3, 3));

        //======== descriptionPane ========
        {
            descriptionPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            descriptionPane.setName("descriptionPane");

            //---- descriptionText ----
            descriptionText.setOpaque(false);
            descriptionText.setEditable(false);
            descriptionText.setName("descriptionText");
            descriptionPane.setViewportView(descriptionText);
        }
        add(descriptionPane, cc.xy(5, 3));

        //---- locationField ----
        locationField.setName("locationField");
        locationField.addChangeListener(e -> locationFieldChanged());
        add(locationField, cc.xywh(1, 5, 5, 1));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables
    private JLabel iconLabel;
    private JXTextField nameField;
    private JLabel descriptionLabel;
    private JTextPane descriptionText;
    private LocationField locationField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables
}
