package gov.takx.commons.date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

/**
 * This class offers a set of utility methods for working with dates and time periods.
 *
 */
public final class DateFormatter
{
    private static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static final long SECOND_TIME = (long) 1e3;
    private static final long MINUTE_TIME = 60 * SECOND_TIME;
    private static final long HOUR_TIME = 60 * MINUTE_TIME;
    private static final long DAY_TIME = 24 * HOUR_TIME;
    private static final long WEEK_TIME = 7 * DAY_TIME;
    private static final long YEAR_TIME = 52 * WEEK_TIME;

    /**
     * This class is not intended to be instantiated, it only provides static methods.  This constructor
     * enforces that (and quiets PMD).
     */
    private DateFormatter()
    {
    }

    public static int getHourOfTheDayFromTime(long time)
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(time);
        return cal.get(Calendar.HOUR_OF_DAY);
    }

    public static int getDayOfTheWeekFromTime(long time)
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(time);
        return cal.get(Calendar.DAY_OF_WEEK);
    }

    public static int getMonthOfTheYearFromTime(long time)
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(time);
        return cal.get(Calendar.MONTH);
    }

    public static int getYearFromTime(long time)
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.setTimeInMillis(time);
        return cal.get(Calendar.YEAR);
    }

    public enum TimePeriod
    {
        YEARS("years"), WEEKS("weeks"), DAYS("days"), HOURS("hours"), MINUTES("minutes"), SECONDS("seconds");

        private final String name;

        TimePeriod(String name)
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }
    }

    /**
     * Formats the number of milliseconds passed as an elapsed time in the format of hh:mm:ss, days - hours, or
     * years - days, depending upon the magnitude of millis.
     *
     * @param elapsedTime Number of milliseconds to convert to an elapsed time format
     * @return Formatted elapsed time
     */
    public static String formatElapsedTime(long elapsedTime)
    {
        long MILLISECONDS_PER_YEAR = 31536000000L;

        int years = 0;
        long yearsInMillis = 0;
        if (elapsedTime >= MILLISECONDS_PER_YEAR)
        {
            years = (int) ((elapsedTime / MILLISECONDS_PER_YEAR) % 365);
            yearsInMillis = (years * MILLISECONDS_PER_YEAR);
        }

        long days = TimeUnit.MILLISECONDS.toDays(elapsedTime - yearsInMillis);

        if (years > 0)
        {
            return String.format("%d years, %d days",
                    years,
                    days);
        } else if (days > 0)
        {
            return String.format("%d days, %d hours",
                    days,
                    TimeUnit.MILLISECONDS.toHours(elapsedTime) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(elapsedTime)));
        } else
        {
            return String.format("%d:%02d:%02d",
                    TimeUnit.MILLISECONDS.toHours(elapsedTime) - TimeUnit.DAYS.toHours(TimeUnit.MILLISECONDS.toDays(elapsedTime)),
                    TimeUnit.MILLISECONDS.toMinutes(elapsedTime) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(elapsedTime)),
                    TimeUnit.MILLISECONDS.toSeconds(elapsedTime) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsedTime)));
        }
    }

    /**
     * This method attempts to parse and format the given date
     * If it fails to parse the date String it returns the original date
     *
     * @param date       to format
     * @param dateFormat String format for date (ie "yyyy-MM-dd HH:mm:ss")
     * @param timeZone   String value for timezone (ie "UTC")
     * @return The original date if parsing fails otherwise return the formatted date
     */
    public static String formatDate(String date, String dateFormat, String timeZone)
    {
        SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateInstance();

        String time;
        try
        {
            Date parsedDate = df.parse(date);
            time = formatDate(parsedDate, dateFormat, timeZone);
        } catch (ParseException e)
        {
            // We failed just return the original time
            time = date;
        }
        return time;
    }

    public static String formatDate(long date, String dateFormat, String timeZone)
    {
        return formatDate(new Date(date), dateFormat, timeZone);
    }

    public static String formatDate(Date date, String dateFormat, String timeZone)
    {
        SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateInstance();
        df.applyPattern(dateFormat);
        df.setTimeZone(TimeZone.getTimeZone(timeZone));

        return df.format(date);
    }

    /**
     * Send date in string format, along with format string and timezone, receive Date object or {@code null}
     *
     * @param date       String date (ie "2008-01-31 11:45:00")
     * @param dateFormat String format for date (ie "yyyy-MM-dd HH:mm:ss")
     * @param timeZone   String value for timezone (ie "UTC")
     * @return parsed Date object, or {@code null} if failed
     */
    public static Date parseDate(String date, String dateFormat, String timeZone)
    {
        try
        {
            SimpleDateFormat df = (SimpleDateFormat) DateFormat.getDateInstance();
            df.applyPattern(dateFormat);
            df.setTimeZone(TimeZone.getTimeZone(timeZone));
            return df.parse(date);
        } catch (ParseException e)
        {
            // Couldn't parse date, return null
            return null;
        }
    }

    public static TimeZone getTimeZone(String timeZone)
    {
        return TimeZone.getTimeZone(timeZone);
    }

    public static long getTimeFromSimplifiedTime(Map<TimePeriod, Long> map)
    {
        long time = 0;
        for (Map.Entry<TimePeriod, Long> entry : map.entrySet())
        {
            final TimePeriod timePeriod = entry.getKey();
            final Long timeValue = entry.getValue();

            switch (timePeriod)
            {
                case DAYS:
                    time += (DAY_TIME * timeValue);
                    break;
                case HOURS:
                    time += (HOUR_TIME * timeValue);
                    break;
                case MINUTES:
                    time += (MINUTE_TIME * timeValue);
                    break;
                case SECONDS:
                    time += (SECOND_TIME * timeValue);
                    break;
                case WEEKS:
                    time += (WEEK_TIME * timeValue);
                    break;
                case YEARS:
                    time += (YEAR_TIME * timeValue);
                    break;
                default:
                    logger.error("Unhandled time period: {}", timePeriod);
            }
        }
        logger.debug("getTimeFromSimplifiedTime = {}", time);
        return time;
    }

    public static Map<TimePeriod, Long> getSimplifiedTime(long time)
    {
        final Map<TimePeriod, Long> map = new HashMap<TimePeriod, Long>();
        long timeLeft = time;
        long years = timeLeft / YEAR_TIME;
        map.put(TimePeriod.YEARS, years);
        timeLeft = timeLeft - (years * YEAR_TIME);
        long weeks = timeLeft / WEEK_TIME;
        map.put(TimePeriod.WEEKS, weeks);
        timeLeft = timeLeft - (weeks * WEEK_TIME);
        long days = timeLeft / DAY_TIME;
        map.put(TimePeriod.DAYS, days);
        timeLeft = timeLeft - (days * DAY_TIME);
        long hours = timeLeft / HOUR_TIME;
        map.put(TimePeriod.HOURS, hours);
        timeLeft = timeLeft - (hours * HOUR_TIME);
        long minutes = timeLeft / MINUTE_TIME;
        map.put(TimePeriod.MINUTES, minutes);
        timeLeft = timeLeft - (minutes * MINUTE_TIME);
        long seconds = timeLeft / SECOND_TIME;
        map.put(TimePeriod.SECONDS, seconds);

        logger.debug("years = {} weeks = {} days = {} hours = {} minutes = {} seconds = {}", years, weeks, days, hours, minutes, seconds);
        return map;
    }

    public static String formatPrecise(long millis)
    {
        long[] hms = millisToHMS(millis);
        return String.format("%02d:%02d:%02d", hms[0], hms[1], hms[2]);
    }

    public static String formatEstimate(long millis)
    {
        StringBuilder result = new StringBuilder();

        long millisLeft = millis;

        // Less than a minute.
        if (millisLeft < MINUTE_TIME)
        {
            result.append("less than 1 minute");
        }
        // Report time in one-minute increments.
        else if (millisLeft < 10L * MINUTE_TIME)
        {
            millisLeft = MINUTE_TIME * Math.round(millisLeft / (double) MINUTE_TIME);
            long m = millisLeft / MINUTE_TIME;
            result.append("about ").append(m).append((m > 1 ? " minutes" : " minute"));
        }
        // Report time in ten-minute increments.
        else if (millisLeft < 55L * MINUTE_TIME)
        {
            millisLeft = 10L * MINUTE_TIME * Math.round(millisLeft / (10d * MINUTE_TIME));
            long m = millisLeft / MINUTE_TIME;
            result.append("about ").append(m).append(" minutes");
        }
        // Report time in half-hour increments.
        else
        {
            millisLeft = 30L * MINUTE_TIME * Math.round(millisLeft / (30d * MINUTE_TIME));
            long h = millisLeft / HOUR_TIME;
            result.append("about ").append(h).append((h > 1 ? " hours" : " hour"));
            long m = (millisLeft / MINUTE_TIME) % 60L;
            if (m > 0)
            {
                result.append(" ").append(m).append(" minutes");
            }
        }

        return result.toString();
    }

    /**
     * Returns the time value in milliseconds given the UTC-based year, month, day of month,
     * hour, minute and second.
     * Note: Month value is 0-based
     *
     * @param year      the value used to set the YEAR field
     * @param month     the value used to set the MONTH field
     *                  Month value is 0-based. e.g., 0 for January
     * @param date      the value used to set the DAY_OF_MONTH field
     * @param hourOfDay the value used to set the HOUR field
     * @param minute    the value used to set the MINUTE field
     * @param second    the value used to set the SECOND field
     * @return the corresponding time as UTC milliseconds from the epoch.
     */
    public static long getTimeFromValues(int year, int month, int date, int hourOfDay, int minute, int second)
    {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(year, month, date, hourOfDay, minute, second);
        return cal.getTimeInMillis();
    }

    private static long[] millisToHMS(long millis)
    {
        return new long[]{
                (long) (Math.floor(millis / (double) HOUR_TIME) % 60d),  // hours
                (long) (Math.floor(millis / (double) MINUTE_TIME) % 60d),  // minutes
                (long) (Math.floor(millis / (double) SECOND_TIME) % 60d)}; // seconds
    }
}
