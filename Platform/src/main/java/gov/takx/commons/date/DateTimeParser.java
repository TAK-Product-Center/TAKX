package gov.takx.commons.date;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.function.BiFunction;

/**
 * Utility class for parsing date/times using {@code DateTimeFormatter}.
 *
 * @since 3.0
 */
public class DateTimeParser
{
    /**
     * Standard fill ISO 8601 date/time format parser.
     */
    public static final DateTimeParser ISO_8601_DATE_FORMAT =
            new DateTimeParser(DateTimeParserSet.ISO_8601_DATE_FORMAT, Style.DateTimeWithZone);

    private static final ZoneId UTC = ZoneId.of("UTC");

    /**
     * Specifies the "style" of a pattern for a {@link DateTimeParser}.  This controls how the pattern is
     * interpreted, by defining which components it contains: date only, time only, date and time, date and time with
     * a timezone.
     */
    public enum Style
    {
        /**
         * Indicates a format that has only a time component.  Assumes a date of {@link LocalDate#EPOCH} and a
         * time zone of {@link ZoneOffset#UTC}
         */
        TimeOnly(
                (input, formatter) -> LocalTime.parse(input, formatter).atDate(LocalDate.EPOCH).toInstant(ZoneOffset.UTC),
                ((instant, formatter) -> LocalTime.ofInstant(instant, UTC).format(formatter))),

        /**
         * Indicates a format that has only a date component. Assumes a time zone of {@link ZoneOffset#UTC} and a time
         * of {@link LocalDate#atStartOfDay()}.
         */
        DateOnly(
                (input, formatter) -> LocalDate.parse(input, formatter).atStartOfDay().toInstant(ZoneOffset.UTC),
                ((instant, formatter) -> LocalDate.ofInstant(instant, UTC).format(formatter))),

        /**
         * Indicates a format that has a date and time component.  Assumes a time zone of {@link ZoneOffset#UTC}.
         */
        DateTime(
                (input, formatter) -> LocalDateTime.parse(input, formatter).toInstant(ZoneOffset.UTC),
                ((instant, formatter) -> LocalDateTime.ofInstant(instant, UTC).format(formatter))),

        /**
         * Indicates a format that has all date, time, and time zone components.
         */
        DateTimeWithZone(
                (input, formatter) -> ZonedDateTime.parse(input, formatter).toInstant(),
                ((instant, formatter) -> ZonedDateTime.ofInstant(instant, UTC).format(formatter)));

        // The function that parses a date/time String, accounting for defined components
        private final BiFunction<String, DateTimeFormatter, Instant> parseFunction;

        // The function that formats an Instant into a date/time String, accounting for defined components
        private final BiFunction<Instant, DateTimeFormatter, String> formatFunction;

        Style(BiFunction<String, DateTimeFormatter, Instant> parseFunction, BiFunction<Instant, DateTimeFormatter, String> formatFunction)
        {
            this.parseFunction = parseFunction;
            this.formatFunction = formatFunction;
        }

        /**
         * Apply the parsing function to the given input and return the resulting {@link Instant}.
         *
         * @param input     Input string to parse
         * @param formatter {@link DateTimeFormatter} to apply
         * @return Instant parsed from input according to our style
         * @throws DateTimeParseException If the input can't be parsed by the formatter
         */
        Instant toInstant(String input, DateTimeFormatter formatter) throws DateTimeParseException
        {
            return parseFunction.apply(input, formatter);
        }

        /**
         * Apply the formatting function to the given input instance and return the resulting String.
         *
         * @param instant   Instant to format
         * @param formatter {@link DateTimeFormatter} to apply
         * @return formatted string
         */
        String format(Instant instant, DateTimeFormatter formatter)
        {
            return formatFunction.apply(instant, formatter);
        }
    }

    private final String pattern; // The original pattern; used for store/restore
    private final DateTimeFormatter formatter; // Formatter based on the supplied pattern
    private final Style style; // Style, indicating how to process an input string

    /**
     * @param pattern The original pattern used to create the parser
     * @param style   Style indicating the type of format
     */
    public DateTimeParser(String pattern, Style style)
    {
        this.style = style;
        this.pattern = pattern;
        formatter = DateTimeFormatter.ofPattern(pattern).withResolverStyle(ResolverStyle.SMART);
    }

    /**
     * @param pattern The original pattern used to create the parser
     * @param style   Style indicating the type of format
     * @param zone    override zone id
     */
    public DateTimeParser(String pattern, Style style, ZoneId zone)
    {
        this.style = style;
        this.pattern = pattern;
        formatter = DateTimeFormatter.ofPattern(pattern).withZone(zone);
    }

    /**
     * @param source Source parser to copy from
     * @param zone   override zone id
     * @see DateTimeFormatter#withZone(ZoneId)
     */
    public DateTimeParser(DateTimeParser source, ZoneId zone)
    {
        style = source.style;
        pattern = source.pattern;
        formatter = source.formatter.withZone(zone);
    }

    /**
     * @return The format pattern
     */
    public String getPattern()
    {
        return pattern;
    }

    /**
     * @return The style defining how to parse input values
     */
    public Style getStyle()
    {
        return style;
    }

    /**
     * Format the given instant using the configured formatter.
     *
     * @param instant Instant to format
     * @return Instant formatted according to our Style using {@link DateTimeFormatter}
     */
    public String format(Instant instant)
    {
        return style.format(instant, formatter);
    }

    /**
     * Format the given milliseconds since epoch value using the configured formatter.
     *
     * @param epochMillis Milliseconds since the epoch to format
     * @return Time formatted according to our Style using {@link DateTimeFormatter}
     */
    public String format(long epochMillis)
    {
        return style.format(Instant.ofEpochMilli(epochMillis), formatter);
    }

    /**
     * Parse a date/time String and return the equivalent {@link Instant}.
     *
     * @param dateTimeString The date/time String to parse
     * @return The parsed {@code Instant}
     * @throws DateTimeParseException If the input can't be parsed by the formatter
     */
    public Instant toInstant(String dateTimeString) throws DateTimeParseException
    {
        return style.toInstant(dateTimeString, formatter);
    }

    /**
     * Parse a date/time String and return the resulting milliseconds since the epoch.
     *
     * @param dateTimeString The date/time String to parse
     * @return Milliseconds since the epoch of the parsed value
     * @throws DateTimeParseException If the input can't be parsed by the formatter
     */
    public long toEpochMilli(String dateTimeString)
    {
        return toInstant(dateTimeString).toEpochMilli();
    }

    /**
     * Create a new parser from a definition string.
     *
     * @see #toDefinition()
     */
    static DateTimeParser fromDefinition(String definitionString)
    {
        final String[] parts = definitionString.split(",", 2);
        return new DateTimeParser(parts[1], Style.valueOf(parts[0]));
    }

    /**
     * @return The definition String for this parser
     */
    String toDefinition()
    {
        // Add the pattern last, in case it has commas or other special chars
        return String.join(",", style.name(), pattern);
    }

    @Override
    public String toString()
    {
        return "[" + style + "] " + '\'' + pattern + '\'';
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DateTimeParser that = (DateTimeParser) o;

        if (!pattern.equals(that.pattern)) return false;
        return style == that.style;
    }

    @Override
    public int hashCode()
    {
        int result = pattern.hashCode();
        result = 31 * result + style.hashCode();
        return result;
    }
}
