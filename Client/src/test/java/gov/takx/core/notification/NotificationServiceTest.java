package gov.takx.core.notification;

import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.notification.INotificationService;
import gov.takx.api.notification.Notification;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import gov.takx.test.utils.SwingTestUtils;
import jakarta.enterprise.event.Event;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link gov.takx.api.notification.INotificationService}.
 *
 * @since 5.2
 */
public class NotificationServiceTest extends AContainerTest
{
    @Mocked
    private Event<NotificationAdded> event;

    @BeforeAll
    public void setup()
    {
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(getBean(INotificationService.class)));
    }

    @Test
    public void show_firesNotificationAddedEvent()
    {
        final INotificationService notificationService = getBean(INotificationService.class);
        notificationService.show(Notification.builder("Test").build());

        SwingTestUtils.waitForEdtEvents(); // The show method is @InvokeOnEDT

        verify(event).fire(any());
    }
}
