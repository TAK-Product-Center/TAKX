package gov.takx.commons.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.net.URI;
import java.util.UUID;

/**
 * Utility class to provide common helpers for parsing data in text format. This class includes
 * helper methods for parsing numbers which may be {@code null} (such as empty strings read from a stream).
 *
 * @since 1.3
 */
public final class ParseUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final String NAN = "NaN";

    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private ParseUtils()
    {
    }

    /**
     * Parse out the class name from the end of a JAR URI.
     *
     * @param uri URI that has a class at the end of the scheme specific part
     * @return Class name parsed, or {@code null} if there was an error parsing the URI
     * @see java.net.JarURLConnection
     */
    public static String parseClassname(URI uri)
    {
        String schemeSpecificPart = uri.getSchemeSpecificPart();
        int bangIdx = schemeSpecificPart.indexOf('!');

        logger.debug("! index: {}", bangIdx);

        // We add 1 here to get rid of the ! character
        String resourcePath = schemeSpecificPart.substring(bangIdx + 1);

        logger.debug("Resource path: {}", resourcePath);

        return resourcePath;
    }

    /**
     * Parse a string into a double, or 0.0 if s is {@code null} or empty or fails to parse.
     *
     * @param s String to parse.
     * @return double value of s, or 0.0.
     */
    public static double parseDouble(String s)
    {
        return parseDouble(s, 0.0);
    }

    /**
     * Parse a string into a double, or return {@code default} if {@code null} or empty or fails to parse.
     * <p>
     * Note: Even though {@link Double#parseDouble(String)} will fail to parse {@code "nan"}, this method supports
     * parsing it regardless of capitalization.
     *
     * @param s   String to parse.
     * @param def Value to return if {@code s} is {@code null} or empty or fails to parse.
     * @return Double value of {@code s} or {@code def}.
     */
    public static double parseDouble(String s, double def)
    {
        try
        {
            return Double.parseDouble(s.trim());
        } catch (Exception e)
        {
            return NAN.equalsIgnoreCase(s) ? Double.NaN : def;
        }
    }

    /**
     * Parse a string into a long, or return 0L if {@code null} or empty or fails to parse.
     *
     * @param s String to parse.
     * @return long value of {@code s} or 0L.
     */
    public static long parseLong(String s)
    {
        return parseLong(s, 0L);
    }

    /**
     * Parse a string into a long, or return {@code def} if {@code null} or empty or fails to parse.
     *
     * @param s   String to parse.
     * @param def Value to return if {@code s} is {@code null} or empty.
     * @return long value of {@code s} or {@code def}.
     */
    public static long parseLong(String s, long def)
    {
        try
        {
            return Long.parseLong(s.trim());
        } catch (Exception e)
        {
            return def;
        }
    }

    /**
     * Parse a string into a short, or return 0 if {@code null} or empty or fails to parse.
     *
     * @param s String to parse.
     * @return Short value of {@code s} or 0
     */
    public static short parseShort(String s)
    {
        return parseShort(s, (short) 0);
    }

    /**
     * Parse a string into a short, or return {@code def} if {@code null} or empty or fails to parse.
     *
     * @param s String to parse.
     * @return Short value of {@code s} or {@code def}.
     */
    public static short parseShort(String s, short def)
    {
        try
        {
            return Short.parseShort(s);
        } catch (Exception e)
        {
            return def;
        }
    }

    /**
     * Parse a string into a boolean, or return false if {@code s} is not equal to {@code "true"}, ignoring case.
     *
     * @param s String to parse.
     * @return Boolean value of {@code s}
     * @see Boolean#parseBoolean(String)
     */
    public static boolean parseBoolean(String s)
    {
        return Boolean.parseBoolean(s);
    }

    /**
     * Parse a string into a float, or return 0.0 if {@code null} or empty or fails to parse.
     *
     * @param s String to parse.
     * @return Float value of {@code s} or 0.0.
     */
    public static float parseFloat(String s)
    {
        return parseFloat(s, 0f);
    }

    /**
     * Parse a string into a float, or return {@code def} if {@code null} or empty or fails to parse.
     * <p>
     * Note: Even though {@link Float#parseFloat(String)} will fail to parse {@code "nan"}, this method supports
     * parsing it regardless of capitalization.
     *
     * @param s String to parse.
     * @param def Value to return if {@code s} is {@code null} or empty or fails to parse.
     * @return Float value of {@code s} or {@code def}
     */
    public static float parseFloat(String s, float def)
    {
        try
        {
            return Float.parseFloat(s);
        } catch (Exception e)
        {
            return NAN.equalsIgnoreCase(s) ? Float.NaN : def;
        }
    }

    /**
     * Parse a string into an integer, or return 0 if {@code null} or empty.
     *
     * @param s String to parse.
     * @return Integer value of {@code s} or 0.
     */
    public static int parseInt(String s)
    {
        return s != null && !s.isBlank() ? Integer.parseInt(s) : 0;
    }

    /**
     * Parse a string into an int, or return {@code def} if {@code null} or empty.
     *
     * @param s   String to parse.
     * @param def Value to return if {@code s} is {@code null} or empty.
     * @return int value of {@code s} or {@code def}.
     */
    public static int parseInt(String s, int def)
    {
        try
        {
            return Integer.parseInt(s.trim());
        } catch (Exception e)
        {
            return def;
        }
    }

    /**
     * Parse a string into an int using {@link Integer#parseUnsignedInt(String, int)},
     * or return {@code def} if {@code null} or empty.
     *
     * @param s     String to parse.
     * @param radix the radix to be used while parsing {@code s}.
     * @param def   Value to return if {@code s} is {@code null} or empty.
     * @return int value of {@code s} or {@code def}.
     * @since 3.0
     */
    public static int parseUnsignedInt(String s, int radix, int def)
    {
        try
        {
            return Integer.parseUnsignedInt(s.trim(), radix);
        } catch (Exception e)
        {
            return def;
        }
    }

    /**
     * Parse a string into a new Double object. Returns {@code null} if {@code s} is {@code null} or empty.
     *
     * @param s String to parse.
     * @return a new Double or {@code null}.
     */
    public static Double newDouble(String s)
    {
        return s != null && !s.isBlank() ? Double.valueOf(s) : null;
    }

    /**
     * Parse a string into a new Short object. Returns {@code null} if {@code s} is {@code null} or empty.
     *
     * @param s String to parse.
     * @return a new Short or {@code null}.
     */
    public static Short newShort(String s)
    {
        return s != null && !s.isBlank() ? Short.valueOf(s) : null;
    }

    /**
     * Parse a string into a new Long object. Returns {@code null} if {@code s} is {@code null} or empty.
     *
     * @param s String to parse.
     * @return a new Long or {@code null}.
     */
    public static Long newLong(String s)
    {
        return s != null && !s.isBlank() ? Long.valueOf(s) : null;
    }

    /**
     * Parse a string into a new Boolean object.
     *
     * @param s String to parse.
     * @return a new Boolean
     */
    public static Boolean newBoolean(String s)
    {
        return Boolean.valueOf(s);
    }

    /**
     * Parse a string into a new Float object. Returns {@code null} if {@code s} is {@code null} or empty.
     *
     * @param s String to parse.
     * @return a new Float or {@code null}.
     */
    public static Float newFloat(String s)
    {
        return s != null && !s.isBlank() ? Float.valueOf(s) : null;
    }

    /**
     * Parse a string into a new Integer object. Returns {@code null} if {@code s} is {@code null} or empty.
     *
     * @param s String to parse.
     * @return a new Integer or {@code null}.
     */
    public static Integer newInt(String s)
    {
        return s != null && !s.isBlank() ? Integer.valueOf(s) : null;
    }

    /**
     * Trim a given input string. If the input string is {@code null} or empty (when trimmed), return the given defaultValue.
     *
     * @param value        Input string to process
     * @param defaultValue Default value to use if string is {@code null}/empty
     * @return Trimmed string, or {@code defaultValue} if input was {@code null}/empty.
     * @since 3.0
     */
    @Nullable
    public static String trimOrDefault(@Nullable String value, @Nullable String defaultValue)
    {
        return (value == null || value.isBlank())
                ? defaultValue
                : value;
    }

    /**
     * Trim a given input string. If the input string is {@code null} or empty (when trimmed), throw an exception.
     *
     * @param value Input string to process
     * @param name  Name to use in exception message
     * @return Trimmed string
     * @throws IllegalArgumentException If input is {@code null} or empty
     * @since 3.0
     */
    @Nonnull
    public static String trimOrThrow(@Nullable String value, @Nonnull String name) throws IllegalArgumentException
    {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("'" + name + "' must be non-empty");

        return value;
    }

    /**
     * Determine if a string supplied is a valid UUID.
     *
     * @param maybeUuid the string to test
     * @return true if it is a UUID
     * @since 3.0
     */
    public static boolean isUuid(String maybeUuid)
    {
        try
        {
            //noinspection ResultOfMethodCallIgnored
            UUID.fromString(maybeUuid);
            return true;
        } catch (Exception e)
        {
            return false;
        }
    }
}
