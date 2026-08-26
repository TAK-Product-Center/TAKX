package gov.takx.core.view.gui.contacts;

import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IIndividualContactManagerListener;
import gov.tak.api.contact.IndividualContactUtils;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.StandardAttributeNames;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.core.chat.protocol.geochat.GeoChatRootGroupContact;
import gov.tak.core.chat.protocol.geochat.GeoChatTeamsContact;
import gov.tak.core.comms.commo.CommonCommoManager;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.mapobject.IMapObject;
import gov.takx.api.mapobject.IMarkerMapObject;
import gov.takx.commons.swing.ImageUtils;
import gov.takx.test.AIntegrationTest;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.Icon;

import java.beans.PropertyChangeEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link IndividualContactManager}
 *
 * @since 3.4.1
 */
public class IndividualContactManagerTest extends AIntegrationTest
{
    // note: the message UID must match the contact's UID
    private static final String HEARTBEAT_MESSAGE_1 = """
            <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
            <event version='2.0' uid='UID3' type='a-f-G-U-C' time='2003-07-24T23:24:50.863Z'
                   start='2003-07-24T23:24:50.863Z' stale='2003-07-24T23:26:05.863Z' how='h-e'>
                <point lat="0" lon="0" hae="166.584" ce="6.0" le="9999999.0"/>
                <detail/>
            </event>
            """;

    // note: the second message's time must be later than the first message's time
    private static final String HEARTBEAT_MESSAGE_2 = """
            <?xml version='1.0' encoding='UTF-8' standalone='yes'?>
            <event version='2.0' uid='UID3' type='a-f-G-U-C' time='2003-07-24T23:25:50.863Z'
                   start='2003-07-24T23:25:50.863Z' stale='2003-07-24T23:27:05.863Z' how='h-e'>
                <point lat="0" lon="0" hae="166.584" ce="6.0" le="9999999.0"/>
                <detail/>
            </event>
            """;

    @Mocked(allowCaching = false)
    private IIndividualContact contact1;

    @Mocked(allowCaching = false)
    private IIndividualContact contact2;

    @Mocked(allowCaching = false)
    private IIndividualContact contact3;

    @Mocked
    private IMarkerMapObject mapObject1;

    @Mocked
    private IIndividualContactManagerListener listener;

    private IndividualContactManager manager;
    private CommonCommoManager commonCommoManager;

    @BeforeAll
    void beforeAll()
    {
        manager = getBean(IndividualContactManager.class);
        commonCommoManager = getBean(CommonCommoManager.class);

        final GroupContactManager groupContactManager = getBean(GroupContactManager.class);

        groupContactManager.contactAdded(new GeoChatTeamsContact());
        groupContactManager.contactAdded(new GeoChatRootGroupContact("clientUid", "client"));
    }

    @SuppressLogging(IndividualContactManager.class) // method must be public until suppression code is improved
    @BeforeEach
    public void beforeEach()
    {
        when(mapObject1.getUid()).thenReturn("UID1");
        when(mapObject1.getSizedIcon(anyInt())).thenReturn(ImageUtils.getScaledDefaultIcon(32));
        when(mapObject1.getMetaString(StandardAttributeNames.TEAM, "")).thenReturn("Blue");
        when(mapObject1.getMetaString(StandardAttributeNames.ATAK_ROLE_TYPE, "")).thenReturn("Team Member");

        when(contact1.getUid()).thenReturn("UID1");
        when(contact2.getUid()).thenReturn("UID2");
        when(contact3.getUid()).thenReturn("UID3");

        when(contact1.getName()).thenReturn("Contact1");
        when(contact2.getName()).thenReturn("Contact2");
        when(contact3.getName()).thenReturn("Contact3");

        manager.removeListener(listener);

        if (manager.getContact(contact1.getUid()) != null) manager.contactRemoved(contact1);
        if (manager.getContact(contact2.getUid()) != null) manager.contactRemoved(contact2);
        if (manager.getContact(contact3.getUid()) != null) manager.contactRemoved(contact3);

        clearInvocations(contact1, contact2, contact3);
    }

    @Test
    public void whenContactManagerUpdated_listenersCalled()
    {
        manager.addListener(listener);

        manager.contactAdded(contact1);
        manager.contactAdded(contact2);
        verify(listener, times(2)).contactAdded(any(IIndividualContact.class));

        manager.contactUpdated(contact1);
        manager.contactUpdated(contact2);
        manager.propertyChange(new PropertyChangeEvent(mapObject1, IMapObject.ICON, null, null));
        verify(listener, times(3)).contactUpdated(any(IIndividualContact.class));

        manager.contactRemoved(contact1);
        manager.contactRemoved(contact2);
        verify(listener, times(2)).contactRemoved(any(IIndividualContact.class));
    }

    @Test
    public void whenMapObjectAddedOrRemoved_propertyChangeListenerUpdated()
    {
        manager.contactAdded(contact1);

        manager.clientMapObjectAdded(mapObject1);
        verify(mapObject1)
                .addPropertyChangeListener(eq(IMapObject.ICON), (any(IndividualContactManager.class)));

        manager.clientMapObjectRemoved(mapObject1);
        verify(mapObject1)
                .removePropertyChangeListener(eq(IMapObject.ICON), any(IndividualContactManager.class));
    }

    @Test
    public void whenContactsAddedOrRemoved_validContactListReturned()
    {
        final IIndividualContact individualContact1 = addContactAndGetWrappedContact(contact1);
        final IIndividualContact individualContact2 = addContactAndGetWrappedContact(contact2);

        assertThat(manager.getContacts()).containsExactlyInAnyOrder(individualContact1, individualContact2);

        manager.contactRemoved(contact1);
        assertThat(manager.getContacts()).containsExactly(individualContact2);

        manager.contactRemoved(contact2);
        assertThat(manager.getContacts()).isEmpty();
    }

    @Test
    public void whenIconPropertyFired_contactIconUpdated()
    {
        final IIndividualContact contact = addContactAndGetWrappedContact(contact1);
        final Icon icon = manager.getContactIconWithStatus(contact.getUid());

        manager.propertyChange(new PropertyChangeEvent(mapObject1, IMapObject.ICON, null, null));
        assertThat(manager.getContactIconWithStatus(contact.getUid())).isNotEqualTo(icon); // just confirm that it's a new object
    }

    @Test
    public void whenMessageReceived_contactTimesAreUpdated() throws InterruptedException
    {
        final IIndividualContact contact = addContactAndGetWrappedContact(contact3);

        when(contact.getAttributes()).thenReturn(new AttributeSet());

        // CoT calls should trigger the ICM's ICotEventListener that updates contacts
        commonCommoManager.cotMessageReceived(HEARTBEAT_MESSAGE_1, "");
        Thread.sleep(100); // Give the CompletableFuture time to complete
        final long staleTime1 = IndividualContactUtils.getStaleTime(contact);

        commonCommoManager.cotMessageReceived(HEARTBEAT_MESSAGE_2, "");
        Thread.sleep(100); // Give the CompletableFuture time to complete
        final long staleTime2 = IndividualContactUtils.getStaleTime(contact);

        assertThat(staleTime1).withFailMessage("Stale time did not change between messages")
                .isNotEqualTo(staleTime2);
    }

    @Nonnull
    private IIndividualContact addContactAndGetWrappedContact(@Nonnull IIndividualContact contact)
    {
        manager.contactAdded(contact);
        //noinspection DataFlowIssue
        return manager.getContact(contact.getUid());
    }
}