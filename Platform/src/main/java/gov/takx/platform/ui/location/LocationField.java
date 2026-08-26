package gov.takx.platform.ui.location;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.jgoodies.forms.factories.Borders;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import gov.tak.api.annotation.IncubatingApi;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.ExceptionUtils;
import gov.takx.commons.swing.FormLayoutUtils;
import gov.takx.commons.swing.JSpinnerExt;
import gov.takx.commons.swing.JXTextFieldExt;
import gov.takx.commons.utils.RegexDocumentFilter;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.ResourceDirectory;
import gov.takx.platform.ui.UIResources;
import gov.takx.platform.ui.units.AltitudeUnitComboBox;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import org.jdesktop.swingx.JXButton;
import org.jdesktop.swingx.prompt.BuddySupport;
import org.jdesktop.swingx.prompt.PromptSupport;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.UIManager;
import javax.swing.border.CompoundBorder;
import javax.swing.border.MatteBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Field to enter and edit an {@link ILocation} object. Edits are made internally to a {@link GeoPoint}, not directly on the
 * given {@link ILocation} value.
 *
 * @since 5.7
 */
@SuppressWarnings("CdiInjectionPointsInspection")
@PseudoBean
@IncubatingApi(since = "5.7")
public class LocationField extends JPanel implements IResourceCleaner
{
    private static final Icon EDIT_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "edit.svg");
    private static final Icon CHECK_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "check_circle.svg");
    private static final Icon COPY_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "file-copy.svg");
    private static final Icon GOTO_ICON = UIResources.getStandardSizedIcon(ResourceDirectory.PLATFORM_ICONS, "go_to.svg");

    private final ChangeEvent changeEvent = new ChangeEvent(this);
    private final List<ChangeListener> changeListeners = new ArrayList<>();
    private final BiMap<CoordinateSystem, Component> coordinateSystemPanelMap = HashBiMap.create();

    @Inject
    private IGlobeManager globeManager;

    @Inject
    private INotificationService notificationService;

    private IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor;
    private IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor;

    private boolean editable = true;
    private boolean elevationEnabled = true;

    private GeoPoint location;
    private CoordinateSystem selectedCoordinateSystem; // The currently selected unit, or null if we're not in edit mode
    private AltitudeUnits selectedAltitudeUnits; // The currently selected unit, or null if we're not in edit mode
    private boolean updating; // Indicates the field is making internal updates and internal listeners should take no action

    /**
     * Construct a new component with a {@code null} location
     */
    public LocationField()
    {
        initComponents();

        coordinateSystemPanelMap.put(CoordinateSystem.MGRS, mgrsPanel);
        coordinateSystemPanelMap.put(CoordinateSystem.DECIMAL_DEGREES, degPanel);
        coordinateSystemPanelMap.put(CoordinateSystem.DM_DEGREES, degMinPanel);
        coordinateSystemPanelMap.put(CoordinateSystem.DMS_DEGREES, degMinSecPanel);
        tabPaneStateChanged();

        elevationUnitField.setSpinner(elevationField);

        copyButton.setIcon(COPY_ICON);
        gotoButton.setIcon(GOTO_ICON);
        editButton.setIcon(EDIT_ICON);

        locationTextField.addBuddy(copyButton, BuddySupport.Position.RIGHT);
        locationTextField.addBuddy(gotoButton, BuddySupport.Position.RIGHT);
        locationTextField.addBuddy(editButton, BuddySupport.Position.RIGHT);

        // The 4 different components of MGRS: Grid Zone Designator, 100,000m Square, Easting, Northing
        // The first two have slightly different restrictions than the easting / northing value
        gzdField.setDocumentFilter(new RegexDocumentFilter(3, true, true));
        sqrField.setDocumentFilter(new RegexDocumentFilter(2, true, false));
        easField.setDocumentFilter(new RegexDocumentFilter(5, false, true));
        norField.setDocumentFilter(new RegexDocumentFilter(5, false, true));
    }

    @PostConstruct
    private void postConstruct()
    {
        coordinateSystemMonitor = UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::updateCoordinateSystem);
        altitudeUnitsMonitor = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, this::updateAltitudeUnits);
        setDetailsPanelVisible(false);
    }

    /**
     * @return {@code true} if the location field is editable, otherwise {@code false}
     */
    public boolean isEditable()
    {
        return editable;
    }

    /**
     * @param editable {@code true} if the location field should be editable, otherwise {@code false}
     */
    public void setEditable(boolean editable)
    {
        if (this.editable != editable)
        {
            this.editable = editable;
            editButton.setVisible(editable);
            locationTextField.setEditable(editable);
        }
    }

    /**
     * @return {@code true} if the elevation field is visible, otherwise {@code false}
     */
    public boolean isElevationEnabled()
    {
        return elevationEnabled;
    }

    /**
     * @param elevationEnabled {@code true} if the elevation field should be visible, otherwise {@code false}
     */
    public void setElevationEnabled(boolean elevationEnabled)
    {
        if (this.elevationEnabled != elevationEnabled)
        {
            this.elevationEnabled = elevationEnabled;
            FormLayoutUtils.hideOrRestoreRow(elevationPanel, !elevationEnabled);
            updateFields(ChangeSource.OTHER);
        }
    }

    /**
     * @param visible {@code true} if the copy button should be visible, otherwise {@code false}
     */
    public void setCopyButtonVisible(boolean visible)
    {
        copyButton.setVisible(visible);
    }

    /**
     * @return {@code true} if the copy button is visible, otherwise {@code false}
     */
    public boolean isCopyButtonVisible()
    {
        return copyButton.isVisible();
    }

    /**
     * @param visible {@code true} if the goto button should be visible, otherwise {@code false}
     */
    public void setGotoButtonVisible(boolean visible)
    {
        gotoButton.setVisible(visible);
    }

    /**
     * @return {@code true} if the goto button is visible, otherwise {@code false}
     */
    public boolean isGotoButtonVisible()
    {
        return gotoButton.isVisible();
    }

    /**
     * @param visible {@code true} if the edit button should be visible, otherwise {@code false}
     */
    public void setEditButtonVisible(boolean visible)
    {
        editButton.setVisible(visible);
    }

    /**
     * @return {@code true} if the edit button is visible, otherwise {@code false}
     */
    public boolean isEditButtonVisible()
    {
        return editButton.isVisible();
    }

    /**
     * @param location the location object to edit
     */
    public void setLocationValue(@Nullable ILocation location)
    {
        if (!Objects.equals(this.location, location))
        {
            this.location = location != null ? new GeoPoint(location.getLatitude(), location.getLongitude(), location.getAltitude(), location.getAltitudeReference(), location.getCE(), location.getLE(), GeoPoint.Access.READ_WRITE) : null;
            updateFields(ChangeSource.OTHER);
            fireChangeEvent();
        }
    }

    /**
     * @return the current location value
     */
    @Nullable
    public ILocation getLocationValue()
    {
        return location;
    }

    /**
     * @return the label text for the location text field
     */
    @Nullable
    public String getLocationLabelText()
    {
        return locationLabel.getText();
    }

    /**
     * @param text the label text for the location text field
     */
    public void setLocationLabelText(@Nullable String text)
    {
        locationLabel.setText(text);
        var vis = text != null;
        if (locationLabel.isVisible() != vis)
        {
            locationLabel.setVisible(vis);
            FormLayoutUtils.hideOrRestoreColumn(locationLabel, !vis);
        }
    }

    /**
     * @param listener the listener to add
     */
    public void addChangeListener(@Nonnull ChangeListener listener)
    {
        changeListeners.add(listener);
    }

    /**
     * @param listener the listener to remove
     */
    @SuppressWarnings("unused")
    public void removeChangeListener(@Nonnull ChangeListener listener)
    {
        changeListeners.remove(listener);
    }

    @Override
    public void releaseResources()
    {
        coordinateSystemMonitor.dispose();
        altitudeUnitsMonitor.dispose();
    }

    @InvokeOnEDT
    private void updateCoordinateSystem(CoordinateSystem coordinateSystem)
    {
        if (!isDetailsPanelVisible())
        {
            selectedCoordinateSystem = coordinateSystem;
            updateTextField();
        }
    }

    @InvokeOnEDT
    private void updateAltitudeUnits(AltitudeUnits altitudeUnits)
    {
        if (!isDetailsPanelVisible())
        {
            selectedAltitudeUnits = altitudeUnits;
            updateTextField();
        }
    }

    private boolean isDetailsPanelVisible()
    {
        return editButton.isSelected();
    }

    private void setDetailsPanelVisible(boolean visible)
    {
        if (detailsPanel.isVisible() != visible)
        {
            detailsPanel.setVisible(visible);
            FormLayoutUtils.hideOrRestoreRow(detailsPanel, !visible);
            editButton.setSelected(visible);
            editButton.setIcon(visible ? CHECK_ICON : EDIT_ICON);
            editButton.setToolTipText(visible ? "Close Details" : "Edit Details");
            locationTextField.setEnabled(!visible);

            selectedCoordinateSystem = coordinateSystemMonitor.getCurrentValue();
            selectedAltitudeUnits = altitudeUnitsMonitor.getCurrentValue();

            if (visible)
            {
                var panel = coordinateSystemPanelMap.get(selectedCoordinateSystem); // This should only ever be null at design time
                tabPane.setSelectedComponent(panel);
                elevationUnitField.setSelectedItem(selectedAltitudeUnits);
            } else
            {
                updateTextField();
            }
        }
    }

    /**
     * Note that this method needs to ensure we exclude fields for whatever coordinate system we are currently working with,
     * so that we don't overwrite user data with values that may be slightly different (due to conversion to decimal
     * degrees).
     *
     * @param source the source of the changes, to determine which fields we should ignore the source
     */
    private void updateFields(@Nonnull ChangeSource source)
    {
        updating = true;
        var location = this.location;
        var altitudeUnits = selectedAltitudeUnits == null ? altitudeUnitsMonitor.getCurrentValue() : selectedAltitudeUnits;
        elevationUnitField.setSelectedItem(altitudeUnits);
        if (location != null)
        {
            if (source != ChangeSource.MGRS)
            {
                var mgrs = CoordinateSystem.getMGRSParts(location.getLatitude(), location.getLongitude());
                gzdField.setText(mgrs != null ? mgrs[0] : null);
                sqrField.setText(mgrs != null ? mgrs[1] : null);
                easField.setText(mgrs != null ? mgrs[2] : null);
                norField.setText(mgrs != null ? mgrs[3] : null);
            }

            if (source != ChangeSource.DEG)
            {
                dLatDegField.setValue(location.getLatitude());
                dLonDegField.setValue(location.getLongitude());
            }

            // This ensures that we retain the same integer value for minutes when converting from DM to DMS
            double[] dmLatParts;
            double[] dmLonParts;

            if (source != ChangeSource.DEG_MIN)
            {
                dmLatParts = CoordinateSystem.getDMDegreesParts(location.getLatitude());
                dmLonParts = CoordinateSystem.getDMDegreesParts(location.getLongitude());
                dmLatDegField.setValueInt((int) dmLatParts[0]);
                dmLatMinField.setValue(dmLatParts[1]);
                dmLonDegField.setValueInt((int) dmLonParts[0]);
                dmLonMinField.setValue(dmLonParts[1]);
            } else
            {
                dmLatParts = new double[]{dmLatDegField.getValueInt(), dmLatMinField.getValueDouble()};
                dmLonParts = new double[]{dmLonDegField.getValueInt(), dmLonMinField.getValueDouble()};
            }

            if (source != ChangeSource.DEG_MIN_SEC)
            {
                var dmsLatParts = CoordinateSystem.getDMSDegreesParts((int) dmLatParts[0], dmLatParts[1]);
                var dmsLonParts = CoordinateSystem.getDMSDegreesParts((int) dmLonParts[0], dmLonParts[1]);
                dmsLatDegField.setValueInt((int) dmsLatParts[0]);
                dmsLatMinField.setValueInt((int) dmsLatParts[1]);
                dmsLatSecField.setValue(dmsLatParts[2]);
                dmsLonDegField.setValueInt((int) dmsLonParts[0]);
                dmsLonMinField.setValueInt((int) dmsLonParts[1]);
                dmsLonSecField.setValue(dmsLonParts[2]);
            }

            if (source != ChangeSource.ELEV)
            {
                elevationUnitField.setValueMeters(location.getElevation());
            }

            updateTextField(); // We always update the text field so we can paste any coordinate system into it and convert it

        } else
        {
            locationTextField.setText(null);
        }
        updating = false;
    }

    private void updateTextField()
    {
        var coordinateSystem = selectedCoordinateSystem != null ? selectedCoordinateSystem : coordinateSystemMonitor.getCurrentValue();
        var altitudeUnits = selectedAltitudeUnits != null ? selectedAltitudeUnits : altitudeUnitsMonitor.getCurrentValue();
        var location = getLocationValueForEdit();
        var locationString = CoordinateSystem.getLocationString(location, coordinateSystem, elevationEnabled, altitudeUnits);
        locationTextField.setText(locationString);
    }

    private GeoPoint getLocationValueForEdit()
    {
        if (location == null) location = new GeoPoint(IGeoPoint.UNKNOWN, IGeoPoint.UNKNOWN, IGeoPoint.UNKNOWN, IGeoPoint.AltitudeReference.HAE, IGeoPoint.UNKNOWN, IGeoPoint.UNKNOWN, GeoPoint.Access.READ_WRITE);
        return location;
    }

    private void applyEdgeLimits(boolean atEdge, boolean isInt, JSpinnerExt spinner)
    {
        // Note there are issues with JSpinner if values and maximums are using different number types, so here we check to see
        //  which we should use
        if (atEdge) spinner.setValue(isInt ? 0 : 0d);
        Integer max = atEdge ? 0 : 60;
        if (isInt)
        {
            spinner.getNumberModel().setMaximum(max);
        } else
        {
            spinner.getNumberModel().setMaximum(max.doubleValue());
        }
    }

    private void gotoButtonAction()
    {
        var location = CoordinateSystem.computeLatLonFromString(locationTextField.getText());
        if (location != null)
        {
            //noinspection DataFlowIssue
            globeManager.getLastFocusedGlobe().goTo(location);
        }
    }

    private void copyButtonAction()
    {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(locationTextField.getText()), null);
        notificationService.show(Notification.builder("Copied location to clipboard").setType(Notification.Type.Info).setLogged(false).build(), 500);
    }

    private void editButtonAction()
    {
        setDetailsPanelVisible(!isDetailsPanelVisible());
    }

    private void mgrsChanged()
    {
        if (!updating)
        {
            var newLocation = CoordinateSystem.computeLatLonFromString(gzdField.getText() + sqrField.getText() + " " + easField.getText() + " " + norField.getText());
            if (newLocation != null)
            {
                var location = getLocationValueForEdit();
                location.setLatitude(newLocation.getLatitude());
                location.setLongitude(newLocation.getLongitude());
            }
            updateFields(ChangeSource.MGRS);
            fireChangeEvent();
        }
    }

    private void degChanged()
    {
        if (!updating)
        {
            var location = getLocationValueForEdit();
            location.setLatitude(dLatDegField.getValueDouble());
            location.setLongitude(dLonDegField.getValueDouble());
            updateFields(ChangeSource.DEG);
            fireChangeEvent();
        }
    }

    private void degMinChanged()
    {
        if (!updating)
        {
            applyEdgeLimits(Math.abs(dmLatDegField.getValueInt()) == 90, false, dmLatMinField);
            applyEdgeLimits(Math.abs(dmLonDegField.getValueInt()) == 180, false, dmLonMinField);
            var newLocation = CoordinateSystem.computeLatLonFromDM(dmLatDegField.getValueDouble(), dmLatMinField.getValueDouble(), dmLonDegField.getValueDouble(), dmLonMinField.getValueDouble());
            var location = getLocationValueForEdit();
            location.setLatitude(newLocation.getLatitude());
            location.setLongitude(newLocation.getLongitude());
            updateFields(ChangeSource.DEG_MIN);
            fireChangeEvent();
        }
    }

    private void degMinSecChanged()
    {
        if (!updating)
        {
            var latEdge = Math.abs(dmsLatDegField.getValueInt()) == 90;
            applyEdgeLimits(latEdge, true, dmsLatMinField);
            applyEdgeLimits(latEdge, false, dmsLatSecField);

            var lonEdge = Math.abs(dmsLonDegField.getValueInt()) == 180;
            applyEdgeLimits(lonEdge, true, dmsLonMinField);
            applyEdgeLimits(lonEdge, false, dmsLonSecField);

            var newLocation = CoordinateSystem.computeLatLonFromDMS(dmsLatDegField.getValueDouble(), dmsLatMinField.getValueDouble(), dmsLatSecField.getValueDouble(), dmsLonDegField.getValueDouble(), dmsLonMinField.getValueDouble(), dmsLonSecField.getValueDouble());
            var location = getLocationValueForEdit();
            location.setLatitude(newLocation.getLatitude());
            location.setLongitude(newLocation.getLongitude());
            updateFields(ChangeSource.DEG_MIN_SEC);
            fireChangeEvent();
        }
    }

    private void locationTextFocusLost()
    {
        if (!updating && !editButton.isSelected())
        {
            var newLocation = CoordinateSystem.computeLatLonFromString(locationTextField.getText());
            if (newLocation != null)
            {
                var location = getLocationValueForEdit();
                location.setLatitude(newLocation.getLatitude());
                location.setLongitude(newLocation.getLongitude());
                location.setElevation(newLocation.getElevation());
                updateFields(ChangeSource.TEXT);
            } else
            {
                updateFields(ChangeSource.OTHER);
            }
        }
    }

    private void tabPaneStateChanged()
    {
        if (isDetailsPanelVisible())
        {
            selectedCoordinateSystem = coordinateSystemPanelMap.inverse().get(tabPane.getSelectedComponent());
            updateTextField();
        }
    }

    private void elevationChanged()
    {
        if (!updating)
        {
            var location = getLocationValueForEdit();
            location.setElevation(elevationUnitField.getValueMeters());
            updateFields(ChangeSource.ELEV);
            fireChangeEvent();
        }
    }

    private void elevationUnitFieldChanged()
    {
        selectedAltitudeUnits = elevationUnitField.getTypedSelectedItem();
    }

    private void fireChangeEvent()
    {
        ExceptionUtils.safeDoOperation(changeListeners, listener -> listener.stateChanged(changeEvent));
    }

    private enum ChangeSource
    {
        MGRS, DEG, DEG_MIN, DEG_MIN_SEC, TEXT, ELEV, OTHER
    }

    @SuppressWarnings("RedundantExplicitVariableType")
    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var textFieldPanel = new JPanel();
        locationLabel = new JLabel();
        locationTextField = new JXTextFieldExt();
        detailsPanel = new JPanel();
        var detailsFormPanel = new JPanel();
        tabPane = new JTabbedPane();
        mgrsPanel = new JPanel();
        var label18 = new JLabel();
        gzdField = new JXTextFieldExt();
        sqrField = new JXTextFieldExt();
        easField = new JXTextFieldExt();
        norField = new JXTextFieldExt();
        degPanel = new JPanel();
        var degLatLabel = new JLabel();
        dLatDegField = new JSpinnerExt();
        var label4 = new JLabel();
        var degLonLabel = new JLabel();
        dLonDegField = new JSpinnerExt();
        var label15 = new JLabel();
        degMinPanel = new JPanel();
        var dmLatLabel = new JLabel();
        dmLatDegField = new JSpinnerExt();
        var label5 = new JLabel();
        dmLatMinField = new JSpinnerExt();
        var label7 = new JLabel();
        var dmLonDegLabel = new JLabel();
        dmLonDegField = new JSpinnerExt();
        var label6 = new JLabel();
        dmLonMinField = new JSpinnerExt();
        var label8 = new JLabel();
        degMinSecPanel = new JPanel();
        var dmLatLabel2 = new JLabel();
        dmsLatDegField = new JSpinnerExt();
        var label9 = new JLabel();
        dmsLatMinField = new JSpinnerExt();
        var label10 = new JLabel();
        dmsLatSecField = new JSpinnerExt();
        var label13 = new JLabel();
        var dmLonLabel2 = new JLabel();
        dmsLonDegField = new JSpinnerExt();
        var label11 = new JLabel();
        dmsLonMinField = new JSpinnerExt();
        var label12 = new JLabel();
        dmsLonSecField = new JSpinnerExt();
        var label14 = new JLabel();
        elevationPanel = new JPanel();
        var elevationLabel = new JLabel();
        elevationField = new JSpinnerExt();
        var label16 = new JLabel();
        elevationUnitField = new AltitudeUnitComboBox();
        copyButton = new JXButton();
        editButton = new JXButton();
        gotoButton = new JButton();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "default:grow",
                "default, $lgap, default"));

        //======== textFieldPanel ========
        {
            textFieldPanel.setName("textFieldPanel");
            textFieldPanel.setLayout(new FormLayout(
                    "default, $lcgap, default:grow",
                    "default"));

            //---- locationLabel ----
            locationLabel.setText("Location:");
            locationLabel.setName("locationLabel");
            textFieldPanel.add(locationLabel, cc.xy(1, 1));

            //---- locationTextField ----
            locationTextField.setPrompt("Enter Location Here");
            locationTextField.setFocusBehavior(PromptSupport.FocusBehavior.SHOW_PROMPT);
            locationTextField.setForeground(UIManager.getColor("Label.disabledForeground"));
            locationTextField.setName("locationTextField");
            locationTextField.addFocusListener(new FocusAdapter()
            {
                @Override
                public void focusLost(FocusEvent e)
                {
                    locationTextFocusLost();
                }
            });
            textFieldPanel.add(locationTextField, cc.xy(3, 1));
        }
        add(textFieldPanel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

        //======== detailsPanel ========
        {
            detailsPanel.setBorder(new CompoundBorder(
                    new TitledBorder(new MatteBorder(0, 1, 1, 1, UIManager.getColor("Component.borderColor")), "", TitledBorder.LEADING, TitledBorder.DEFAULT_POSITION, null, Color.white),
                    Borders.createEmptyBorder("0dlu, 4dlu, 4dlu, 4dlu")));
            detailsPanel.setName("detailsPanel");
            detailsPanel.setLayout(new FormLayout(
                    "default:grow",
                    "default"));

            //======== detailsFormPanel ========
            {
                detailsFormPanel.setName("detailsFormPanel");
                detailsFormPanel.setLayout(new FormLayout(
                        "default:grow",
                        "default, $lgap, default"));

                //======== tabPane ========
                {
                    tabPane.setName("tabPane");
                    tabPane.addChangeListener(e -> tabPaneStateChanged());

                    //======== mgrsPanel ========
                    {
                        mgrsPanel.setBorder(Borders.createEmptyBorder("4dlu, 0dlu, 0dlu, 0dlu"));
                        mgrsPanel.setName("mgrsPanel");
                        mgrsPanel.setLayout(new FormLayout(
                                "40dlu, $lcgap, 20dlu, 3*($lcgap, default)",
                                "fill:default, $lgap, default"));
                        ((FormLayout) mgrsPanel.getLayout()).setColumnGroups(new int[][]{{3, 5, 7, 9}});

                        //---- label18 ----
                        label18.setText("Grid:");
                        label18.setName("label18");
                        mgrsPanel.add(label18, cc.xy(1, 1));

                        //---- gzdField ----
                        gzdField.setName("gzdField");
                        gzdField.addFocusListener(new FocusAdapter()
                        {
                            @Override
                            public void focusLost(FocusEvent e)
                            {
                                mgrsChanged();
                            }
                        });
                        mgrsPanel.add(gzdField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- sqrField ----
                        sqrField.setName("sqrField");
                        sqrField.addFocusListener(new FocusAdapter()
                        {
                            @Override
                            public void focusLost(FocusEvent e)
                            {
                                mgrsChanged();
                            }
                        });
                        mgrsPanel.add(sqrField, cc.xy(5, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- easField ----
                        easField.setName("easField");
                        easField.addFocusListener(new FocusAdapter()
                        {
                            @Override
                            public void focusLost(FocusEvent e)
                            {
                                mgrsChanged();
                            }
                        });
                        mgrsPanel.add(easField, cc.xy(7, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- norField ----
                        norField.setName("norField");
                        norField.addFocusListener(new FocusAdapter()
                        {
                            @Override
                            public void focusLost(FocusEvent e)
                            {
                                mgrsChanged();
                            }
                        });
                        mgrsPanel.add(norField, cc.xy(9, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
                    }
                    tabPane.addTab("MGRS", mgrsPanel);

                    //======== degPanel ========
                    {
                        degPanel.setBorder(Borders.createEmptyBorder("4dlu, 0dlu, 0dlu, 0dlu"));
                        degPanel.setAlignmentX(0.0F);
                        degPanel.setName("degPanel");
                        degPanel.setLayout(new FormLayout(
                                "2*(40dlu, $lcgap), default",
                                "fill:default, $lgap, default"));

                        //---- degLatLabel ----
                        degLatLabel.setText("Latitude:");
                        degLatLabel.setName("degLatLabel");
                        degPanel.add(degLatLabel, cc.xy(1, 1));

                        //---- dLatDegField ----
                        dLatDegField.setModel(new SpinnerNumberModel(0.0, -90.0, 90.0, 1.0));
                        dLatDegField.setMaximumSize(new Dimension(100, 34));
                        dLatDegField.setMinimumSize(new Dimension(100, 0));
                        dLatDegField.setName("dLatDegField");
                        dLatDegField.addChangeListener(e -> degChanged());
                        degPanel.add(dLatDegField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label4 ----
                        label4.setText("\u00b0");
                        label4.setName("label4");
                        degPanel.add(label4, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.TOP));

                        //---- degLonLabel ----
                        degLonLabel.setText("Longitude:");
                        degLonLabel.setName("degLonLabel");
                        degPanel.add(degLonLabel, cc.xy(1, 3));

                        //---- dLonDegField ----
                        dLonDegField.setModel(new SpinnerNumberModel(0.0, -180.0, 180.0, 1.0));
                        dLonDegField.setMaximumSize(new Dimension(100, 34));
                        dLonDegField.setMinimumSize(new Dimension(100, 0));
                        dLonDegField.setName("dLonDegField");
                        dLonDegField.addChangeListener(e -> degChanged());
                        degPanel.add(dLonDegField, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label15 ----
                        label15.setText("\u00b0");
                        label15.setName("label15");
                        degPanel.add(label15, cc.xy(5, 3, CellConstraints.DEFAULT, CellConstraints.TOP));
                    }
                    tabPane.addTab("Deg", degPanel);

                    //======== degMinPanel ========
                    {
                        degMinPanel.setBorder(Borders.createEmptyBorder("4dlu, 0dlu, 0dlu, 0dlu"));
                        degMinPanel.setName("degMinPanel");
                        degMinPanel.setLayout(new FormLayout(
                                "2*(40dlu, $lcgap), 2*(default, $lcgap), default",
                                "fill:default, $lgap, default"));
                        ((FormLayout) degMinPanel.getLayout()).setColumnGroups(new int[][]{{3, 7}, {5, 9}});

                        //---- dmLatLabel ----
                        dmLatLabel.setText("Latitude:");
                        dmLatLabel.setName("dmLatLabel");
                        degMinPanel.add(dmLatLabel, cc.xy(1, 1));

                        //---- dmLatDegField ----
                        dmLatDegField.setModel(new SpinnerNumberModel(0, -90, 90, 1));
                        dmLatDegField.setMaximumSize(new Dimension(100, 34));
                        dmLatDegField.setMinimumSize(new Dimension(100, 0));
                        dmLatDegField.setName("dmLatDegField");
                        dmLatDegField.addChangeListener(e -> degMinChanged());
                        degMinPanel.add(dmLatDegField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label5 ----
                        label5.setText("\u00b0");
                        label5.setName("label5");
                        degMinPanel.add(label5, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.TOP));

                        //---- dmLatMinField ----
                        dmLatMinField.setModel(new SpinnerNumberModel(0.0, 0.0, 60.0, 1.0));
                        dmLatMinField.setMaximumSize(new Dimension(100, 34));
                        dmLatMinField.setMinimumSize(new Dimension(100, 0));
                        dmLatMinField.setName("dmLatMinField");
                        dmLatMinField.addChangeListener(e -> degMinChanged());
                        degMinPanel.add(dmLatMinField, cc.xy(7, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label7 ----
                        label7.setText("'");
                        label7.setName("label7");
                        degMinPanel.add(label7, cc.xy(9, 1, CellConstraints.LEFT, CellConstraints.TOP));

                        //---- dmLonDegLabel ----
                        dmLonDegLabel.setText("Longitude:");
                        dmLonDegLabel.setName("dmLonDegLabel");
                        degMinPanel.add(dmLonDegLabel, cc.xy(1, 3));

                        //---- dmLonDegField ----
                        dmLonDegField.setModel(new SpinnerNumberModel(0, -180, 180, 1));
                        dmLonDegField.setMaximumSize(new Dimension(100, 34));
                        dmLonDegField.setMinimumSize(new Dimension(100, 0));
                        dmLonDegField.setName("dmLonDegField");
                        dmLonDegField.addChangeListener(e -> degMinChanged());
                        degMinPanel.add(dmLonDegField, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label6 ----
                        label6.setText("\u00b0");
                        label6.setName("label6");
                        degMinPanel.add(label6, cc.xy(5, 3, CellConstraints.DEFAULT, CellConstraints.TOP));

                        //---- dmLonMinField ----
                        dmLonMinField.setModel(new SpinnerNumberModel(0.0, 0.0, 60.0, 1.0));
                        dmLonMinField.setMaximumSize(new Dimension(100, 34));
                        dmLonMinField.setMinimumSize(new Dimension(100, 0));
                        dmLonMinField.setName("dmLonMinField");
                        dmLonMinField.addChangeListener(e -> degMinChanged());
                        degMinPanel.add(dmLonMinField, cc.xy(7, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label8 ----
                        label8.setText("'");
                        label8.setName("label8");
                        degMinPanel.add(label8, cc.xy(9, 3, CellConstraints.LEFT, CellConstraints.TOP));
                    }
                    tabPane.addTab("Deg/Min", degMinPanel);

                    //======== degMinSecPanel ========
                    {
                        degMinSecPanel.setBorder(Borders.createEmptyBorder("4dlu, 0dlu, 0dlu, 0dlu"));
                        degMinSecPanel.setName("degMinSecPanel");
                        degMinSecPanel.setLayout(new FormLayout(
                                "2*(40dlu, $lcgap), 4*(default, $lcgap), default",
                                "fill:default:grow, $lgap, default"));
                        ((FormLayout) degMinSecPanel.getLayout()).setColumnGroups(new int[][]{{3, 7, 11}, {5, 9, 13}});

                        //---- dmLatLabel2 ----
                        dmLatLabel2.setText("Latitude:");
                        dmLatLabel2.setName("dmLatLabel2");
                        degMinSecPanel.add(dmLatLabel2, cc.xy(1, 1));

                        //---- dmsLatDegField ----
                        dmsLatDegField.setModel(new SpinnerNumberModel(0, -90, 90, 1));
                        dmsLatDegField.setName("dmsLatDegField");
                        dmsLatDegField.addChangeListener(e -> degMinSecChanged());
                        degMinSecPanel.add(dmsLatDegField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label9 ----
                        label9.setText("\u00b0");
                        label9.setName("label9");
                        degMinSecPanel.add(label9, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.TOP));

                        //---- dmsLatMinField ----
                        dmsLatMinField.setModel(new SpinnerNumberModel(0, 0, 60, 1));
                        dmsLatMinField.setName("dmsLatMinField");
                        dmsLatMinField.addChangeListener(e -> degMinSecChanged());
                        degMinSecPanel.add(dmsLatMinField, cc.xy(7, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label10 ----
                        label10.setText("'");
                        label10.setName("label10");
                        degMinSecPanel.add(label10, cc.xy(9, 1, CellConstraints.LEFT, CellConstraints.TOP));

                        //---- dmsLatSecField ----
                        dmsLatSecField.setModel(new SpinnerNumberModel(0.0, 0.0, 60.0, 1.0));
                        dmsLatSecField.setName("dmsLatSecField");
                        dmsLatSecField.addChangeListener(e -> degMinSecChanged());
                        degMinSecPanel.add(dmsLatSecField, cc.xy(11, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label13 ----
                        label13.setText("\"");
                        label13.setName("label13");
                        degMinSecPanel.add(label13, cc.xy(13, 1, CellConstraints.LEFT, CellConstraints.TOP));

                        //---- dmLonLabel2 ----
                        dmLonLabel2.setText("Longitude:");
                        dmLonLabel2.setName("dmLonLabel2");
                        degMinSecPanel.add(dmLonLabel2, cc.xy(1, 3));

                        //---- dmsLonDegField ----
                        dmsLonDegField.setModel(new SpinnerNumberModel(0, -180, 180, 1));
                        dmsLonDegField.setName("dmsLonDegField");
                        dmsLonDegField.addChangeListener(e -> degMinSecChanged());
                        degMinSecPanel.add(dmsLonDegField, cc.xy(3, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label11 ----
                        label11.setText("\u00b0");
                        label11.setName("label11");
                        degMinSecPanel.add(label11, cc.xy(5, 3, CellConstraints.DEFAULT, CellConstraints.TOP));

                        //---- dmsLonMinField ----
                        dmsLonMinField.setModel(new SpinnerNumberModel(0, 0, 60, 1));
                        dmsLonMinField.setName("dmsLonMinField");
                        dmsLonMinField.addChangeListener(e -> degMinSecChanged());
                        degMinSecPanel.add(dmsLonMinField, cc.xy(7, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label12 ----
                        label12.setText("'");
                        label12.setName("label12");
                        degMinSecPanel.add(label12, cc.xy(9, 3, CellConstraints.LEFT, CellConstraints.TOP));

                        //---- dmsLonSecField ----
                        dmsLonSecField.setModel(new SpinnerNumberModel(0.0, 0.0, 60.0, 1.0));
                        dmsLonSecField.setName("dmsLonSecField");
                        dmsLonSecField.addChangeListener(e -> degMinSecChanged());
                        degMinSecPanel.add(dmsLonSecField, cc.xy(11, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

                        //---- label14 ----
                        label14.setText("\"");
                        label14.setName("label14");
                        degMinSecPanel.add(label14, cc.xy(13, 3, CellConstraints.LEFT, CellConstraints.TOP));
                    }
                    tabPane.addTab("Deg/Min/Sec", degMinSecPanel);
                }
                detailsFormPanel.add(tabPane, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                //======== elevationPanel ========
                {
                    elevationPanel.setName("elevationPanel");
                    elevationPanel.setLayout(new FormLayout(
                            "40dlu, $lcgap, 50dlu, $lcgap, default, $lcgap, default:grow",
                            "default"));

                    //---- elevationLabel ----
                    elevationLabel.setText("Elevation:");
                    elevationLabel.setName("elevationLabel");
                    elevationPanel.add(elevationLabel, cc.xy(1, 1));

                    //---- elevationField ----
                    elevationField.setModel(new SpinnerNumberModel(0.0, null, null, 1.0));
                    elevationField.setName("elevationField");
                    elevationField.addChangeListener(e -> elevationChanged());
                    elevationPanel.add(elevationField, cc.xy(3, 1, CellConstraints.FILL, CellConstraints.DEFAULT));

                    //---- label16 ----
                    label16.setText("\u00b0");
                    label16.setVisible(false);
                    label16.setName("label16");
                    elevationPanel.add(label16, cc.xy(5, 1, CellConstraints.DEFAULT, CellConstraints.TOP));

                    //---- elevationUnitField ----
                    elevationUnitField.setName("elevationUnitField");
                    elevationUnitField.addItemListener(e -> elevationUnitFieldChanged());
                    elevationPanel.add(elevationUnitField, cc.xy(7, 1, CellConstraints.LEFT, CellConstraints.DEFAULT));
                }
                detailsFormPanel.add(elevationPanel, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.DEFAULT));
            }
            detailsPanel.add(detailsFormPanel, cc.xy(1, 1, CellConstraints.FILL, CellConstraints.DEFAULT));
        }
        add(detailsPanel, cc.xy(1, 3, CellConstraints.FILL, CellConstraints.DEFAULT));

        //---- copyButton ----
        copyButton.setBorderPainted(false);
        copyButton.setOpaque(false);
        copyButton.setBackground(null);
        copyButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        copyButton.setToolTipText("Copy to Clipboard");
        copyButton.setContentAreaFilled(false);
        copyButton.setName("copyButton");
        copyButton.addActionListener(e -> copyButtonAction());

        //---- editButton ----
        editButton.setBorderPainted(false);
        editButton.setOpaque(false);
        editButton.setBackground(null);
        editButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        editButton.setToolTipText("Edit Location");
        editButton.setContentAreaFilled(false);
        editButton.setName("editButton");
        editButton.addActionListener(e -> editButtonAction());

        //---- gotoButton ----
        gotoButton.setToolTipText("Go To Location");
        gotoButton.setBorderPainted(false);
        gotoButton.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        gotoButton.setOpaque(false);
        gotoButton.setBackground(null);
        gotoButton.setContentAreaFilled(false);
        gotoButton.setName("gotoButton");
        gotoButton.addActionListener(e -> gotoButtonAction());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel locationLabel;
    private JXTextFieldExt locationTextField;
    private JPanel detailsPanel;
    private JTabbedPane tabPane;
    private JPanel mgrsPanel;
    private JXTextFieldExt gzdField;
    private JXTextFieldExt sqrField;
    private JXTextFieldExt easField;
    private JXTextFieldExt norField;
    private JPanel degPanel;
    private JSpinnerExt dLatDegField;
    private JSpinnerExt dLonDegField;
    private JPanel degMinPanel;
    private JSpinnerExt dmLatDegField;
    private JSpinnerExt dmLatMinField;
    private JSpinnerExt dmLonDegField;
    private JSpinnerExt dmLonMinField;
    private JPanel degMinSecPanel;
    private JSpinnerExt dmsLatDegField;
    private JSpinnerExt dmsLatMinField;
    private JSpinnerExt dmsLatSecField;
    private JSpinnerExt dmsLonDegField;
    private JSpinnerExt dmsLonMinField;
    private JSpinnerExt dmsLonSecField;
    private JPanel elevationPanel;
    private JSpinnerExt elevationField;
    private AltitudeUnitComboBox elevationUnitField;
    private JXButton copyButton;
    private JXButton editButton;
    private JButton gotoButton;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
