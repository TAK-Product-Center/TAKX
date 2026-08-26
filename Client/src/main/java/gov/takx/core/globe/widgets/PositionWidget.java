package gov.takx.core.globe.widgets;

import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.map.elevation.ElevationManager;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.PositionEvent;
import gov.nasa.worldwind.event.PositionListener;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.view.BasicView;
import gov.tak.api.commons.graphics.Font;
import gov.tak.api.commons.graphics.TextFormat;
import gov.tak.api.engine.map.MapRenderer;
import gov.tak.platform.util.LimitingThread;
import gov.tak.platform.widgets.TextWidget;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.RemoveObserver;
import gov.takx.api.coordinatesystem.CoordinateSystem;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.globe.AWidget;
import gov.takx.api.globe.CoreWidgetNames;
import gov.takx.api.globe.IGlobeDelegate;
import gov.takx.api.globe.IPositionOverlayOverride;
import gov.takx.api.playback.IPlaybackManager;
import gov.takx.api.playback.PlaybackLiveEvent;
import gov.takx.api.preferences.IPreferenceMonitor;
import gov.takx.commons.utils.StringUtils;
import gov.takx.platform.globe.GlobeTextUtils;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import gov.takx.platform.ui.FontUtils;
import gov.takx.platform.utils.WWUtils;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import java.awt.Color;
import java.text.DecimalFormat;

