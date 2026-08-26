package gov.takx.platform.utils;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

/**
 * Supports conversion between bearing in degrees and the ordinal direction name for the 8 cardinal points.
 *
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public enum DirectionType
{
    NORTH("N", 0),
    NORTHEAST("NE", 45),
    EAST("E", 90),
    SOUTHEAST("SE", 135),
    SOUTH("S", 180),
    SOUTHWEST("SW", 225),
    WEST("W", 270),
    NORTHWEST("NW", 315),
    UNKNOWN("UNK", Double.NaN);

    private static final DirectionType[] VALUES = values();

    private final String abbreviation;
    private final double bearing;

    DirectionType(String abbreviation, double bearing)
    {
        this.bearing = bearing;
        this.abbreviation = abbreviation;
    }

    /**
     * @return The ordinal abbreviation of this direction type
     */
    public String getAbbreviation()
    {
        return abbreviation;
    }

    /**
     * @return The canonical bearing of this direction type
     */
    public double getBearing()
    {
        return bearing;
    }

    /**
     * @param bearing the value in degrees from 1-360
     * @return the direction type for the given {@code bearing}
     */
    @Nonnull
    public static DirectionType getDirectionType(double bearing)
    {
        if (Double.isNaN(bearing)) return UNKNOWN;
        if (bearing < 0) bearing += 360;

        bearing += 22.5;
        final int direction = (int) (bearing / 45) % (VALUES.length - 1);
        return VALUES[direction];
    }

    /**
     * @param bearing the value in degrees from 1-360
     * @return the direction abbreviation for the given {@code bearing}
     */
    @Nonnull
    public static String getDirectionAbbreviation(double bearing)
    {
        return getDirectionType(bearing).getAbbreviation();
    }
}
