package gov.takx.platform.utils.units;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.NorthReference;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;

/**
 * Defines the set of available bearing units-of-measure and provides conversion utility methods. Essentially wraps
 * {@link Angle} to make interactions consistent with other units.
 *
 * @since 5.5
 */
public enum BearingUnits
{
    DEGREE(Angle.DEGREE, 1),
    MIL(Angle.MIL, 0),
    RADIAN(Angle.RADIAN, 2);

    private final Angle angle;
    private final int decimalPoints; // default if not specified

    BearingUnits(@Nonnull Angle angle, int decimalPoints)
    {
        this.angle = angle;
        this.decimalPoints = decimalPoints;
    }

    /**
     * @return The angle equivalent of this bearing unit
     */
    public Angle getAngle()
    {
        return angle;
    }

    /**
     * @return The unit's display name
     */
    public String getDisplayName()
    {
        return angle.getName();
    }

    /**
     * @return The abbreviation that should be displayed to the user
     */
    public String getAbbreviation()
    {
        return angle.getAbbrev();
    }

    /**
     * @return The default number of decimal places used when formatting bearings with this bearing unit
     * @see #format(IGeoPoint, double, double) 
     */
    public int getDefaultDecimals()
    {
        return decimalPoints;
    }

    /**
     * Converts a bearing to the unit selected by the user using the unit's default number of decimal places.
     *
     * @param location the location
     * @param range    the range
     * @param bearing  the bearing
     * @return The text to use when displaying the bearing using the default number of decimal places
     * @see NorthReference#format(double, GeoPoint, Angle, NorthReference, int)
     */
    public String format(@Nonnull IGeoPoint location, double range, double bearing)
    {
        return format(location, range, bearing, decimalPoints);
    }

    /**
     * Converts a bearing to the unit selected by the user with the specified number of decimal places.
     *
     * @param location the location
     * @param range    the range
     * @param bearing  the bearing
     * @param decimalPoints The number of decimal points to include
     * @return The text to use when displaying the bearing
     * @see NorthReference#format(double, GeoPoint, Angle, NorthReference, int)
     */
    public String format(@Nonnull IGeoPoint location, double range, double bearing, int decimalPoints)
    {
        return NorthReference.format(bearing, location, range, angle,
                UserPreferenceEntry.DISPLAY_NORTH_REFERENCE.getValueAs(NorthReference.class), decimalPoints);
    }

    /**
     * @param angle The angle
     * @return The equivalent bearing units
     */
    public static BearingUnits from(@Nonnull Angle angle)
    {
        return angle == Angle.MIL ? MIL : angle == Angle.RADIAN ? RADIAN : DEGREE;
    }

    @Override
    public String toString()
    {
        return getDisplayName();
    }

    /**
     * Represents 16 compass directions (4 cardinal, 4 primary intercardinal, and 8 secondary intercardinal). While this
     * could in theory be considered a proper {@link BearingUnits} instance, it is almost certainly too inaccurate to be
     * useful as a primary unit. However, there are parts of the UI where it is convenient for users to see this value, so
     * this class exists to handle the conversion.
     *
     * @since 5.6
     */
    public enum Direction
    {
        N, NNE, NE, ENE, E, ESE, SE, SSE, S, SSW, SW, WSW, W, WNW, NW, NNW;

        public static final Direction[] VALUES = values();

        /**
         * @param degrees the bearing degrees to convert
         * @return the corresponding compass direction
         */
        @Nonnull
        public static Direction fromDegrees(double degrees)
        {
            return VALUES[(int) Math.round(degrees / (360d / 16)) % 16];
        }

        /**
         * @param degrees the bearing degrees to convert
         * @return the corresponding compass direction, limited to the 4 cardinal and 4 primary intercardinal directions
         */
        @Nonnull
        public static Direction fromDegrees8(double degrees)
        {
            return values()[((int) Math.round(degrees / (360d / 8)) % 8) * 2];
        }
    }
}
