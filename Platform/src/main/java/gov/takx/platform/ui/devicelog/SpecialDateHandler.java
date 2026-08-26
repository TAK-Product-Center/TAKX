package gov.takx.platform.ui.devicelog;

import gov.takx.api.query.ComparisonType;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Handle special date formats in filters.  This includes date search specifications of the form
 * "January" or "Wednesday" or "11:00" to search against an month, day, or hour, respectively.
 * <p/>
 * In order to get correct results, you must first call {@link #parseSpecialDateType(String, java.text.ParseException)}
 * prior to calling {@link #matchDate(long, ComparisonType, String, ComparisonType, String)}.
 * <p/>
 * This class is package access since it is not well designed and it serves a very special purpose (it only supports
 * field filtering in the device log).  We can decide to make it more available when the interface is cleaned up.
 * I didn't write this, I just pulled it out of the device log panel into a stand alone class.
 *
 * @since 1.49
 */
class SpecialDateHandler
{
    private enum MONTHS
    {
        Jan, Feb, Mar, Apr, May, Jun, Jul, Aug, Sep, Oct, Nov, Dec
    }

    private enum WEEKDAYS
    {
        Mon, Tue, Wed, Thu, Fri, Sat, Sun
    }

    private enum SpecialDate
    {
        Hour(hourFormat),
        Weekday(weekdayFormat),
        Month(monthFormat);

        private final SimpleDateFormat format;

        private SpecialDate(SimpleDateFormat format)
        {
            this.format = format;
        }
    }

    private SpecialDate specialDateType;

    private static final SimpleDateFormat hourFormat;
    private static final SimpleDateFormat extendedHourFormat;
    private static final SimpleDateFormat weekdayFormat;
    private static final SimpleDateFormat monthFormat;

    static
    {
        hourFormat = new SimpleDateFormat("kk:mm");
        extendedHourFormat = new SimpleDateFormat("kk:mm:ss");
        weekdayFormat = new SimpleDateFormat("EEE");
        monthFormat = new SimpleDateFormat("MMM");

        hourFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        extendedHourFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        weekdayFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
        monthFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    /**
     * Clear out any prior date type determined by a call to {@link #parseSpecialDateType(String, java.text.ParseException)}.
     */
    public void clear()
    {
        specialDateType = null;
    }

    /**
     * @return {@code true} if a special date type was matched
     */
    public boolean isSpecialDateTypeMatched()
    {
        return specialDateType != null;
    }

    /**
     * Given a value entered by the user, determine which (if any) special date type it matches.  Return the given
     * value formatted according to the matched special date type and initialize the matched date type.
     * <p/>
     * In the case that we fail to properly match/parse a supported date/time specifier, we will rethrow the
     * supplied parse exception.
     *
     * @param value The user entered value to match against
     * @param pe    An original parse exception to rethrow if the given value fails to fully match one of our types
     * @return The input value formatted according to the matched special type
     * @throws ParseException
     */
    public Object parseSpecialDateType(String value, ParseException pe) throws ParseException
    {
        String result = null;

        final int valueLength = value.length();
        boolean findSecondCriteria = specialDateType != null;
        final ParsePosition pp = new ParsePosition(0);

        Date date = hourFormat.parse(value, pp);

        if (pp.getIndex() == valueLength)
        {
            result = hourFormat.format(date);

            if (findSecondCriteria)
            {
                findSecondCriteria = false;
                if (specialDateType != SpecialDate.Hour)
                {
                    throw pe;
                }
            } else
            {
                specialDateType = SpecialDate.Hour;
            }
        }

        if (specialDateType == null || findSecondCriteria)
        {
            pp.setIndex(0);
            date = weekdayFormat.parse(value, pp);

            if (pp.getIndex() == valueLength)
            {
                result = weekdayFormat.format(date);

                if (findSecondCriteria)
                {
                    findSecondCriteria = false;
                    if (specialDateType != SpecialDate.Weekday)
                    {
                        throw pe;
                    }
                } else
                {
                    specialDateType = SpecialDate.Weekday;
                }
            }
        }

        if (specialDateType == null || findSecondCriteria)
        {
            pp.setIndex(0);
            date = monthFormat.parse(value, pp);

            if (pp.getIndex() == valueLength)
            {
                result = monthFormat.format(date);

                if (findSecondCriteria)
                {
                    findSecondCriteria = false;
                    if (specialDateType != SpecialDate.Month)
                    {
                        throw pe;
                    }
                } else
                {
                    specialDateType = SpecialDate.Month;
                }
            }
        }

        if (specialDateType == null || findSecondCriteria)
        {
            throw pe;
        }

        return result;
    }

    /**
     * Try to match a given value against the currently configured special date type using the supplied comparison
     * types and values.  You must have called {@link #parseSpecialDateType(String, java.text.ParseException)}
     * before calling this method.
     *
     * @param time               The value to compare
     * @param filter1Comparison  First comparison type
     * @param filter1ValueObject First comparison value
     * @param filter2Comparison  Second comparison type
     * @param filter2ValueObject Second comparison value
     * @return {@code true} if the given value matches the comparison constraints
     */
    public boolean matchDate(long time, ComparisonType filter1Comparison, String filter1ValueObject,
                             ComparisonType filter2Comparison, String filter2ValueObject)
            throws IllegalStateException
    {
        if (!isSpecialDateTypeMatched())
        {
            throw new IllegalStateException("You must call parseSpecialDateType() before this method");
        }

        try
        {
            boolean or = false;

            if (filter2ValueObject != null)
            {
                // If the range is something like Saturday < value < Tuesday,
                // we must use an OR comparison to cross the max value boundary.
                if (specialDateType == SpecialDate.Month)
                {
                    if (filter1Comparison == ComparisonType.GT || filter1Comparison == ComparisonType.GTE)
                    {
                        or = monthOrdinal(filter1ValueObject) > monthOrdinal(filter2ValueObject);
                    } else
                    {
                        or = monthOrdinal(filter1ValueObject) < monthOrdinal(filter2ValueObject);
                    }
                } else if (specialDateType == SpecialDate.Weekday)
                {
                    if (filter1Comparison == ComparisonType.GT || filter1Comparison == ComparisonType.GTE)
                    {
                        or = weekdayOrdinal(filter1ValueObject) > weekdayOrdinal(filter2ValueObject);
                    } else
                    {
                        or = weekdayOrdinal(filter1ValueObject) < weekdayOrdinal(filter2ValueObject);
                    }
                } else
                {
                    if (filter1Comparison == ComparisonType.GT || filter1Comparison == ComparisonType.GTE)
                    {
                        or = hourFormat.parse(filter1ValueObject).getTime() > hourFormat.parse(filter2ValueObject).getTime();
                    } else
                    {
                        or = hourFormat.parse(filter1ValueObject).getTime() < hourFormat.parse(filter2ValueObject).getTime();
                    }
                }
            }

            if (or)
            {
                return compare(time, filter1Comparison, filter1ValueObject) ||
                        (compare(time, filter2Comparison, filter2ValueObject));
            } else
            {
                return compare(time, filter1Comparison, filter1ValueObject) &&
                        (filter2ValueObject == null || compare(time, filter2Comparison, filter2ValueObject));
            }
        } catch (ParseException pe)
        {
            return false;
        }
    }

    /**
     * @return Ordinal of MONTH enum with the given name
     */
    private int monthOrdinal(String monthName)
    {
        return MONTHS.valueOf(monthName).ordinal();
    }

    /**
     * @return Ordinal of WEEKDAYS enum with the given name
     */
    private int weekdayOrdinal(String monthName)
    {
        return WEEKDAYS.valueOf(monthName).ordinal();
    }

    private boolean compare(long time, ComparisonType ct, String comparisonString)
            throws ParseException
    {
        final DateFormat format = specialDateType.format;
        final Date timeAsDate = new Date(time);

        // If equal, we're done. Hour format must be rechecked in the Hour if-block below
        //  because we need to use seconds in the comparison as well.
        if (ct == ComparisonType.EQ || ct == ComparisonType.GTE || ct == ComparisonType.LTE)
        {
            String timeString = format.format(timeAsDate);

            if (timeString.equals(comparisonString))
            {
                return true;
            }
        }

        final boolean isGTType = ct == ComparisonType.GTE || ct == ComparisonType.GT;
        final boolean isLTType = ct == ComparisonType.LTE || ct == ComparisonType.LT;

        if (specialDateType == SpecialDate.Hour)
        {
            String partialDateStringValue = extendedHourFormat.format(timeAsDate);

            // Convert to date chunk string, then back to 0-based date.
            Date d1 = extendedHourFormat.parse(partialDateStringValue);
            Date d2 = format.parse(comparisonString);

            if (ct == ComparisonType.EQ || ct == ComparisonType.GTE || ct == ComparisonType.LTE)
            {
                if (d1.compareTo(d2) == 0)
                {
                    return true;
                }
            }

            return isGTType
                    ? d1.compareTo(d2) > 0
                    : (ct == ComparisonType.LTE || ct == ComparisonType.LT) && d1.compareTo(d2) < 0;
        } else if (specialDateType == SpecialDate.Weekday)
        {
            final String partialDateStringValue = format.format(timeAsDate);
            final int partialDateWeekday = WEEKDAYS.valueOf(partialDateStringValue).ordinal();
            final int comparisonValueWeekday = WEEKDAYS.valueOf(comparisonString).ordinal();

            if (isGTType)
            {
                return partialDateWeekday > comparisonValueWeekday;
            } else
            {
                return isLTType && partialDateWeekday < comparisonValueWeekday;
            }
        } else
        {
            final String partialDateStringValue = format.format(timeAsDate);
            final int partialDateMonth = MONTHS.valueOf(partialDateStringValue).ordinal();
            final int comparisonValueMonth = MONTHS.valueOf(comparisonString).ordinal();

            if (isGTType)
            {
                return partialDateMonth > comparisonValueMonth;
            } else
            {
                return isLTType && partialDateMonth < comparisonValueMonth;
            }
        }
    }
}
