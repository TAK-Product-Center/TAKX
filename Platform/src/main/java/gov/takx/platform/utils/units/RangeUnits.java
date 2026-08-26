package gov.takx.platform.utils.units;

import com.atakmap.coremap.conversions.ConversionFactors;
import gov.takx.api.annotation.Experimental;
import jakarta.annotation.Nonnull;

import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * Defines the set of available range units-of-measure and provides conversion utility methods.
 * <p>
 * Each unit defines a "large" threshold, retrievable via {@link #getLargeThreshold()} and {@link #getLargeAreaThreshold()}).
 * If the meters value exceeds this threshold, {@link #format(double, int)} will provide a String that uses the large
 * version of the unit, e.g. kilometers instead of meters or miles instead of feet. The various getters that take {@code meters}
 * as a parameter will also return their unit's large value equivalent when the threshold is exceeded. For
 * example {@link #FEET}.{@link #getMultiplier()} will always return the multiplier required to convert meters to feet,
 * while {@link #FEET}.{@link #getMultiplier(double)} will return the multiplier required to convert meters to miles if
 * meters is over the threshold.
 * <p>
 * If manually formatting text rather than using {@link #format(double, int)}, callers should use the same type of
 * getters (with or without the meters parameter) together:
 * <pre> {@code
 * double meters = 2100;
 * String a = meters * FEET.getMultiplier() + " " + FEET.getAbbreviation() // "6889 ft"
 * String b = meters * FEET.getMultiplier(meters) + " " + FEET.getAbbreviation(meters) // "1.3048795032 mi"
 * String c = FEET.format(meters, 1) // "1.3 mi"
 * }
 * </pre>
 *
 * @since 5.5
 */
@Experimental
public enum RangeUnits
{
    METERS("Meters/Kilometers (m/km)", "meters", "kilometers",
            "m", "km", "m²", "km²",
            1.0, ConversionFactors.METERS_TO_KM, 2000, 100_000.0),
    FEET("Feet/Miles (ft/mi)", "feet", "miles",
            "ft", "mi", "ft²", "mi²",
            ConversionFactors.METERS_TO_FEET, ConversionFactors.METERS_TO_MILES, ConversionFactors.MILES_TO_METERS,
            92_902), // ~999,999 sq ft
    NAUTICAL_MILES("Nautical Miles (nm)", "nautical miles", "nautical miles",
            "nm", "nm", "nm²", "nm²",
            ConversionFactors.METERS_TO_NM, ConversionFactors.METERS_TO_NM, Double.MAX_VALUE, Double.MAX_VALUE);

    private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = ThreadLocal.withInitial(() -> {
        var format = new DecimalFormat(",##0");
        format.setRoundingMode(RoundingMode.HALF_UP);
        return format;
    });
    private static final int IGNORE_SIZE = 0; // Pass when thresholds should be ignored

    private final String displayName;
    private final String unitName;
    private final String largeUnitName;
    private final String abbreviation;
    private final String largeAbbreviation;
    private final String areaAbbreviation;
    private final String largeAreaAbbreviation;
    private final double conversionFactor;
    private final double largeConversionFactor;
    private final double areaConversionFactor;
    private final double largeAreaConversionFactor;
    private final double largeThreshold;
    private final double largeAreaThreshold;

    RangeUnits(@Nonnull String displayName, @Nonnull String unitName, @Nonnull String largeUnitName,
               @Nonnull String abbreviation, @Nonnull String largeAbbreviation,
               @Nonnull String areaAbbreviation, @Nonnull String largeAreaAbbreviation,
               double conversionFactor, double largeConversionFactor, double largeThreshold, double largeAreaThreshold)
    {
        this.displayName = displayName;
        this.unitName = unitName;
        this.largeUnitName = largeUnitName;
        this.abbreviation = abbreviation;
        this.largeAbbreviation = largeAbbreviation;
        this.areaAbbreviation = areaAbbreviation;
        this.largeAreaAbbreviation = largeAreaAbbreviation;
        this.conversionFactor = conversionFactor;
        this.largeConversionFactor = largeConversionFactor;
        this.largeThreshold = largeThreshold;
        this.largeAreaThreshold = largeAreaThreshold;

        areaConversionFactor = Math.pow(conversionFactor, 2);
        largeAreaConversionFactor = Math.pow(largeConversionFactor, 2);
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
     * @return The unit name that should be displayed to the user if no unit adjustments will be made based on size 
     * (e.g. meters will not be converted to kilometers if over a threshold)
     */
    @Nonnull
    public String getUnitName()
    {
        return getUnitName(IGNORE_SIZE);
    }

    /**
     * @param meters the meters that will be converted
     * @return The unit name that should be displayed to the user if the unit will be converted to its large variation
     * if over the large unit {@link #getLargeThreshold() threshold} using {@link #getMultiplier(double)}
     */
    @Nonnull
    public String getUnitName(double meters)
    {
        return meters < largeThreshold ? unitName : largeUnitName;
    }

    /**
     * @return The abbreviation that should be displayed to the user if no unit adjustments will be made based on size 
     * (e.g. meters will not be converted to kilometers if over a threshold)
     */
    @Nonnull
    public String getAbbreviation()
    {
        return getAbbreviation(IGNORE_SIZE);
    }

    /**
     * @param meters the meters that will be converted
     * @return The abbreviation that should be displayed to the user if the unit will be converted to its large variation
     * if over the large unit {@link #getLargeThreshold() threshold} using {@link #getMultiplier(double)}
     */
    @Nonnull
    public String getAbbreviation(double meters)
    {
        return meters < largeThreshold ? abbreviation : largeAbbreviation;
    }

    /**
     * @return The area abbreviation that should be displayed to the user if no unit adjustments will be made based on 
     * size (e.g. square meters will not be converted to square kilometers if over a threshold)
     */
    @Nonnull
    public String getAreaAbbreviation()
    {
        return getAreaAbbreviation(IGNORE_SIZE);
    }

    /**
     * @param squareMeters The square meters that will be converted
     * @return The area abbreviation that should be displayed to the user if the unit will be converted to its large 
     * variation if over the large unit area {@link #getLargeAreaThreshold() threshold} using {@link #getAreaMultiplier(double)}
     */
    @Nonnull
    public String getAreaAbbreviation(double squareMeters)
    {
        return squareMeters < largeAreaThreshold ? areaAbbreviation : largeAreaAbbreviation;
    }

    /**
     * @return The multiplier that should be used to convert meters to this unit without making adjustments based on 
     * size (e.g. meters will not be converted to kilometers if over a threshold)
     */
    public double getMultiplier()
    {
        return getMultiplier(IGNORE_SIZE);
    }

    /**
     * @param meters The meters that will be converted
     * @return The multiplier that should be used to convert meters to this unit of measure. If {@code meters} is over 
     * the large {@link #getLargeThreshold() threshold}, the large unit multiplier will be returned. This method should 
     * be used in conjunction with {@link #getUnitName(double)} or {@link #getAbbreviation(double)}. 
     */
    public double getMultiplier(double meters)
    {
        return meters < largeThreshold ? conversionFactor : largeConversionFactor;
    }

    /**
     * @return The multiplier that should be used to convert meters to this unit without making adjustments based on 
     * size (e.g. meters will not be converted to kilometers if over a threshold)
     */
    public double getAreaMultiplier()
    {
        return getAreaMultiplier(IGNORE_SIZE);
    }

    /**
     * @param squareMeters The square meters that will be converted
     * @return The multiplier that should be used to convert square meters to this unit of measure. If 
     * {@code squareMeters} is over the large area {@link #getLargeAreaThreshold() threshold}, the large area
     * multiplier will be returned. This method should be used in conjunction with {@link #getUnitName(double)} or 
     * {@link #getAbbreviation(double)}. 
     */
    public double getAreaMultiplier(double squareMeters)
    {
        return squareMeters < largeAreaThreshold ? areaConversionFactor : largeAreaConversionFactor;
    }

    /**
     * @return The threshold (in meters) at which meters will be converted to this unit's large unit variation when
     * calling {@link #getMultiplier(double)}, {@link #getUnitName(double)} or {@link #getAbbreviation(double)}.
     */
    public double getLargeThreshold()
    {
        return largeThreshold;
    }

    /**
     * @return The threshold (in square meters) at which square meters will be converted to this unit's large unit
     * variation when calling {@link #getAreaMultiplier(double)} or {@link #getAreaAbbreviation(double)}.
     */
    public double getLargeAreaThreshold()
    {
        return largeAreaThreshold;
    }

    /**
     * Converts meters to the range unit selected by the user as a decimal or a whole number, depending on the
     * {@code decimalPoints} parameter value. If metric units are selected, the value will be displayed in meters if
     * less than the threshold set in {@link #largeThreshold} or in kilometers if above the threshold. If English units
     * are selected, the value will be displayed in feet if under the threshold, or miles if over it.
     *
     * @param meters The meters to convert
     * @param decimalPoints The number of decimal points to include, between 0 and 4
     * @return The display string
     */
    public String format(double meters, int decimalPoints)
    {
        final double range = meters * getMultiplier(meters);

        return getFormatter(decimalPoints).format(range) + " " + getAbbreviation(meters);
    }

    /**
     * Converts square meters to the range unit selected by the user as a decimal or a whole number, depending on the
     * {@code decimalPoints} parameter value. If metric units are selected, the value will be displayed in square meters
     * if less than the threshold set in {@link #largeAreaThreshold} or in square kilometers if above the threshold. If
     * English units are selected, the value will be displayed in square feet if under the threshold, or square miles if
     * over it.
     *
     * @param squareMeters The area to convert
     * @param decimalPoints The number of decimal points to include, between 0 and 4
     * @return The display string
     */
    public String formatArea(double squareMeters, int decimalPoints)
    {
        final double area = squareMeters * getAreaMultiplier(squareMeters);

        return getFormatter(decimalPoints).format(area) + " " + getAreaAbbreviation(squareMeters);
    }

    @Override
    public String toString()
    {
        return displayName;
    }

    /**
     * @param decimalPoints The number of decimal places to display
     * @return This thread's decimal formatter configured to display the requested number of decimal points
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
