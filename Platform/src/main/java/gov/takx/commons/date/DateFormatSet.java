package gov.takx.commons.date;

import gov.takx.commons.file.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.invoke.MethodHandles;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

/**
 * A date formatter that can parse dates in a variety of formats.  All the formatters managed here are
 * instances of {@link SimpleDateFormat} based on pattern strings.
 *
 * @since 1.3
 */
@SuppressWarnings({"UnusedDeclaration", "WeakerAccess"}) // Mostly called from plugins
public class DateFormatSet
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Accept string dates in RFC 822 format. See http://asg.web.cmu.edu/rfc/rfc822.html
     */
    public static final String RFC_822_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss ZZZ";

    /**
     * Accept string dates in ISO 8601 format. See http://www.w3.org/TR/NOTE-datetime
     */
    public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ssZZZ";

    /**
     * Accept string dates in ISO 8601 format, using 'Z' to specify UTC time. The Java date parser
     * does not seem to recognize this notation.
     */
    public static final String ISO_8601_DATE_FORMAT_Z = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    /**
     * Also accept ISO 8601 dates without a timezone.
     */
    public static final String ISO_8601_DATE_FORMAT_NO_TZ = "yyyy-MM-dd'T'HH:mm:ss";

    // The formatters we use.  Use a set to avoid any duplicates. Use linked set to keep insert order.
    private final Set<SimpleDateFormat> dateFormats = new LinkedHashSet<>();

    /**
     * The default set of date format patterns.
     *
     * @since 1.31
     */
    public static final String[] DEFAULT_FORMATS = {
            // Add formats defined by specifications
            RFC_822_DATE_FORMAT,
            ISO_8601_DATE_FORMAT,
            ISO_8601_DATE_FORMAT_Z,
            ISO_8601_DATE_FORMAT_NO_TZ,

            // Add other common date formats
            "yyyy-MM-dd HH:mm:ss ZZZ",
            "yyyy-MM-dd HH:mm:ss.SSS ZZZ",
            "yyyy-MM-dd hh:mm:ss a",
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss.SSS",
            "yyyy-MM-dd HH:mm",

            "yyyy.MM.dd HH:mm:ss ZZZ",
            "yyyy.MM.dd HH:mm:ss.SSS ZZZ",
            "yyyy.MM.dd hh:mm:ss a",
            "yyyy.MM.dd HH:mm:ss",
            "yyyy.MM.dd HH:mm:ss.SSS",
            "yyyy.MM.dd HH:mm",
            "yyyy.MM.dd",

            "MM/dd/yyyy hh:mm:ss ZZZ",
            "MM/dd/yyyy hh:mm:ss.sss ZZZ",
            "MM/dd/yyyy hh:mm:ss a",
            "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy HH:mm:ss.SSS",
            "MM/dd/yyyy HH:mm",
            "MM/dd/yyyy",

            "MM/dd/yy hh:mm:ss ZZZ",
            "MM/dd/yy hh:mm:ss.sss ZZZ",
            "MM/dd/yy hh:mm:ss a",
            "MM/dd/yy HH:mm:ss",
            "MM/dd/yy HH:mm:ss.SSS",
            "MM/dd/yy HH:mm",
            "MM/dd/yy",

            "dd/MM/yyyy HH:mm:ss",

            "EEE, dd MMM yyyy",
            "yyyy-MM-dd",
            "dd-MMM-yy",
            "EEE, MMM d, ''yy",
            "h:mm a",
            "K:mm a, z",
            "HH:mm:ss",
            "HH:mm:ss.SSS",
            "HH:mm",
    };

    /**
     * Construct a format set using the default format list.  This default set can be replaced by calling
     */
    public DateFormatSet()
    {
        this(DEFAULT_FORMATS);
    }

    /**
     * Construct using the given set of format patterns.
     *
     * @param patterns Set of date format patterns to use
     * @since 1.31
     */
    public DateFormatSet(String[] patterns)
    {
        internFormats(patterns);
    }

    /**
     * Add a format to the set.
     *
     * @param format New date format. May not be {@code null}.
     */
    public void addFormat(SimpleDateFormat format)
    {
        dateFormats.add(format);
    }

    /**
     * Get all the formats in the set.
     *
     * @return All date formats in this set.
     */
    public List<? extends SimpleDateFormat> getFormats()
    {
        return new ArrayList<>(dateFormats);
    }

    /**
     * Get a list of the pattern strings from the formatters.
     *
     * @return List of pattern strings
     * @since 1.31
     */
    public List<String> getPatterns()
    {
        final List<String> patterns = new ArrayList<>(dateFormats.size());

        // Extract the patterns from the formats
        for (SimpleDateFormat format : dateFormats)
        {
            patterns.add(format.toPattern());
        }
        Collections.sort(patterns);
        return patterns;
    }

    /**
     * Parse a date. This method will take each date format in the set and attempt to parse the string. It will return
     * as soon as a parse attempt succeeds. The formats are tried in the order in which they were added.
     *
     * @param dateStr String to parse. May not be {@code null}.
     * @return The parsed date, or {@code null} if none of the available formats could parse the input.
     */
    public synchronized Date parseDate(String dateStr)
    {
        if (dateStr == null) throw new IllegalArgumentException("Parameter 'dateStr' may not be null");
        Date date = null;

        logger.debug("parsing date: {}", dateStr);

        for (SimpleDateFormat format : dateFormats)
        {
            try
            {
                date = format.parse(dateStr);
                logger.debug("Date: {}  From: {}  Format: {}", date, dateStr, format);
                break;
            } catch (ParseException ignored) //NOPMD
            {
                // Ignore, try next format
            }
        }

        return date;
    }

    /**
     * Attempt to parse a date string, and return the format that successfully parsed the string. This method can be
     * used to determine for date format used in a string.
     *
     * @param dateStr String to parse. May not be {@code null}.
     * @return A date format from the defined set that was able to parse {@code dateStr}, or {@code null} if no such
     * format was found.
     */
    public synchronized SimpleDateFormat findFormat(String dateStr)
    {
        if (dateStr == null) throw new IllegalArgumentException("Parameter 'dateStr' may not be null");

        for (SimpleDateFormat format : dateFormats)
        {
            try
            {
                Date d = format.parse(dateStr);
                if (d.getTime() > 0)    // yyyy will not throw an exception parsing yy, but it will give a negative date
                {
                    return format;
                }
            } catch (ParseException ignored) //NOPMD
            {
                // Ignore, try next format
            }
        }
        return null;
    }

    /**
     * Load a set of date formats from the specified file.  If the file does not exist, or is not readable, then
     * the default set of date formats will be loaded.  The file is expected to be a simple text file containing
     * one date format pattern per line.  Empty lines will be ignored as will leading and trailing whitespace.
     *
     * @param formatsFile File to read from
     * @return {@code true} If the file was loaded, {@code false} if not (and the default format set was used)
     * @since 1.31
     */
    public boolean loadFormats(File formatsFile)
    {
        boolean loaded = false;
        String[] formats = DEFAULT_FORMATS; // Fallback to defaults

        // If the file doesn't exist or we can't read it, then use the default set.
        if (!formatsFile.exists() || !formatsFile.canRead())
        {
            logger.warn("Unable to read formats from file: {}", formatsFile);
        } else
        {
            final String[] formatsFromFile = FileUtils.readLines(formatsFile);

            if (formatsFromFile.length > 0)
            {
                formats = formatsFromFile;
                loaded = true;
            }
        }

        internFormats(formats); // Internalize the formats

        return loaded;
    }

    /**
     * Write the list of format strings to the given file.
     *
     * @param formatsFile The file to write to
     * @return {@code true} if the file was written, {@code false} on error
     * @since 1.31
     */
    public boolean saveFormats(File formatsFile)
    {
        // Create a list of pattern strings from the formats (avoid any duplicates)
        final List<String> patterns = new ArrayList<>(dateFormats.size());

        for (SimpleDateFormat format : dateFormats)
        {
            final String pattern = format.toPattern();
            if (!patterns.contains(pattern))
            {
                patterns.add(pattern);
            }
        }

        // And write them out to the file
        return FileUtils.writeLines(formatsFile, patterns.toArray(new String[0]));
    }

    /**
     * Given a set of string format patterns, internalize them and create our set of date formats.  To the list
     * provided, we will add the short, medium, long, and full instance formats for the current locale.  All formats
     * will have a default TZ of UTC set.
     *
     * @param patterns Set of format patterns to process
     */
    protected void internFormats(String[] patterns)
    {
        dateFormats.clear(); // Start clean

        // Add all the given formats (avoid duplicates)

        for (String pattern : patterns)
        {
            dateFormats.add(new SimpleDateFormat(pattern));
        }

        // Add default formats for the current Locale
        dateFormats.add((SimpleDateFormat) SimpleDateFormat.getDateInstance(SimpleDateFormat.SHORT));
        dateFormats.add((SimpleDateFormat) SimpleDateFormat.getDateInstance(SimpleDateFormat.MEDIUM));
        dateFormats.add((SimpleDateFormat) SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG));
        dateFormats.add((SimpleDateFormat) SimpleDateFormat.getDateInstance(SimpleDateFormat.FULL));

        // Default dates without explicit timezone to UTC
        TimeZone utc = TimeZone.getTimeZone("UTC");
        for (SimpleDateFormat format : dateFormats)
        {
            format.setTimeZone(utc);
        }
    }
}
