package gov.takx.api.persist;

import gov.takx.api.eventlog.IRaptorEventLog;

import java.util.Date;
import java.util.List;

/**
 */
public interface IEventLogPersist
{
    public void logAdded(IRaptorEventLog newLog);

    List<IRaptorEventLog> searchDate(Date start, Date end);

    List<IRaptorEventLog> searchDate(Date start, Date end, Integer maxRows, Integer firstRow);

    long countDate(Date start, Date end);

    /**
     * Find the first and last times that messages were received.
     *
     * @return an array of two elements: [min_date, max_date].
     */
    Date[] queryDateRange();
}
