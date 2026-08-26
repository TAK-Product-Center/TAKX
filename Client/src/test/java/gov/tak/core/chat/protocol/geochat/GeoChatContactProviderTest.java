package gov.tak.core.chat.protocol.geochat;

import com.atakmap.commoncommo.Commo;
import gov.tak.api.contact.IGroupContact;
import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import gov.tak.platform.contact.ContactStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test class for the {@link GeoChatContactProvider}.
 *
 * @since 3.2
 */
@ExtendWith(BlacksmithExtension.class)
public class GeoChatContactProviderTest
{
    private static final String TEST_UID = "Test UID";
    private static final String TEST_CALLSIGN = "Test Callsign";

    private GeoChatContactProvider contactProviderUnderTest;
    private String groupContactOneUid;

    static
    {
        MockStrategy strategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes)
                .withMockStrategy(strategy));
    }

    @Mocked
    private Commo commo;

    @Mocked
    private ContactStore contactStore;

    @Mocked
    private IGroupContact groupContactOne;

    @BeforeEach
    public void setUp()
    {
        clearInvocations(commo, contactStore, groupContactOne);

        contactProviderUnderTest = new GeoChatContactProvider(commo, contactStore);
        groupContactOneUid = UUID.randomUUID().toString();

        when(groupContactOne.getUid()).thenReturn(groupContactOneUid);
    }

    @Test
    public void start_AddsAllChatRoomsContactToContactStore()
    {
        contactProviderUnderTest.start(TEST_UID, TEST_CALLSIGN);

        verify(contactStore, times(1)).addContact(any(GeoChatAllChatRoomsContact.class));
    }

    @Test
    public void start_AddsRootGroupContactToContactStore()
    {
        contactProviderUnderTest.start(TEST_UID, TEST_CALLSIGN);

        verify(contactStore, times(1)).addContact(any(GeoChatRootGroupContact.class));
    }

    @Test
    public void start_ThrowsNullPointerException_WhenUidStringIsNull()
    {
        assertThatNullPointerException().isThrownBy(() -> contactProviderUnderTest.start(null, TEST_CALLSIGN));
    }

    @Test
    public void start_ThrowsNullPointerException_WhenCallsignStringIsNull()
    {
        assertThatNullPointerException().isThrownBy(() -> contactProviderUnderTest.start(TEST_UID, null));
    }

    @Test
    public void addOwnedGroupContact_ThrowsNullPointerException_WhenGroupContactIsNull()
    {
        contactProviderUnderTest.start(TEST_UID, TEST_CALLSIGN);
        assertThatNullPointerException()
                .isThrownBy(() -> contactProviderUnderTest.addOwnedGroupContact(null));
    }

    @Test
    public void addUnownedGroupContact_ThrowsNullPointerException_WhenGroupContactIsNull()
    {
        contactProviderUnderTest.start(TEST_UID, TEST_CALLSIGN);
        assertThatNullPointerException()
                .isThrownBy(() -> contactProviderUnderTest.addUnownedGroupContact(null));
    }

    @Test
    public void removeGroup_ThrowsNullPointerException_WhenGroupContactIsNull()
    {
        contactProviderUnderTest.start(TEST_UID, TEST_CALLSIGN);
        assertThatNullPointerException().isThrownBy(() -> contactProviderUnderTest.removeGroup(null));
    }
}
