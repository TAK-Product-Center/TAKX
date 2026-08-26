package gov.takx.core.view.gui.maptools;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.map.elevation.ElevationManager;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.IBullseye;
import gov.takx.api.shapes.IMutableBullseye;
import gov.takx.api.shapes.IRingedShape;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.ui.location.ILocationInputListener;
import gov.takx.commons.swing.FormLayoutUtils;
import gov.takx.commons.swing.IPanelStateListener;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.ui.location.LegacyLocationInputPanel;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.beans.PropertyChangeEvent;
import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;

import static gov.takx.platform.ui.ResourceDirectory.TAK_ASSETS_PNG;

/**
 * Editor panel for the {@link IBullseye} shape.  Controls are provided to edit a bullseye to have specific locations,
 * radii, number of rings, and ring intervals.
 *
 * @since 5.5
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class BullseyeEditorPanel extends AShapePropertiesEditorPanel<IMutableBullseye>
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final Pattern RADIUS_REGEX = Pattern.compile("^\\s*[-+]?\\d*(\\.\\d+)?\\s*?$", Pattern.CASE_INSENSITIVE);
    private static final URL ICON_URL = UIResources.getUrl(TAK_ASSETS_PNG, "Bullseye_32x32.png");
    private static final String TO_CENTER = "To Center";
    private static final String FROM_CENTER = "From Center";
    private static final String DEGREES = "Degrees";
    private static final String MILS = "Mils";

    private static final String INVALID_RADIUS = "Bullseye radius must be between " + IBullseye.MIN_RADIUS + "m and " + IBullseye.MAX_RADIUS + "m";
    private static final String INVALID_RING_RADIUS = "Bullseye (Ring Radius * Ring Count) must be between " + IBullseye.MIN_RING_RADIUS + "m and " + IBullseye.MAX_RING_RADIUS + "m";

    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor = UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::coordinateSystemChanged);

    @Inject
    private INotificationService notificationService;

    /**
     * Private constructor for use by the other constructors.
     *
     * @param bullseye {@link IBullseye} to edit
     * @param directActionMode Pass {@code true} to immediately apply changes
     * @param panelListener Listener for "panel dirty" events, {@code null} if not being used
     */
    private BullseyeEditorPanel(@Nonnull IMutableBullseye bullseye, boolean directActionMode,
                                @Nullable IPanelStateListener panelListener)
    {
        super("Bullseye", bullseye, directActionMode, panelListener);
        initComponents();
        setUpComponents();
        setShape(bullseye);
    }

    /**
     * Constructor used when this panel is being used in the Preferences dialog. Implicitly not in "direct action" mode.
     *
     * @param bullseye {@link IBullseye} to edit
     * @param panelListener Listener for "panel dirty" events
     */
    public BullseyeEditorPanel(@Nonnull IMutableBullseye bullseye, @Nonnull IPanelStateListener panelListener)
    {
        this(bullseye, false, panelListener);
    }

    /**
     * Constructor used when this panel is in the right-side properties panel to edit an existing shape. Implicitly 
     * in "direct action" mode.
     * 
     * @param bullseye the {@link IBullseye} to display for editing
     */
    public BullseyeEditorPanel(@Nonnull IMutableBullseye bullseye)
    {
        this(bullseye, true, null);
    }

    @Override
    public URL getIcon()
    {
        return ICON_URL;
    }

    @Override
    public void releaseResources()
    {
        coordinateSystemMonitor.dispose();
    }

    private void setShape(final IMutableBullseye bullseye)
    {
        logger.debug("Setting panel shape {}", bullseye);
        updatingPanel = true; // Avoid cascading updates
        target = bullseye;

        nameField.setText(bullseye.getName());
        setCenterLocationButton(bullseye.getLocation());
        setDirectionButton(bullseye.isPointingEdgeToCenter());
        setBearingUnitsButton(bullseye.getBearingUnits());
        setNorthReferenceButton(bullseye.getNorthReference());
        radiusEdit.setText(SpanUtilities.formatType(Span.ENGLISH, target.getRadius(), Span.METER, true));
        radiusUnits.setSelectedItem(bullseye.getSpanUnits());
        ringRadiusEdit.setText(SpanUtilities.formatType(Span.ENGLISH, target.getRingRadius(), Span.METER, true));
        ringRadiusUnits.setSelectedItem(bullseye.getSpanUnits());
        showRingsCheckbox.setSelected(bullseye.isShowRangeRings());
        showRingsCheckboxChanged();
        ringCountSpinner.setValue(bullseye.getRingCount());
        remarksField.setText(bullseye.getRemarks());

        updatingPanel = false;
        bullseye.addPropertyChangeListener(IBullseye.PROPERTIES_TO_WATCH, this);
    }

    @InvokeOnEDT
    private void setUpComponents()
    {
        radiusInputPanel.setVisible(false);
        ringRadiusInputPanel.setVisible(false);
        locationInputPanel.setVisible(false);
        locationInputPanel.addListener(new ILocationInputListener()
        {
            @Override
            public void onLocationSelected(@Nonnull ILocation location, @Nonnull CoordinateSystem coordinateSystem, @Nullable URI markerIconUri, @Nullable String cotType)
            {
                updateProperty(() -> {
                    if (!location.hasElevation()) location.setElevation(ElevationManager.getElevation(location.getLatitude(), location.getLongitude(), null));
                    target.setLocation(location);
                    setCenterLocationButton(location);
                    locationInputPanel.setVisible(false);
                });
            }

            @Override
            public void onCancel()
            {
                locationInputPanel.setVisible(false);
            }
        });

        if (!directActionMode || !initialCreationOnly) FormLayoutUtils.hideRow(nameField);
    }

    @Override
    protected void updateGeometryControls()
    {
        // no-op, Bullseye handles its own units and measurements
    }

    private void coordinateSystemChanged(CoordinateSystem ignored)
    {
        if (target != null) setCenterLocationButton(target.getLocation());
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt)
    {
        final String propertyName = evt.getPropertyName();
        final Object value = evt.getNewValue();

        logger.debug("property change: ignore {} : {}={}", ignoreEvent, propertyName, value);

        if (ignoreEvent) return;
        updatingPanel = true;  // Avoid cascading events

        // Update our controls to match the updated property
        switch (propertyName)
        {
            case IMapObject.NAME -> nameField.setText((String) value);
            case ShapeProperties.LOCATION -> setCenterLocationButton((ILocation) value);
            case ShapeProperties.RADIUS ->
            {
                double radius = (double) value;
                setRadiusField(radius);
                Span existingUnits = (Span) radiusUnits.getSelectedItem();
                radiusEdit.setText(SpanUtilities.formatType(existingUnits != null ? existingUnits.getType() : Span.ENGLISH, radius, Span.METER, true));
            }
            case ShapeProperties.INTERVAL ->
            {
                double ringRadius = (double) value;
                setRingRadiusField(ringRadius);
                Span existingUnits = (Span) ringRadiusUnits.getSelectedItem();
                ringRadiusEdit.setText(SpanUtilities.formatType(existingUnits != null ? existingUnits.getType() : Span.ENGLISH, ringRadius, Span.METER, true));
            }
            case ShapeProperties.RING_COUNT -> ringCountSpinner.setValue(value);
            case IBullseye.NORTH_REFERENCE -> setNorthReferenceButton((NorthReference) value);
            case IBullseye.BEARING_UNITS -> setBearingUnitsButton((Angle) value);
            case IBullseye.DIRECTION -> setDirectionButton((boolean) value);
            case IBullseye.SHOW_RANGE_RINGS ->
            {
                showRingsCheckbox.setSelected((boolean) value);
                showRingsCheckboxChanged();
            }
            default -> logger.debug("Ignored {}", evt);
        }

        updatingPanel = false;
    }

    private void nameFieldChanged()
    {
        updateProperty(() -> {
            String name = nameField.getText().strip();
            if (StringUtils.isEmpty(name))
            {
                notificationService.show(Notification.builder("Name field is empty", Notification.Type.Warning).build());
                return;
            }
            target.setName(name);
        });
    }

    private void setCenterLocationButton(@Nonnull ILocation origLocation)
    {
        String locationString = CoordinateSystem.getLocationString(origLocation, coordinateSystemMonitor.getCurrentValue(), false) +
                " " + CoordinateSystem.getElevationString(origLocation.getElevation()) + "m MSL";
        centerButton.setText(locationString);
        locationInputPanel.setLocation(origLocation, coordinateSystemMonitor.getCurrentValue());
    }

    @InvokeOnEDT
    private void centerButtonClicked()
    {
        locationInputPanel.setLocation(target.getLocation(), coordinateSystemMonitor.getCurrentValue());
        locationInputPanel.setVisible(!locationInputPanel.isVisible());
    }

    private void setDirectionButton(boolean pointingEdgeToCenter)
    {
        directionButton.setText(pointingEdgeToCenter ? TO_CENTER : FROM_CENTER);
    }

    private void directionButtonClicked()
    {
        boolean nowPointEdgeToCenter = !directionButton.getText().equals(TO_CENTER);
        setDirectionButton(nowPointEdgeToCenter);
        updateProperty(() -> target.setPointingEdgeToCenter(nowPointEdgeToCenter));
    }

    private void setBearingUnitsButton(@Nonnull Angle newUnits)
    {
        bearingUnitsButton.setText(newUnits == Angle.DEGREE ? DEGREES : MILS);
    }

    private void bearingUnitsButtonClicked()
    {
        Angle newAngle = bearingUnitsButton.getText().equalsIgnoreCase(DEGREES) ? Angle.MIL : Angle.DEGREE;
        setBearingUnitsButton(newAngle);
        updateProperty(() -> target.setBearingUnits(newAngle));
    }

    private void setNorthReferenceButton(@Nonnull NorthReference newReference)
    {
        northReferenceButton.setText(newReference.getAbbrev() + "N");
    }

    private void northReferenceButtonClicked()
    {
        NorthReference oldReference = NorthReference.findFromAbbrev(northReferenceButton.getText().substring(0, 1));
        NorthReference newReference = NorthReference.fromOrdinal((oldReference.getValue() + 1) % NorthReference.values().length);
        setNorthReferenceButton(newReference);
        updateProperty(() -> target.setNorthReference(newReference));
    }

    @InvokeOnEDT
    private void radiusEditClicked()
    {
        radiusUnits.setSelectedItem(target.getSpanUnits());
        setRadiusField(target.getRadius());
        radiusInputPanel.setVisible(!radiusInputPanel.isVisible());
    }

    private void radiusUnitsItemChanged()
    {
        setRadiusField(target.getRadius());
        //noinspection DataFlowIssue - the combobox is not editable, so the units will not be null
        radiusEdit.setText(SpanUtilities.formatType(((Span) radiusUnits.getSelectedItem()).getType(), target.getRadius(), Span.METER, true));
    }

    private void setRadiusField(double radiusMeters)
    {
        double convertedUnits = SpanUtilities.convert(radiusMeters, Span.METER, (Span) radiusUnits.getSelectedItem());
        radiusField.setText(String.format("%.2f", convertedUnits));
    }

    @InvokeOnEDT
    private void applyRadiusClicked()
    {
        String radiusText = radiusField.getText().strip();
        if (!RADIUS_REGEX.matcher(radiusText).matches())
        {
            notificationService.show(Notification.builder("Invalid input for Bullseye radius", Notification.Type.Warning).build());
            return;
        }
        double radius = Double.parseDouble(radiusText);
        Span units = (Span) radiusUnits.getSelectedItem();

        if (IBullseye.isValidRadius(radius, units))
        {
            updateProperty(() -> {
                try
                {
                    //noinspection DataFlowIssue - the combobox is not editable, so the units will not be null
                    target.setRadius(radius, units);
                    target.setSpanUnits(units);
                } catch (IllegalArgumentException e)
                {
                    notificationService.show(Notification.builder(e.getMessage(), Notification.Type.Warning).build());
                }
            });
            //noinspection DataFlowIssue - the combobox is not editable, so the units will not be null
            radiusEdit.setText(SpanUtilities.formatType(units.getType(), radius, units, true));
            radiusInputPanel.setVisible(false);
            if (units != ringRadiusUnits.getSelectedItem()) ringRadiusUnits.setSelectedItem(units);
        } else
        {
            notificationService.show(Notification.builder(INVALID_RADIUS).build());
        }

    }

    private void showRingsCheckboxClicked()
    {
        updateProperty(() -> target.setShowRangeRings(!target.isShowRangeRings()));
        showRingsCheckboxChanged();
    }

    @InvokeOnEDT
    private void showRingsCheckboxChanged()
    {
        boolean enabled = showRingsCheckbox.isSelected();
        ringRadiusEdit.setEnabled(enabled);
        if (ringRadiusInputPanel.isVisible()) ringRadiusInputPanel.setVisible(false);
        ringCountSpinner.setEnabled(enabled);
    }

    @InvokeOnEDT
    private void ringRadiusEditClicked()
    {
        ringRadiusUnits.setSelectedItem(target.getSpanUnits());
        setRingRadiusField(target.getRingRadius());
        ringRadiusInputPanel.setVisible(!ringRadiusInputPanel.isVisible());
    }

    private void ringRadiusUnitsItemChanged()
    {
        setRingRadiusField(target.getRingRadius());
        //noinspection DataFlowIssue - the combobox is not editable, so the units will not be null
        ringRadiusEdit.setText(SpanUtilities.formatType(((Span) ringRadiusUnits.getSelectedItem()).getType(), target.getRingRadius(), Span.METER, true));
    }

    private void setRingRadiusField(double value)
    {
        double convertedUnits = SpanUtilities.convert(value, Span.METER, (Span) ringRadiusUnits.getSelectedItem());
        ringRadiusField.setText(String.format("%.2f", convertedUnits));
    }

    @InvokeOnEDT
    private void applyRingRadiusClicked()
    {
        String ringRadiusText = ringRadiusField.getText().strip();
        if (!RADIUS_REGEX.matcher(ringRadiusText).matches())
        {
            notificationService.show(Notification.builder("Invalid input for Bullseye ring radius", Notification.Type.Warning).build());
            return;
        }
        double ringRadius = Double.parseDouble(ringRadiusText);
        Span units = (Span) ringRadiusUnits.getSelectedItem();

        if (IBullseye.isValidRingRadius(ringRadius, units, (int) ringCountSpinner.getValue()))
        {
            updateProperty(() -> {
                try
                {
                    //noinspection DataFlowIssue - the combobox is not editable, so the units will not be null
                    target.setRingRadius(ringRadius, units);
                    target.setSpanUnits(units);
                } catch (IllegalArgumentException e)
                {
                    notificationService.show(Notification.builder(e.getMessage(), Notification.Type.Warning).build());
                }
            });
            //noinspection DataFlowIssue - the combobox is not editable, so the units will not be null
            ringRadiusEdit.setText(SpanUtilities.formatType(units.getType(), ringRadius, units, true));
            ringRadiusInputPanel.setVisible(false);
            if (units != radiusUnits.getSelectedItem()) radiusUnits.setSelectedItem(units);
        } else
        {
            notificationService.show(Notification.builder("Reduce ring count or ring radius (ring count * ring radius <= " + IBullseye.MAX_RING_RADIUS + "m)", Notification.Type.Warning).build());
        }

    }

    private void ringCountSpinnerChanged()
    {
        updateProperty(() -> {
            int ringCount = (int) ringCountSpinner.getValue();
            try
            {
                target.setRingCount(ringCount);
            } catch (IllegalArgumentException e)
            {
                notificationService.show(Notification.builder(e.getMessage(), Notification.Type.Warning).build());
            }

            int targetRingCount = target.getRingCount();
            if (targetRingCount != ringCount) ringCountSpinner.setValue(targetRingCount);
        });

    }

    private void remarksFieldChanged()
    {
        updateProperty(() -> target.setRemarks(remarksField.getText().strip()));
    }

    @Override
    protected void updateTargetProperties()
    {
        target.setName(nameField.getText().strip());
        target.setLocation(CoordinateSystem.computeLatLonFromString(centerButton.getText().strip()));
        target.setPointingEdgeToCenter(directionButton.getText().equals(TO_CENTER));
        target.setBearingUnits(bearingUnitsButton.getText().equals(DEGREES) ? Angle.DEGREE : Angle.MIL);
        target.setNorthReference(NorthReference.findFromAbbrev(northReferenceButton.getText().substring(0, 1)));
        //noinspection DataFlowIssue - the combobox is not editable, so the selected item will not be null
        target.setSpanUnits(((Span) radiusUnits.getSelectedItem()));
        target.setRadius(Double.parseDouble(radiusField.getText().strip()), ((Span) radiusUnits.getSelectedItem()));
        target.setShowRangeRings(showRingsCheckbox.isSelected());
        //noinspection DataFlowIssue - the combobox is not editable, so the selected item will not be null
        target.setRingRadius(Double.parseDouble(ringRadiusField.getText().strip()), ((Span) ringRadiusUnits.getSelectedItem()));
        target.setRingCount((int) ringCountSpinner.getValue());
        target.setRemarks(remarksField.getText().strip());
    }

    @Override
    public String validateContents()
    {
        String name = nameField.getText().strip();
        String radiusString = radiusField.getText().strip();
        String ringRadiusString = ringRadiusField.getText().strip();
        int ringCount = (int) ringCountSpinner.getValue();

        double ringRadius = -1;

        StringBuilder issues = new StringBuilder();
        if (StringUtils.isEmpty(name)) issues.append("Name is empty\n");
        if (StringUtils.isEmpty(radiusString))
        {
            issues.append("Radius field is empty\n");
        } else
        {
            try
            {
                double radius = Double.parseDouble(radiusString);
                Span selectedUnits = (Span) radiusUnits.getSelectedItem();
                if (!IBullseye.isValidRadius(radius, selectedUnits)) issues.append(INVALID_RADIUS).append("\n");
            } catch (NumberFormatException e)
            {
                issues.append("Radius is not a number\n");
            }
        }
        if (StringUtils.isEmpty(ringRadiusString))
        {
            issues.append("Ring Radius field is empty\n");
        } else
        {
            try
            {
                ringRadius = Double.parseDouble(ringRadiusString);
                Span selectedUnits = (Span) ringRadiusUnits.getSelectedItem();
                if (!IBullseye.isValidRingRadius(ringRadius, selectedUnits, ringCount)) issues.append(INVALID_RING_RADIUS).append("\n");
            } catch (NumberFormatException e)
            {
                issues.append("Ring radius is not a number\n");
            }
        }

        if (ringCount < IRingedShape.MIN_RING_COUNT)
        {
            issues.append("Ring count must be >=" + IRingedShape.MIN_RING_COUNT + "\n");
        } else if (ringCount > IRingedShape.MAX_RING_COUNT)
        {
            issues.append("Ring count must be <=" + IRingedShape.MAX_RING_COUNT + "\n");
        }

        if (ringRadius != -1 && ringCount * ringRadius > IBullseye.MAX_RING_RADIUS) issues.append("Ring radius too large for given number of rings\n");

        return issues.isEmpty() ? null : issues.toString();
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var nameLabel = new JLabel();
        nameField = new JTextField();
        var centerLabel = new JLabel();
        var panel1 = new JPanel();
        centerButton = new JButton();
        locationInputPanel = new LegacyLocationInputPanel();
        var directionLabel = new JLabel();
        var directionPanel = new JPanel();
        directionButton = new JButton();
        bearingUnitsButton = new JButton();
        northReferenceButton = new JButton();
        var radiusLabel = new JLabel();
        var panel2 = new JPanel();
        radiusEdit = new JButton();
        radiusInputPanel = new JPanel();
        radiusField = new JTextField();
        radiusUnits = new JComboBox<>(Span.values());
        var applyRadius = new JButton();
        var showRingsLabel = new JLabel();
        showRingsCheckbox = new JCheckBox();
        var ringRadiusLabel = new JLabel();
        var panel3 = new JPanel();
        ringRadiusEdit = new JButton();
        ringRadiusInputPanel = new JPanel();
        ringRadiusField = new JTextField();
        ringRadiusUnits = new JComboBox<>(Span.values());
        var applyRingRadius = new JButton();
        var ringCountLabel = new JLabel();
        ringCountSpinner = new JSpinner();
        var remarksLabel = new JLabel();
        remarksField = new JTextField();
        var authorLabel = new JLabel();
        authorField = new JTextField();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "0dlu, $lcgap, left:default, $lcgap, default, $lcgap, 0dlu",
                "0dlu, 9*($lgap, default), $lgap, 0dlu"));

        //---- nameLabel ----
        nameLabel.setText("Name:");
        nameLabel.setName("nameLabel");
        add(nameLabel, cc.xy(3, 3));

        //---- nameField ----
        nameField.setName("nameField");
        nameField.addActionListener(e -> nameFieldChanged());
        nameField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                nameFieldChanged();
            }
        });
        add(nameField, cc.xy(5, 3));

        //---- centerLabel ----
        centerLabel.setText("Center:");
        centerLabel.setName("centerLabel");
        add(centerLabel, cc.xy(3, 5));

        //======== panel1 ========
        {
            panel1.setName("panel1");
            panel1.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, min:grow"));

            //---- centerButton ----
            centerButton.setText("text");
            centerButton.setName("centerButton");
            centerButton.addActionListener(e -> centerButtonClicked());
            panel1.add(centerButton, cc.xy(1, 1));

            //---- locationInputPanel ----
            locationInputPanel.setVisible(false);
            locationInputPanel.setName("locationInputPanel");
            panel1.add(locationInputPanel, cc.xy(1, 3));
        }
        add(panel1, cc.xy(5, 5));

        //---- directionLabel ----
        directionLabel.setText("Direction:");
        directionLabel.setName("directionLabel");
        add(directionLabel, cc.xy(3, 7));

        //======== directionPanel ========
        {
            directionPanel.setName("directionPanel");
            directionPanel.setLayout(new FormLayout(
                    "2*(default:grow, $lcgap), default:grow",
                    "default"));

            //---- directionButton ----
            directionButton.setText("From Center");
            directionButton.setName("directionButton");
            directionButton.addActionListener(e -> directionButtonClicked());
            directionPanel.add(directionButton, cc.xy(1, 1));

            //---- bearingUnitsButton ----
            bearingUnitsButton.setText("Degrees");
            bearingUnitsButton.setName("bearingUnitsButton");
            bearingUnitsButton.addActionListener(e -> bearingUnitsButtonClicked());
            directionPanel.add(bearingUnitsButton, cc.xy(3, 1));

            //---- northReferenceButton ----
            northReferenceButton.setText("TN");
            northReferenceButton.setName("northReferenceButton");
            northReferenceButton.addActionListener(e -> northReferenceButtonClicked());
            directionPanel.add(northReferenceButton, cc.xy(5, 1));
        }
        add(directionPanel, cc.xy(5, 7));

        //---- radiusLabel ----
        radiusLabel.setText("Bullseye Radius:");
        radiusLabel.setName("radiusLabel");
        add(radiusLabel, cc.xy(3, 9));

        //======== panel2 ========
        {
            panel2.setName("panel2");
            panel2.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, default"));

            //---- radiusEdit ----
            radiusEdit.setText("text");
            radiusEdit.setName("radiusEdit");
            radiusEdit.addActionListener(e -> radiusEditClicked());
            panel2.add(radiusEdit, cc.xy(1, 1));

            //======== radiusInputPanel ========
            {
                radiusInputPanel.setName("radiusInputPanel");
                radiusInputPanel.setLayout(new FormLayout(
                        "default:grow, 2*($lcgap, default)",
                        "default"));

                //---- radiusField ----
                radiusField.setName("radiusField");
                radiusInputPanel.add(radiusField, cc.xy(1, 1));

                //---- radiusUnits ----
                radiusUnits.setName("radiusUnits");
                radiusUnits.addItemListener(e -> radiusUnitsItemChanged());
                radiusInputPanel.add(radiusUnits, cc.xy(3, 1));

                //---- applyRadius ----
                applyRadius.setText("Apply");
                applyRadius.setName("applyRadius");
                applyRadius.addActionListener(e -> applyRadiusClicked());
                radiusInputPanel.add(applyRadius, cc.xy(5, 1));
            }
            panel2.add(radiusInputPanel, cc.xy(1, 3));
        }
        add(panel2, cc.xy(5, 9));

        //---- showRingsLabel ----
        showRingsLabel.setText("Show Range Rings:");
        showRingsLabel.setName("showRingsLabel");
        add(showRingsLabel, cc.xy(3, 11));

        //---- showRingsCheckbox ----
        showRingsCheckbox.setName("showRingsCheckbox");
        showRingsCheckbox.addActionListener(e -> showRingsCheckboxClicked());
        add(showRingsCheckbox, cc.xy(5, 11));

        //---- ringRadiusLabel ----
        ringRadiusLabel.setText("Ring Radius:");
        ringRadiusLabel.setName("ringRadiusLabel");
        add(ringRadiusLabel, cc.xy(3, 13));

        //======== panel3 ========
        {
            panel3.setName("panel3");
            panel3.setLayout(new FormLayout(
                    "default:grow",
                    "default, $lgap, default"));

            //---- ringRadiusEdit ----
            ringRadiusEdit.setText("text");
            ringRadiusEdit.setName("ringRadiusEdit");
            ringRadiusEdit.addActionListener(e -> ringRadiusEditClicked());
            panel3.add(ringRadiusEdit, cc.xy(1, 1));

            //======== ringRadiusInputPanel ========
            {
                ringRadiusInputPanel.setName("ringRadiusInputPanel");
                ringRadiusInputPanel.setLayout(new FormLayout(
                        "default:grow, 2*($lcgap, default)",
                        "default"));

                //---- ringRadiusField ----
                ringRadiusField.setName("ringRadiusField");
                ringRadiusInputPanel.add(ringRadiusField, cc.xy(1, 1));

                //---- ringRadiusUnits ----
                ringRadiusUnits.setName("ringRadiusUnits");
                ringRadiusUnits.addItemListener(e -> ringRadiusUnitsItemChanged());
                ringRadiusInputPanel.add(ringRadiusUnits, cc.xy(3, 1));

                //---- applyRingRadius ----
                applyRingRadius.setText("Apply");
                applyRingRadius.setName("applyRingRadius");
                applyRingRadius.addActionListener(e -> applyRingRadiusClicked());
                ringRadiusInputPanel.add(applyRingRadius, cc.xy(5, 1));
            }
            panel3.add(ringRadiusInputPanel, cc.xy(1, 3));
        }
        add(panel3, cc.xy(5, 13));

        //---- ringCountLabel ----
        ringCountLabel.setText("Rings:");
        ringCountLabel.setName("ringCountLabel");
        add(ringCountLabel, cc.xy(3, 15));

        //---- ringCountSpinner ----
        ringCountSpinner.setName("ringCountSpinner");
        ringCountSpinner.addChangeListener(e -> ringCountSpinnerChanged());
        add(ringCountSpinner, cc.xy(5, 15));

        //---- remarksLabel ----
        remarksLabel.setText("Remarks:");
        remarksLabel.setName("remarksLabel");
        add(remarksLabel, cc.xy(3, 17));

        //---- remarksField ----
        remarksField.setName("remarksField");
        remarksField.addActionListener(e -> remarksFieldChanged());
        remarksField.addFocusListener(new FocusAdapter()
        {
            @Override
            public void focusLost(FocusEvent e)
            {
                remarksFieldChanged();
            }
        });
        add(remarksField, cc.xy(5, 17));

        //---- authorLabel ----
        authorLabel.setText("Original Author:");
        authorLabel.setName("authorLabel");
        add(authorLabel, cc.xy(3, 19));

        //---- authorField ----
        authorField.setEditable(false);
        authorField.setEnabled(false);
        authorField.setName("authorField");
        add(authorField, cc.xy(5, 19));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JTextField nameField;
    private JButton centerButton;
    private LegacyLocationInputPanel locationInputPanel;
    private JButton directionButton;
    private JButton bearingUnitsButton;
    private JButton northReferenceButton;
    private JButton radiusEdit;
    private JPanel radiusInputPanel;
    private JTextField radiusField;
    private JComboBox<Span> radiusUnits;
    private JCheckBox showRingsCheckbox;
    private JButton ringRadiusEdit;
    private JPanel ringRadiusInputPanel;
    private JTextField ringRadiusField;
    private JComboBox<Span> ringRadiusUnits;
    private JSpinner ringCountSpinner;
    private JTextField remarksField;
    private JTextField authorField;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
