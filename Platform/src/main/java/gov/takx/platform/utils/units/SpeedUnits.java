package gov.takx.platform.utils.units;

import com.atakmap.coremap.conversions.ConversionFactors;
import gov.takx.api.annotation.Experimental;
import jakarta.annotation.Nonnull;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Defines the set of available speed units-of-measure and provides conversion utility methods.
 *
 * @since 5.5
 */
@Experimental
public enum SpeedUnits
{
    MPH("Miles per Hour (mph)", "mph", ConversionFactors.METERS_PER_S_TO_MILES_PER_H),
    KMPH("Kilometers per Hour (kmph)", "kmph", ConversionFactors.METERS_PER_S_TO_KILOMETERS_PER_H),
    KTS("KGS (kts)", "kts", ConversionFactors.METERS_PER_S_TO_KNOTS),
    MPS("Meters per Second (mps)", "mps", 1.0);

    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = ThreadLocal.withInitial(() -> {
        var format = new DecimalFormat(",##0");
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    });
    private final String displayName;
    private final String abbreviation;
    private final double conversionFactor;

    SpeedUnits(@Nonnull String displayName, @Nonnull String abbreviation, double conversionFactor)
    {
        this.displayName = displayName;
        this.abbreviation = abbreviation;
        this.conversionFactor = conversionFactor;
    }

    /**
     * @return The name that should be displayed to the user
     */
    @Nonnull
    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * @return The abbreviation that should be displayed to the user
     */
    @Nonnull
    public String getAbbreviation()
    {
        return abbreviation;
    }

    /**
     * @return The multiplier required to convert a value from meter-per-second to this range's unit of measure.
     */
    public double getMultiplier()
    {
        return conversionFactor;
    }

    /**
     * Converts meters-per-second to the speed unit selected by the user as a decimal or a whole number, depending on
     * the {@code decimalPoints} parameter value.
     *
     * @param metersPerSecond The value to convert and format
     * @param decimalPoints The number of decimal points to include
     * @return The display string
     */
    public String format(double metersPerSecond, int decimalPoints)
    {
        final double value = metersPerSecond * getMultiplier();

        return getFormatter(decimalPoints).format(value) + " " + getAbbreviation();
    }

    @Override
    public String toString()
    {
        return displayName;
    }

    /**
     * @param decimalPoints The number of decimal places to include
     * @return A thread local decimal formatter
     */
    private DecimalFormat getFormatter(int decimalPoints)
    {
        final var decimalFormat = DECIMAL_FORMAT.get();

        if (decimalFormat.getMinimumFractionDigits() != decimalPoints)
        {
            decimalFormat.setMinimumFractionDigits(decimalPoints);
            decimalFormat.setMaximumFractionDigits(decimalPoints);
        }

        return decimalFormat;
    }
}
