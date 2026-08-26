package gov.takx.platform.utils.units;

import com.atakmap.coremap.conversions.Span;
import gov.takx.api.annotation.Internal;
import jakarta.annotation.Nonnull;

/**
 * Utilities for converting units of measure to kernel span systems. Putting these conversions here rather than in the
 * units of measure classes keeps them out of our API.
 *
 * @since 5.5
 */
@Internal
public class SpanConversionUtils
{
    /**
     * @param spanSystem The span system
     * @return The altitude units to use for the span system
     */
    public static AltitudeUnits altitudeUnitsFromSpanSystem(int spanSystem)
    {
        return switch (spanSystem)
        {
            case Span.METRIC -> AltitudeUnits.METERS;
            case Span.ENGLISH -> AltitudeUnits.FEET;
            default -> throw new IllegalArgumentException("Only metric and English span systems are supported");
        };
    }

    /**
     * @param spanSystem The span system
     * @return The range units to use for the span system
     */
    public static RangeUnits rangeUnitsFromSpanSystem(int spanSystem)
    {
        return switch (spanSystem)
        {
            case Span.METRIC -> RangeUnits.METERS;
            case Span.ENGLISH -> RangeUnits.FEET;
            case Span.NM -> RangeUnits.NAUTICAL_MILES;
            default -> throw new IllegalArgumentException("Only metric and English span systems are supported");
        };
    }

    /**
     * @param spanSystem The span system
     * @return The units to use for the span system
     * @throws IllegalArgumentException if the span system is not supported
     */
    public static SpeedUnits speedUnitsFromSpanSystem(int spanSystem)
    {
        return switch (spanSystem)
        {
            case Span.METRIC -> SpeedUnits.MPS;
            case Span.ENGLISH -> SpeedUnits.MPH;
            case Span.NM -> SpeedUnits.KTS;
            default -> throw new IllegalArgumentException("Only metric, English and nautical span systems are supported");
        };
    }

    /**
     * @return The altitude unit's span system (metric, English) id.
     * @see Span#getType()
     */
    public static int toSpanSystem(@Nonnull RangeUnits units)
    {
        return switch (units)
        {
            case FEET -> Span.ENGLISH;
            case METERS -> Span.METRIC;
            case NAUTICAL_MILES -> Span.NM;
        };
    }

    /**
     * @return The altitude unit's span system (metric, English) id.
     * @see Span#getType()
     */
    public static int toSpanSystem(@Nonnull AltitudeUnits units)
    {
        return switch (units)
        {
            case FEET -> Span.ENGLISH;
            case METERS -> Span.METRIC;
        };
    }

    private SpanConversionUtils()
    {
    }
}
