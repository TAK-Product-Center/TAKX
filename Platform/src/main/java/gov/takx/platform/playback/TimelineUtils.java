package gov.takx.platform.playback;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * A collection of static utility methods for working with times and time spans.
 *
 * @since 1.47
 */
@SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
public class TimelineUtils
{
    public static final long SECOND = 1000;
    public static final long MINUTE = SECOND * 60;
    public static final long HOUR = MINUTE * 60;
    public static final long DAY = HOUR * 24;
    public static final long WEEK = DAY * 7;
    public static final long MONTH = DAY * 31;
    public static final long YEAR = DAY * 365;
    public static final long FIVE_YEARS = YEAR * 5;

    public static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEE, MM/dd/yy HH:mm:ss:SSS");
    public static final SimpleDateFormat SHORT_DATE_FORMAT = new SimpleDateFormat("MM/dd HH:mm");
    public static final SimpleDateFormat SHORTER_DATE_FORMAT = new SimpleDateFormat("MM/dd/yy");
    public static final SimpleDateFormat MINUTE_FORMAT = new SimpleDateFormat("HH:mm");
    public static final SimpleDateFormat SECOND_FORMAT = new SimpleDateFormat("HH:mm:ss");
    public static final SimpleDateFormat MILLISECOND_FORMAT = new SimpleDateFormat("HH:mm:ss:SSS");

