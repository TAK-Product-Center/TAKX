package gov.takx.core.view.gui.navigation;

import com.atakmap.coremap.maps.coords.NorthReference;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IShapeArrowMapObject;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.shapes.ShapeProperties;
import gov.takx.api.ui.IResourceCleaner;
import gov.takx.commons.lang.ListenerManager;
import gov.takx.core.navigation.NavigationConstants;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.RangeUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.time.DurationFormatUtils;

import javax.swing.JLabel;

import java.util.List;

import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_BEARING_UNIT;
import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_NORTH_REFERENCE;
import static gov.takx.platform.preferences.UserPreferenceEntry.DISPLAY_RANGE_UNITS;

/**
 * A utility class that handles updating {@link JLabel} instances with data from a {@link IShapeArrowMapObject}. The
 * justification for this class is that displaying arrow details requires both listening for property change events and
 * monitoring for preference change events, all of which needs to happen on multiple fields. Duplicating this logic in
 * various UI components is tedious and wasteful. This class handles both the property change listener and the preference
 * monitor and then updates the given labels with the properly formatted, up-to-date values. It also handles releasing the
 * monitors and listeners when necessary.
 *
 * @since 5.7
 */
@PseudoBean
public class ArrowDetailsUpdater implements IResourceCleaner
{
    private final ListenerManager listenerManager = new ListenerManager();

    private final JLabel bearingLabel;
    private final JLabel rangeLabel;
    private final JLabel relativeBearingLabel;
    private final JLabel headingLabel;
    private final JLabel etaLabel;
    private final JLabel speedLabel;

    private IShapeArrowMapObject arrow;
    private IPreferenceMonitor<NorthReference> northReferenceMonitor;
    private IPreferenceMonitor<RangeUnits> rangeUnitsMonitor;
    private IPreferenceMonitor<BearingUnits> bearingUnitsMonitor;
    private IPreferenceMonitor<SpeedUnits> speedUnitsMonitor;

    /**
     * Construct a new instance to monitor a given arrow and update label texts.
     *
     * @param arrow The arrow to monitor
     * @param bearingLabel The Label to set formatted bearing value
     * @param rangeLabel The Label to hold formatted range value
     * @param relativeBearingLabel The Label to hold formatted relative bearing value
     * @param headingLabel The Label to hold formatted heading value
     * @param etaLabel The Label to hold formatted eta value
     * @param speedLabel The Label to hold formatted speed value
     */
    public ArrowDetailsUpdater(@Nullable IShapeArrowMapObject arrow, @Nullable JLabel bearingLabel, @Nullable JLabel rangeLabel, @Nullable JLabel relativeBearingLabel, @Nullable JLabel headingLabel, @Nullable JLabel etaLabel, @Nullable JLabel speedLabel)
    {
        this.bearingLabel = bearingLabel;
        this.rangeLabel = rangeLabel;
        this.relativeBearingLabel = relativeBearingLabel;
        this.headingLabel = headingLabel;
        this.etaLabel = etaLabel;
        this.speedLabel = speedLabel;
        this.arrow = arrow;
    }

    @PostConstruct
    private void postConstruct()
    {
        northReferenceMonitor = DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, value -> update());
        rangeUnitsMonitor = DISPLAY_RANGE_UNITS.newMonitor(RangeUnits.class, value -> update());
        bearingUnitsMonitor = DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, value -> update());
        speedUnitsMonitor = UserPreferenceEntry.DISPLAY_SPEED_UNITS.newMonitor(SpeedUnits.class, value -> update());
        if (arrow != null) setArrow(arrow);
    }

    /**
     * @param newArrow the arrow to monitor
     */
    public void setArrow(@Nullable IShapeArrowMapObject newArrow)
    {
        listenerManager.unregisterAll();
        arrow = newArrow;
        if (arrow != null)
        {
            listenerManager.addPropertyChangeListener(arrow, List.of(ShapeProperties.RANGE), e -> update());
            if (arrow.getStartAnchor() != null) listenerManager.addPropertyChangeListener(arrow.getStartAnchor(), List.of(IMapObject.SPEED, IMapObject.HEADING), e -> update());
        }
        update();
    }

    /**
     * @return the current arrow being monitored
     */
    @Nullable
    public IShapeArrowMapObject getArrow()
    {
        return arrow;
    }

    @Override
    public void releaseResources()
    {
        northReferenceMonitor.dispose();
        rangeUnitsMonitor.dispose();
        bearingUnitsMonitor.dispose();
        speedUnitsMonitor.dispose();
        setArrow(null);
    }

    @InvokeOnEDT
    protected void update()
    {
        var rangeText = "";
        var bearingText = "";
        var headingText = "";
        var speedText = "";
        var relativeText = "";
        var etaText = "";
        if (arrow != null && arrow.getStartAnchor() != null)
        {
            var range = arrow.getRange();
            var bearing = arrow.getBearing();
            var source = arrow.getStartAnchor();
            var heading = source.getHeading();
            var location = source.getLocation();
            var speed = source.getSpeedInMps();
            var relative = Double.isNaN(bearing) || Double.isNaN(heading) ? Double.NaN : (bearing - heading + 360) % 360;
            var eta = speed > 0 ? range / speed : Double.NaN;
            rangeText = Double.isNaN(range) ? "" : rangeUnitsMonitor.getCurrentValue().format(range, 1);
            bearingText = Double.isNaN(bearing) ? "" : bearingUnitsMonitor.getCurrentValue().format(location, range, bearing);
            relativeText = Double.isNaN(relative) ? "" : bearingUnitsMonitor.getCurrentValue().format(location, 0, relative);
            etaText = eta < 0 || Double.isNaN(eta) ? "" : DurationFormatUtils.formatDuration((long) (eta * 1000), NavigationConstants.ETA_FORMAT);
            speedText = Double.isNaN(speed) ? "" : speedUnitsMonitor.getCurrentValue().format(speed, 1);
            headingText = Double.isNaN(heading) ? "" : bearingUnitsMonitor.getCurrentValue().format(location, 0, heading);
        }

        if (rangeLabel != null) rangeLabel.setText(rangeText);
        if (bearingLabel != null) bearingLabel.setText(bearingText);
        if (relativeBearingLabel != null) relativeBearingLabel.setText(relativeText);
        if (etaLabel != null) etaLabel.setText(etaText);
        if (speedLabel != null) speedLabel.setText(speedText);
        if (headingLabel != null) headingLabel.setText(headingText);
    }
}
