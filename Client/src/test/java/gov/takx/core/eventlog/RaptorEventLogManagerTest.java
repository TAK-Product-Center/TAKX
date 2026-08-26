package gov.takx.core.eventlog;

import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.eventlog.IRaptorEventLog;
import gov.takx.api.eventlog.IRaptorEventLogListener;
import gov.takx.api.eventlog.IRaptorEventLogManager;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.persist.IEventLogPersist;
import gov.takx.core.persist.EventLogPersist;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.when;

/**
 * @since 2.30
 */
@SuppressWarnings("JavaUtilDate")
public class RaptorEventLogManagerTest extends AContainerTest
{
    private static final Date LOG_TIME = new Date();
    private static final String DETAILS = "DETAILS";
    private static final String MESSAGE = "MESSAGE";
    private static final String FAMILY = "FAMILY";
    private static final String TYPE = "TYPE";

    private IRaptorEventLogManager eventLogManager;

    @SuppressWarnings("unused")
    @Mocked(parentTypes = IEventLogPersist.class)
    private EventLogPersist eventLogPersist;

    @Mocked
    private IMapObject mapObject;

    @BeforeAll
    public void setup()
    {
        eventLogManager = getBean(RaptorEventLogManager.class);
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(eventLogManager));

        startService(RaptorEventLogManager.class);
    }

    @AfterAll
    public void teardown()
    {
        stopService(RaptorEventLogManager.class);
    }

    /**
     * Unit Id is not required
     */
    @Test
    public void testLogPluginEvent()
    {
        eventLogManager.logPluginEvent(LOG_TIME, FAMILY, TYPE, null, MESSAGE, DETAILS);
    }

    /**
     * Family is required
     */
    @Test
    public void testLogPluginEventNoFamily()
    {
        //noinspection DataFlowIssue - passing null on purpose
        assertThatNullPointerException()
                .isThrownBy(() -> eventLogManager.logPluginEvent(LOG_TIME, null, TYPE, null, MESSAGE, DETAILS));
    }

    /**
     * Type is required
     */
    @Test
    public void testLogPluginEventNoType()
    {
        //noinspection DataFlowIssue - passing null on purpose
        assertThatNullPointerException()
                .isThrownBy(() -> eventLogManager.logPluginEvent(LOG_TIME, FAMILY, null, null, MESSAGE, DETAILS));
    }

    /**
     * A map object is required
     */
    @Test
    public void testLogAppEventNoMO()
    {
        //noinspection DataFlowIssue - passing null on purpose
        assertThatNullPointerException()
                .isThrownBy(() -> eventLogManager.logAppEvent(LOG_TIME, null, MESSAGE, DETAILS));
    }

    /**
     * Message is required
     */
    @Test
    public void testLogEventNoMessage()
    {
        when(mapObject.shouldCreateEventLogEntries()).thenReturn(true);
        assertThatNullPointerException()
                .isThrownBy(() -> eventLogManager.logAppEvent(LOG_TIME, mapObject, null, DETAILS));
    }

    /**
     * Details are not required. No exception should be thrown.
     */
    @Test
    public void testLogEventNoDetails()
    {
        when(mapObject.shouldCreateEventLogEntries()).thenReturn(true);
        eventLogManager.logAppEvent(LOG_TIME, mapObject, MESSAGE, null);
    }

    /**
     * Makes sure that the listener is invoked
     */
    @Test
    public void testAddEventLogListener()
    {
        when(mapObject.shouldCreateEventLogEntries()).thenReturn(true);

        final IRaptorEventLog[] log = new IRaptorEventLog[1];
        //noinspection AnonymousInnerClassMayBeStatic
        eventLogManager.addEventLogListener(new IRaptorEventLogListener()
        {
            @Override
            public void logAdded(IRaptorEventLog newLog)
            {
                log[0] = newLog;
            }

            @Override
            public void wipeData(IMapObject mapObject)
            {
            }
        });

        eventLogManager.logPluginEvent(LOG_TIME, mapObject, MESSAGE, DETAILS);

        assertThat(log[0].getMessage()).isEqualTo(MESSAGE);
        assertThat(log[0].getDetails()).isEqualTo(DETAILS);
    }
}
