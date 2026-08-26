package gov.takx.core.persist;

import gov.takx.api.annotation.UseClientClassLoader;
import gov.takx.api.eventlog.IRaptorEventLog;
import gov.takx.api.persist.IEventLogPersist;
import gov.takx.api.persist.IPersistServiceProvider;
import gov.takx.api.query.param.HQLParameter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Date;
import java.util.List;

/**
 * Handle EventLog persistence operations
 */
@ApplicationScoped
public class EventLogPersist implements IEventLogPersist
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // SQL fragment to select entries with no MO relationship or with a relationship to MOs that are not deleted
    private static final String NO_MO_OR_NOT_DELETED = " (mapObjectId is null or" +
            " mapObjectId NOT IN (select distinct(dmo.mapObjectId) from DeletedMapObject dmo))";

    @Inject
    private IPersistServiceProvider persistServiceProvider;

    @Override
    @UseClientClassLoader
    public void logAdded(IRaptorEventLog newLog)
    {
        if (newLog.getId() == null)
        {
            persistServiceProvider.getPersistService().persistEntity(newLog);
        } else
        {
            if (logger.isDebugEnabled()) logger.debug("Avoiding write of already persisted EventLog entry: {}", newLog);
        }
    }

    @Override
    public List<IRaptorEventLog> searchDate(Date start, Date end)
    {
        return searchDate(start, end, null, null);
    }

    @Override
    public List<IRaptorEventLog> searchDate(Date start, Date end, Integer maxRows, Integer firstRow)
    {
        String hql = "from RaptorEventLog where " + NO_MO_OR_NOT_DELETED;

        if (start != null) hql += " and logDate >= :start";
        if (end != null) hql += " and logDate <= :end";

        return persistServiceProvider.getPersistService()
                .query(hql, maxRows, firstRow, new HQLParameter("start", start), new HQLParameter("end", end));
    }

    @Override
    public long countDate(Date start, Date end)
    {
        String where = NO_MO_OR_NOT_DELETED;

        if (start != null) where += " and logDate >= :start";
        if (end != null) where += " and logDate <= :end";

        return persistServiceProvider.getPersistService()
                .count("RaptorEventLog", where, new HQLParameter("start", start), new HQLParameter("end", end));
    }

    /**
     * Find the first and last times that messages were received.
     *
     * @return an array of two elements: [min_date, max_date].
     */
    @Override
    public Date[] queryDateRange()
    {
        final String hql = "select min(logDate), max(logDate) from RaptorEventLog";

        final Object[] query = persistServiceProvider.getPersistService().queryUnique(hql);

        return new Date[]{(Date) query[0], (Date) query[1]};
    }
}
