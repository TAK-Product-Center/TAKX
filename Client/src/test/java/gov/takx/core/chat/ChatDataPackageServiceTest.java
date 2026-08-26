package gov.takx.core.chat;

import com.atakmap.commoncommo.CommoException;
import com.atakmap.commoncommo.Contact;
import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.IChatService;
import gov.tak.api.comms.commo.IContactManager;
import gov.tak.api.comms.takserver.IExtendedDataPackageManager;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.annotation.CallSign;
import gov.takx.api.annotation.NodeUUID;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.annotation.SuppressLogging;
import gov.takx.api.notification.INotificationService;
import gov.takx.commons.lang.ErrorThrowingFunction;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.CdiTestUtils;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link ChatDataPackageService}.
 *
 * @since 5.2
 */
@SuppressWarnings("JUnitMalformedDeclaration") // params okay
@PseudoBean
public class ChatDataPackageServiceTest extends AIntegrationTest
{
    private static final ErrorThrowingFunction<File, List<ChatLink>, Exception> CHAT_LINKS_FUNCTION = file -> List.of(new ChatLink(0, "", "file.txt"));

    @Mocked
    private IExtendedDataPackageManager dataPackageManager;
    @Mocked
    private IContactManager contactManager;
    @Mocked
    private INotificationService notificationService;
    @Mocked
    private IChatService chatService;

    @Inject
    @NodeUUID
    private UUID nodeUuid;
    @Inject
    @CallSign
    private String callSign;
    @Inject
    private ChatDataPackageService chatDataPackageService;

