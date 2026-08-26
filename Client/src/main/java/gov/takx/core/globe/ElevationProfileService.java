package gov.takx.core.globe;

import com.atakmap.map.elevation.ElevationManager;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.api.platform.ICoreExecutorService;
import gov.takx.core.view.gui.CoreComponentType;
import gov.takx.core.view.gui.IExtendedUISpaceManager;
import gov.takx.platform.utils.ShapeUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.SwingUtilities;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Profiles a set of {@link IGeoPoint points} and returns a {@link ElevationProfile profile} with a new set of
 * {@link IGeoPoint points} with elevations.
 * 
 * @since 5.6
 */
@ApplicationScoped
public class ElevationProfileService
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final int DEFAULT_MAX = 20_000;

    private final IExtendedUISpaceManager uiSpaceManager;
    private final Event<ElevationProfile> profileEvent;
    private final ICoreExecutorService executorService;
    private final INotificationService notificationService;

    private CompletableFuture<Void> future;

    @Inject
    ElevationProfileService(IExtendedUISpaceManager uiSpaceManager, Event<ElevationProfile> profileEvent,
                            ICoreExecutorService executorService, INotificationService notificationService)
    {
        this.uiSpaceManager = uiSpaceManager;
        this.profileEvent = profileEvent;
        this.executorService = executorService;
        this.notificationService = notificationService;
    }

    /**
     * Profiles the supplied points and displays the elevation profile UI. Intermediate points along the path will be 
     * generated at regular intervals to fill in the gaps, with a max of {@link #DEFAULT_MAX} total points, including
     * the original points.
     * 
     * @param points The points to profile
     * @param start The point collection start time, or {@code null} if unknown
     * @param end The point collection end time, or {@code null} if unknown
     */
    public void showProfile(@Nonnull List<? extends IGeoPoint> points, @Nullable Instant start, @Nullable Instant end)
    {
        if (future != null) future.cancel(true);

        future = CompletableFuture.supplyAsync(() -> {
            Thread.currentThread().setName("Elevation Profile");
            return getProfile(points, DEFAULT_MAX, start, end);
        }, executorService)
                .thenAccept(profileEvent::fire)
                .thenRun(() -> SwingUtilities.invokeLater(() -> uiSpaceManager.showComponent(CoreComponentType.ELEVATION_PROFILER_PANEL)))
                .exceptionally(t -> {
                    final var msg = "Error calculating elevation profile";
                    logger.error(msg, t);
                    notificationService.show(Notification.builder(msg).setType(Notification.Type.Error).build());
                    return null;
                });
    }

    /**
     * @param points The points to profile
     * @param maxProfiledPoints The max number of points to include in the result, including the original points and those
     *                          generated for the steps between the original points
     * @param start The point collection start time, or {@code null} if unknown
     * @param end The point collection end time, or {@code null} if unknown
     * @return The elevation profile or {@code null} if there are not enough points to profile. Note: points with
     * unavailable elevations will have an altitude value of {@link Double#NaN}.
     */
    @Nullable
    public ElevationProfile getProfile(@Nonnull List<? extends IGeoPoint> points, int maxProfiledPoints, @Nullable Instant start, @Nullable Instant end)
    {
        final var pointCount = points.size();

        if (pointCount < 2) return null;

        final var length = getLength(points);
        final var samples = (int) Math.min(length, maxProfiledPoints); // ~1 sample per meter or max
        final var lengthRadians = length / ShapeUtils.RADIUS_OF_EARTH;
        final var stepLengthRadians = lengthRadians / (samples - 1);

        var geoPoint = points.get(0);
        var nextGeoPoint = points.get(1);

        final var resultPoints = new ArrayList<IGeoPoint>(pointCount);

        resultPoints.add(geoPoint); // Set first sample at path start

        // Compute in between samples
        int segmentIndex = 0;

        for (int i = 1; i < samples - 1; i++)
        {
            var remainingStepLength = stepLengthRadians;

            while (remainingStepLength > 0)
            {
                final double distance = ShapeUtils.rhumbDistance(geoPoint, nextGeoPoint);

                if (distance >= remainingStepLength)
                {
                    final var azimuth = ShapeUtils.rhumbAzimuth(geoPoint, nextGeoPoint);
                    geoPoint = ShapeUtils.rhumbEndPosition((ILocation) geoPoint, azimuth, Math.toDegrees(remainingStepLength));
                    resultPoints.add(geoPoint);
                    remainingStepLength = 0;
                } else
                {
                    segmentIndex++;

                    // Fractional remaining sample length might move past last point
                    if (segmentIndex + 1 == points.size())
                    {
                        i = samples; // end for loop
                        break;
                    }

                    geoPoint = points.get(segmentIndex);
                    nextGeoPoint = points.get(segmentIndex + 1);
                    remainingStepLength -= distance;
                }
            }
        }

        // Set last sample at path end
        resultPoints.add(points.get(pointCount - 1));

        return getElevationProfile(resultPoints, length, start, end);
    }

    /**
     * @param points The path points
     * @return The length of the path
     */
    private double getLength(@Nonnull List<? extends IGeoPoint> points)
    {
        double length = 0;
        IGeoPoint previousPoint = null;

        for (IGeoPoint point : points)
        {
            length += previousPoint != null ? GeoCalculations.haversine(previousPoint, point) : 0;
            previousPoint = point;
        }

        return length;
    }

    /**
     * @param points The points to analyze
     * @param length The length of the point path
     * @param start The point collection start time, or {@code null} if unknown
     * @param end The point collection end time, or {@code null} if unknown
     * @return An elevation profile with {@link IGeoPoint points} that include elevation data (in MSL), min and max
     * elevations and the provided path metadata. Note: points with unavailable elevations will have an altitude value
     * of {@link Double#NaN}.
     */
    @Nonnull
    private ElevationProfile getElevationProfile(List<? extends IGeoPoint> points, double length, @Nullable Instant start, @Nullable Instant end)
    {
        final var elevations = new double[points.size()];
        final var atakGeoPointIterator = points.stream()
                .map(point -> new com.atakmap.coremap.maps.coords.GeoPoint(point.getLatitude(), point.getLongitude()))
                .iterator();
        ElevationManager.getElevation(atakGeoPointIterator, elevations, null);

        var minElevation = Double.MAX_VALUE;
        var maxElevation = -Double.MAX_VALUE;

        final var pointsWithElevation = new ArrayList<IGeoPoint>();
        int nanCount = 0;

        for (int i = 0; i < points.size(); i++)
        {
            final var point = points.get(i);
            var elevation = elevations[i];
            final var hasElevation = !Double.isNaN(elevation);

            // Convert to MSL
            if (hasElevation)
            {
                elevation = GeoCalculations.haeToMsl(point.getLatitude(), point.getLongitude(), elevation);
                minElevation = Math.min(elevation, minElevation);
                maxElevation = Math.max(elevation, maxElevation);
            }

            if (!hasElevation) nanCount++;

            pointsWithElevation.add(new GeoPoint(point.getLatitude(), point.getLongitude(), elevation));
        }

        if (logger.isDebugEnabled()) logger.debug("points: {} min: {} max: {} length: {} no elevation value: {}",
                points.size(), minElevation, maxElevation, length, nanCount);

        return new ElevationProfile(pointsWithElevation, length, minElevation, maxElevation, start, end);
    }

    /**
     * @param points Points with elevation values in AMSL (meters above mean sea level) or {@link Double#NaN} if unavailable
     * @param length The total length of the point path, in meters
     * @param minElevation The min elevation along the path in meters AMSL
     * @param maxElevation The max elevation along the path in meters AMSL
     * @param start The point collection start time or {@code null} if unavailable
     * @param end The point collection end time or {@code null} if unavailable
     */
    public record ElevationProfile(@Nonnull List<? extends IGeoPoint> points, double length, double minElevation, double maxElevation,
            @Nullable Instant start, @Nullable Instant end)
    {
    }
}
