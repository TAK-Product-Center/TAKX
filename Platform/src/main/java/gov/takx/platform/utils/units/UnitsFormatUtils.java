package gov.takx.platform.utils.units;

import gov.takx.api.annotation.Internal;
import gov.takx.platform.utils.ILengthMeasurer;
import gov.takx.platform.utils.UnitsFormat;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Provides temporary support for the deprecated {@link gov.takx.platform.utils.UnitsFormat}. This should be removed
 * when the related deprecated classes and methods are removed.
 * 
 * @since 5.5
 */
@Internal
@SuppressWarnings("removal")
public class UnitsFormatUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Converts {@link UnitsFormat} to {@link AltitudeUnits}.
     *
     * @param unitsOfMeasureString The units of measure String
     * @return The equivalent AltitudeUnits
     * @see gov.takx.platform.utils.UnitsFormat#toUnits(String)
     */
    public static AltitudeUnits toAltitudeUnits(@Nonnull String unitsOfMeasureString)
    {
        return toAltitudeUnits(UnitsFormat.toUnits(unitsOfMeasureString));
    }

    /**
     * Converts {@link UnitsFormat} to {@link AltitudeUnits}.
     *
     * @param unitsFormat The units format
     * @return The equivalent AltitudeUnits
     */
    public static AltitudeUnits toAltitudeUnits(@Nonnull gov.nasa.worldwind.util.UnitsFormat unitsFormat)
    {
        final String unitsFormatAltitudeUnits = unitsFormat.getAltitudeUnits();

        return switch (unitsFormatAltitudeUnits)
        {
            case UnitsFormat.MILES, UnitsFormat.YARDS, UnitsFormat.FEET -> AltitudeUnits.FEET;
            case UnitsFormat.KILOMETERS, UnitsFormat.METERS -> AltitudeUnits.METERS;
            default ->
            {
                logger.warn("The deprecated altitude unit {} is not supported. Defaulting to metric.", unitsFormatAltitudeUnits);
                yield AltitudeUnits.METERS;
            }
        };
    }

    /**
     * Converts {@link UnitsFormat} to {@link RangeUnits}.
     *
     * @param unitsFormat The units format
     * @return The equivalent RangeUnits
     */
    public static RangeUnits toRangeUnits(@Nonnull UnitsFormat unitsFormat)
    {
        final String unitsFormatRangeUnits = unitsFormat.getLengthUnits();

        return switch (unitsFormatRangeUnits)
        {
            case UnitsFormat.MILES, UnitsFormat.YARDS, UnitsFormat.FEET -> RangeUnits.FEET;
            case UnitsFormat.KILOMETERS, UnitsFormat.METERS -> RangeUnits.METERS;
            case UnitsFormat.NAUTICAL_MILES -> RangeUnits.NAUTICAL_MILES;
            default ->
            {
                logger.warn("The deprecated length unit {} is not supported. Defaulting to metric.", unitsFormatRangeUnits);
                yield RangeUnits.METERS;
            }
        };
    }

    /**
     * Converts {@link RangeUnits} to {@link UnitsFormat}.
     *
     * @param rangeUnits The range units to convert
     * @return The (approximate) equivalent UnitsFormat
     */
    public static UnitsFormat toUnitsFormat(@Nonnull RangeUnits rangeUnits)
    {
        return switch (rangeUnits)
        {
            case FEET -> UnitsFormat.FT_FT2;
            case NAUTICAL_MILES -> UnitsFormat.NM_MI2;
            default -> UnitsFormat.M_M2;
        };
    }

    /**
     * Converts {@link RangeUnits} to its {@link UnitsFormat} preference String.
     *
     * @param rangeUnits The range units to convert
     * @return The (approximate) equivalent UnitsFormat preference String
     */
    public static String unitsFormatStringFromRangeUnits(@Nonnull RangeUnits rangeUnits)
    {
        return switch (rangeUnits)
        {
            case FEET -> ILengthMeasurer.FEET__FEET_SQR;
            case NAUTICAL_MILES -> ILengthMeasurer.NAUTICAL_MILES__MILES_SQR;
            default -> ILengthMeasurer.METERS__METERS_SQR;
        };
    }

    /**
     * Converts {@link UnitsFormat} to {@link SpeedUnits}. There's no direct equivalent, so just base it on the unit
     * system.
     *
     * @param unitsFormat The units format
     * @return The equivalent RangeUnits
     */
    public static SpeedUnits toSpeedUnits(@Nonnull UnitsFormat unitsFormat)
    {
        return SpanConversionUtils.speedUnitsFromSpanSystem(unitsFormat.getSpanSystem());
    }

    private UnitsFormatUtils()
    {
    }
}
