package gov.takx.api.coordinatesystem;

import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.coords.MGRSCoord;
import gov.nasa.worldwind.util.WWMath;
import gov.tak.api.maps.coords.GeoPoint;
import gov.takx.platform.utils.units.AltitudeUnits;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;

import java.lang.invoke.MethodHandles;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 */
public enum CoordinateSystem
{
    DECIMAL_DEGREES("Decimal Degrees"),
    DM_DEGREES("DM Degrees"),
    DMS_DEGREES("DMS Degrees"),
    MGRS("MGRS");
    // TODO KMB: Will need to add UTM and address for parity with ATAK/WinTAK

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String name;

    private static final CoordinateSystem[] VALUES = values();

    /**
     * Default string returned when a location can not be properly formatted.
     *
     * @since 3.0
     */
    public static final String NO_LOCATION = "No Location";

    private static final String DECIMAL_DEGREES_STRING = "Lat 33.27\u00B0 Lon -85.73\u00B0 Elev 254m";
    private static final String DM_DEGREES_STRING = "Lat 9\u00B045.461\u2019 Lon -121\u00B031.547\u2019 Elev 5m";
    private static final String DMS_DEGREES_STRING = "Lat 32\u00B042\u20199.2\u201d Lon -114\u00B054\u201930.8\u201d Elev 254m";
    private static final String MGRS_STRING = "09QUB 59649 94501 Elev 115m";

    // Regex to match an elevation specification, e.g. " Elev 123m"
    private static final String ELEVATION_REGEX = "\\s+elev\\s+([-+]?(?:\\d{1,3}(?:,\\d{3})+|\\d+)(\\.\\d+)?)\\s*([mkf])?\\s*";
    private static final Pattern ELEVATION_PTTN = Pattern.compile(ELEVATION_REGEX, Pattern.CASE_INSENSITIVE);

    // MGRS location regex allowing whitespace.
    private static final String MGRS_REGEX = "(\\d{1,2}[A-Za-z])\\s*([A-Za-z]{2})\\s*(\\d{1,5})\\s*(\\d{1,5})";
    private static final Pattern MGRS_PTTN = Pattern.compile(MGRS_REGEX);

    // Separators used in several lat/long formats
    private static final String LAT_LONG_SEPARATORS = "(\\s+|\\s*,\\s*)";
    private static final String DECIMAL_NUM_REGEX = "\\s*\\d+(\\.\\d+)?";

    // cardinal directions always optional
    private static final String NORTH_SOUTH_REGEX = "\\s*[NS]?";
    private static final String EAST_WEST_REGEX = "\\s*[EW]?";

    // degree symbols always required in DMS/DMM, optional in DD
    private static final String DEGREE_SYMBOLS = "D*\u00B0"; // d, D, *, and the unicode degree symbol
    private static final String DEGREE_SYMBOLS_REQUIRED_REGEX = "[" + DEGREE_SYMBOLS + "]";
    private static final String DEGREE_SYMBOLS_OPTIONAL_REGEX = "[" + DEGREE_SYMBOLS + "]?";

    // minute symbols always required in DMS/DMM
    private static final String MINUTE_SYMBOLS = "'\u2019"; // ' (apostrophe) and the unicode designation of one
    private static final String MINUTE_SYMBOLS_REGEX = "[" + MINUTE_SYMBOLS + "]";

    // minute symbols always required in DMS
    private static final String SECOND_SYMBOLS = "\"\u201d"; // " (quotes) and the unicode designation of one
    private static final String SECOND_SYMBOLS_REGEX = "[" + SECOND_SYMBOLS + "]";

    // LAT/LON always optional
    private static final String LAT_LABEL_REGEX = "(LAT)?\\s*";
    private static final String LON_LABEL_REGEX = "(LON)?\\s*";

    private static final String LAT_GROUP_LABEL = "LAT";
    private static final String LATSIGN_GROUP_LABEL = "LATSIGN";
    private static final String LATCARD_GROUP_LABEL = "LATCARD";

    private static final String LON_GROUP_LABEL = "LON";
    private static final String LONSIGN_GROUP_LABEL = "LONSIGN";
    private static final String LONCARD_GROUP_LABEL = "LONCARD";

    // Location regex to match a pair of signed decimal values separated by white space or a comma (with optional white space on each side of the comma).
    // Allows degree symbol & E, W, S, N suffixes.  Implied order is LAT then LON.
    private static final String DECIMAL_LAT_LONG_REGEX =
            LAT_LABEL_REGEX + "(?<" + LAT_GROUP_LABEL + ">(?<" + LATSIGN_GROUP_LABEL + ">[-+]?)(" + DECIMAL_NUM_REGEX + DEGREE_SYMBOLS_OPTIONAL_REGEX + "(?<" + LATCARD_GROUP_LABEL + ">" + NORTH_SOUTH_REGEX + ")))" + LAT_LONG_SEPARATORS +
                    LON_LABEL_REGEX + "(?<" + LON_GROUP_LABEL + ">(?<" + LONSIGN_GROUP_LABEL + ">[-+]?)(" + DECIMAL_NUM_REGEX + DEGREE_SYMBOLS_OPTIONAL_REGEX + "(?<" + LONCARD_GROUP_LABEL + ">" + EAST_WEST_REGEX + ")))";
    private static final Pattern DECIMAL_LAT_LONG_PTTN = Pattern.compile(DECIMAL_LAT_LONG_REGEX, Pattern.CASE_INSENSITIVE);

