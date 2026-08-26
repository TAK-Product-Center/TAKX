package gov.takx.api.messages;

import gov.takx.commons.date.DateFormatSet;
import gov.takx.commons.date.DateTimeParser;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.stream.Stream;

/**
 * Utility methods for encoding and decoding {@link IDataField} values to/from String form.
 *
 * @since 3.0
 */
@SuppressWarnings("UseOfObsoleteDateTimeApi")
public final class DataFieldEncoding
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Delimiter used to separate encoded array values.
     */
    public static final String DELIMITER = "|*|";

    public static final String DELIMITER_REGEX = "\\|\\*\\|";

    /**
     * Default date formatter used to format dates to/from String form.
     */
    public static final DateTimeParser DEFAULT_DATE_FORMATTER =
            new DateTimeParser(DateTimeParser.ISO_8601_DATE_FORMAT, ZoneId.of("UTC"));

    private static final DateFormatSet ALL_DATE_FORMATS = new DateFormatSet();

    private static final String[] EMPTY_STRINGS = new String[0];

    /**
     * Make the string version of the given field value, according to the given type.  The formatting used is:
     * <ul>
     *     <li>Primitive types are converted using {@link Object#toString()}</li>
     *     <li>Dates are formatted using the given date formatter</li>
     *     <li>Byte arrays are {@link Base64} encoded</li>
     *     <li>Array types are formatted by joining the string form of each element separated by
     *     {@link #DELIMITER}.  Date arrays are first formatted as Date types and then joined.</li>
     * </ul>
     *
     * @param fieldName  Name of field, for error messages
     * @param fieldType  Type of field to encode
     * @param value      Field value to encode
     * @param dateFormat Format to use for Date/Date array types
     * @return String form of value
     * @since 3.0
     */
    public static String encodeFieldValue(String fieldName, int fieldType, Object value, DateTimeParser dateFormat)
    {
        if (value == null) return "";

        try
        {
            final DateTimeParser formatter = dateFormat != null ? dateFormat : DEFAULT_DATE_FORMATTER;

            switch (fieldType)
            {
                case IDataType.STRING:
                case IDataType.BOOLEAN:
                case IDataType.INT:
                case IDataType.FLOAT:
                case IDataType.SHORT:
                case IDataType.LONG:
                case IDataType.DOUBLE:
                case IDataType.UUID:
                    return value.toString();

                case IDataType.DATE:
                    return formatDate((Date) value, formatter);

                case IDataType.DATE_ARRAY:
                    final String[] strings = Stream.of((Date[]) value)
                            .map(date -> formatDate(date, formatter))
                            .toArray(String[]::new);
                    return StringUtils.join(strings, DELIMITER, null);

                case IDataType.BYTE_ARRAY:
                    return Base64.getEncoder().encodeToString((byte[]) value);

                case IDataType.STRING_ARRAY:
                    return StringUtils.join((String[]) value, DELIMITER, null);
                case IDataType.INT_ARRAY:
                    return StringUtils.join((int[]) value, DELIMITER, null);
                case IDataType.SHORT_ARRAY:
                    return StringUtils.join((short[]) value, DELIMITER, null);
                case IDataType.LONG_ARRAY:
                    return StringUtils.join((long[]) value, DELIMITER, null);
                case IDataType.BOOLEAN_ARRAY:
                    return StringUtils.join((boolean[]) value, DELIMITER, null);
                case IDataType.FLOAT_ARRAY:
                    return StringUtils.join((float[]) value, DELIMITER, null);
                case IDataType.DOUBLE_ARRAY:
                    return StringUtils.join((double[]) value, DELIMITER, null);
                default:
                    logger.error("Unknown data type on field name {}: {}", fieldName, fieldType);
            }
        } catch (Exception e)
        {
            logger.error("Unable to encode the value of field {} with data type {} into a string", fieldName, fieldType, e);
        }

        return "";
    }

    /**
     * Decode the given String value according to the field type.
     *
     * @param fieldName      The name of the data field
     * @param fieldType      The type the value should be decoded as
     * @param value          The string value to decode
     * @param dateTimeParser Nullable: If the type is a date this is the format that will be applied to it.
     * @return The parsed value as the passed type, may be {@code null}
     * @throws NumberFormatException If a primitive value can't be parsed
     */
    @Nullable
    public static Object decodeFieldValue(@Nonnull final String fieldName, final int fieldType, @Nonnull final String value, @Nullable DateTimeParser dateTimeParser)
            throws NumberFormatException
    {
        final String[] split;

        switch (fieldType)
        {
            case IDataType.STRING:
                return value;
            case IDataType.BOOLEAN:
                return Boolean.valueOf(value);
            case IDataType.INT:
                return Integer.valueOf(value);
            case IDataType.FLOAT:
                return Float.valueOf(value);
            case IDataType.DOUBLE:
                return Double.valueOf(value);
            case IDataType.SHORT:
                return Short.valueOf(value);
            case IDataType.LONG:
                return Long.valueOf(value);
            case IDataType.DATE:
                return parseDate(value, dateTimeParser);
            case IDataType.UUID:
                return java.util.UUID.fromString(value);

            case IDataType.BYTE_ARRAY:
                return Base64.getDecoder().decode(value);

            case IDataType.STRING_ARRAY:
                return value.split(DELIMITER_REGEX);
            case IDataType.SHORT_ARRAY:
                split = splitValue(value);
                final short[] shorts = new short[split.length];
                for (int i = 0; i < split.length; i++) shorts[i] = Short.parseShort(split[i]);
                return shorts;
            case IDataType.INT_ARRAY:
                split = splitValue(value);
                final int[] ints = new int[split.length];
                for (int i = 0; i < split.length; i++) ints[i] = Integer.parseInt(split[i]);
                return ints;
            case IDataType.LONG_ARRAY:
                split = splitValue(value);
                final long[] longs = new long[split.length];
                for (int i = 0; i < split.length; i++) longs[i] = Long.parseLong(split[i]);
                return longs;
            case IDataType.BOOLEAN_ARRAY:
                split = splitValue(value);
                final boolean[] booleans = new boolean[split.length];
                for (int i = 0; i < split.length; i++) booleans[i] = Boolean.parseBoolean(split[i]);
                return booleans;
            case IDataType.FLOAT_ARRAY:
                split = splitValue(value);
                final float[] floats = new float[split.length];
                for (int i = 0; i < split.length; i++) floats[i] = Float.parseFloat(split[i]);
                return floats;
            case IDataType.DOUBLE_ARRAY:
                split = splitValue(value);
                final double[] doubles = new double[split.length];
                for (int i = 0; i < split.length; i++) doubles[i] = Double.parseDouble(split[i]);
                return doubles;
            case IDataType.DATE_ARRAY:
                split = splitValue(value);
                final Date[] dates = new Date[split.length];
                for (int i = 0; i < split.length; i++) dates[i] = parseDate(split[i], dateTimeParser);
                return dates;
            default:
                logger.error("Unknown data type on field name {}: {}", fieldName, fieldType);
                return null;
        }
    }

    /**
     * Format a Date using the given parser, or the default if {@code null}.
     *
     * @param date   Date to format
     * @param parser Formatter to use, if {@code null} use {@link #DEFAULT_DATE_FORMATTER}
     * @return Formatted date
     */
    private static String formatDate(@Nonnull Date date, @Nullable DateTimeParser parser)
    {
        if (parser == null) parser = DEFAULT_DATE_FORMATTER;
        return parser.format(Instant.ofEpochMilli((date).getTime()));
    }

    /**
     * Parse a date string into a Date.
     *
     * @param value          Value to parse
     * @param dateTimeParser Parser to use, if {@code null} use {@link DateFormatSet}
     * @return Parsed date, or {@code null}
     */
    private static Date parseDate(String value, DateTimeParser dateTimeParser)
    {
        return dateTimeParser != null
                ? Date.from(dateTimeParser.toInstant(value))
                : ALL_DATE_FORMATS.parseDate(value);
    }

    /**
     * Split the given value using our default delimiter.  If the result is empty, then return an empty array.
     *
     * @param value Value to split
     * @return Array of values, or empty if there were no values in the input
     * @since 3.0
     */
    private static String[] splitValue(String value)
    {
        final String[] split = value.split(DELIMITER_REGEX);

        return split.length == 1 && split[0].isEmpty()
                ? EMPTY_STRINGS
                : split;
    }

    // No instances
    private DataFieldEncoding()
    {
    }
}
