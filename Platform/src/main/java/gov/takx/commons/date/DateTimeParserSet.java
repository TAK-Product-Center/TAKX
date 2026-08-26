package gov.takx.commons.date;

import gov.takx.commons.date.DateTimeParser.Style;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Utility class for parsing date/times using {@code DateTimeFormatter}.
 *
 * @since 3.0
 */
public class DateTimeParserSet
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String RFC_822_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
    public static final String ISO_8601_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss[.SSS]ZZZZZ";
    public static final String ISO_8601_DATE_FORMAT_NO_TZ = "yyyy-MM-dd'T'HH:mm:ss[.SSS]";

    // List of configured parsers
    private final List<DateTimeParser> parsers = new ArrayList<>();

    // The default date patterns
    private static final List<String> DATE_PATTERNS = List.of(
            "M/d/yy", "M-d-yy", "M.d.yy",        // month + day + year, 2 year digits
            "M/d/yyyy", "M-d-yyyy", "M.d.yyyy",  // month + day + year, 4 year digits
            "yyyy/M/d", "yyyy-M-d", "yyyy.M.d"   // 4 digit year, month, day
    );

    // The default time patterns
    private static final List<String> TIME_PATTERNS = List.of(
            "H:mm[:ss[.SSS]]",    // 24h, optional 0/2 seconds, 3 milliseconds
            "H:mm[:ss[.SS]]",     // 24h, optional 0/2 seconds, 2 milliseconds
            "H:mm[:ss[.S]]",      // 24h, optional 0/2 seconds, 1 milliseconds
            "h:mm[:ss[.SSS]] a",  // 12h, optional 0/2 seconds, 3 milliseconds
            "h:mm[:ss[.SS]] a",   // 12h, optional 0/2 seconds, 2 milliseconds
            "h:mm[:ss[.S]] a"     // 12h, optional 0/2 seconds, 1 milliseconds
    );

    private static final List<DateTimeParser> DEFAULT_PARSERS = new ArrayList<>();

    static
    {
        createDefaultParsers();
    }

    /**
     * Create a {@link DateTimeParserSet} from the definitions in the specified file, which must have been created
     * by {@link #storeToDefinitionFile(File)}.  The format of the file content is intentionally opaque.
     * <p>
     * If the file can't be read or no valid parser definitions are found, the defaults will be used.
     *
     * @param parsersFile The file to read
     */
    @Nonnull
    public static DateTimeParserSet fromDefinitionFile(@Nonnull File parsersFile)
    {
        try
        {
            final List<DateTimeParser> parsers = Files.readAllLines(parsersFile.toPath())
                    .stream()
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(DateTimeParserSet::fromDefinition)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            return parsers.isEmpty()
                    ? new DateTimeParserSet() // If nothing useful in the file, use defaults
                    : new DateTimeParserSet(parsers);
        } catch (FileNotFoundException | NoSuchFileException e)
        {
            logger.info("Date/Time parser definition file not found, using defaults: {}", parsersFile);
        } catch (IOException e)
        {
            logger.error("Failed to read: {}", parsersFile, e);
        }

        return new DateTimeParserSet(); // use defaults
    }

    /**
     * Create a parser from the given definition, logging a warning if construction failed.
     */
    @Nullable
    private static DateTimeParser fromDefinition(@Nonnull String parserDefinition)
    {
        try
        {
            return DateTimeParser.fromDefinition(parserDefinition);
        } catch (Exception e)
        {
            logger.warn("Skipping invalid parser definition {}", parserDefinition);
            return null;
        }
    }

    /**
     * Default constructor. Uses the default date/time parsers.
     */
    public DateTimeParserSet()
    {
        this(DEFAULT_PARSERS);
    }

    /**
     * Construct using a specified collection of parsers.
     *
     * @param parsers The parsers to add to the set
     */
    public DateTimeParserSet(@Nonnull List<DateTimeParser> parsers)
    {
        this.parsers.addAll(parsers);
    }

    /**
     * Add the given parser to this set.
     *
     * @return this, for chaining
     */
    @Nonnull
    public DateTimeParserSet addParser(@Nonnull DateTimeParser parser)
    {
        parsers.add(parser);
        return this;
    }

    /**
     * @return The collection of parsers contained in this set.
     */
    @Nonnull
    public List<DateTimeParser> getParsers()
    {
        return Collections.unmodifiableList(parsers);
    }

    /**
     * Returns all the patterns that have been added to this class.
     *
     * @return The patterns that have been added to this class
     */
    @Nonnull
    public List<String> getPatterns()
    {
        return parsers.stream().map(DateTimeParser::getPattern).collect(Collectors.toList());
    }

    /**
     * @return The first parser that can parse the given {@code dateTimeString}, or {@code null} if none can.
     */
    @Nullable
    public DateTimeParser findParser(@Nonnull String dateTimeString)
    {
        logger.debug("findParser for '{}'", dateTimeString);

        for (DateTimeParser parser : parsers)
        {
            try
            {
                final Instant instant = parser.toInstant(dateTimeString);
                logger.debug("   got: {} using {}", instant, parser);
                return parser;
            } catch (DateTimeParseException ignore)
            {
            }
        }

        return null;
    }

    /**
     * @return The parser with the given pattern, or else {@code null}
     */
    @Nullable
    public DateTimeParser findParserByPattern(@Nonnull String pattern)
    {
        return parsers.stream()
                .filter(p -> p.getPattern().equals(pattern))
                .findFirst()
                .orElse(null);
    }

    /**
     * @return The pattern that matches the {@code dateTimeString} parameter, or {@code null} if no pattern matches.
     */
    @Nullable
    public String getPattern(@Nonnull String dateTimeString)
    {
        final DateTimeParser parser = findParser(dateTimeString);
        return parser != null ? parser.getPattern() : null;
    }

    /**
     * Attempt to parse the given input string using each of the configured parsers.  Return the {@link Instant} from
     * the first parser that successfully parses the input String.
     *
     * @param dateTimeString The date/time String to parse
     * @return The {@link Instant} value for the date/time
     * @see LocalDate#atStartOfDay()
     */
    @Nullable
    public Instant getInstant(@Nonnull String dateTimeString)
    {
        for (DateTimeParser parser : parsers)
        {
            try
            {
                return parser.toInstant(dateTimeString);
            } catch (DateTimeParseException ignore)
            {
            }
        }

        return null;
    }

    /**
     * Write definitions for the parsers in this set to the specified file.
     *
     * @param parsersFile The output File
     */
    public void storeToDefinitionFile(@Nonnull File parsersFile) throws IOException
    {
        Files.write(parsersFile.toPath(),
                parsers.stream().map(DateTimeParser::toDefinition).collect(Collectors.toList()));
    }

    /**
     * Create the default parsers.
     */
    private static void createDefaultParsers()
    {
        // Standard RFC/ISO patterns
        addDefaultParser(RFC_822_DATE_FORMAT, Style.DateTimeWithZone);
        addDefaultParser(ISO_8601_DATE_FORMAT, Style.DateTimeWithZone);
        addDefaultParser(ISO_8601_DATE_FORMAT_NO_TZ, Style.DateTime);

        // Standard Date patterns
        DATE_PATTERNS.forEach(pattern -> addDefaultParser(pattern, Style.DateOnly));

        // Standard Time patterns
        TIME_PATTERNS.forEach(pattern -> addDefaultParser(pattern, Style.TimeOnly));

        // Make full date/time combinations out of our date and time fragments
        DATE_PATTERNS.forEach(datePattern -> TIME_PATTERNS.forEach(timePattern -> {
            addDefaultParser(datePattern + " " + timePattern, Style.DateTime); // No TZ
            addDefaultParser(datePattern + " " + timePattern + " z", Style.DateTimeWithZone); // with TZ
        }));
    }

    /**
     * Create and add a default parser with the given pattern and style.
     */
    private static void addDefaultParser(String pattern, Style style)
    {
        DEFAULT_PARSERS.add(new DateTimeParser(pattern, style));
    }
}
