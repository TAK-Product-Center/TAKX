package gov.takx.api.shapes;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.coords.NorthReference;
import gov.tak.api.annotation.IncubatingApi;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * Interface to an immutable bullseye shape.
 * @since 5.5
 */
@IncubatingApi(since = "5.5")
@NoPluginImplementations
public interface IBullseye extends IRingedShape
{
    String DIRECTION = "DIRECTION";
    String SPAN_UNITS = "SPAN_UNITS";
    String NORTH_REFERENCE = "NORTH_REFERENCE";
    String BEARING_UNITS = "BEARING_UNITS";
    String SHOW_RANGE_RINGS = "SHOW_RANGE_RINGS";

    /** Minimum bullseye radius in {@link Span#METER} **/
    double MIN_RADIUS = .01;
    /** Maximum bullseye radius in {@link Span#METER} **/
    double MAX_RADIUS = 1_500_000;

    /** Minimum bullseye ring radius/interval in {@link Span#METER} **/
    double MIN_RING_RADIUS = .01;
    /**
     * Maximum bullseye ring radius/interval in {@link Span#METER}
     * <p>
     * Calculated as {@link #getRingCount()}*{@link #getRingRadius()}
     **/
    double MAX_RING_RADIUS = 20_000_000;

    List<String> PROPERTIES_TO_WATCH = List.of(
            ShapeProperties.NAME, ShapeProperties.LOCATION, ShapeProperties.RADIUS,
            ShapeProperties.INTERVAL, ShapeProperties.RING_COUNT, ShapeProperties.VISIBILITY,
            NORTH_REFERENCE, BEARING_UNITS, DIRECTION, SPAN_UNITS, SHOW_RANGE_RINGS);

    /** Limited subset of available {@link Angle Angles} that are acceptable for Bullseye map objects **/
    List<Angle> BEARING_OPTIONS = List.of(Angle.DEGREE, Angle.MIL);

    /**
     * @return radius of the bullseye in {@link Span#METER}.
     */
    double getRadius();

    /**
     * @return radius interval of the bullseye rings in {@link Span#METER}.
     */
    double getRingRadius();

    /**
     * @return {@code true} if the bullseye is pointing from the edge to the center, otherwise {@code false}
     */
    boolean isPointingEdgeToCenter();

    /**
     * @return the current {@link NorthReference} of the bullseye.
     */
    @Nonnull
    NorthReference getNorthReference();

    /**
     * @return the current {@link Angle} bearing units of the bullseye.
     */
    @Nonnull
    Angle getBearingUnits();

    /**
     * @return the current {@link Span} units defining the bullseye.
     */
    @Nonnull
    Span getSpanUnits();

    /**
     * @return {@code true} if the range rings are being shown
     */
    boolean isShowRangeRings();

    /**
     * @param newRadius Proposed radius value to check
     * @param units {@link Span} units of the new radius
     * @return {@code true} if the passed in radius in the provided units is valid
     * @see IMutableBullseye#setRadius(double, Span)
     */
    static boolean isValidRadius(double newRadius, Span units)
    {
        double radiusMeters = units == Span.METER ? newRadius : SpanUtilities.convert(newRadius, units, Span.METER);
        return radiusMeters >= MIN_RING_RADIUS && radiusMeters <= MAX_RADIUS;
    }

    /**
     * Checks if the new radius (in the given units) is valid given the provided number of rings
     * @param newRingRadius Proposed ring radius value to check
     * @param units {@link Span} units of the ring radius
     * @param ringCount proposed ring count to check
     * @return {@code true} if the passed in ring radius in the provided units is valid
     * @see IMutableBullseye#setRingRadius(double, Span)
     */
    static boolean isValidRingRadius(double newRingRadius, Span units, double ringCount)
    {
        if (ringCount > MAX_RING_COUNT || ringCount < MIN_RING_COUNT) return false;
        double ringRadiusMeters = units == Span.METER ? newRingRadius : SpanUtilities.convert(newRingRadius, units, Span.METER);
        return ringRadiusMeters >= MIN_RING_RADIUS && ringRadiusMeters * ringCount <= MAX_RING_RADIUS;
    }
}
