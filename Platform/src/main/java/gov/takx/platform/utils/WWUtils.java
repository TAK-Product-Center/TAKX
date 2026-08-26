package gov.takx.platform.utils;

import com.atakmap.map.elevation.ElevationManager;
import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.pick.PickedObject;
import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.api.coordinatesystem.ILocation;
import gov.takx.api.coordinatesystem.RaptorLocation;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.platform.cdi.InjectableContainerSupport;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Point;
import java.lang.invoke.MethodHandles;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Static methods for converting between World Wind {@link Position} and TAKX {@link ILocation} instances and other
 * utility operations utilizing the World Wind libs.
 *
 * @author Proprietary information subject to the terms of a EULA
 * @since 2.38 (this util was originally part of the TAKX client module and was created in 2008)
 */
public class WWUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Sector.getCorners return order: SW, SE, NE, NW
    private static final int SW_CORNER = 0;
    private static final int NE_CORNER = 2;

    private static final InjectableContainerSupport BEAN_ACCESSOR = new InjectableContainerSupport();

    /**
     * Create a bounding {@link Sector} containing all the given locations.
     *
     * @param locations Locations from which to determine the bounding sector
     * @return Bounding Sector for points
     * @since 3.0
     */
    @Nonnull
    public static Sector boundingSector(@Nonnull Collection<? extends ILocation> locations)
    {
        // Convert locations to LatLon
        final List<LatLon> latLonList = locations.stream()
                .map(location -> LatLon.fromDegrees(location.getLatitude(), location.getLongitude()))
                .collect(Collectors.toList());

        return Sector.boundingSector(latLonList);
    }

    /**
     * Return an array containing the minimum and maximum corners from the bounding sector of the given locations.
     *
     * @param locations list of locations to process
     * @return Array of {@code [minLocation, maxLocation]}
     * @see #boundingSector(Collection)
     * @see Sector#getCorners()
     * @since 3.0
     */
    @Nonnull
    public static ILocation[] getMinMaxCorners(@Nonnull Collection<? extends ILocation> locations)
    {
        final LatLon[] sectorCorners = boundingSector(locations).getCorners();

        final ILocation minCorner = latLonToRaptorLocation(sectorCorners[SW_CORNER]);
        final ILocation maxCorner = latLonToRaptorLocation(sectorCorners[NE_CORNER]);

        return new ILocation[]{minCorner, maxCorner};
    }

    /**
     * Construct a new Position object from the given Location. The elevation will be set to 0 if
     * {@code useElevation} is false, otherwise the elevation will be extracted from the Location,
     * if it has one.
     *
     * @param location     object to process
     * @param useElevation if {@code false} Position has elevation set to 0, otherwise elevation comes
     *                     from the Location object if it has one.
     * @return newly minted Position object, or {@code null} if the provided location was {@code null}
     */
    @Nullable
    public static Position locationToPosition(@Nullable ILocation location, boolean useElevation)
    {
        if (location == null) return null;
        if (location == GeoPoint.UNKNOWN_POINT) return Position.ZERO; // this is the default for GeoPointMetaData

        try
        {
            return new Position(Angle.fromDegrees(location.getLatitude()), Angle.fromDegrees(location.getLongitude()),
                    ((useElevation && location.hasElevation()) ? location.getElevation() : 0));
        } catch (Exception e)
        {
            // Try to be resilient against bogus locations, return 0,0,0 if we catch an exception
            logger.error("Failed to convert location: {}", location, e);
            return Position.ZERO;
        }
    }

    /**
     * Construct a Position object from the given location including elevation if the Location has one.
     *
     * @param location object to process
     * @return newly minted Position object, or {@code null} if the provided location was {@code null}
     */
    @Nullable
    public static Position locationToPosition(@Nullable ILocation location)
    {
        return locationToPosition(location, true);
    }

    /**
     * Construct a LatLon object from the given location. Note: the elevation is not used.
     *
     * @param location Location to translate to LatLon;
     * @return new LatLon object.
     */
    @Nullable
    public static LatLon locationToLatLon(@Nullable ILocation location)
    {
        return location == null
                ? null
                : LatLon.fromDegrees(location.getLatitude(), location.getLongitude());
    }

    /**
     * Construct a {@link RaptorLocation} object from a WorldWind LatLon object. The resulting object does
     * not have any elevation data set.
     *
     * @param latLon object to process
     * @return new RaptorLocation object with properties (lat, lon) set from {@code latLot}
     */
    @Nullable
    public static ILocation latLonToRaptorLocation(@Nullable LatLon latLon)
    {
        return latLon == null
                ? null
                : new RaptorLocation(latLon.getLatitude().getDegrees(), latLon.getLongitude().getDegrees());
    }

    /**
     * Convert an iterable of {@link LatLon}s to a list of {@link RaptorLocation}s.
     *
     * @param iterable Iterable to process
     * @return List of RaptorLocation instances, may be empty, never {@code null}
     * @since 3.3
     */
    @Nonnull
    public static List<? extends ILocation> latLonIterableToRaptorLocations(@Nonnull Iterable<? extends LatLon> iterable)
    {
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(location -> new RaptorLocation(location.getLatitude().degrees, location.getLongitude().degrees))
                .toList();
    }

    /**
     * Convert an iterable of {@link LatLon}s to a list of {@link RaptorLocation}s.
     *
     * @param iterable Iterable to process
     * @return List of RaptorLocation instances, may be empty, never {@code null}
     * @since 5.5
     */
    @Nonnull
    public static List<ILocation> latLonIterableToILocations(@Nonnull Iterable<? extends LatLon> iterable)
    {
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(location -> (ILocation) new RaptorLocation(location.getLatitude().degrees, location.getLongitude().degrees))
                .toList();
    }

    /**
     * Convert an iterable of {@link LatLon}s to a list of {@link RaptorLocation}s.  If a given element is actually
     * a {@link Position}, then the {@link RaptorLocation} created will include the elevation from the position,
     * otherwise, no elevation will be set.
     *
     * @param iterable Iterable to process
     * @return List of RaptorLocation instances, may be empty, never {@code null}
     * @since 3.3
     */
    @Nonnull
    public static List<? extends ILocation> positionIterableToRaptorLocations(@Nonnull Iterable<? extends LatLon> iterable)
    {
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(location -> location instanceof Position
                        ? positionToRaptorLocation((Position) location)
                        : new RaptorLocation(location.getLatitude().degrees, location.getLongitude().degrees))
                .toList();
    }

    /**
     * Construct a {@link RaptorLocation} object from a World Wind Position object
     *
     * @param position object to process
     * @return newly minted RaptorLocation object with properties (lat, lon, elev) set from {@code position}
     */
    @Nullable
    public static ILocation positionToRaptorLocation(@Nullable Position position)
    {
        return position == null
                ? null
                : new RaptorLocation(position.getLatitude().getDegrees(), position.getLongitude().getDegrees(), position.getElevation());
    }

    /**
     * @return the elevation in meters MSL at the given coordinates
     * @since 3.3
     */
    public static double getElevation(WorldWindow wwd, ILocation location)
    {
        return getElevation(wwd, Angle.fromDegreesLatitude(location.getLatitude()), Angle.fromDegreesLongitude(location.getLongitude()));
    }

    /**
     * @return the elevation in meters MSL at the given coordinates
     * @since 3.3
     */
    public static double getElevation(WorldWindow wwd, Angle latitude, Angle longitude)
    {
        final double elevation = ElevationManager.getElevation(latitude.degrees, longitude.degrees, null);
        return GeoCalculations.haeToMsl(latitude.degrees, longitude.degrees, elevation);
    }

    /**
     * Compute the position of a given screen coordinate using the current view of the globe associated with the given
     * world window. This method should provide more accurate results than just calling
     * {@link View#computePositionFromScreenPoint(double, double)} because it attempts to intersect against the
     * terrain model.
     *
     * @param wwd World window to locate globe instance
     * @param x   Screen X coordinate
     * @param y   Screen Y coordinate
     * @return Position of screen point on globe, {@code null} if calculations fail
     * @since 1.49
     * @deprecated at 5.4, use {@link gov.takx.api.globe.IGlobeDelegate#getGlobeLocationFromPoint(Point)} instead
     */
    @Nullable
    @Deprecated(since = "5.4", forRemoval = true)
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    public static Position computePositionFromScreenPoint(@Nonnull WorldWindow wwd, double x, double y)
    {
        //noinspection DataFlowIssue
        final var geoPoint = BEAN_ACCESSOR.getBean(IGlobeManager.class).getGlobeForComponent(wwd)
                .getGlobeLocationFromPoint(new Point((int) x, (int) y));

        return geoPoint == null ? null : Position.fromDegrees(geoPoint.getLatitude(), geoPoint.getLongitude(), geoPoint.getElevation());
    }

    /**
     * @param wwd World Window to test
     * @return True if the mouse click occurred on terrain
     * @since 3.3
     */
    public static boolean clickedOnTerrain(@Nonnull WorldWindow wwd)
    {
        final PickedObject topPickedObject = wwd.getObjectsAtCurrentPosition().getTopPickedObject();
        return topPickedObject != null && topPickedObject.isTerrain();
    }

    /**
     * Return the top object from the objects at the current position if it is of the requested type,
     * {@code null} otherwise.
     *
     * @param wwd World Window to query
     * @param type Requested object type
     * @return Properly typed object, or {@code null} if the top object is not of the requested type
     * @param <T> inferred type
     * @see WorldWindow#getObjectsAtCurrentPosition()
     * @since 3.3
     */
    @Nullable
    public static <T> T getTopObjectOfType(@Nonnull WorldWindow wwd, @Nonnull Class<T> type)
    {
        final Object topObject = wwd.getObjectsAtCurrentPosition().getTopObject();
        return type.isInstance(topObject) ? type.cast(topObject) : null;
    }

    /**
     * Return the first object from the objects at the current position if it is of the requested type,
     * {@code null} otherwise.
     *
     * @param wwd World Window to query
     * @param type Requested object type
     * @return Properly typed object, or {@code null} if no objects are of the requested type
     * @param <T> inferred type
     * @see WorldWindow#getObjectsAtCurrentPosition()
     * @since 3.3
     */
    @Nullable
    public static <T> T findTopObjectOfType(@Nonnull WorldWindow wwd, @Nonnull Class<T> type)
    {
        return wwd.getObjectsAtCurrentPosition().stream()
                .filter(Objects::nonNull)
                .map(PickedObject::getObject)
                .filter(type::isInstance)
                .map(type::cast)
                .findFirst()
                .orElse(null);

    }
}
