package gov.tak.core.comms.cot;

import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.cot.importer.ICotImporterManager;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.globe.IGlobeManager;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import jakarta.inject.Inject;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Validate {@link CotEventReceiver} functionality.
 *
 * @since 3.0
 */
@SuppressWarnings("FutureReturnValueIgnored")
@PseudoBean
public class CotEventReceiverTest extends AContainerTest
{
    @Mocked(parentTypes = ICotImporterManager.class)
    private CotImporterManager cotImporterManager;

    @Inject
    private @NodeUUID UUID nodeUuid;

    @BeforeAll
    public void setup()
    {
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(getBean(CotEventReceiver.class)));
    }

    @BeforeEach
    public void startClean()
    {
        stopService(CotEventReceiver.class); // Ensure stopped, and nothing queued
    }

    @AfterEach
    public void stopClean()
    {
        stopService(IGlobeManager.class);
        stopService(CotEventReceiver.class);
    }

    @Test
    public void test_queued_events_are_processed_on_start() throws TimeoutException
    {
        final CotEvent event = new CotEvent();
        event.setUID("uid");

        final CotEventReceiver eventReceiver = getBean(CotEventReceiver.class);

        eventReceiver.cotEventReceived(event);
        eventReceiver.cotEventReceived(event);
        eventReceiver.cotEventReceived(event);

        Assertions.assertThat(eventReceiver.getEventQueueSize()).isEqualTo(3);

        startService(IGlobeManager.class);
        startService(CotEventReceiver.class);

        // Wait for it to drain, up to 1 second
        final long start = System.currentTimeMillis();
        while (eventReceiver.getEventQueueSize() > 0)
        {
            if (System.currentTimeMillis() - start > 1_000) throw new TimeoutException();
        }

        verify(cotImporterManager, times(3)).importEvent(any());
    }

    @Test
    public void test_events_with_empty_uid_are_not_processed()
    {
        final CotEventReceiver eventReceiver = getBean(CotEventReceiver.class);

        eventReceiver.cotEventReceived(new CotEvent());

        Assertions.assertThat(eventReceiver.getEventQueueSize()).isEqualTo(0);
    }

    @Test
    void test_marker_from_self_uid_not_queued()
    {
        final CotEventReceiver eventReceiver = getBean(CotEventReceiver.class);

        for (String sfx : new String[]{".SPI1", ".SPI3", ".PLRF1", ".PLRF-XYZ"})
        {
            final CotEvent event = new CotEvent();
            final String uid = nodeUuid + sfx;
            event.setUID(uid);
            eventReceiver.cotEventReceived(event);

            Assertions.assertThat(eventReceiver.getEventQueueSize())
                    .withFailMessage("Expected no event on UID '%s', node UUID %s", uid, nodeUuid)
                    .isEqualTo(0);
        }
    }
}