    // Regex to match a "degrees minutes seconds" string
    // eg: -123* 34' 42.2"     +45d 12' 30.2"
    // eg:  123D 34' 42.2"S     45° 12' 30.2"W
    private static final String DMS_REGEX = "\\s*(\\d{1,3}" + DEGREE_SYMBOLS_REQUIRED_REGEX + ")\\s*(\\d{1,2}\\s*" + MINUTE_SYMBOLS_REGEX + ")\\s*(\\d{1,2}(\\.\\d+)?\\s*" + SECOND_SYMBOLS_REGEX + ")";

    // Regex to match a "degrees decimal minutes" string
    // eg: -123* 34.2'     +45d 12.2'
    // eg:  123D 34.2'S     45° 12.2'W
    private static final String DMM_REGEX = "\\s*(\\d{1,3}" + DEGREE_SYMBOLS_REQUIRED_REGEX + ")\\s*(\\d{1,2}(\\.\\d+)?\\s*" + MINUTE_SYMBOLS_REGEX + ")";

    // separate DMS/DMM to keep code simple & because named subgroups have to be unique
    private static final String DMS_LAT_REGEX = LAT_LABEL_REGEX + "(?<" + LAT_GROUP_LABEL + ">(?<" + LATSIGN_GROUP_LABEL + ">[-+]?)" + DMS_REGEX + "(?<" + LATCARD_GROUP_LABEL + ">" + NORTH_SOUTH_REGEX + "))";
    private static final String DMS_LON_REGEX = LON_LABEL_REGEX + "(?<" + LON_GROUP_LABEL + ">(?<" + LONSIGN_GROUP_LABEL + ">[-+]?)" + DMS_REGEX + "(?<" + LONCARD_GROUP_LABEL + ">" + EAST_WEST_REGEX + "))";
    private static final String DMS_LAT_LONG_REGEX = DMS_LAT_REGEX + LAT_LONG_SEPARATORS + DMS_LON_REGEX;
    private static final Pattern DMS_LAT_LONG_PTTN = Pattern.compile(DMS_LAT_LONG_REGEX, Pattern.CASE_INSENSITIVE);

    private static final String DMM_LAT_REGEX = LAT_LABEL_REGEX + "(?<" + LAT_GROUP_LABEL + ">(?<" + LATSIGN_GROUP_LABEL + ">[-+]?)" + DMM_REGEX + "(?<" + LATCARD_GROUP_LABEL + ">" + NORTH_SOUTH_REGEX + "))";
    private static final String DMM_LON_REGEX = LON_LABEL_REGEX + "(?<" + LON_GROUP_LABEL + ">(?<" + LONSIGN_GROUP_LABEL + ">[-+]?)" + DMM_REGEX + "(?<" + LONCARD_GROUP_LABEL + ">" + EAST_WEST_REGEX + "))";
    private static final String DMM_LAT_LONG_REGEX = DMM_LAT_REGEX + LAT_LONG_SEPARATORS + DMM_LON_REGEX;
    private static final Pattern DMM_LAT_LONG_PTTN = Pattern.compile(DMM_LAT_LONG_REGEX, Pattern.CASE_INSENSITIVE);

    // Format elevation, including up to 2 decimal places if relevant.
    private static final DecimalFormat ELEVATION_STRING_FORMAT = new DecimalFormat("0.00");

    // In the case where the user inputs an invalid string, we want to try to guess what the user was trying to input using
    // a more lenient regex pattern to show them the proper formatting
    private static final String FUZZY_MATCH_MGRS_REGEX = "\\d{1,2}[A-Za-z]+[\\w\\W]*";
    private static final Pattern FUZZY_MATCH_MGRS_PTTN = Pattern.compile(FUZZY_MATCH_MGRS_REGEX);
    private static final String FUZZY_MATCH_DECIMAL_REGEX = "^-?[0-9]+\\.?\\d*[\\w\\W]*";
    private static final Pattern FUZZY_MATCH_DECIMAL_PTTN = Pattern.compile(FUZZY_MATCH_DECIMAL_REGEX);
    private static final String FUZZY_MATCH_DMS_REGEX = "^-?[0-9]+\\.?\\d*[\\w\\W]*[\\°\\'\\\"]+[\\w\\W]*";
    private static final Pattern FUZZY_MATCH_DMS_PTTN = Pattern.compile(FUZZY_MATCH_DMS_REGEX);