    @BeforeAll
    void beforeAll()
    {
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(chatDataPackageService));
    }

    @BeforeEach
    void beforeEach()
    {
        clearInvocations(chatService);
        clearInvocations(dataPackageManager);
        clearInvocations(notificationService);
    }

    @Test
    public void sendPackage_whenIndividualContact_packageSentToContact(@Mocked IIndividualContact individualContact,
                                                                       @Mocked Contact commoContact)
            throws CommoException
    {
        final String contactUid = "uid";

        initContact(individualContact, commoContact, contactUid);

        chatDataPackageService.sendPackage(individualContact, CHAT_LINKS_FUNCTION);

        verify(dataPackageManager).sendPackageToContacts(any(), argThat(arg -> arg.size() == 1 && arg.contains(commoContact)));
    }

    @Test
    public void sendPackage_whenGroupContact_packageSentToAllGroupMembers(@Mocked(allowCaching = false) IGroupContact groupContact,
                                                                          @Mocked(allowCaching = false) IIndividualContact individualContact1,
                                                                          @Mocked(allowCaching = false) IIndividualContact individualContact2,
                                                                          @Mocked(allowCaching = false) IGroupContact subGroupContact,
                                                                          @Mocked(allowCaching = false) Contact commoContact1,
                                                                          @Mocked(allowCaching = false) Contact commoContact2,
                                                                          @Mocked(allowCaching = false) Contact commoContact3)
            throws CommoException
    {
        final String contactUid1 = "uid1";
        final String contactUid2 = "uid2";
        final String contactUid3 = "uid3";
        final Set<IContact> groupMembers = Set.of(individualContact1, subGroupContact, individualContact2);

        initGroupContact(groupContact, groupMembers, "groupUid", null);
        initContact(individualContact1, commoContact1, contactUid1);
        initContact(individualContact2, commoContact2, contactUid2);
        initGroupContact(subGroupContact, null, contactUid3, commoContact3);

        chatDataPackageService.sendPackage(groupContact, CHAT_LINKS_FUNCTION);

        // Package should only be sent to the 2 individual members
        verify(dataPackageManager).sendPackageToContacts(any(),
                argThat(arg -> arg.size() == 2 && arg.containsAll(List.of(commoContact1, commoContact2))));
    }

    @Test
    public void sendPackage_whenIndividualContact_chatMessageSentToMember(@Mocked IIndividualContact individualContact,
                                                                          @Mocked Contact commoContact)
    {
        final String contactUid = "uid";

        initContact(individualContact, commoContact, contactUid);

        chatDataPackageService.sendPackage(individualContact, CHAT_LINKS_FUNCTION);

        final ArgumentCaptor<IChatMessage> captor = ArgumentCaptor.forClass(IChatMessage.class);
        verify(chatService).sendMessage(captor.capture(), any());
        verifyChatMessage(captor, contactUid);
    }

    @Test
    public void sendPackage_whenGroupContact_chatMessageSentToGroupContact(@Mocked IGroupContact groupContact,
                                                                           @Mocked(allowCaching = false) IIndividualContact individualContact1,
                                                                           @Mocked(allowCaching = false) IIndividualContact individualContact2,
                                                                           @Mocked(allowCaching = false) Contact commoContact1,
                                                                           @Mocked(allowCaching = false) Contact commoContact2)
    {
        final String groupUid = "groupUid";
        final String contactUid1 = "uid1";
        final String contactUid2 = "uid2";
        final Set<IContact> groupMembers = Set.of(individualContact1, individualContact2);

        initGroupContact(groupContact, groupMembers, groupUid, null);
        initContact(individualContact1, commoContact1, contactUid1);
        ;
        initContact(individualContact2, commoContact2, contactUid2);

        chatDataPackageService.sendPackage(groupContact, CHAT_LINKS_FUNCTION);

        final ArgumentCaptor<IChatMessage> captor = ArgumentCaptor.forClass(IChatMessage.class);
        verify(chatService).sendMessage(captor.capture(), any());
        verifyChatMessage(captor, groupUid);
    }

    @SuppressLogging(ChatDataPackageService.class)
    @Test
    public void sendPackage_whenNoMatchingCommoContact_notificationDisplayed(@Mocked IIndividualContact individualContact)
    {
        final String contactUid = "badUid";

        initContact(individualContact, null, contactUid);
        when(contactManager.findContactByUid(contactUid)).thenReturn(null);

        chatDataPackageService.sendPackage(individualContact, CHAT_LINKS_FUNCTION);

        verify(notificationService).show(any());
    }

    @SuppressLogging(ChatDataPackageService.class)
    @Test
    public void sendPackage_whenDataPackageManagerException_notificationDisplayed(@Mocked IIndividualContact individualContact,
                                                                                  @Mocked Contact commoContact)
            throws IOException
    {
        initContact(individualContact, commoContact, "uid");

        when(dataPackageManager.createNewTempDataPackage(anyString())).thenThrow(new IOException());

        chatDataPackageService.sendPackage(individualContact, CHAT_LINKS_FUNCTION);

        verify(notificationService).show(any());

        reset(dataPackageManager);
    }

    @Test
    public void tempFileRegex_matchesTempFileNames()
    {
        Predicate<String> isChatPackage = chatDataPackageService::isChatPackage;
        Predicate<String> isntChatPackage = Predicate.not(isChatPackage);

        assertThat("chat-transfer.zip").matches(isChatPackage);
        assertThat("chat-transfer(1).zip").matches(isChatPackage);
        assertThat("chat-transfer(11).zip").matches(isChatPackage);
        assertThat("chat-transfer (11).zip").matches(isChatPackage);

        assertThat("chat-transfe.zip").matches(isntChatPackage);
        assertThat("chat-transfer.zi").matches(isntChatPackage);
        assertThat("chat-transferz.zip").matches(isntChatPackage);
        assertThat("chat-transfer1).zip").matches(isntChatPackage);
        assertThat("chat-transfer1.zip").matches(isntChatPackage);
        assertThat("chat-transfer().zip").matches(isntChatPackage);
        assertThat("chat-transfer(1.zip").matches(isntChatPackage);
        assertThat("chat-transfer  (1).zip").matches(isntChatPackage);
        assertThat("chat-transfer(1) .zip").matches(isntChatPackage);
        assertThat("chat-transfer(z).zip").matches(isntChatPackage);
    }

    private void initGroupContact(@Nonnull IGroupContact groupContact, @Nullable Set<IContact> groupMembers,
                                  @Nonnull String uid, @Nullable Contact commoContact)
    {
        when(groupContact.getName()).thenReturn(uid);
        when(groupContact.getUid()).thenReturn(uid);
        if (groupMembers != null) when(groupContact.getGroupMembers()).thenReturn(groupMembers);
        if (commoContact != null) when(contactManager.findContactByUid(uid)).thenReturn(commoContact);
    }

    private void initContact(@Nonnull IContact contact,
                             @Nullable Contact commoContact,
                             @Nonnull String uid)
    {
        when(contact.getName()).thenReturn(uid);
        when(contact.getUid()).thenReturn(uid);
        if (commoContact != null) when(contactManager.findContactByUid(uid)).thenReturn(commoContact);
    }

    private void verifyChatMessage(@Nonnull ArgumentCaptor<IChatMessage> captor, @Nonnull String contactUid)
    {
        final IChatMessage chatMessage = captor.getValue();
        assertThat(chatMessage.getSenderUid()).isEqualTo(nodeUuid.toString());
        assertThat(chatMessage.getSenderName()).isEqualTo(callSign);
        assertThat(chatMessage.getConversationUid()).isEqualTo(contactUid);
    }
}
