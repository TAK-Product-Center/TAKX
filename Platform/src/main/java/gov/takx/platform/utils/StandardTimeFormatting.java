package gov.takx.platform.utils;

import gov.takx.api.annotation.InjectableService;
import gov.takx.api.annotation.ProgramPreference;
import gov.takx.api.annotation.UserPreference;
import gov.takx.commons.date.DateTimeParser;
import gov.takx.platform.preferences.PreferenceEntry;
import gov.takx.platform.preferences.UserPreferenceEntry;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * Methods for providing a common, and consistent, formatting of date/times according to the
 * program and user preferences.
 *
 * @since 2.37
 */
@ApplicationScoped
@InjectableService
public class StandardTimeFormatting
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Our standard date and time formats.
     */
    public static final String DEFAULT_FORMAT = "yyyy-MM-dd HH:mm:ss z";
    public static final String DEFAULT_DATE_TIME_FORMAT = DEFAULT_FORMAT;
    public static final String DEFAULT_DATE_ONLY_FORMAT = "yyyy-MM-dd";
    public static final String DEFAULT_TIME_ONLY_FORMAT = "HH:mm:ss";
    /** @since 5.2 */
    public static final String DEFAULT_TIME_SIMPLE = "HH:mm";
    /** @since 5.2 */
    public static final String DEFAULT_MONTH_DAY = "MMM dd";
    /** @since 5.4 */
    public static final String DEFAULT_DAY_FORMAT = "dd";
    /** @since 5.4 */
    public static final String DEFAULT_MONTH_FORMAT = "MMM";
    /** @since 5.4 */
    public static final String DEFAULT_YEAR_FORMAT = "yyyy";
    /** @since 5.4 */
    public static final String ISO_INSTANT_SHORT = "yyyy-MM-dd'T'HH:mm:ss.S'Z'";

    /**
     * A formatter initialized with {@link #DEFAULT_FORMAT} and a time zone of UTC.
     */
    public static final DateTimeFormatter STANDARD_UTC_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_FORMAT).withZone(ZoneOffset.UTC);

    private final String localTimeZoneId;
    private final String dateFormat;
    private final String userTimeZoneId;
    private boolean displayLocalTime;

    // TODO KMB: To reduce risk for including this change in MVP1, I kept the existing functionality the same and
    //  simply added new methods for chat to use. Ideally, I think we can just get rid of these formatters (and the
    //  formatWithCurrent() method) after the release when we have time to test to make sure it doesn't break anything.
    private DateTimeFormatter defaultDateFormatter;
    private DateTimeFormatter localDateTimeFormatter;
    private DateTimeFormatter currentDateTimeFormatter;

    private DateTimeFormatter defaultTimeSimpleFormatter;
    private DateTimeFormatter localTimeSimpleFormatter;
    private DateTimeFormatter currentTimeSimpleFormatter;
    private DateTimeFormatter defaultMonthDayFormatter;
    private DateTimeFormatter localMonthDayFormatter;
    private DateTimeFormatter currentMonthDayFormatter;
    private DateTimeFormatter defaultDayFormatter;
    private DateTimeFormatter localDayFormatter;
    private DateTimeFormatter currentDayFormatter;
    private DateTimeFormatter defaultMonthFormatter;
    private DateTimeFormatter localMonthFormatter;
    private DateTimeFormatter currentMonthFormatter;
    private DateTimeFormatter defaultYearFormatter;
    private DateTimeFormatter localYearFormatter;
    private DateTimeFormatter currentYearFormatter;

    private DateTimeParser currentDateTimeParser;
    private DateTimeParser currentDateOnlyParser;
    private DateTimeParser currentTimeOnlyParser;

    private DateTimeParser defaultDateTimeParser;
    private DateTimeParser defaultDateOnlyParser;
    private DateTimeParser defaultTimeOnlyParser;

    private DateTimeParser localDateTimeParser;
    private DateTimeParser localDateOnlyParser;
    private DateTimeParser localTimeOnlyParser;

    @Inject
    StandardTimeFormatting(@ProgramPreference(PreferenceEntry.LOCAL_TIME_ZONE) String localTimeZoneId,
                           @UserPreference(UserPreferenceEntry.DATE_FORMAT) String dateFormat,
                           @UserPreference(UserPreferenceEntry.TIME_ZONE) String userTimeZoneId,
                           @UserPreference(UserPreferenceEntry.DISPLAY_LOCAL_TIME) boolean displayLocalTime)
    {
        this.localTimeZoneId = localTimeZoneId;
        this.dateFormat = dateFormat;
        this.userTimeZoneId = userTimeZoneId;
        this.displayLocalTime = displayLocalTime;
    }

    @PostConstruct
    private void postConstruct()
    {
        try
        {
            final ZoneId zoneId = ZoneId.of(userTimeZoneId);
            defaultDateFormatter = DateTimeFormatter.ofPattern(dateFormat).withZone(zoneId);
            defaultDateTimeParser = new DateTimeParser(dateFormat, DateTimeParser.Style.DateTime, zoneId);

            // There are no user preferences for date only and time only formats, so just use default for now
            defaultDateOnlyParser = new DateTimeParser(DEFAULT_DATE_ONLY_FORMAT, DateTimeParser.Style.DateTimeWithZone, zoneId);
            defaultTimeOnlyParser = new DateTimeParser(DEFAULT_TIME_ONLY_FORMAT, DateTimeParser.Style.DateTimeWithZone, zoneId);

            defaultTimeSimpleFormatter = DateTimeFormatter.ofPattern(DEFAULT_TIME_SIMPLE).withZone(zoneId);
            defaultMonthDayFormatter = DateTimeFormatter.ofPattern(DEFAULT_MONTH_DAY).withZone(zoneId);
            defaultDayFormatter = DateTimeFormatter.ofPattern(DEFAULT_DAY_FORMAT).withZone(zoneId);
            defaultMonthFormatter = DateTimeFormatter.ofPattern(DEFAULT_MONTH_FORMAT).withZone(zoneId);
            defaultYearFormatter = DateTimeFormatter.ofPattern(DEFAULT_YEAR_FORMAT).withZone(zoneId);
        } catch (Exception e)
        {
            logger.error("Failed to initialize standard date/time formatters", e);
            defaultDateFormatter = STANDARD_UTC_FORMATTER;
            defaultDateTimeParser = new DateTimeParser(DEFAULT_DATE_TIME_FORMAT, DateTimeParser.Style.DateTimeWithZone, ZoneOffset.UTC);
            defaultDateOnlyParser = new DateTimeParser(DEFAULT_DATE_ONLY_FORMAT, DateTimeParser.Style.DateTimeWithZone, ZoneOffset.UTC);
            defaultTimeOnlyParser = new DateTimeParser(DEFAULT_TIME_ONLY_FORMAT, DateTimeParser.Style.DateTimeWithZone, ZoneOffset.UTC);

            defaultTimeSimpleFormatter = DateTimeFormatter.ofPattern(DEFAULT_TIME_SIMPLE).withZone(ZoneOffset.UTC);
            defaultMonthDayFormatter = DateTimeFormatter.ofPattern(DEFAULT_MONTH_DAY).withZone(ZoneOffset.UTC);
            defaultDayFormatter = DateTimeFormatter.ofPattern(DEFAULT_DAY_FORMAT).withZone(ZoneOffset.UTC);
            defaultMonthFormatter = DateTimeFormatter.ofPattern(DEFAULT_MONTH_FORMAT).withZone(ZoneOffset.UTC);
            defaultYearFormatter = DateTimeFormatter.ofPattern(DEFAULT_YEAR_FORMAT).withZone(ZoneOffset.UTC);
        }

        try
        {
            final ZoneId zoneId = ZoneId.of(localTimeZoneId);
            localDateTimeFormatter = DateTimeFormatter.ofPattern(dateFormat).withZone(zoneId);
            localDateTimeParser = new DateTimeParser(dateFormat, DateTimeParser.Style.DateTime, zoneId);
            localDateOnlyParser = new DateTimeParser(DEFAULT_DATE_ONLY_FORMAT, DateTimeParser.Style.DateTimeWithZone, zoneId);
            localTimeOnlyParser = new DateTimeParser(DEFAULT_TIME_ONLY_FORMAT, DateTimeParser.Style.DateTimeWithZone, zoneId);

            localTimeSimpleFormatter = DateTimeFormatter.ofPattern(DEFAULT_TIME_SIMPLE).withZone(zoneId);
            localMonthDayFormatter = DateTimeFormatter.ofPattern(DEFAULT_MONTH_DAY).withZone(zoneId);
            localDayFormatter = DateTimeFormatter.ofPattern(DEFAULT_DAY_FORMAT).withZone(zoneId);
            localMonthFormatter = DateTimeFormatter.ofPattern(DEFAULT_MONTH_FORMAT).withZone(zoneId);
            localYearFormatter = DateTimeFormatter.ofPattern(DEFAULT_YEAR_FORMAT).withZone(zoneId);
        } catch (Exception e)
        {
            logger.error("Failed to initialize standard local date/time formatters", e);
            localDateTimeFormatter = STANDARD_UTC_FORMATTER;
            localDateTimeParser = new DateTimeParser(DEFAULT_DATE_TIME_FORMAT, DateTimeParser.Style.DateTimeWithZone, ZoneOffset.UTC);
            localDateOnlyParser = new DateTimeParser(DEFAULT_DATE_ONLY_FORMAT, DateTimeParser.Style.DateTimeWithZone, ZoneOffset.UTC);
            localTimeOnlyParser = new DateTimeParser(DEFAULT_TIME_ONLY_FORMAT, DateTimeParser.Style.DateTimeWithZone, ZoneOffset.UTC);

            localTimeSimpleFormatter = DateTimeFormatter.ofPattern(DEFAULT_TIME_SIMPLE).withZone(ZoneOffset.UTC);
            localMonthDayFormatter = DateTimeFormatter.ofPattern(DEFAULT_MONTH_DAY).withZone(ZoneOffset.UTC);
            localDayFormatter = DateTimeFormatter.ofPattern(DEFAULT_DAY_FORMAT).withZone(ZoneOffset.UTC);
            localMonthFormatter = DateTimeFormatter.ofPattern(DEFAULT_MONTH_FORMAT).withZone(ZoneOffset.UTC);
            localYearFormatter = DateTimeFormatter.ofPattern(DEFAULT_YEAR_FORMAT).withZone(ZoneOffset.UTC);
        }

        selectFormatters();
    }

    /**
     * Select the current formatter based on the "displayLocalTime" value.
     */
    private void selectFormatters()
    {
        currentDateTimeFormatter = displayLocalTime ? localDateTimeFormatter : defaultDateFormatter;
        currentDateTimeParser = displayLocalTime ? localDateTimeParser : defaultDateTimeParser;
        currentDateOnlyParser = displayLocalTime ? localDateOnlyParser : defaultDateOnlyParser;
        currentTimeOnlyParser = displayLocalTime ? localTimeOnlyParser : defaultTimeOnlyParser;
        currentTimeSimpleFormatter = displayLocalTime ? localTimeSimpleFormatter : defaultTimeSimpleFormatter;
        currentMonthDayFormatter = displayLocalTime ? localMonthDayFormatter : defaultMonthDayFormatter;
        currentDayFormatter = displayLocalTime ? localDayFormatter : defaultDayFormatter;
        currentMonthFormatter = displayLocalTime ? localMonthFormatter : defaultMonthFormatter;
        currentYearFormatter = displayLocalTime ? localYearFormatter : defaultYearFormatter;
    }

    /**
     * Check the current {@link UserPreferenceEntry#DISPLAY_LOCAL_TIME} value. Update the current parsers/formatters if
     * needed.
     * <p>
     * NOTE: Don't use {@code @Observes} to update the local time preference and parsers/formatters. It will lead to
     * race conditions with classes that are also observing the preference and calling this class when it changes.
     *
     * @since 5.2
     */
    private void checkLocalTimePreference()
    {
        final boolean displayLocalTime = UserPreferenceEntry.DISPLAY_LOCAL_TIME.getBooleanValue();

        if (this.displayLocalTime != displayLocalTime)
        {
            this.displayLocalTime = displayLocalTime;
            selectFormatters();
        }
    }

    /**
     * Format the given time using the default formatter initialized from the date format and time zone specified in
     * the user preferences.
     *
     * @param epochMillis time to format
     * @return Formatted time
     */
    public String formatWithDefault(long epochMillis)
    {
        checkLocalTimePreference();
        return defaultDateFormatter.format(Instant.ofEpochMilli(epochMillis));
    }

    /**
     * Format the given time using the "current" formatter initialized from the date format specified in
     * the user preferences.  The time zone is determined by the current setting of
     * {@link UserPreferenceEntry#DISPLAY_LOCAL_TIME}.
     *
     * @param epochMillis time to format
     * @return Formatted time
     */
    public String formatWithCurrent(long epochMillis)
    {
        checkLocalTimePreference();
        return currentDateTimeFormatter.format(Instant.ofEpochMilli(epochMillis));
    }

    /**
     * Format the given milliseconds since epoch as a date/time string using the "current" formatter initialized from
     * the date format specified in the user preferences. The time zone is determined by the current setting of
     * {@link UserPreferenceEntry#DISPLAY_LOCAL_TIME}.
     *
     * @param epochMillis date/time to format in milliseconds since epoch
     * @return Formatted date/time as string
     * @since 3.0
     */
    public String formatDateTimeWithCurrent(long epochMillis)
    {
        checkLocalTimePreference();
        return currentDateTimeParser.format(epochMillis);
    }

    /**
     * Format the given milliseconds since epoch as a date string using the "current" formatter initialized from the
     * date format specified in the user preferences. The time zone is determined by the current setting of
     * {@link UserPreferenceEntry#DISPLAY_LOCAL_TIME}.
     *
     * @param epochMillis date to format in milliseconds since epoch
     * @return Formatted date as string
     * @since 3.0
     */
    public String formatDateOnlyWithCurrent(long epochMillis)
    {
        checkLocalTimePreference();
        return currentDateOnlyParser.format(epochMillis);
    }

    /**
     * Format the given milliseconds since epoch as a time string using the "current" formatter initialized from the
     * date format specified in the user preferences. The time zone is determined by the current setting of
     * {@link UserPreferenceEntry#DISPLAY_LOCAL_TIME}.
     *
     * @param epochMillis time to format in milliseconds since epoch
     * @return Formatted time as string
     * @since 3.0
     */
    public String formatTimeOnlyWithCurrent(long epochMillis)
    {
        checkLocalTimePreference();
        return currentTimeOnlyParser.format(epochMillis);
    }

    /**
     * Converts epoch milliseconds to a date or time String. If the date is today, a time String with hours and
     * minutes will be returned (e.g. 17:56). Otherwise, a date String with the abbreviated month and day will be
     * returned (e.g. Mar 13).
     *
     * @param epochMillis time to format in milliseconds since epoch
     * @return The date or time String
     * @since 5.2
     */
    public String formatTimeSimpleOrMonthDay(long epochMillis)
    {
        return formatTimeSimpleOrMonthDay(Instant.ofEpochMilli(epochMillis));
    }

    /**
     * @param instant The instant to format
     * @return The day number String
     * @since 5.4
     */
    public String formatDay(@Nonnull Instant instant)
    {
        checkLocalTimePreference();
        return currentDayFormatter.format(instant);
    }

    /**
     * @param instant The instant to format
     * @return The abbreviated month name String
     * @since 5.4
     */
    public String formatMonth(@Nonnull Instant instant)
    {
        checkLocalTimePreference();
        return currentMonthFormatter.format(instant);
    }

    /**
     * @param instant The instant to format
     * @return The year String
     * @since 5.4
     */
    public String formatYear(@Nonnull Instant instant)
    {
        checkLocalTimePreference();
        return currentYearFormatter.format(instant);
    }

    /**
     * Converts an Instant to a date or time String. If the Instant is from today, a time String with hours and minutes
     * will be returned (e.g. 17:56). Otherwise, a date String with the abbreviated month and day will be returned
     * (e.g. Mar 13).
     *
     * @param instant The Instant to convert
     * @return The date or time String
     * @since 5.2
     */
    public String formatTimeSimpleOrMonthDay(@Nonnull Instant instant)
    {
        checkLocalTimePreference();

        final LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, Clock.systemDefaultZone().getZone());
        final LocalDate localDate = localDateTime.toLocalDate();

        // If the message was sent today, display the time. Otherwise, display the date.
        return LocalDate.now().equals(localDate)
                ? currentTimeSimpleFormatter.format(instant)
                : currentMonthDayFormatter.format(instant);
    }

    /**
     * Provides the approximate duration between the provided Instants. If more than a day, the value will be returned
     * as "1 day" or "[] days". Otherwise, if the duration is more than an hour, the value will be returned as "[]h []s".
     * Otherwise, the value will be returned as "[]m []s".
     * <p>
     * Whole values will be used and will not be rounded (e.g. 2.5 days will be returned as 2 days).
     * <p>
     * Example results:
     * <ul>
     *     <li>1m 34s</li>
     *     <li>4h 20m</li>
     *     <li>739 days</li>
     * </ul>
     *
     * @param start The start of the duration
     * @param end The end of the duration
     * @return The formatted duration
     * @since 5.3
     */
    @Nonnull
    public String formatApproximateDuration(@Nonnull Instant start, @Nonnull Instant end)
    {
        final var duration = Duration.between(start, end);

        final long days = duration.toDaysPart();

        if (days > 0) return days + (days > 1 ? " days" : " day");

        final int hours = duration.toHoursPart();
        final int minutes = duration.toMinutesPart();

        if (hours > 0 && minutes > 0) return hours + "h " + minutes + "m";

        final long seconds = duration.toSecondsPart();
        return minutes + "m " + seconds + "s";
    }

    /**
     * Provides the approximate duration between the provided Instant and now. See {@link #formatApproximateDuration(Instant, Instant)}
     * for formatting info. If the duration is greater than six days, the date will be returned.
     * <p>
     * Examples results if no suffix is supplied:
     * <ul>
     *     <li>1m 34s</li>
     *     <li>4h 20m</li>
     *     <li>3 days</li>
     *     <li>1989-12-13</li>
     * </ul>
     * <p>
     * Examples results if the suffix "ago" is supplied:
     * <ul>
     *     <li>1m 34s ago</li>
     *     <li>4h 20m ago</li>
     *     <li>3 days ago</li>
     *     <li>1989-12-13</li>
     * </ul>
     *
     * @param start The start of the duration
     * @param suffix Optional suffix to add to the end of the duration if the result is not a date, e.g. "ago" for "1h 10m ago"
     * @return The formatted duration
     * @since 5.3
     * @see #formatApproximateDuration(Instant, Instant)
     */
    @Nonnull
    public String formatApproximateDurationSince(@Nonnull Instant start, @Nullable String suffix)
    {
        final Instant now = Instant.now();

        return Duration.between(start, now).toDaysPart() > 6
                ? formatDateOnlyWithCurrent(start.toEpochMilli())
                : formatApproximateDuration(start, now) + (suffix != null ? " " + suffix : "");
    }
}
