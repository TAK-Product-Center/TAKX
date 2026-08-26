package gov.takx.platform.utils;

import com.atakmap.coremap.conversions.Span;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.util.WWMath;
import gov.tak.api.annotation.DeprecatedApi;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Map;

/**
 * An extension to the WorldWind {@link gov.nasa.worldwind.util.UnitsFormat} class that adds speed related methods and
 * a conversion method to translate constant identifiers to {@link UnitsFormat} objects.<br><br>
 * <p/>
 * TAKX always stores speed in meters/sec. If the user selects an English UoM, speed should be displayed in
 * miles per hour. If the user selects nautical miles as the length UoM, speed should be displayed in knots. For all
 * metric UoM, speed should be displayed in kph.
 *
 * @since 1.14
 * @deprecated Use {@link gov.takx.platform.utils.units.AltitudeUnits}, {@link gov.takx.platform.utils.units.RangeUnits}
 * or {@link gov.takx.platform.utils.units.SpeedUnits} instead.
 */
@Deprecated(since = "5.5", forRemoval = true)
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
@SuppressWarnings("UnusedDeclaration")
public class UnitsFormat extends gov.nasa.worldwind.util.UnitsFormat
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Identifier for speed units in miles per hour.
     */
    public static final String MILES_PER_HOUR = "UnitsFormat.mph";

    /**
     * Identifier for speed units in kilometers per hour.
     */
    public static final String KILOMETERS_PER_HOUR = "UnitsFormat.kph";

    /**
     * Identifier for speed units in knots.
     */
    public static final String KNOTS = "UnitsFormat.knots";

    /**
     * Symbol for speed in miles per hour.
     */
    public static final String SYMBOL_MILES_PER_HOUR = "mph";

    /**
     * Symbol for speed in knots.
     */
    public static final String SYMBOL_KNOTS = "kn";

    /**
     * Symbol for speed in kilometers per hour.
     */
    public static final String SYMBOL_KILOMETERS_PER_HOUR = "kph";

    /**
     * Common units: Meters/Square Meters
     */
    public static final UnitsFormat M_M2 = new UnitsFormat(UnitsFormat.METERS, UnitsFormat.SQUARE_METERS);

    /**
     * Common units: Kilometers/Square Kilometers
     */
    public static final UnitsFormat KM_KM2 = new UnitsFormat(UnitsFormat.KILOMETERS, UnitsFormat.SQUARE_KILOMETERS);

    /**
     * Common units: Kilometers/Hectares
     */
    public static final UnitsFormat KM_HECTARE = new UnitsFormat(UnitsFormat.KILOMETERS, UnitsFormat.HECTARE);

    /**
     * Common units: Feet/Square feet
     */
    public static final UnitsFormat FT_FT2 = new UnitsFormat(UnitsFormat.FEET, UnitsFormat.SQUARE_FEET);

    /**
     * Common units: Miles/Square Miles
     */
    public static final UnitsFormat MI_MI2 = new UnitsFormat(UnitsFormat.MILES, UnitsFormat.SQUARE_MILES);

    /**
     * Common units: Nautical Miles/Square Miles
     */
    public static final UnitsFormat NM_MI2 = new UnitsFormat(UnitsFormat.NAUTICAL_MILES, UnitsFormat.SQUARE_MILES);

    /**
     * Common units: Yards/Acres
     */
    public static final UnitsFormat YD_ACRE = new UnitsFormat(UnitsFormat.YARDS, UnitsFormat.ACRE);

    private static final Map<UnitsFormat, Integer> UNITS_TO_SPAN_SYSTEM = Map.of(
            M_M2, Span.METRIC,
            KM_KM2, Span.METRIC,
            KM_HECTARE, Span.METRIC,
            FT_FT2, Span.ENGLISH,
            MI_MI2, Span.ENGLISH,
            NM_MI2, Span.NM);

    private static final Map<UnitsFormat, Span> UNITS_TO_SPAN = Map.of(
            M_M2, Span.METER,
            KM_KM2, Span.KILOMETER,
            KM_HECTARE, Span.KILOMETER,
            FT_FT2, Span.FOOT,
            MI_MI2, Span.MILE,
            NM_MI2, Span.NAUTICALMILE);

    private String speedUnits;
    private String speedUnitsSymbol;
    private double speedUnitsMultiplier; // factor to convert from kph to current units

    /**
     * Construct an instance that displays length in kilometers, area in square kilometers and angles in decimal
     * degrees.
     */
    public UnitsFormat()
    {
        this(KILOMETERS, SQUARE_KILOMETERS, false);
    }

    /**
     * Constructs an instance that display length and area in specified units, and angles in decimal degrees.
     *
     * @param lengthUnits the desired length units. Available length units are {@code METERS}, {@code KILOMETERS},
     *                    {@code MILES}, {@code NAUTICAL_MILES}, {@code YARDS}, and {@code FEET}.
     * @param areaUnits   the desired area units. Available area units are {@code SQUARE_METERS},
     *                    {@code SQUARE_KILOMETERS}, {@code HECTARE}, {@code ACRE}, {@code SQUARE_YARD},
     *                    and {@code SQUARE_FEET}.
     * @throws IllegalArgumentException if either {@code lengthUnits} or {@code areaUnits} is {@code null}.
     */
    public UnitsFormat(String lengthUnits, String areaUnits)
    {
        this(lengthUnits, areaUnits, false);
    }

    /**
     * Constructs an instance that display length and area in specified units, and angles in a specified format.
     *
     * @param lengthUnits the desired length units. Available length units are {@code METERS}, {@code KILOMETERS},
     *                    {@code MILES}, {@code NAUTICAL_MILES}, {@code YARDS}, and {@code FEET}.
     * @param areaUnits   the desired area units. Available area units are {@code SQUARE_METERS},
     *                    {@code SQUARE_KILOMETERS}, {@code HECTARE}, {@code ACRE}, {@code SQUARE_YARD},
     *                    and {@code SQUARE_FEET}.
     * @param showDMS     true if the desired angle format is degrees-minutes-seconds, false if the format is decimal
     *                    degrees.
     * @throws IllegalArgumentException if either {@code lengthUnits} or {@code areaUnits} is {@code null}.
     */
    public UnitsFormat(String lengthUnits, String areaUnits, boolean showDMS)
    {
        super(lengthUnits, areaUnits, showDMS);
    }

    /**
     * @return The Span system for this units format.  One of {@link Span#METRIC}, {@link Span#ENGLISH}, or {@link Span#NM}
     * @since 5.0
     */
    public int getSpanSystem()
    {
        return UNITS_TO_SPAN_SYSTEM.getOrDefault(this, Span.METRIC);
    }

    /**
     * @return The Span for this units format.
     * One of {@link Span#METER}, {@link Span#KILOMETER}, {@link Span#FOOT}, {@link Span#MILE}, {@link Span#NAUTICALMILE}
     * @since 5.0
     */
    @Nonnull
    public Span getSpan()
    {
        return UNITS_TO_SPAN.getOrDefault(this, Span.METER);
    }

    @Override
    public void setLengthUnits(String lengthUnits)
    {
        super.setLengthUnits(lengthUnits);

        // Internally, we store as meters/sec
        final double mpsToKphMultiplier = UnitsOfMeasureUtils.MPS_TO_KPH_MULTIPLIER;

        switch (lengthUnits)
        {
            case UnitsFormat.MILES:
            case UnitsFormat.YARDS:
            case UnitsFormat.FEET:
                speedUnits = MILES_PER_HOUR;
                speedUnitsMultiplier = mpsToKphMultiplier / UnitsOfMeasureUtils.KILOMETERS_PER_MILE;
                speedUnitsSymbol = SYMBOL_MILES_PER_HOUR;
                break;
            case UnitsFormat.NAUTICAL_MILES:
                speedUnits = KNOTS;
                speedUnitsMultiplier = mpsToKphMultiplier / UnitsOfMeasureUtils.KILOMETERS_PER_NAUTICAL_MILE;
                speedUnitsSymbol = SYMBOL_KNOTS;
                break;
            default:
                speedUnits = KILOMETERS_PER_HOUR;
                speedUnitsMultiplier = mpsToKphMultiplier;
                speedUnitsSymbol = SYMBOL_KILOMETERS_PER_HOUR;
                break;
        }
    }

    /**
     * TAKX speeds are always stored in KPH. If the current length UoM is an English unit, speed should be displayed
     * in MPH. If the current length UoM is in nautical miles, it should be displayed in knots. For all metric UoM,
     * show speed in KPH.
     *
     * @return The multiplier required to convert the speed stored in TAKX, which is in KPH, to either MPH, knots or KPH.
     */
    public double getSpeedMultiplier()
    {
        return speedUnitsMultiplier;
    }

    /**
     * Returns the constant for the current speed UoM.
     *
     * @return The constant for the current speed UoM.
     * @see #MILES_PER_HOUR
     * @see #KNOTS
     * @see #KILOMETERS_PER_HOUR
     */
    public String getSpeedUnits()
    {
        return speedUnits;
    }

    /**
     * Returns the symbol identifying the current speed UoM: "mph" for miles per hour, "kn" for knots and
     * "kph" for kilometers per hour.
     *
     * @return The symbol identifying the current speed UoM.
     * @see #SYMBOL_MILES_PER_HOUR
     * @see #SYMBOL_KNOTS
     * @see #SYMBOL_KILOMETERS_PER_HOUR
     */
    public String getSpeedSymbol()
    {
        return speedUnitsSymbol;
    }

    /**
     * Format the given height according to this units format.  This will use a default height format of
     * {@code "%,.1f"} and the result will include the units symbol if {@code includeSymbol} is {@code true}.
     * The height value will be scaled to the proper units, it is assumed to be in meters.
     *
     * @param height        The height to format, assumed to be in meters
     * @param includeSymbol Pass {@code true} if the units symbol should be appended to the height value
     * @return Formatted, scaled
     */
    public String formatVerticalHeight(double height, boolean includeSymbol)
    {
        return formatVerticalHeight(height, "%,.1f", includeSymbol);
    }

    /**
     * Format the given height according to this units format using the given height format string. The result
     * will include the units symbol if {@code includeSymbol} is {@code true}.  The height value will
     * be scaled to the proper units, it is assumed to be in meters.
     * <p/>
     * The {@code heightFormat} must be an appropriate argument to {@link String#format(String, Object...)} taking a
     * double argument.  For example, {@code "%,.1f"}
     *
     * @param height        The height to format, assumed to be in meters
     * @param includeSymbol Pass {@code true} if the units symbol should be appended to the height value
     * @return Formatted, scaled
     */
    public String formatVerticalHeight(double height, String heightFormat, boolean includeSymbol)
    {
        final double scaledHeight = height * getVerticalHeightMultiplier();

        final String baseString = String.format(heightFormat, scaledHeight);

        return includeSymbol
                ? baseString + " " + getVerticalHeightUnitsSymbol()
                : baseString;
    }

    /**
     * This method will provide a multiplier value to convert a shape's vertical height to meters anytime the preferred
     * units of measure are set to UnitsFormat.METRIC_SYSTEM (km, m, etc).
     * Similarly this method will provide a multiplier value to convert a shape's vertical height to feet anytime the
     * preferred units of measure are set to UnitsFormat.IMPERIAL_SYSTEM (miles, ft, etc).
     *
     * @return multiplier to convert to meters or feet depending on system units (METRIC or IMPERIAL)
     */
    public double getVerticalHeightMultiplier()
    {
        return IMPERIAL_SYSTEM.equals(getLengthUnitsSystem())
                ? WWMath.METERS_TO_FEET
                : 1d;
    }

    /**
     * Provides a way to extract a units of measure symbol ("m" or "ft") depending on system units (METRIC or IMPERIAL)
     *
     * @return the appropriate units of measure symbol ("m" or "ft") depending on system units (METRIC or IMPERIAL)
     */
    public String getVerticalHeightUnitsSymbol()
    {
        return UnitsFormat.IMPERIAL_SYSTEM.equals(getLengthUnitsSystem())
                ? SYMBOL_FEET
                : SYMBOL_METERS;
    }

    /**
     * Converts an {@link ILengthMeasurer} constant unit of measure String value into a {@link UnitsFormat}.
     *
     * @param value String representation of Units of Measure as documented within {@link ILengthMeasurer}.
     * @return Converted string to a length {@link UnitsFormat}.
     */
    public static UnitsFormat toUnits(String value)
    {
        return switch (value)
        {
            case ILengthMeasurer.METERS__METERS_SQR -> M_M2;
            case ILengthMeasurer.KILOMETERS__KILOMETERS_SQR -> KM_KM2;
            case ILengthMeasurer.KILOMETERS_HECTARE -> KM_HECTARE;
            case ILengthMeasurer.FEET__FEET_SQR -> FT_FT2;
            case ILengthMeasurer.MILES__MILES_SQR -> MI_MI2;
            case ILengthMeasurer.NAUTICAL_MILES__MILES_SQR -> NM_MI2;
            case ILengthMeasurer.YARDS__ACRES -> YD_ACRE;
            default ->
            {
                logger.error("Attempt to convert to an unrecognized unit of measure: {}", value);
                yield KM_KM2;
            }
        };
    }

    /**
     * Format an angle according to the current angle format. Prepend a specified label.
     *
     * @param label a label to prepend to the formatted angle, or {@code null} to indicate no label
     * @param angle the angle to format
     *
     * @return a string containing the formatted angle prepended with the specified label
     * @since 5.5
     */
    public String angle(@Nullable String label, double angle)
    {
        return angle(label, Angle.fromDegrees(angle));
    }

    @Override
    public String toString()
    {
        return "UnitsFormat{" +
                "speedUnits='" + speedUnits + '\'' +
                ", areaUnits='" + areaUnits + '\'' +
                '}';
    }
}
