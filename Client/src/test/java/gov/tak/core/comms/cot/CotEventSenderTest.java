package gov.tak.core.comms.cot;

import com.atakmap.commoncommo.CoTSendMethod;
import com.atakmap.commoncommo.Commo;
import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.Contact;
import gov.tak.api.comms.commo.ICommonCommoManager;
import gov.tak.api.comms.commo.IContactManager;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.MethodBehavior;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.comms.commo.CommonCommoManager;
import gov.tak.core.comms.commo.ContactManager;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.test.AContainerTest;
import gov.takx.test.utils.CdiTestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link CotEventSender} functionality.
 *
 * @since 3.0
 */
@SuppressWarnings({"ResultOfObjectAllocationIgnored"})
public class CotEventSenderTest extends AContainerTest
{
    @Mocked(parentTypes = ICommonCommoManager.class, methodBehavior = MethodBehavior.RETURN_REAL_METHOD)
    private CommonCommoManager commoManager;

    @Mocked(parentTypes = IContactManager.class)
    private ContactManager contactManager;

    @Mocked(allowCaching = false, forBeanUpdating = false)
    private Contact contactA;
    @Mocked(allowCaching = false, forBeanUpdating = false)
    private Contact contactB;
    @Mocked(allowCaching = false, forBeanUpdating = false)
    private Contact contactC;

    @SuppressWarnings("unused")
    @Mocked
    private Commo commo;

    final CotEvent event = new CotEvent();
    final String eventMessage;

    private final CotEventSender cotEventSender;

    public CotEventSenderTest()
    {
        event.setUID("uid");
        eventMessage = event.toString();
        cotEventSender = getBean(CotEventSender.class);
        BlacksmithExtension.setBeansToUpdate(
                CdiTestUtils.getInstance(getBean(CommonCommoManager.class)),
                CdiTestUtils.getInstance(getBean(ContactManager.class)),
                CdiTestUtils.getInstance(getBean(CotEventSender.class)));
    }

    @BeforeEach
    public void setupEach()
    {
        clearInvocations(commoManager, contactManager);
        CdiTestUtils.setField(commoManager, "cotEventInterceptors", new ArrayList<>());
    }

    @Test
    public void test_broadcast_event_is_sent_to_commo() throws CommoException
    {
        CdiTestUtils.setField(commoManager, "cotSendListeners", new CopyOnWriteArrayList<>());
        cotEventSender.broadcastEvent(event);

        verify(commoManager, times(1)).broadcastCoT(eq(eventMessage));
    }

    @SuppressLogging(CotEventSender.class)
    @Test
    public void test_send_to_uids_with_no_contacts_does_not_call_commo() throws CommoException
    {
        final List<String> contactUids = Arrays.asList("A", "B", "C");

        when(contactManager.findContactByUid(anyString())).thenReturn(null);

        cotEventSender.sendEvent(event, contactUids);

        verify(commoManager, never()).sendCoT(any(), eq(eventMessage), eq(CoTSendMethod.ANY));
    }

    @SuppressWarnings("JdkObsolete")
    @Test
    public void test_send_calls_commo() throws CommoException
    {
        final List<String> contactUids = Arrays.asList("A", "B", "C");

        when(contactManager.findContactByUid("A")).thenReturn(contactA);
        when(contactManager.findContactByUid("B")).thenReturn(contactB);
        when(contactManager.findContactByUid("C")).thenReturn(contactC);

        CdiTestUtils.setField(commoManager, "cotSendListeners", new CopyOnWriteArrayList<>());
        cotEventSender.sendEvent(event, contactUids);

        final Vector<Contact> contacts = new Vector<>(Arrays.asList(contactA, contactB, contactC));

        verify(commoManager, times(1)).sendCoT(eq(contacts), eq(eventMessage), eq(CoTSendMethod.ANY));
    }

    @Test
    public void test_send_to_empty_list_throws()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> cotEventSender.sendEvent(event, Collections.emptyList()));
    }
}
