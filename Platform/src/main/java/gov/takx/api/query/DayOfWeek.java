package gov.takx.api.query;

/**
 * Constants indicating the "Day of the Week" as can be used in query operations.  These constants are ordered
 * such that Sunday is 0 and Saturday is 6, as required for proper comparisons against the database.
 *
 */
public interface DayOfWeek
{
    int SUNDAY = 0;
    int MONDAY = 1;
    int TUESDAY = 2;
    int WEDNESDAY = 3;
    int THURSDAY = 4;
    int FRIDAY = 5;
    int SATURDAY = 6;
}
