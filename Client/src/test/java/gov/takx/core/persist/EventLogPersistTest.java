package gov.takx.core.persist;

import gov.takx.api.eventlog.EventLogType;
import gov.takx.api.eventlog.IRaptorEventLog;
import gov.takx.api.eventlog.IRaptorEventLogManager;
import gov.takx.api.mapobject.IIconMapObject;
import gov.takx.api.persist.IEventLogPersist;
import gov.takx.api.persist.IMapObjectPersist;
import gov.takx.dataservices.persist.RaptorEventLog;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import gov.takx.test.datasim.BloodHoundSim;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test Event Log features.
 *
 */
@SuppressWarnings("JavaUtilDate")
public class EventLogPersistTest extends AContainerTest
{
    private final Date[] dates = new Date[4];
    private PersistTestSetup test;

    /**
     * Map object persistence helper.
     */
    private IMapObjectPersist moPersist;
    private IEventLogPersist eventLogPersist;

    @BeforeAll
    public void setup()
    {
        // Set up the environment.
        test = new PersistTestSetup();
        test.init();

        eventLogPersist = getBean(IEventLogPersist.class);
        moPersist = getBean(IMapObjectPersist.class);
        startService(IRaptorEventLogManager.class);

        GregorianCalendar gc = new GregorianCalendar(2008, Calendar.FEBRUARY, 1);
        for (int i = 0; i < 4; i++)
        {
            dates[i] = new Date(gc.getTimeInMillis());
            RaptorEventLog log1 = new RaptorEventLog(
                    dates[i],
                    BloodHoundSim.BLOODHOUND_FAMILY,
                    BloodHoundSim.BLOODHOUND_TYPE,
                    "6",
                    "State changed to inactive",
                    "",
                    EventLogType.STATE_CHANGE,
                    "Bloodhound",
                    null);
            eventLogPersist.logAdded(log1);
            gc.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    @AfterAll
    public void tearDown()
    {
        // Shutdown and clean up.
        stopService(IRaptorEventLogManager.class);
        test.shutdown();
    }

    /**
     * Test to ensure that cached Event Log query results are removed when the associated map object is deleted.
     * This method does its own persistence setup, as it needs to enable caching to properly test the described
     * scenario. If it makes sense to keep caching on for the other tests as they are implemented, the persistence
     * setup may be moved to the {@link #setup()} method.
     *
     * @since 1.25.9
     */
    @Test
    @Disabled // TODO KMB: Fix test (TAKX-1333)
    public void testQueryAfterDelete()
    {
        // Create one map object.
        IIconMapObject cmo = test.persistBloodHound(10, PersistTestSetup.OFFSET, 0);

        // Log a map-object specific event.
        IRaptorEventLogManager manager = getBean(IRaptorEventLogManager.class);
        manager.logMapObjectOnline(new Date(System.currentTimeMillis()), cmo, "Online");

        // Query the database. The query will be cached.
        List<IRaptorEventLog> events = eventLogPersist.searchDate(null, null);
        assertThat(events != null && !events.isEmpty()).withFailMessage("No events logged after creating map object.").isTrue();

        // Delete the map object.
        moPersist.deleteMapObject(cmo);

        // Run the same query and verify that no ObjectNotFoundExceptions are thrown.
        events = eventLogPersist.searchDate(null, null);

        // If the cache was properly cleared, no events will be returned.
        assertThat(events).withFailMessage("Events still returned after deleting map object.").isEmpty();
    }

    @Test
    public void searchDate()
    {
        List<IRaptorEventLog> list = eventLogPersist.searchDate(null, null);
        assertThat(list).hasSize(4);
        list = eventLogPersist.searchDate(dates[2], null);
        assertThat(list).hasSize(2);
        list = eventLogPersist.searchDate(null, dates[2]);
        assertThat(list).hasSize(3);
        list = eventLogPersist.searchDate(dates[1], dates[2]);
        assertThat(list).hasSize(2);
    }
}