    private static final String BASE_34_CHAR_MAP = "0123456789ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");
    private static final Instant START_OF_YEAR = new Calendar.Builder()
            .setTimeZone(UTC_TIME_ZONE)
            .setDate(Calendar.getInstance(UTC_TIME_ZONE).get(Calendar.YEAR), Calendar.JANUARY, 1)
            .setTimeOfDay(0, 0, 0).build().toInstant();

    public static String getDateRangeString(TimeBoundary timeBoundary)
    {
        return getDateRangeString(timeBoundary.startTime, timeBoundary.endTime);
    }

    /**
     * Given a start and end time, return an html fragment showing the start, end, and length of the time span.
     *
     * @param startTime Start time
     * @param endTime   End time
     * @return html string
     */
    public static String getDateRangeString(long startTime, long endTime)
    {
        return "<html>Start: " + getDateString(startTime) +
                "<br>End: " + getDateString(endTime) +
                "<br>Length: " + elapsedTime(endTime - startTime) + "</html>";
    }

    /**
     * Format the given time as a date using  {@link #DATE_FORMAT}.
     *
     * @param time Time to format
     * @return Formatted date string
     */
    public static String getDateString(long time)
    {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(time);
        return DATE_FORMAT.format(date.getTime());
    }

    /**
     * Format the given time as a date using  {@link #SECOND_FORMAT}.
     *
     * @param time Time to format
     * @return Formatted string
     */
    public static String getSecondString(long time)
    {
        return SECOND_FORMAT.format(getDateFromMillis(time));
    }

    /**
     * Format the given time as a date using  {@link #MINUTE_FORMAT}.
     *
     * @param time Time to format
     * @return Formatted string
     */
    public static String getMinuteString(long time)
    {
        return MINUTE_FORMAT.format(getDateFromMillis(time));
    }

    /**
     * Format the given time as a date using  {@link #MILLISECOND_FORMAT}.
     *
     * @param time Time to format
     * @return Formatted string
     */
    public static String getMillisecondString(long time)
    {
        return MILLISECOND_FORMAT.format(getDateFromMillis(time));
    }

    /**
     * Format the given time as a date using  {@link #SHORT_DATE_FORMAT}.
     *
     * @param time Time to format
     * @return Formatted string
     */
    public static String getShortDateString(long time)
    {
        return SHORT_DATE_FORMAT.format(time);
    }

    /**
     * Format the given time as a date using  {@link #SHORTER_DATE_FORMAT}.
     *
     * @param time Time to format
     * @return Formatted string
     */
    public static String getShorterDateString(long time)
    {
        return SHORTER_DATE_FORMAT.format(time);
    }

    /**
     * Get a Date instance initialized with the given time.
     *
     * @param millis Time to use
     * @return new Date instance
     */
    public static Date getDateFromMillis(long millis)
    {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(millis);
        return date.getTime();
    }

    /**
     * Return the whole minute greater than the given time unless the time is exactly a whole minute.
     *
     * @param time Time to operate upon
     * @return Ceiling minute
     */
    public static long ceilMinute(long time)
    {
        Calendar date = Calendar.getInstance();

        // Add 1 ms less than 1 minute, so when we truncate, we have the ceiling
        date.setTimeInMillis(time + (60 * 1000) - 1);

        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }

    /**
     * Round the given time to the nearest minute.
     *
     * @param time Time to round
     * @return Given time rounded to nearest whole minute
     */
    public static long roundMinute(long time)
    {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(time);

        date.add(Calendar.SECOND, 30);
        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }

    /**
     * Truncate the given time to a whole minute, no rounding.
     *
     * @param time Time to truncate
     * @return Given time truncated to whole minute
     */
    public static long floorMinute(long time)
    {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(time);

        date.set(Calendar.SECOND, 0);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }

    /**
     * Round the given time to the nearest second.
     *
     * @param time Time to round
     * @return Given time rounded to nearest whole second
     */
    public static long roundSecond(long time)
    {
        Calendar date = Calendar.getInstance();
        date.setTimeInMillis(time);
        date.add(Calendar.MILLISECOND, 500);
        date.set(Calendar.MILLISECOND, 0);
        return date.getTimeInMillis();
    }

    /**
     * code from:
     * http://stackoverflow.com/questions/6710094/how-to-format-an-elapsed-time-interval-in-hhmmss-sss-format-in-java
     * <p/>
     * The returned format is {@code String.format(&quot;%02d:%02d:%02d.%03d&quot;, hr, min, sec, ms)}
     *
     * @param time time in milliseconds
     * @return formatted string of elapsed time
     */
    public static String elapsedTime(long time)
    {
        final long hr = TimeUnit.MILLISECONDS.toHours(time);
        final long min = TimeUnit.MILLISECONDS.toMinutes(time - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(time - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        final long ms = TimeUnit.MILLISECONDS.toMillis(time - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min) - TimeUnit.SECONDS.toMillis(sec));
        return String.format("%02d:%02d:%02d.%03d", hr, min, sec, ms);
    }

    /**
     * code from:
     * http://stackoverflow.com/questions/6710094/how-to-format-an-elapsed-time-interval-in-hhmmss-sss-format-in-java
     * <p/>
     * The returned format is {@code String.format(&quot%02d:%02d:%02d&quot, hr, min, sec)}
     *
     * @param time time in milliseconds
     * @return formatted string of elapsed time
     */
    public static String elapsedTimeNoMillis(long time)
    {
        final long hr = TimeUnit.MILLISECONDS.toHours(time);
        final long min = TimeUnit.MILLISECONDS.toMinutes(time - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(time - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        return String.format("%02d:%02d:%02d", hr, min, sec);
    }

    /**
     * code from:
     * http://stackoverflow.com/questions/6710094/how-to-format-an-elapsed-time-interval-in-hhmmss-sss-format-in-java
     * <p/>
     * The returned format is {@code String.format(&quot%02d:%02d:%02d:%02d&quot, day, hr, min, sec)}
     *
     * @param time time in milliseconds
     * @return formatted string of elapsed time
     */
    public static String elapsedTimeDays(long time)
    {
        final long day = TimeUnit.MILLISECONDS.toDays(time);
        final long hr = TimeUnit.MILLISECONDS.toHours(time - TimeUnit.DAYS.toMillis(day));
        final long min = TimeUnit.MILLISECONDS.toMinutes(time - TimeUnit.DAYS.toMillis(day) - TimeUnit.HOURS.toMillis(hr));
        final long sec = TimeUnit.MILLISECONDS.toSeconds(time - TimeUnit.DAYS.toMillis(day) - TimeUnit.HOURS.toMillis(hr) - TimeUnit.MINUTES.toMillis(min));
        return String.format("%02d:%02d:%02d:%02d", day, hr, min, sec);
    }

    private TimelineUtils()
    {
        /* no instances */
    }
}