    /**
     * An HTML table element containing descriptions &amp; examples of acceptable formats to use for location input fields
     * to be parsed by this class.
     */
    public static final String LOCATION_STRING_FORMAT_TABLE =
            "<table>" +
                    "<tr>" +
                    "<td colspan=\"2\">Locations can be in any of the following formats, where items enclosed in [ ] are optional</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td colspan=\"2\">Pattern matching is case insensitive for all characters</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td>&bull; Decimal degrees (DD):</td>" +
                    "<td>[Lat] 33.27[&deg;][,] [Lon] -85.73[&deg;][ Elev 254m]</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td>&bull; Degrees Decimal Minutes (DMM):</td>" +
                    "<td>[Lat] 9&deg;45.461&rsquo;[,] [Lon] -121&deg;31.547&rsquo;[ Elev 5m]</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td>&bull; Degrees Minutes (Decimal) Seconds (DMS):</td>" +
                    "<td>[Lat] 32&deg;42&rsquo;9.2&quot;[,] [Lon] -114&deg;54&rsquo;30.8&quot;[ Elev 254m]</td>" +
                    "</tr>" +
                    "<tr>" +
                    "<td>&bull; MGRS:</td>" +
                    "<td>09QUB 59649 94501[ Elev 115m]</td>" +
                    "</tr>" +
                    "</table>";

    /**
     * Constructor
     *
     * @param name Name of coordinate system.
     */
    CoordinateSystem(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String toString()
    {
        return name;
    }

    public static CoordinateSystem getCoordinateSystem(String coordinateSystemString)
            throws UnknownCoordinateSystemException
    {
        for (CoordinateSystem coordSys : VALUES)
        {
            if (coordinateSystemString.equals(coordSys.getName()) || coordinateSystemString.equals(coordSys.name()))
            {
                return coordSys;
            }
        }

        // If we get here, then we didn't find a match
        throw new UnknownCoordinateSystemException(coordinateSystemString);
    }

    /**
     * Get the CoordinateSystem named in the coordinateSystemName parameter.  If that name is not recognized,
     * spit out an error and return the default value provided.
     *
     * @param coordinateSystemName Name of coordinate system
     * @param defaultCS            Default CoordinateSystem to use if lookup fails
     * @return CoordinateSystem
     */
    public static CoordinateSystem getCoordinateSystem(String coordinateSystemName, CoordinateSystem defaultCS)
    {
        CoordinateSystem system = defaultCS;

        try
        {
            system = getCoordinateSystem(coordinateSystemName);
        } catch (UnknownCoordinateSystemException e)
        {
            logger.error("Unable to resolve requested coordinate system: {}", coordinateSystemName, e);
        }

        return system;
    }

    /**
     * @param location The location; if {@code null} or an {@link GeoPoint#UNKNOWN_POINT}, the {@link #NO_LOCATION} 
     *                 string will be returned
     * @param coordinateSystem The coordinate system to use when formatting the coordinates
     * @param altitudeUnits The altitude units to use when formatting the location elevation
     * @return The location string, or the {@link #NO_LOCATION} string if the {@code location} parameter is {@code null}
     * or a {@link GeoPoint#UNKNOWN_POINT}
     * @since 5.5
     */
    public static String getFormattedLocationString(@Nullable ILocation location,
                                                    @Nonnull CoordinateSystem coordinateSystem,
                                                    @Nonnull AltitudeUnits altitudeUnits)
    {
        return getLocationString(location, coordinateSystem, true, altitudeUnits).replaceAll("Elev", "<b>Elev</b>");
    }

    public static String getLocationString(ILocation location, CoordinateSystem coordinateSystem)
    {
        try
        {
            return getLocationString(location, coordinateSystem, true);
        } catch (Exception e)
        {
            logger.error("Failed to convert location to string", e);
        }
        return NO_LOCATION;
    }

    public static String getLocationString(ILocation location, CoordinateSystem coordinateSystem, boolean withElev)
    {
        if (location == null || location.equals(GeoPoint.UNKNOWN_POINT)) return NO_LOCATION;

        String elevationString = "";
        if (location.hasElevation() && withElev)
        {
            elevationString = " Elev " + getElevationString(location.getElevation()) + "m";
        }

        return internalGetLocationString(location, coordinateSystem) + elevationString;
    }

    /**
     * @param location The location; if {@code null} or an {@link GeoPoint#UNKNOWN_POINT}, the {@link #NO_LOCATION}
     *                 string will be returned
     * @param coordinateSystem The coordinate system to use when formatting the coordinates
     * @param withElev {@code true} if elevation should be included in the string
     * @param altitudeUnits The altitude units to use when formatting the elevation value
     * @return The location string, or the {@link #NO_LOCATION} if the {@code location} parameter is {@code null} or
     * an {@link GeoPoint#UNKNOWN_POINT unknown point}
     * @since 5.5
     */
    public static String getLocationString(@Nullable ILocation location, @Nonnull CoordinateSystem coordinateSystem,
                                           boolean withElev, @Nonnull AltitudeUnits altitudeUnits)
    {
        if (location == null || location.equals(GeoPoint.UNKNOWN_POINT)) return NO_LOCATION;

        String elevationString = "";
        if (location.hasElevation() && withElev)
        {
            elevationString = " Elev " + altitudeUnits.format(location.getElevation(), 2);
        }

        return internalGetLocationString(location, coordinateSystem) + elevationString;
    }

    private static String internalGetLocationString(ILocation location, CoordinateSystem coordinateSystem)
    {
        return switch (coordinateSystem)
        {
            case DECIMAL_DEGREES -> getDecimalDegreesString(location.getLatitude(), location.getLongitude());
            case DM_DEGREES -> getHMDegreesString(location.getLatitude(), location.getLongitude());
            case DMS_DEGREES -> getHMSDegreesString(location.getLatitude(), location.getLongitude());
            case MGRS -> getMGRSString(location.getLatitude(), location.getLongitude());
        };
    }

    /**
     * Format the given latitude and longitude according to the given coordinate system.
     *
     * @param latitude         Latitude of location
     * @param longitude        Longitude of location
     * @param coordinateSystem Coordinate system to use to format the location
     * @return formatted location string
     */
    public static String getLocationString(double latitude, double longitude, CoordinateSystem coordinateSystem)
    {
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) return NO_LOCATION;

        return switch (coordinateSystem)
        {
            case DECIMAL_DEGREES -> getDecimalDegreesString(latitude, longitude);
            case DM_DEGREES -> getHMDegreesString(latitude, longitude);
            case DMS_DEGREES -> getHMSDegreesString(latitude, longitude);
            case MGRS -> getMGRSString(latitude, longitude);
            default ->
            {
                logger.error("Unknown coordinate system: {}", coordinateSystem);
                yield NO_LOCATION;
            }
        };
    }