/**
 * Display view and position information as a text widget at the bottom of the globe viewport.
 * <p>
 * Information displayed:
 * <ul>
 * <li>Eye altitude</li>
 * <li>mouse (or view center) position</li>
 * <li>view heading</li>
 * <li>eye altitude</li>
 * <li>playback indicator</li>
 * </ul>
 * If playback is active (not in "live" mode), then the text color of the widget is changed.
 *
 * @since 5.2
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class PositionWidget extends AWidget implements PositionListener, MapRenderer.OnCameraChangedListener
{
    private static final Font FONT = new Font(GlobeTextUtils.DEFAULT_MONOSPACE_GLOBE_FONT.getName(), Font.Style.Normal, FontUtils.DEFAULT_FONT_SIZE);
    private static final DecimalFormat HEADING_FORMATTER = new DecimalFormat("##0.#°");

    private static final String PLAYBACK = "PLAYBACK";
    private static final String OFF_GLOBE = "Off Globe";
    private static final String MISSING_ELEVATION_INDICATOR = "N/A";

    private final IPreferenceMonitor<CoordinateSystem> coordinateSystemMonitor = UserPreferenceEntry.COORDINATE_SYSTEM.newMonitor(CoordinateSystem.class, this::coordinateSystemChanged);
    private final IPreferenceMonitor<AltitudeUnits> altitudeUnitsMonitor = UserPreferenceEntry.DISPLAY_ALTITUDE_UNITS.newMonitor(AltitudeUnits.class, this::altitudeUnitsChanged);
    private final IPreferenceMonitor<Boolean> showMousePosition = PreferenceEntry.GLOBE_POSITION_OVERLAY_TRACKS_MOUSE.newMonitor(Boolean.class, this::trackMouseChanged);

    private CoordinateSystem coordinateSystem = coordinateSystemMonitor.getCurrentValue();
    private AltitudeUnits altitudeUnits = altitudeUnitsMonitor.getCurrentValue();
    private volatile boolean playbackActive; // Are we in "playback" vs. live mode?

    private final StringBuilder stringBuilder = new StringBuilder();

    // Used to move elevation queries off of the main thread
    private final LimitingThread fetchElevationThread = new LimitingThread("PositionWidgetElevationFetcher", this::fetchElevationAndUpdateLocationInBackground);
    private volatile Position queryPosition;

    private final IGlobeDelegate globe;
    private final WorldWindow wwd;
    private final TextWidget textWidget;
    private String positionDisplay = OFF_GLOBE;
    private String altDisplay = "";
    private volatile String headingDisplay = "";

    private double lastLatitude = Double.NaN;
    private double lastLongitude = Double.NaN;
    private double lastAltitude = Double.NaN;
    private String lastElevationSource = GeoPointMetaData.UNKNOWN;
    private double lastEyeAltitude;
    private double lastHeading;

    @Inject
    protected IPlaybackManager playbackManager;

    /**
     * Construct widget for the given globe using the given root layout.
     *
     * @param globe Globe on which listeners are registered.
     */
    public PositionWidget(@Nonnull IGlobeDelegate globe)
    {
        this.globe = globe;

        wwd = globe.getWorldWindow();

        textWidget = new TextWidget("", new TextFormat(FONT, 0));
        textWidget.setBackground(TextWidget.TRANSLUCENT_BLACK);
        textWidget.setColor(Color.white.getRGB());
        textWidget.setPadding(3);

        setTouchable(false);
        addChildWidget(textWidget);
        setName(CoreWidgetNames.POSITION_OVERLAY);
    }

    @PostConstruct
    private void postConstruct()
    {
        playbackActive = !playbackManager.isLive(); // Start off in the correct state

        wwd.addPositionListener(this);
        globe.getPortableMapRenderer().addOnCameraChangedListener(this);
        setVisible(true);
    }

    @RemoveObserver
    @Override
    public void releaseResources()
    {
        wwd.removePositionListener(this);
        fetchElevationThread.dispose();
        globe.getPortableMapRenderer().removeOnCameraChangedListener(this);
        coordinateSystemMonitor.dispose();
        altitudeUnitsMonitor.dispose();
    }

    /**
     * On any given playback "live" event, update our display based on the new state.
     *
     * @param event The event fired
     */
    private void onPlaybackLiveEvent(@Observes PlaybackLiveEvent event)
    {
        final boolean active = !event.isLive();
        if (playbackActive != active)
        {
            playbackActive = active;
            textWidget.setColor(active ? Color.green.getRGB() : Color.white.getRGB());

            update();
        }
    }

    /**
     * The mouse has changed position, update our internal state.
     *
     * @param event Position event, {@code null} if no position (off globe)
     */
    @Override
    public void moved(PositionEvent event)
    {
        // Skip if mouse tracking is disabled. Such cases will be handled with the render event handling.
        if (!showMousePosition.getCurrentValue()) return;

        // Don't update if not visible
        if (!isVisible()) return;

        // Update our text based on the given position.
        Position newPos = event.getPosition();
        if (newPos != null)
        {
            final IPositionOverlayOverride overrideObject = getOverrideObject(event);
            final ILocation overrideLocation = overrideObject != null ? overrideObject.getLocation() : null;
            if (overrideLocation != null)
            {
                final String overrideName = overrideObject.getName();
                // If we have an "override" location, then show that position, not the mouse position
                updateLocation(overrideLocation.getLatitude(), overrideLocation.getLongitude(),
                        overrideLocation.hasElevation() ? overrideLocation.getElevation() : Double.NaN,
                        GeoPointMetaData.UNKNOWN,
                        overrideName);
            } else
            {
                // If no override, then use the position from the event and fetch the elevation in a background thread
                queryPosition = newPos;
                fetchElevationThread.exec();
            }
        } else
        {
            updateLocationToOffscreen();
        }
        update();
    }

    private void fetchElevationAndUpdateLocationInBackground()
    {
        Position pos = queryPosition;
        fetchElevationAndUpdateLocation(pos.getLatitude().degrees, pos.getLongitude().degrees);
        updateWidgetText();
    }

    @Override
    public void onCameraChanged(MapRenderer renderer)
    {
        update(); // The update internals will handle any differences in mouse tracking vs center viewport tracking.
    }

    /**
     * On altitude unit changes, update altitude, heading, and position strings.
     */
    private void altitudeUnitsChanged(AltitudeUnits altitudeUnits)
    {
        this.altitudeUnits = altitudeUnits;
        makeEyeAltitudeDescription(lastEyeAltitude);
        makeHeadingDescription(lastHeading);
        makePositionDescription(lastLatitude, lastLongitude, lastAltitude, lastElevationSource, null);
        update();
    }

    /**
     * On Coordinate System change, recreate the position description.
     */
    private void coordinateSystemChanged(CoordinateSystem coordinateSystem)
    {
        this.coordinateSystem = coordinateSystem;
        makePositionDescription(lastLatitude, lastLongitude, lastAltitude, lastElevationSource, null);
        update();
    }

    /**
     * On mouse tracking change, update altitude, heading, and position strings.
     */
    private void trackMouseChanged(Boolean tracked)
    {
        update();
    }

    @Override
    public boolean setVisible(boolean visible)
    {
        if (super.setVisible(visible))
        {
            if (visible) updateWidgetText();
            return true;
        }
        return false;
    }

    /**
     * Fetch altitude, heading, and view center location (if enabled) and update widget text.
     */
    private void update()
    {
        boolean visible = isVisible();

        // Skip updates when not being drawn.
        if (!visible) return;

        // Update location from the view center if we're not showing the mouse location
        if (!showMousePosition.getCurrentValue()) fetchCenterPoint();

        // Fetch the latest heading and eye altitude
        fetchHeadingAndEyeAltitude();

        // Update the widget text with latest values
        updateWidgetText();
    }

    private void fetchCenterPoint()
    {
        final BasicView view = (BasicView) wwd.getView();
        final Position position = view.getCenterPosition();
        if (position != null)
        {
            fetchElevationAndUpdateLocation(position.getLatitude().degrees, position.getLongitude().degrees);
        } else
        {
            updateLocationToOffscreen();
        }
    }

    /**
     * Update the altitude and heading from the globe view.
     */
    private void fetchHeadingAndEyeAltitude()
    {
        final BasicView view = (BasicView) wwd.getView();
        updateHeading(view.getHeading().degrees);
        updateEyeAltitude(view.getEyePosition() != null ? view.getEyePosition().getAltitude() : Double.NaN);
    }

    /**
     * Update our state/display using the given latitude and longitude by fetching the altitude
     * and elevation source from the terrain.
     */
    private void fetchElevationAndUpdateLocation(double latitude, double longitude)
    {
        ElevationResult elevationResult = queryElevation(latitude, longitude);
        double altitude = elevationResult.altitude;
        // Convert to MSL
        if (!Double.isNaN(elevationResult.altitude)) altitude = altitude - ElevationManager.getGeoidHeight(latitude, longitude);
        updateLocation(latitude, longitude, altitude, elevationResult.source, null);
    }

    /**
     * Update our state/display using the given location.
     */
    @InvokeOnEDT
    private void updateLocation(double latitude, double longitude, double altitude, String elevationSource, @Nullable String overrideObjectName)
    {
        if (latitude != lastLatitude || longitude != lastLongitude || altitude != lastAltitude || !lastElevationSource.equals(elevationSource))
        {
            lastLatitude = latitude;
            lastLongitude = longitude;
            lastAltitude = altitude;
            lastElevationSource = elevationSource;
            makePositionDescription(latitude, longitude, altitude, elevationSource, overrideObjectName);
        }
    }

    /**
     * Update our state/display to reflect the position being off-screen
     */
    private void updateLocationToOffscreen()
    {
        updateLocation(Double.NaN, Double.NaN, Double.NaN, GeoPointMetaData.UNKNOWN, null);
    }

    /**
     * Update our heading state with a possibly new heading.
     *
     * @param heading Most recent heading
     */
    protected void updateHeading(double heading)
    {
        if (heading != lastHeading)
        {
            lastHeading = heading;
            makeHeadingDescription(heading);
        }
    }

    /**
     * Update our eye altitude with a possibly new value.
     *
     * @param eyeAltitude Most recent eye altitude
     */
    protected void updateEyeAltitude(double eyeAltitude)
    {
        if (eyeAltitude != lastEyeAltitude)
        {
            lastEyeAltitude = eyeAltitude;
            makeEyeAltitudeDescription(eyeAltitude);
        }
    }

    /**
     * Generate the display string for the given location information.
     *
     * @param latitude the latitude to display, or {@link Double#NaN} if offscreen
     * @param longitude the longitude to display, or {@link Double#NaN} if offscreen
     * @param altitude the altitude to display, or {@link Double#NaN} if not available
     * @param elevationSource the elevation source to display
     * @param overrideObjectName If over an {@link IPositionOverlayOverride}, display its name instead of the elevation source (elevationSource must be {@link GeoPointMetaData#UNKNOWN})
     */
    private void makePositionDescription(double latitude, double longitude, double altitude, String elevationSource, @Nullable String overrideObjectName)
    {
        if (!Double.isNaN(latitude) && !Double.isNaN(longitude))
        {
            if (!Double.isNaN(altitude))
            {
                positionDisplay = CoordinateSystem.getLocationString(new RaptorLocation(latitude, longitude, altitude), coordinateSystem, true, altitudeUnits) + " msl ";
                if (!StringUtils.isEmpty(overrideObjectName, true) && GeoPointMetaData.UNKNOWN.equals(elevationSource))
                {
                    positionDisplay += "(" + overrideObjectName + ")";
                } else
                {
                    positionDisplay += "(" + elevationSource + ")";
                }
            } else
            {
                stringBuilder.setLength(0);

                // Construct a location string using the missing elevation indicator
                stringBuilder.append(CoordinateSystem.getLocationString(latitude, longitude, coordinateSystem));
                stringBuilder.append(" Elev ").append(MISSING_ELEVATION_INDICATOR);
                positionDisplay = stringBuilder.toString();
            }
        } else
        {
            positionDisplay = OFF_GLOBE;
        }
    }

    /**
     * Generate the eye altitude display string.
     *
     * @param eyeAltitude eye altitude to format
     */
    private void makeEyeAltitudeDescription(double eyeAltitude)
    {
        altDisplay = !Double.isNaN(eyeAltitude)
                ? "Eye Alt " + altitudeUnits.format(eyeAltitude, 0)
                : "";
    }

    /**
     * Generate the heading display string.
     *
     * @param heading Heading to format
     */
    private synchronized void makeHeadingDescription(double heading)
    {
        // If heading is 0, then just don't show it
        headingDisplay = heading != 0 ? String.format("Heading %s", HEADING_FORMATTER.format(heading)) : "";
    }

    /**
     * Composes the separate display values and assigns the formatted string to the text display.
     */
    @InvokeOnEDT
    private void updateWidgetText()
    {
        String newText = headingDisplay.isEmpty()
                ? altDisplay + " " + positionDisplay
                : altDisplay + " " + headingDisplay + " " + positionDisplay;
        if (playbackActive) newText += " " + PLAYBACK;
        textWidget.setText(newText);
    }

    /**
     * If the mouse is over a renderable that can provide an "override" object, return that object.
     * Else return {@code null}.
     *
     * @param event Event containing objects under the mouse cursor
     * @return The {@link IPositionOverlayOverride} at the event location, or {@code null}
     *
     * @since 5.4
     */
    @Nullable
    private IPositionOverlayOverride getOverrideObject(@Nonnull PositionEvent event)
    {
        return WWUtils.getTopObjectOfType((WorldWindow) event.getSource(), IPositionOverlayOverride.class);
    }

    /**
     * Query the {@link ElevationManager} for the elevation and elevation source at the provided latitude and longitude.
     */
    private ElevationResult queryElevation(double latitude, double longitude)
    {
        GeoPointMetaData metaData = new GeoPointMetaData();
        double alt = ElevationManager.getElevation(latitude, longitude, null, metaData);
        return new ElevationResult(alt, metaData.getAltitudeSource());
    }

    private record ElevationResult(double altitude, String source)
    {
    }
}
