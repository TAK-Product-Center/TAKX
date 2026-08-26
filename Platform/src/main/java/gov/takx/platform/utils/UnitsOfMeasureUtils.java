package gov.takx.platform.utils;

import gov.nasa.worldwind.util.WWMath;

import java.util.concurrent.TimeUnit;

/**
 * Utility class used to convert to and from metric units of measure.
 * <p/>
 * All values in this application are stored using metric units of measure. Developers are expected to use this class
 * to convert values in any other unit of measure to their metric equivalents using this utility. This application uses
 * this utility class to perform conversions internally and the constants contained herein represent the values used to
 * perform those conversions. Developers and users should be aware that conversions may result in a loss of precision,
 * as dictated by the limitations of Java's floating point arithmetic and the precision of the values used to perform
 * the conversions.
 *
 * @since 1.14
 */
@SuppressWarnings("unused")
public final class UnitsOfMeasureUtils
{
    public static final long SECONDS_PER_HOUR = TimeUnit.HOURS.toSeconds(1);

    /**
     * Multiplier to convert from miles to kilometers (i.e., the number of kilometers in a mile).
     *
     * @since 3.0
     */
    public static final double KILOMETERS_PER_MILE = WWMath.METERS_TO_KILOMETERS / WWMath.METERS_TO_MILES;

    /**
     * Multiplier to convert from Nautical mile to kilometers (i.e., the number of kilometers in a nautical mile).
     *
     * @since 3.0
     */
    public static final double KILOMETERS_PER_NAUTICAL_MILE = WWMath.METERS_TO_KILOMETERS / WWMath.METERS_TO_NAUTICAL_MILES;

    /**
     * Multiplier to convert from kilometers/hour to meters/second.
     *
     * @since 3.0
     */
    public static final double KPH_TO_MPS_MULTIPLIER = 1000d / SECONDS_PER_HOUR;

    /**
     * Multiplier to convert from meters/second to kilometers/hour.
     *
     * @since 3.0
     */
    public static final double MPS_TO_KPH_MULTIPLIER = SECONDS_PER_HOUR / 1000d;

    /**
     * Multiplier to convert miles/hour to meters/second.
     *
     * @since 3.0
     */
    public final static double MPH_TO_MPS_MULTIPLIER = 1 / WWMath.METERS_TO_MILES / SECONDS_PER_HOUR;

    /**
     * Convert a value in kilometers/hr to meters/sec.
     *
     * @param kph kilometers/hr  to convert
     * @return input converted to meters/sec
     * @since 3.0
     */
    public static double convertKphToMps(double kph)
    {
        return kph * KPH_TO_MPS_MULTIPLIER;
    }

    /**
     * @return The value used to calculate the number of metric kilometers per English mile.
     */
    public static double getKmPerMileMultiplier()
    {
        return KILOMETERS_PER_MILE;
    }

    /**
     * @return The value used to calculate the number of metric kilometers per nautical mile.
     */
    public static double getKmPerNmMultiplier()
    {
        return KILOMETERS_PER_NAUTICAL_MILE;
    }

    /**
     * Converts English miles to metric kilometers.
     *
     * @param miles The English mile value to convert to metric kilometers.
     * @return The metric kilometer equivalent of the value specified in the {@code miles} parameter.
     * @see #getKmPerMileMultiplier()
     */
    public static double convertMilesToKilometers(double miles)
    {
        return miles * KILOMETERS_PER_MILE;
    }

    /**
     * Converts metric kilometers to English miles.
     *
     * @param km The metric kilometer value to convert to English miles.
     * @return The English mile equivalent of the value specified in the {@code km} parameter.
     * @see #getKmPerMileMultiplier()
     */
    public static double convertKilometersToMiles(double km)
    {
        return km / KILOMETERS_PER_MILE;
    }

    /**
     * Converts English square miles to metric square kilometers.
     *
     * @param squareMiles The English square mile value to convert to metric square kilometers.
     * @return The metric square kilometer equivalent of the value specified in the {@code squareMiles} parameter.
     * @see #convertMilesToKilometers(double)
     */
    public static double convertSquareMilesToSquareKilometers(double squareMiles)
    {
        return square(convertMilesToKilometers(Math.sqrt(squareMiles)));
    }

    /**
     * Converts metric square kilometers to English square miles.
     *
     * @param squareKm The metric square kilometer value to convert to English square miles.
     * @return The English square mile equivalent of the value specified in the {@code squareKm} parameter.
     * @see #convertKilometersToMiles(double)
     */
    public static double convertSquareKilometersToSquareMiles(double squareKm)
    {
        return square(convertKilometersToMiles(Math.sqrt(squareKm)));
    }

    /**
     * Converts nautical miles to metric kilometers.
     *
     * @param nm The nautical mile value to convert to metric kilometers.
     * @return The metric kilometer equivalent of the value specified in the {@code nm} parameter.
     * @see #getKmPerNmMultiplier()
     */
    public static double convertNauticalMilesToKilometers(double nm)
    {
        return nm * KILOMETERS_PER_NAUTICAL_MILE;
    }

    /**
     * Converts metric kilometers to nautical miles.
     *
     * @param km The metric kilometer value to convert to nautical miles.
     * @return The nautical mile equivalent of the value specified in the {@code km} parameter.
     * @see #getKmPerMileMultiplier()
     */
    public static double convertKilometersToNauticalMiles(double km)
    {
        return km / KILOMETERS_PER_NAUTICAL_MILE;
    }

    /**
     * Converts nautical square miles to metric square kilometers.
     *
     * @param squareNM The nautical square mile value to convert to metric square kilometers.
     * @return The metric square kilometer equivalent of the value specified in the {@code squareNM} parameter.
     * @see #convertNauticalMilesToKilometers(double)
     */
    public static double convertSquareNauticalMilesToSquareKm(double squareNM)
    {
        return square(convertNauticalMilesToKilometers(Math.sqrt(squareNM)));
    }

    /**
     * Converts metric square kilometers to nautical square miles.
     *
     * @param squareKm The metric square kilometer value to convert to nautical square miles.
     * @return The nautical square mile equivalent of the value specified in the {@code squareKm} parameter.
     * @see #convertKilometersToNauticalMiles(double)
     */
    public static double convertSquareKilometersToSquareNauticalMiles(double squareKm)
    {
        return square(convertKilometersToNauticalMiles(Math.sqrt(squareKm)));
    }

    /**
     * Converts metric meters to English feet.
     *
     * @param meters The metric meter value to convert to English feet.
     * @return The English feet equivalent of the value specified in {@code meters}.
     */
    public static double convertMetersToFeet(double meters)
    {
        return meters * WWMath.METERS_TO_FEET;
    }

    /**
     * Converts English feet to metric meters.
     *
     * @param feet The English feet value to convert to metric meters.
     * @return The metric meters equivalent of the value specified in {@code feet}.
     */
    public static double convertFeetToMeters(double feet)
    {
        return feet / WWMath.METERS_TO_FEET;
    }

    private static double square(double root)
    {
        return root * root;
    }
}
