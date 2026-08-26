package gov.takx.core.view.gui.maptools.elevationtools.viewshed;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.jgoodies.forms.layout.Sizes;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.swing.BoundedDoubleTextField;
import gov.takx.core.globe.GlobeClickService;
import gov.takx.core.globe.tak.layer.ViewshedLayer;
import gov.takx.core.view.gui.maptools.UnitOfMeasureComboBox;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.RangeUnits;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import java.awt.Dimension;

/**
 * Editing panel for {@link ViewshedLayer}.
 * @since 5.7
 */
@PseudoBean
public class ViewshedEditorPanel extends JPanel implements IResourceCleaner
{
    @Inject
    private GlobeClickService globeClickService;

    private IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor;
    private IPreferenceMonitor<RangeUnits> rangeUnitsMonitor;

    private ViewshedLayer viewshedLayer;

    public ViewshedEditorPanel()
    {
        initComponents();

        heightUnitComboBox.setComponentAccessors(markerHeightInput::getDoubleValue, markerHeightInput::setValue);
        markerHeightInput.setBounds(0d, null);
        markerHeightInput.setChangeListener(e -> {
            if (viewshedLayer != null) viewshedLayer.setViewHeightAboveMarker(heightUnitComboBox.toMeters());
        });

        radiusUnitComboBox.setComponentAccessors(radiusInput::getDoubleValue, radiusInput::setValue);
        radiusInput.setBounds(0.1d, null);
        radiusInput.setChangeListener(e -> {
            if (viewshedLayer != null) viewshedLayer.setRadius(radiusUnitComboBox.toMeters());
        });

        // The default width of a slider is too wide, so shrink it down so this panel isn't stupidly wide
        final var dimension = intensitySlider.getPreferredSize();
        final int pixelSize = Sizes.dluX(75).getPixelSize(this);
        intensitySlider.setPreferredSize(new Dimension(pixelSize, dimension.height));
        intensitySlider.addChangeListener(e -> {
            intensityPercent.setText(intensitySlider.getValue() + "%");
            if (viewshedLayer != null) viewshedLayer.setOpacity(intensitySlider.getValue());
        });

        adjustRadiusButton.addActionListener(l -> globeClickService.activate("Click on the globe to set the radius", loc -> {
            double distance = GeoCalculations.distance(viewshedLayer.getPointOfInterest(), loc);
            viewshedLayer.setRadius(distance);
            globeClickService.deactivate();
            updatePanel();
        }));
    }