    /**
     * Format the given latitude and longitude according to the given coordinate system.
     *
     * @param latitude         Latitude of location
     * @param longitude        Longitude of location
     * @param coordinateSystem Coordinate system to use to format the location
     * @return formatted location string
     */
    public static String getLocationString(double latitude, double longitude, String coordinateSystem)
    {
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) return NO_LOCATION;

        return getLocationString(latitude, longitude, getCoordinateSystem(coordinateSystem, DECIMAL_DEGREES));
    }

    /**
     * Format the given LatLon location according to the given coordinate system.
     *
     * @param location         LatLon location to format
     * @param coordinateSystem Coordinate system to use to format the location
     * @return formatted location string
     */
    public static String getLocationString(LatLon location, String coordinateSystem)
    {
        return getLocationString(location.latitude.degrees, location.longitude.degrees, coordinateSystem);
    }

    /**
     * Format the given LatLon location according to the given coordinate system.
     *
     * @param location         LatLon location to format
     * @param coordinateSystem Coordinate system to use to format the location
     * @return formatted location string
     * @since 2.35
     */
    public static String getLocationString(LatLon location, CoordinateSystem coordinateSystem)
    {
        return getLocationString(location.latitude.degrees, location.longitude.degrees, coordinateSystem);
    }

    public static String getLocationString(ILocation location, String coordinateSystemString)
    {
        //
        // Default to decimal degrees.  The only way the default would be used
        // is if the getCoordinateSystem fails because the string passed is
        // unsupported.
        //
        return getLocationString(location, getCoordinateSystem(coordinateSystemString, DECIMAL_DEGREES));
    }

    public static String getLocationString(ILocation location, String coordinateSystemString, boolean withElev)
    {
        //
        // Default to decimal degrees.  The only way the default would be used
        // is if the getCoordinateSystem fails because the string passed is
        // unsupported.
        //
        return getLocationString(location, getCoordinateSystem(coordinateSystemString, DECIMAL_DEGREES), withElev);
    }

    /**
     * Format the given LatLon location according to the given coordinate system, elevation and unit of measure preference.
     *
     * @param location               LatLon location to format
     * @param coordinateSystemString Coordinate system to use to format the location
     * @param withElev               Boolean - Include Elevation?
     * @param altitudeUnits          Unit of measure from preferences
     * @return formatted location string
     * @since 5.5
     */
    public static String getLocationString(ILocation location, String coordinateSystemString, boolean withElev, AltitudeUnits altitudeUnits)
    {
        //
        // Default to decimal degrees.  The only way the default would be used
        // is if the getCoordinateSystem fails because the string passed is
        // unsupported.
        //
        // Default to KM_KM2 unit format.  The only way the default would be used
        // is if the unit of measure preference is invalid.
        //

        return getLocationString(location, getCoordinateSystem(coordinateSystemString, DECIMAL_DEGREES), withElev, altitudeUnits);
    }

    /**
     * Given and angle (latitude or longitude) as double value, return a formatted string according to the
     * specified coordinate system.
     *
     * @param angle            Angle to format
     * @param coordinateSystem Coordinate system to use for formatting
     * @return Formatted value according to coordinate system
     */
    public static String getAngleString(double angle, CoordinateSystem coordinateSystem)
    {
        return switch (coordinateSystem)
        {
            case DECIMAL_DEGREES -> getDecimalDegreesString(angle);
            case DM_DEGREES -> getHMDegreesString(angle);
            case DMS_DEGREES -> getHMSDegreesString(angle);
            case MGRS ->
            {
                logger.warn("Can't format a single angle in MGRS coordinates: angle = {}", angle);
                yield NO_LOCATION;
            }
            default -> NO_LOCATION;
        };
    }

    public static String getLatitudeStringInDecimalDegrees(double latitude)
    {
        return String.format("%9.5f ", latitude);
    }

    public static String getLongitudeStringInDecimalDegrees(double longitude)
    {
        return String.format("%9.5f ", longitude);
    }

    public static String getElevationString(double elevation)
    {
        synchronized (ELEVATION_STRING_FORMAT)
        {
            return ELEVATION_STRING_FORMAT.format(elevation);
        }
    }

    /**
     * Determines the type of Coordinate System used within a location string.
     * Can determine MGRS, Decimal Lat Long, and DMS strings
     *
     * @param coordString Formatted location string, with or without elevation data
     * @return Corresponding coordinate system used in string. If there are no matches, return {@code null}
     * @since 2.38
     */
    public static CoordinateSystem getLocationTypeFromString(@Nonnull String coordString)
    {
        coordString = coordString.trim();

        // First, see if it contains an elevation specifier at the end
        Matcher matcher = ELEVATION_PTTN.matcher(coordString);
        if (matcher.find())
        {
            coordString = coordString.substring(0, matcher.start());
        }

        // MGRS
        matcher = MGRS_PTTN.matcher(coordString);
        if (matcher.matches()) return MGRS;

        // Decimal Lat Long
        matcher = DECIMAL_LAT_LONG_PTTN.matcher(coordString);
        if (matcher.matches()) return DECIMAL_DEGREES;

        matcher = DMM_LAT_LONG_PTTN.matcher(coordString);
        if (matcher.matches()) return DM_DEGREES;

        // Degrees Minutes Seconds
        matcher = DMS_LAT_LONG_PTTN.matcher(coordString);
        if (matcher.matches()) return DMS_DEGREES;

        return null;
    }

    /**
     * Perform a fuzzy match on an invalid string to attempt to guess what coordinate system the user is trying to use
     *
     * @param invalidString Invalid string that does not parse into a valid coordinate system
     * @return coordinate system the user is potentially trying to use, or {@code null} if no match
     * @since 2.38.1
     */
    public static CoordinateSystem guessCoordinateSystemFromInvalidString(@Nonnull String invalidString)
    {
        // MGRS
        Matcher matcher = FUZZY_MATCH_MGRS_PTTN.matcher(invalidString);
        if (matcher.matches()) return MGRS;

        // DMS
        matcher = FUZZY_MATCH_DMS_PTTN.matcher(invalidString);
        if (matcher.matches()) return DMS_DEGREES;

        // Decimal Degrees
        matcher = FUZZY_MATCH_DECIMAL_PTTN.matcher(invalidString);
        if (matcher.matches()) return DECIMAL_DEGREES;

        return null;
    }

    /**
     * Computes a location lat / lon degrees, minutes and seconds.
     *
     * @param latDeg latitude degrees
     * @param latMin latitude minutes
     * @param latSec latitude seconds
     * @param lonDeg longitude degrees
     * @param lonMin longitude minutes
     * @param lonSec longitude seconds
     * @return location instance for the given point.
     * @since 5.7
     */
    @Nonnull
    public static ILocation computeLatLonFromDMS(double latDeg, double latMin, double latSec, double lonDeg, double lonMin, double lonSec)
    {
        double lat = Math.abs(latDeg) + (latMin / 60) + (latSec / 3600);
        double lon = Math.abs(lonDeg) + (lonMin / 60) + (lonSec / 3600);
        return new RaptorLocation(latDeg < 0 ? -lat : lat, lonDeg < 0 ? -lon : lon);
    }

    /**
     * Computes location from latitude and longitude degrees and minutes.
     *
     * @param latDeg latitude degrees
     * @param latMin latitude minutes
     * @param lonDeg longitude degrees
     * @param lonMin longitude minutes
     * @return location instance for the given point.
     * @since 5.7
     */
    @Nonnull
    public static ILocation computeLatLonFromDM(double latDeg, double latMin, double lonDeg, double lonMin)
    {
        return computeLatLonFromDMS(latDeg, latMin, 0, lonDeg, lonMin, 0);
    }

    /**
     * Tries to extract a Location (latitude, longitude, and elevation) from the given text string.
     *
     * @param coordString the input string.
     * @return the computed {@link ILocation} or {@code null}.
     */
    public static ILocation computeLatLonFromString(@Nonnull String coordString)
    {
        coordString = coordString.trim();

        Angle lat = null;
        Angle lon = null;
        boolean hasElevation = false;
        double elevation = 0;

        // First, see if it contains an elevation specifier at the end
        Matcher matcher = ELEVATION_PTTN.matcher(coordString);

        if (matcher.find())
        {
            // Strip the matched substring from the input
            coordString = coordString.substring(0, matcher.start());
            final String unitString = matcher.group(3);
            final String elevationString = matcher.group(1).replace(",", "");
            try
            {
                final char unitLetter = unitString != null && !unitString.isEmpty() ? unitString.charAt(0) : 'm';

                // Assume meters, allowing 'k' for kilometers, and 'f' for feet
                final double multiplier = switch (Character.toLowerCase(unitLetter))
                {
                    case 'k' -> 1000;
                    case 'f' -> 1 / WWMath.METERS_TO_FEET;
                    case 'm' -> 1;
                    default ->
                    {
                        logger.warn("Unsupported elevation unit of measure '{}' in '{}'; assuming meters",
                                unitString, coordString);
                        yield 1;
                    }
                };

                elevation = Double.parseDouble(elevationString) * multiplier;
                hasElevation = true;

                logger.trace("Parsed elevation '{}' to value: {}", elevationString, elevation);
            } catch (NumberFormatException e)
            {
                logger.warn("Unable to parse elevation string: '{}'", elevationString);
            }
        }

        // Try MGRS - allow spaces
        matcher = MGRS_PTTN.matcher(coordString);
        if (matcher.matches())
        {
            try
            {
                MGRSCoord coord = MGRSCoord.fromString(coordString, null); // spaces removed by fromString method
                // NOTE: the MGRS converter does not always report errors with invalid strings,
                // but will set lat and lon to zero
                if (coord.getLatitude().degrees != 0 || coord.getLongitude().degrees != 0)
                {
                    lat = coord.getLatitude();
                    lon = coord.getLongitude();
                }
            } catch (Exception e)
            {
                return null;
            }
        }

        // Try to extract a pair of signed decimal values separated by a space, ',' or ', '
        // Allow E, W, S, N suffixes
        if (lat == null || lon == null)
        {
            matcher = DECIMAL_LAT_LONG_PTTN.matcher(coordString);
            if (matcher.matches())
            {
                lat = getAngleFromMatcher(matcher, true);
                lon = getAngleFromMatcher(matcher, false);
            }
        }

        // Try to extract two degrees minute decimal second blocks separated by a space, ',' or ', '
        // Allow S, N, W, E suffixes and signs (lat/lon order dependent)
        // eg: -123* 34' 42.2"     +45d 12' 30.2"
        // eg:  123D 34' 42.2"S     45° 12' 30.2"W
        if (lat == null || lon == null)
        {
            matcher = DMS_LAT_LONG_PTTN.matcher(coordString);
            if (matcher.matches())
            {
                lat = getAngleFromMatcher(matcher, true);
                lon = getAngleFromMatcher(matcher, false);
            }
        }

        // Try to extract two degrees decimal minute blocks separated by a space, ',' or ', '
        // Allow S, N, W, E suffixes and signs (lat/lon order dependent)
        // eg: -123* 34.2'     +45d 12.2'
        // eg:  123D 34.2'S     45° 12.2'W
        if (lat == null || lon == null)
        {
            matcher = DMM_LAT_LONG_PTTN.matcher(coordString);
            if (matcher.matches())
            {
                lat = getAngleFromMatcher(matcher, true);
                lon = getAngleFromMatcher(matcher, false);
            }
        }

        if (lat == null || lon == null)
        {
            logger.debug("Unable to parse coordinate string '{}'", coordString);

            return null;
        }

        if (lat.degrees >= -90 && lat.degrees <= 90 && lon.degrees >= -180 && lon.degrees <= 180)
        {
            return new RaptorLocation(lat.getDegrees(), lon.getDegrees(), hasElevation, elevation);
        }

        logger.warn("Parsed coordinates out of range from string '{}'", coordString);
        return null;
    }

    /**
     * Returns the {@code Angle} from {@link #getAngleFromString}. Only works on LAT or LON at one time. Must have these groups:
     * LAT, LATSIGN, LATCARD, LON, LONSIGN, LONCARD
     * @param matcher {@code Matcher} containing the groups listed above
     * @param isLatitude differentiates latitude & longitude calls
     * @return An {@code Angle} calculated from the {@code Matcher}
     */
    private static Angle getAngleFromMatcher(Matcher matcher, boolean isLatitude)
    {
        String group = matcher.group(isLatitude ? LAT_GROUP_LABEL : LON_GROUP_LABEL).trim();
        String signGroup = matcher.group(isLatitude ? LATSIGN_GROUP_LABEL : LONSIGN_GROUP_LABEL).trim();
        String cardinalGroup = matcher.group(isLatitude ? LATCARD_GROUP_LABEL : LONCARD_GROUP_LABEL).trim();

        return getAngleFromString(group, signGroup, cardinalGroup);
    }

    /**
     * Parse a single lat or lon coordinate string in DD, DMS, or DMM format
     * @param coordString The string to parse in DD, DMS, or DMM format
     * @param signGroup The Matcher group containing the sign character, if any exists
     * @param cardinalGroup The Matcher group containing the cardinality character, if any exists
     * @return the corresponding {@code Angle} or {@code null}.
     */
    private static Angle getAngleFromString(String coordString, String signGroup, String cardinalGroup)
    {
        // Replace degree, min and sec signs with space
        coordString = coordString.replaceAll("[" + DEGREE_SYMBOLS + MINUTE_SYMBOLS + SECOND_SYMBOLS + "]", " ");
        // Replace multiple spaces with single ones, trim, & uppercase it
        coordString = coordString.replaceAll("\\s+", " ").trim();
        StringBuilder coordStringBuilder = new StringBuilder(coordString);

        // get sign based on prefix & suffix (sign & cardinal)
        // changes StringBuilder by removing them when they are parsed
        int sign = getCoordSignRemoveSignAndCardinal(coordStringBuilder, signGroup, cardinalGroup);

        // Process values
        // Can handle Decimal Degrees, DMS, & DMM
        String[] dms = coordStringBuilder.toString().split(" ");
        double d = Double.parseDouble(dms[0]);
        double m = dms.length > 1 ? Double.parseDouble(dms[1]) : 0;
        double s = dms.length > 2 ? Double.parseDouble(dms[2]) : 0;

        if (m >= 0 && m <= 60 && s >= 0 && s <= 60)
        {
            return Angle.fromDegrees(d * sign + m / 60 * sign + s / 3600 * sign);
        }

        return null;
    }

    /**
     * Takes in a StringBuilder, checks the sign & cardinal direction, removes them from the Stringbuilder if present, and returns the appropriate sign
     * @param coordSb StringBuilder to be operated on
     * @param signGroup The Matcher group containing the sign character, if any exists
     * @param cardinalGroup The Matcher group containing the cardinality character, if any exists
     * @return Integer (-1, 1) representing the sign that needs to be on the final coordinate
     */
    private static int getCoordSignRemoveSignAndCardinal(StringBuilder coordSb, String signGroup, String cardinalGroup)
    {
        int sign = 1;
        if (!signGroup.isEmpty())
        {
            sign *= "-".equals(signGroup) ? -1 : 1;
            coordSb.deleteCharAt(0);
        }
        if (!cardinalGroup.isEmpty())
        {
            sign *= ("N".equalsIgnoreCase(cardinalGroup) || "E".equalsIgnoreCase(cardinalGroup)) ? 1 : -1;
            coordSb.deleteCharAt(coordSb.length() - 1);
        }
        return sign;
    }

    // COORDS TO STRING METHODS

    public static String getMGRSString(double lat, double lon)
    {
        if (Double.isNaN(lat) || Double.isNaN(lon)) return NO_LOCATION;

        try
        {
            return MGRSCoord.fromLatLon(Angle.fromDegreesLatitude(lat), Angle.fromDegreesLongitude(lon)).toString();
        } catch (Exception e)
        {
            logger.warn("Failed to construct MGRS coordinate from lat {}, lon {}", lat, lon, e);
        }

        return NO_LOCATION;
    }

    /**
     * @param latitude latitude degrees
     * @param longitude longitude degrees
     * @return the four MGRS coordinate components, or {@code null} if it lat/lon could not be converted
     * @since 5.7
     */
    @Nullable
    public static String[] getMGRSParts(double latitude, double longitude)
    {
        if (Double.isNaN(latitude) || Double.isNaN(longitude)) return null;
        try
        {
            // TODO DCW TAKX-4291 this can create MGRS coordinates in polar regions that are not reciprocally parseable e.g
            //  MGRSCoord.fromLatLon(Angle.fromDegreesLatitude(88.17362955739844), Angle.fromDegreesLongitude(78.4059673262079)) = "ZBG 98649 59245"
            //  MGRSCoord.fromString("ZBG 98649 59245", null)
            var mgrs = MGRSCoord.fromLatLon(Angle.fromDegreesLatitude(latitude), Angle.fromDegreesLongitude(longitude));
            var matcher = MGRS_PTTN.matcher(mgrs.toString());
            matcher.find();
            return new String[]{matcher.group(1), matcher.group(2), matcher.group(3), matcher.group(4)};
        } catch (Exception e)
        {
            logger.warn("Failed to construct MGRS coordinate from lat {}, lon {}", latitude, longitude, e);
        }
        return null;
    }

    /**
     * Get the string value of the given lat/lon coordinate formatted as Degrees, Minutes, and Seconds.
     *
     * @param lat Latitude
     * @param lon Longitude
     * @return Formatted value
     */
    public static String getHMSDegreesString(double lat, double lon)
    {
        return "Lat " + getHMSDegreesString(lat) +
                " Lon " + getHMSDegreesString(lon);
    }

    /**
     * Get the string value of the given angle formatted as Degrees, Minutes, and Seconds.
     *
     * @param angle Angle to format
     * @return Formatted value
     */
    @Nonnull
    public static String getHMSDegreesString(double angle)
    {
        var parts = getDMSDegreesParts(angle);
        return String.format("%2d°%2d'%4.1f\"", (int) parts[0], (int) parts[1], parts[2]);
    }

    /**
     * Get the string value of the given lat/lon coordinate formatted as Degrees and decimal Minutes.
     *
     * @param lat Latitude
     * @param lon Longitude
     * @return Formatted value
     */
    public static String getHMDegreesString(double lat, double lon)
    {
        return "Lat " + getHMDegreesString(lat) +
                " Lon " + getHMDegreesString(lon);
    }

    /**
     * Get the string value of the given angle formatted as Degrees and decimal Minutes.
     *
     * @param angle Angle to format
     * @return Formatted value
     */
    @Nonnull
    public static String getHMDegreesString(double angle)
    {
        double[] parts = getDMDegreesParts(angle);
        return String.format("%2d°%6.3f'", (int) parts[0], parts[1]);
    }

    /**
     * @param angle angle, in degrees, to parse
     * @return the given angle, divided into integer degrees and decimal minutes
     * @since 5.7
     */
    @Nonnull
    public static double[] getDMDegreesParts(double angle)
    {
        var absAngle = Math.abs(angle);
        var deg = (int) absAngle;
        var minutes = (absAngle - deg) * 60D;
        return new double[]{angle < 0 ? -deg : deg, minutes};
    }

    /**
     * @param angle angle, in degrees, to parse
     * @return the given angle, divided into integer degrees, integer minutes, and decimal seconds
     * @since 5.7
     */
    @Nonnull
    public static double[] getDMSDegreesParts(double angle)
    {
        var absAngle = Math.abs(angle);
        var deg = (int) absAngle;
        var minutes = (absAngle - deg) * 60D;
        var seconds = (minutes - Math.floor(minutes)) * 60D;
        return new double[]{angle < 0 ? -deg : deg, (int) minutes, seconds};
    }

    /**
     * @param degrees degrees
     * @param minutes minutes
     * @return the given angle, divided into integer degrees, integer minutes, and decimal seconds
     * @since 5.7
     */
    @Nonnull
    public static double[] getDMSDegreesParts(int degrees, double minutes)
    {
        var minInt = (int) minutes;
        var seconds = (minutes - minInt) * 60D;
        return new double[]{degrees, minInt, seconds};
    }

    /**
     * Get the string value of the given lat/lon coordinate formatted as decimal degrees.
     *
     * @param lat Latitude
     * @param lon Longitude
     * @return Formatted value
     */
    public static String getDecimalDegreesString(double lat, double lon)
    {
        return "Lat " + getDecimalDegreesString(lat) +
                " Lon " + getDecimalDegreesString(lon);
    }

    /**
     * Get the string value of the given angle formatted as decimal degrees.
     *
     * @param angle Angle to format
     * @return Formatted value
     */
    public static String getDecimalDegreesString(double angle)
    {
        return String.format("%9.5f°", angle);
    }

    /**
     * Returns the suffixes for each component of the specified coordinate format
     *
     * @param coordinateSystem whose suffixes to return
     * @return a Map of Strings containing all of the suffixes and integers containing the max length of the component
     */
    @SuppressWarnings("unused")
    public static JFormattedTextField getCoordinateSystemFormattedTextField(CoordinateSystem coordinateSystem)
    {
        try
        {
            return switch (coordinateSystem)
            {
                case DECIMAL_DEGREES -> new JFormattedTextField(new MaskFormatter("####.##"));
                case DM_DEGREES, DMS_DEGREES -> new JFormattedTextField(new MaskFormatter("######.##"));
                default -> new JFormattedTextField(new MaskFormatter("#UUU##")); // MGRS
            };
        } catch (ParseException e)
        {
            logger.error("Failed to parse format specification", e);
            return new JFormattedTextField();
        }
    }

    /**
     * Get an example string formatted according to the coordinate system.
     *
     * @param coordinateSystem coordinate system to get example string for
     * @return example string for the coordinate system, or an empty string if an unexpected coordinate system
     */
    public static String getExampleString(CoordinateSystem coordinateSystem)
    {
        return switch (coordinateSystem)
        {
            case DECIMAL_DEGREES -> DECIMAL_DEGREES_STRING;
            case DM_DEGREES -> DM_DEGREES_STRING;
            case DMS_DEGREES -> DMS_DEGREES_STRING;
            case MGRS -> MGRS_STRING;
            default ->
            {
                logger.warn("Unexpected coordinate system: {}. Returning empty example string", coordinateSystem.getName());
                yield "";
            }
        };
    }

    public static CoordinateSystem[] sortedValues()
    {
        final List<CoordinateSystem> list = Arrays.asList(DECIMAL_DEGREES, DM_DEGREES, DMS_DEGREES, MGRS);
        Collections.sort(list);
        return list.toArray(new CoordinateSystem[0]);
    }
}
