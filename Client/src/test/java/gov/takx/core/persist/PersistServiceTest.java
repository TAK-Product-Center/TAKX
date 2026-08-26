package gov.takx.core.persist;

import com.google.common.util.concurrent.Uninterruptibles;
import gov.takx.api.messages.IDataField;
import gov.takx.dataservices.persist.DataField;
import gov.takx.dataservices.persist.MapEntity;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static gov.takx.api.messages.IDataType.BOOLEAN;
import static gov.takx.api.messages.IDataType.BYTE_ARRAY;
import static gov.takx.api.messages.IDataType.DATE;
import static gov.takx.api.messages.IDataType.DOUBLE;
import static gov.takx.api.messages.IDataType.FLOAT;
import static gov.takx.api.messages.IDataType.INT;
import static gov.takx.api.messages.IDataType.LONG;
import static gov.takx.api.messages.IDataType.SHORT;
import static gov.takx.api.messages.IDataType.STRING;
import static org.assertj.core.api.Assertions.assertThat;

/**
 */
@SuppressWarnings("JavaUtilDate")
public class PersistServiceTest extends AContainerTest
{
    private PersistTestSetup test;

    @BeforeAll
    public void beforeTest()
    {
        test = new PersistTestSetup();
        test.setup();
    }

    @AfterAll
    public void close()
    {
        test.shutdown();
    }

    @Test
    public void persistMapEntity()
    {
        test.getService().persistEntity(new MapEntity("test", "NG"));
    }

    @Test
    public void testDefaultFieldValues()
    {
        final String stringDefault = "test";
        IDataField f = createDataField("STRING", STRING, stringDefault);
        assertThat(stringDefault).isEqualTo(f.getDefaultValue());

        f = createDataField("BOOLEAN", BOOLEAN, true);
        assertThat((Boolean) f.getDefaultValue()).isTrue();

        final Integer intDefault = 1;
        f = createDataField("INT", INT, intDefault);
        assertThat(f.getDefaultValue()).isEqualTo(intDefault);

        final Float floatDefault = 1.23f;
        f = createDataField("FLOAT", FLOAT, floatDefault);
        assertThat(f.getDefaultValue()).isEqualTo(floatDefault);

        final Short shortDefault = 2;
        f = createDataField("SHORT", SHORT, shortDefault);
        assertThat(f.getDefaultValue()).isEqualTo(shortDefault);

        final Long longDefault = 3L;
        f = createDataField("LONG", LONG, longDefault);
        assertThat(f.getDefaultValue()).isEqualTo(longDefault);

        final Double doubleDefault = 2.34;
        f = createDataField("DOUBLE", DOUBLE, doubleDefault);
        assertThat(f.getDefaultValue()).isEqualTo(doubleDefault);

        final Date dateDefault = new Date();
        f = createDataField("DATE", DATE, dateDefault);
        assertThat(f.getDefaultValue()).isEqualTo(dateDefault);

        final byte[] bytesDefault = {0x01, 0x02, 'A', 'B'};
        f = createDataField("BYTES", BYTE_ARRAY, bytesDefault);
        assertThat(f.getDefaultValue()).isEqualTo(bytesDefault);
    }

    private IDataField createDataField(final String name, final int dataType, final Object defaultValue)
    {
        return new DataField(name, dataType, defaultValue, 0);
    }

    @Test
    public void testDeadThreadCleanup()
    {
        final Map<Thread, PersistService> map = PersistService.getThread2ServiceMap();

        final AtomicBoolean t1IsOpen = new AtomicBoolean(false);
        assertThat(countOpenSessions(map)).withFailMessage("Thread2ServiceMap size should be 1").isEqualTo(1);

        final Thread t1 = new Thread(() -> {
            PersistService ps = (PersistService) test.getService();
            ps.getCurrentSession();
            t1IsOpen.set(map.get(Thread.currentThread()).isSessionOpen());
            // this opens the session but never closes it.
        });

        t1.start();
        Uninterruptibles.joinUninterruptibly(t1);

        assertThat(t1IsOpen.get()).withFailMessage("The session should be open").isTrue();

        final int[] t2Counts = new int[4];

        Thread t2 = new Thread(() -> {
            t2Counts[0] = countOpenSessions(map);
            PersistService ps = (PersistService) test.getService();
            PersistService.cleanupDeadThreads();

            t2Counts[1] = countOpenSessions(map);
            ps.getCurrentSession();
            t2Counts[2] = countOpenSessions(map);
            ps.cleanupAndClose();
            t2Counts[3] = countOpenSessions(map);
        });

        t2.start();
        Uninterruptibles.joinUninterruptibly(t2);

        assertThat(t2Counts[0]).withFailMessage("size should start at 2").isEqualTo(2);
        assertThat(t2Counts[1]).withFailMessage("size should be 1 after closing the Session from t1").isEqualTo(1);
        assertThat(t2Counts[2]).withFailMessage("size should be 2 after opening the t2 session").isEqualTo(2);
        assertThat(t2Counts[3]).withFailMessage("size should be 1 after manually closing the t2 session").isEqualTo(1);
    }

    private int countOpenSessions(Map<Thread, PersistService> map)
    {
        int cnt = 0;
        for (PersistService ps : map.values())
        {
            if (ps.isSessionOpen()) cnt++;
        }
        return cnt;
    }
}