    @PostConstruct
    private void postConstruct()
    {
        // These need to be in an @PC so that JFD can instantiate this class within IntelliJ
        altitudeUnitsMonitor = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, p -> updatePanel());
        rangeUnitsMonitor = UserPreferenceEntry.DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, p -> updatePanel());

        updatePanel();
    }

    @Override
    public void releaseResources()
    {
        altitudeUnitsMonitor.dispose();
        rangeUnitsMonitor.dispose();
    }

    /**
     * Change the Viewshed being operated on.
     *
     * @param viewshedLayer the new {@link ViewshedLayer} to edit
     */
    public void setViewshed(@Nullable ViewshedLayer viewshedLayer)
    {
        this.viewshedLayer = viewshedLayer;
        updatePanel();
    }

    @InvokeOnEDT
    protected void updatePanel()
    {
        final var altitudeUnits = altitudeUnitsMonitor.getCurrentValue();
        heightUnitComboBox.setSelectedItem(altitudeUnits);
        radiusUnitComboBox.setSelectedItem(rangeUnitsMonitor.getCurrentValue());

        if (viewshedLayer == null || viewshedLayer.getPointOfInterest() == null)
        {
            viewshedName.setText("None Selected");
            viewshedElevation.setText(" "); // Prevent the row from dropping to 0 height

            markerHeightInput.setEnabled(false);
            heightUnitComboBox.setEnabled(false);
            markerHeightInput.setValue(heightUnitComboBox.convertToUnit(ViewshedLayer.DEFAULT_VIEWSHED_HEIGHT));

            radiusInput.setEnabled(false);
            radiusUnitComboBox.setEnabled(false);
            adjustRadiusButton.setEnabled(false);
            radiusInput.setValue(radiusUnitComboBox.convertToUnit(ViewshedLayer.DEFAULT_VIEWSHED_RADIUS));

            intensitySlider.setEnabled(false);
            intensitySlider.setValue(ViewshedLayer.DEFAULT_VIEWSHED_INTENSITY);
        } else
        {

            markerHeightInput.setValue(heightUnitComboBox.convertToUnit(viewshedLayer.getViewHeightAboveMarker()));
            markerHeightInput.setEnabled(true);
            heightUnitComboBox.setEnabled(true);
            radiusInput.setValue(radiusUnitComboBox.convertToUnit(viewshedLayer.getRadius()));
            radiusInput.setEnabled(true);
            radiusUnitComboBox.setEnabled(true);

            intensitySlider.setValue(viewshedLayer.getOpacitySeen());
            intensitySlider.setEnabled(true);

            adjustRadiusButton.setEnabled(true);

            viewshedName.setText(viewshedLayer.getName());
            final String height = altitudeUnits.format(viewshedLayer.getMarkerLocation().getElevation(), 0);
            viewshedElevation.setText(height + " MSL");
        }
    }

    private void initComponents()
    {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        var namePanel = new JPanel();
        viewshedName = new JLabel();
        viewshedElevation = new JLabel();
        var detailsPanel = new JPanel();
        var markerHeightLabel = new JLabel();
        markerHeightInput = new BoundedDoubleTextField();
        heightUnitComboBox = new UnitOfMeasureComboBox();
        var radiusLabel = new JLabel();
        radiusInput = new BoundedDoubleTextField();
        radiusUnitComboBox = new UnitOfMeasureComboBox();
        adjustRadiusButton = new JButton();
        var intensityLabel = new JLabel();
        intensitySlider = new JSlider();
        intensityPercent = new JLabel();
        CellConstraints cc = new CellConstraints();

        //======== this ========
        setName("this");
        setLayout(new FormLayout(
                "4dlu, default:grow, 4dlu",
                "4dlu, default, $lgap, default, 4dlu"));

        //======== namePanel ========
        {
            namePanel.setPreferredSize(null);
            namePanel.setName("namePanel");
            namePanel.setLayout(new FormLayout(
                    "left:default:grow",
                    "default, $lgap, default"));

            //---- viewshedName ----
            viewshedName.setText("text");
            viewshedName.setName("viewshedName");
            namePanel.add(viewshedName, cc.xy(1, 1));

            //---- viewshedElevation ----
            viewshedElevation.setText("text");
            viewshedElevation.setName("viewshedElevation");
            namePanel.add(viewshedElevation, cc.xy(1, 3));
        }
        add(namePanel, cc.xy(2, 2));

        //======== detailsPanel ========
        {
            detailsPanel.setPreferredSize(null);
            detailsPanel.setName("detailsPanel");
            detailsPanel.setLayout(new FormLayout(
                    "right:default, $lcgap, default:grow, $lcgap, [default,30dlu]",
                    "3*(default, $lgap), default"));

            //---- markerHeightLabel ----
            markerHeightLabel.setText("Height Above Marker");
            markerHeightLabel.setName("markerHeightLabel");
            detailsPanel.add(markerHeightLabel, cc.xy(1, 1));

            //---- markerHeightInput ----
            markerHeightInput.setText("2");
            markerHeightInput.setName("markerHeightInput");
            detailsPanel.add(markerHeightInput, cc.xy(3, 1));

            //---- heightUnitComboBox ----
            heightUnitComboBox.setName("heightUnitComboBox");
            detailsPanel.add(heightUnitComboBox, cc.xy(5, 1));

            //---- radiusLabel ----
            radiusLabel.setText("Radius");
            radiusLabel.setName("radiusLabel");
            detailsPanel.add(radiusLabel, cc.xy(1, 3));

            //---- radiusInput ----
            radiusInput.setText("3000");
            radiusInput.setName("radiusInput");
            detailsPanel.add(radiusInput, cc.xy(3, 3));

            //---- radiusUnitComboBox ----
            radiusUnitComboBox.setName("radiusUnitComboBox");
            detailsPanel.add(radiusUnitComboBox, cc.xy(5, 3));

            //---- adjustRadiusButton ----
            adjustRadiusButton.setText("Adjust Radius");
            adjustRadiusButton.setToolTipText("Set the viewshed radius by clicking on the globe");
            adjustRadiusButton.setFocusable(false);
            adjustRadiusButton.setName("adjustRadiusButton");
            detailsPanel.add(adjustRadiusButton, cc.xy(3, 5));

            //---- intensityLabel ----
            intensityLabel.setText("Intensity");
            intensityLabel.setName("intensityLabel");
            detailsPanel.add(intensityLabel, cc.xy(1, 7));

            //---- intensitySlider ----
            intensitySlider.setSnapToTicks(true);
            intensitySlider.setName("intensitySlider");
            detailsPanel.add(intensitySlider, cc.xy(3, 7));

            //---- intensityPercent ----
            intensityPercent.setText("50%");
            intensityPercent.setName("intensityPercent");
            detailsPanel.add(intensityPercent, cc.xy(5, 7));
        }
        add(detailsPanel, cc.xy(2, 4));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JLabel viewshedName;
    private JLabel viewshedElevation;
    private BoundedDoubleTextField markerHeightInput;
    private UnitOfMeasureComboBox heightUnitComboBox;
    private BoundedDoubleTextField radiusInput;
    private UnitOfMeasureComboBox radiusUnitComboBox;
    private JButton adjustRadiusButton;
    private JSlider intensitySlider;
    private JLabel intensityPercent;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
