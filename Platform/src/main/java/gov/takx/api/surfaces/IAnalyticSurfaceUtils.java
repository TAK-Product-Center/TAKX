package gov.takx.api.surfaces;

/**
 * Common set of Analytic Surface mathematical utilities.
 *
 * @since 1.4
 */
public interface IAnalyticSurfaceUtils
{
    public static final double SECOND_TO_MILLIS = 1000.0;
    public static final double MINUTE_TO_MILLIS = 60000.0;
    public static final double HOUR_TO_MILLIS = 3600000.0;

    public static final double METERS_TO_KILOMETERS = 1e-3;
    public static final double METERS_TO_MILES = 0.000621371192;
    public static final double METERS_TO_NAUTICAL_MILES = 0.000539956803;
    public static final double METERS_TO_YARDS = 1.0936133;
    public static final double METERS_TO_FEET = 3.280839895;

    public static final double SQUARE_METERS_TO_SQUARE_KILOMETERS = 1e-6;
    public static final double SQUARE_METERS_TO_SQUARE_MILES = 3.86102159e-7;
    public static final double SQUARE_METERS_TO_SQUARE_YARDS = 1.19599005;
    public static final double SQUARE_METERS_TO_SQUARE_FEET = 10.7639104;
    public static final double SQUARE_METERS_TO_HECTARES = 1e-4;
    public static final double SQUARE_METERS_TO_ACRES = 0.000247105381;

    IBufferWrapper createBuffer(int numValues);

    /**
     * Convenience method to compute the log base 2 of a value.
     *
     * @param value the value to take the log of.
     * @return the log base 2 of the specified value.
     */
    double logBase2(double value);

    /**
     * Convenience method for testing whether a value is a power of two.
     *
     * @param value the value to test for power of 2
     * @return true if power of 2, else false
     */
    boolean isPowerOfTwo(int value);

    /**
     * Returns the value that is the nearest power of 2 greater than or equal to the given value.
     *
     * @param reference the reference value. The power of 2 returned is greater than or equal to this value.
     * @return the value that is the nearest power of 2 greater than or equal to the reference value
     */
    int powerOfTwoCeiling(int reference);

    /**
     * Returns the value that is the nearest power of 2 less than or equal to the given value.
     *
     * @param reference the reference value. The power of 2 returned is less than or equal to this value.
     * @return the value that is the nearest power of 2 less than or equal to the reference value
     */
    int powerOfTwoFloor(int reference);

    /**
     * Clamps a value to a given range.
     *
     * @param v   the value to clamp.
     * @param min the floor.
     * @param max the ceiling
     * @return the nearest value such that min &lt;= v &lt;= max.
     */
    double clamp(double v, double min, double max);

    /**
     * Returns the interpolation factor for {@code v} given the specified range {@code [x, y]}. The
     * interpolation factor is a number between 0 and 1 (inclusive), representing the value's relative position between
     * {@code x} and {@code y}. For example, 0 corresponds to {@code x}, 1 corresponds to {@code y},
     * and anything in between corresponds to a linear combination of {@code x} and {@code y}.
     *
     * @param v the value to compute the interpolation factor for.
     * @param x the first value.
     * @param y the second value.
     * @return the interpolation factor for {@code v} given the specified range {@code [x, y]}
     */
    double computeInterpolationFactor(double v, double x, double y);

    /**
     * Returns the linear interpolation of {@code x} and {@code y} according to the function: {@code (1 - a) *
     * x + a * y}. The interpolation factor {@code a} defines the weight given to each value, and is clamped
     * to the range [0, 1]. If {@code a} is 0 or less, this returns x. If {@code a} is 1 or more, this returns
     * {@code y}. Otherwise, this returns the linear interpolation of {@code x} and {@code y}. For
     * example, when {@code a} is {@code 0.5} this returns {@code (x + y)/2}.
     *
     * @param a the interpolation factor.
     * @param x the first value.
     * @param y the second value.
     * @return the linear interpolation of {@code x} and {@code y}.
     */
    double mix(double a, double x, double y);

    /**
     * Returns the smooth hermite interpolation of {@code x} and {@code y} according to the function: {@code (1
     * - t) * x + t * y}, where {@code t = a * a * (3 - 2 * a)}. The interpolation factor {@code a}
     * defines the weight given to each value, and is clamped to the range [0, 1]. If {@code a} is 0 or less, this
     * returns {@code x}. If {@code a} is 1 or more, this returns {@code y}. Otherwise, this returns the
     * smooth hermite interpolation of {@code x} and {@code y}. Like the linear function {@link #mix(double,
     * double, double)}, when {@code a} is {@code 0.5} this returns {@code (x + y)/2}. But unlike the
     * linear function, the hermite function's slope gradually increases when {@code a} is near 0, then gradually
     * decreases when {@code a} is near 1. This is a useful property where a more gradual transition from
     * {@code x} to {@code y} is desired.
     *
     * @param a the interpolation factor.
     * @param x the first value.
     * @param y the second value.
     * @return the smooth hermite interpolation of {@code x} and {@code y}.
     */
    double mixSmooth(double a, double x, double y);

    /**
     * converts meters to feet.
     *
     * @param meters the value in meters.
     * @return the value converted to feet.
     */
    double convertMetersToFeet(double meters);

    /**
     * converts meters to miles.
     *
     * @param meters the value in meters.
     * @return the value converted to miles.
     */
    double convertMetersToMiles(double meters);

    /**
     * Converts time in seconds to time in milliseconds.
     *
     * @param seconds time in seconds.
     * @return time in milliseconds.
     */
    double convertSecondsToMillis(double seconds);

    /**
     * Converts time in milliseconds to time in seconds.
     *
     * @param millis time in milliseconds.
     * @return time in seconds.
     */
    double convertMillisToSeconds(double millis);

    /**
     * Converts time in minutes to time in milliseconds.
     *
     * @param minutes time in minutes.
     * @return time in milliseconds.
     */
    double convertMinutesToMillis(double minutes);

    /**
     * Converts time in milliseconds to time in minutes.
     *
     * @param millis time in milliseconds.
     * @return time in minutes.
     */
    double convertMillisToMinutes(double millis);

    /**
     * Converts time in hours to time in milliseconds.
     *
     * @param hours time in hours.
     * @return time in milliseconds.
     */
    double convertHoursToMillis(double hours);

    /**
     * Converts time in milliseconds to time in hours.
     *
     * @param mills time in milliseconds.
     * @return time in hours.
     */
    double convertMillisToHours(double mills);
}
