package gov.takx.core.globe.widgets;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.maps.coords.NorthReference;
import gov.nasa.worldwind.event.InputHandler;
import gov.tak.api.commons.graphics.Font;
import gov.tak.api.commons.graphics.FontMetrics;
import gov.tak.api.commons.graphics.TextFormat;
import gov.tak.api.location.ILocationProviderManager;
import gov.tak.api.maps.coords.GeoPoint;
import gov.tak.api.maps.coords.GeoPointMetaData;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.location.providers.NoLocationProvider;
import gov.tak.platform.ui.MotionEvent;
import gov.tak.platform.widgets.TextWidget;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.event.location.LocationChanged;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.api.timer.ITimerService;
import gov.takx.commons.swing.EscapeKeyListener;
import gov.takx.core.globe.GlobeClickMouseListener;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.utils.units.AltitudeUnits;
import gov.takx.platform.utils.units.BearingUnits;
import gov.takx.platform.utils.units.SpeedUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.awt.event.KeyListener;
import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Provide a text widget to display the current self location information.  If there is no location provider configured,
 * then the text widget is clickable to allow the user to manually pick a location on the globe.
 * <p>
 * Information displayed:
 * <ul>
 *     <li>GPS Status</li>
 *     <li>Callsign</li>
 *     <li>Location as reported by the current {@link ILocationProviderManager}</li>
 *     <li>Altitude</li>
 *     <li>Heading</li>
 *     <li>Speed</li>
 *     <li>GPS Accuracy</li>
 * </ul>
 *
 * @since 3.4.1
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class SelfLocationWidget extends AWidget
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final Font FONT = new Font("Segoe UI", Font.Style.Normal, 14);
    private static final FontMetrics FONT_METRICS = new FontMetrics(FONT);
    private static final float SPACE_WIDTH = FONT_METRICS.measureTextWidth(" ");

    private static final DecimalFormat NO_DECIMAL_FORMATTER = new DecimalFormat("0");
    private static final String SET_LOCATION_TEXT = "NO GPS\nPlease click here to set\nyour location manually";
    private static final String SET_LOCATION_ACTIVE_TEXT = "NO GPS\n                                   \n ";

    private static final int RED_RGB = Color.RED.getRGB();
    private static final int CYAN_RGB = Color.CYAN.getRGB();
    private static final int[] NO_GPS_LINE_COLORS = {RED_RGB, CYAN_RGB, CYAN_RGB, CYAN_RGB, CYAN_RGB};
    private static final int[] GPS_LINE_COLORS = {CYAN_RGB, CYAN_RGB, CYAN_RGB, CYAN_RGB};
    private static final int[] NO_LOCATION_LINE_COLORS = {RED_RGB, RED_RGB, RED_RGB};
    private static final int[] SET_LOCATION_LINE_COLORS = {RED_RGB, RED_RGB, RED_RGB};

    @Inject
    private IGlobeManager globeManager;
    @Inject
    private ILocationProviderManager locationProviderManager;
    @Inject
    private ITimerService timerService;

    private final IPreferenceMonitor<String> callsignMonitor = PreferenceEntry.CALL_SIGN.newStringMonitor(v -> updateLocationText());
    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor =
            UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, v -> updateLocationText());
    private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor =
            UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, v -> updateLocationText());
    private final IPreferenceMonitor<SpeedUnits> speedUnitsMonitor =
            UserPreferenceEntry.DISPLAY_SPEED_UNITS.newMonitor(SpeedUnits.class, v -> updateLocationText());
    private final IPreferenceMonitor<NorthReference> northReferenceMonitor =
            UserPreferenceEntry.DISPLAY_NORTH_REFERENCE.newMonitor(NorthReference.class, v -> updateLocationText());
    private final IPreferenceMonitor<BearingUnits> bearingUnitsMonitor =
            UserPreferenceEntry.DISPLAY_BEARING_UNIT.newMonitor(BearingUnits.class, v -> updateLocationText());
    private final IPreferenceMonitor<Boolean> showWidgetMonitor = PreferenceEntry.SHOW_SELF_LOCATION_WIDGET.newBooleanMonitor(this::setVisible);

    private final IGlobeDelegate globe;
    private final TextWidget textWidget;
    private GeoPointMetaData lastLocationMetaData;

    /**
     * Construct overlay for the given globe using the given root layout.
     *
     * @param globe Globe on which to operate, used to allow user to click on the globe to set their location
     */
    public SelfLocationWidget(@Nonnull IGlobeDelegate globe)
    {
        this.globe = globe;

        textWidget = new TextWidget("", new TextFormat(FONT, 0));
        textWidget.setBackground(TextWidget.TRANSLUCENT_BLACK);
        textWidget.addOnClickListener(new WidgetClickListener());
        addChildWidget(textWidget);
        setPadding(4);
        setName(CoreWidgetNames.SELF_LOCATION);
        setVisible(showWidgetMonitor.getCurrentValue());
    }

    @PostConstruct
    private void postConstruct()
    {
        lastLocationMetaData = locationProviderManager.getCurrentLocation();
        updateLocationText();
        setVisible(true);
    }

    private void update(@Observes LocationChanged event)
    {
        lastLocationMetaData = event.newLocation();
        updateLocationText();
        setVisible(isVisible());
    }

    @Override
    public boolean setVisible(boolean visible)
    {
        if (super.setVisible(visible))
        {
            if (visible) updateLocationText();
            return true;
        }
        return false;
    }

    @SuppressWarnings("AccessToNonThreadSafeStaticField")
    @InvokeOnEDT
    private void updateLocationText()
    {
        if (lastLocationMetaData == null || !lastLocationMetaData.get().isValid())
        {
            showNoLocationText();
            return;
        }

        final var coordinateSystem = coordinateSystemMonitor.getCurrentValue();
        final var altitudeUnits = altitudeUnitsMonitor.getCurrentValue();
        final var speedUnits = speedUnitsMonitor.getCurrentValue();
        final var northReference = northReferenceMonitor.getCurrentValue();
        final Angle bearingUnits = bearingUnitsMonitor.getCurrentValue().getAngle();

        final AttributeSet metaData = lastLocationMetaData.getMetaData();
        final GeoPoint location = lastLocationMetaData.get();

        final String gpsSource = lastLocationMetaData.getGeopointSource();
        final String callsignString = "Callsign: " + callsignMonitor.getCurrentStringValue();
        final String locationString = CoordinateSystem.getLocationString(location, coordinateSystem, false);
        String altString = "--- " + altitudeUnits.getAbbreviation() + " MSL";
        String headingString = "--- " + bearingUnits.getAbbrev() + northReference.getAbbrev();
        String speedString = "";
        String accuracyString = "";

        if (location.isAltitudeValid()) altString = altitudeUnits.format(location.getElevation(), 1) + " MSL";

        final double heading = metaData.getDoubleAttribute(StandardAttributeNames.BEARING, Double.NaN);
        if (!Double.isNaN(heading))
        {
            headingString = NorthReference.format(heading, location, bearingUnits, northReference, 0);
        }

        final double speed = metaData.getDoubleAttribute(StandardAttributeNames.SPEED, Double.NaN);
        if (!Double.isNaN(speed)) speedString = speedUnits.format(speed, 0);

        final double accuracy = location.getCE();
        if (!Double.isNaN(accuracy)) accuracyString = "+/- " + NO_DECIMAL_FORMATTER.format(accuracy) + "m";

        final float locationWidth = FONT_METRICS.measureTextWidth(locationString);
        final float callsignWidth = FONT_METRICS.measureTextWidth(callsignString);
        final float altWidth = FONT_METRICS.measureTextWidth(altString);
        final float headingWidth = FONT_METRICS.measureTextWidth(headingString);
        final float speedWidth = FONT_METRICS.measureTextWidth(speedString);
        final float accuracyWidth = FONT_METRICS.measureTextWidth(accuracyString);

        final float maxWidth = Collections.max(List.of(callsignWidth, locationWidth, altWidth + headingWidth, speedWidth + accuracyWidth));
        final int pad1 = (int) Math.max(1, (maxWidth - altWidth - headingWidth) / SPACE_WIDTH);
        final int pad2 = (int) Math.max(1, (maxWidth - speedWidth - accuracyWidth) / SPACE_WIDTH);

        final var locationText = new StringBuilder(512);
        final int[] lineColors;

        if (GeoPointMetaData.UNKNOWN.equals(gpsSource))
        {
            locationText.append("NO GPS\n");
            lineColors = NO_GPS_LINE_COLORS;
        } else
        {
            lineColors = GPS_LINE_COLORS;
        }

        locationText.append("Callsign: ").append(callsignMonitor.getCurrentStringValue());
        locationText.append('\n');

        locationText.append(locationString);
        locationText.append('\n');

        locationText.append(altString);
        locationText.append(" ".repeat(pad1));
        locationText.append(headingString);
        locationText.append('\n');

        if (!speedString.isBlank())
        {
            locationText.append(speedString);
            locationText.append(" ".repeat(pad2));
        }

        if (!accuracyString.isBlank()) locationText.append(accuracyString);

        textWidget.setText(locationText);
        textWidget.setColors(lineColors);
    }

    private void showNoLocationText()
    {
        // First see if there is a location provider in place.  If not, then we need to give the user a means to
        // manually set their location.  If there is, then we just indicate we have no location in the text.

        if (noLocationProvider())
        {
            textWidget.setText(SET_LOCATION_TEXT);
            textWidget.setColors(SET_LOCATION_LINE_COLORS);
        } else
        {
            final var locationText = new StringBuilder(512);
            locationText.append("NO GPS\n");

            locationText.append("Callsign: ").append(callsignMonitor.getCurrentStringValue());
            locationText.append('\n');

            locationText.append("No location available");

            textWidget.setText(locationText);
            textWidget.setColors(NO_LOCATION_LINE_COLORS);
        }
    }

    /**
     * @return {@code true} if the NO LOCATION provider is active
     */
    private boolean noLocationProvider()
    {
        return locationProviderManager.getActiveLocationProvider().getUniqueKey().equals(NoLocationProvider.KEY);
    }

    @RemoveObserver
    @Override
    public void releaseResources()
    {
        callsignMonitor.dispose();
        coordinateSystemMonitor.dispose();
        altitudeUnitsMonitor.dispose();
        speedUnitsMonitor.dispose();
        northReferenceMonitor.dispose();
        bearingUnitsMonitor.dispose();
        showWidgetMonitor.dispose();
    }

    /**
     * Listener to detect mouse clicks on the text widget and invoke manual location setting or a "go to".
     */
    private class WidgetClickListener implements IMapWidget.OnClickListener
    {
        private final GlobeClickMouseListener clickListener = new GlobeClickMouseListener(this::setLocation);
        private final KeyListener escapeListener = new EscapeKeyListener(() -> {
            dispose();
            updateLocationText();
        });
        private final InputHandler inputHandler = globe.getWorldWindow().getInputHandler();
        private final AtomicBoolean firstClick = new AtomicBoolean(false);

        @Override
        public void onMapWidgetClick(IMapWidget widget, MotionEvent event)
        {
            final GeoPoint location = lastLocationMetaData != null ? lastLocationMetaData.get() : GeoPoint.UNKNOWN_POINT;

            if (location.isValid())
            {
                globe.goTo(location);
            } else if (noLocationProvider())
            {
                if (firstClick.compareAndSet(false, true))
                {
                    // Delay adding the globe listener for .5 seconds to avoid a double click setting the location
                    // under the text widget.
                    timerService.createTask(this::addGlobeListener, 500);

                    textWidget.setText(SET_LOCATION_ACTIVE_TEXT);
                    textWidget.setColors(SET_LOCATION_LINE_COLORS);

                    globeManager.showGlobeMessage("Click to set location; Press ESC to cancel");
                }
            }
        }

        /**
         * Install the click listener on the globe to collect the user's location.
         */
        private void addGlobeListener()
        {
            clickListener.setArmed(true);
            inputHandler.addMouseListener(clickListener);
            inputHandler.addKeyListener(escapeListener);
        }

        /**
         * Callback when user clicks on the globe.
         */
        private void setLocation(ILocation location)
        {
            logger.debug("clicked location: {}", location);
            locationProviderManager.setFixedLocation(new GeoPoint(location.getLatitude(), location.getLongitude(), location.getElevation()));
            dispose();
        }

        private void dispose()
        {
            clickListener.setArmed(false);
            inputHandler.removeMouseListener(clickListener);
            inputHandler.removeKeyListener(escapeListener);
            globeManager.hideGlobeMessage();
            firstClick.set(false);
        }
    }
}
