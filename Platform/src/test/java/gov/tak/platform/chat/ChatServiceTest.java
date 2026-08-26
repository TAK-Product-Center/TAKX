package gov.tak.platform.chat;

import gov.tak.api.chat.ChatOperationResult;
import gov.tak.api.chat.ChatOperationResult.Operation;
import gov.tak.api.chat.ChatOperationResult.Status;
import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.IChatMessageBuilder;
import gov.tak.api.chat.IChatMessageFinder;
import gov.tak.api.chat.IChatOperationCallback;
import gov.tak.api.chat.protocol.IChatProtocolHandler;
import gov.tak.api.chat.protocol.IChatProtocolHandlerDelegate;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.contact.IPersistableGroupContact;
import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import gov.tak.core.chat.protocol.geochat.GeoChatRootGroupContact;
import gov.tak.platform.chat.client.IChatServiceClient;
import gov.tak.platform.contact.GroupContactBuilder;
import gov.tak.platform.contact.IndividualContactBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link ChatService} class.
 *
 * @since 3.0.1
 */
@SuppressWarnings({"ConstantConditions", "unchecked"})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(BlacksmithExtension.class)
public class ChatServiceTest
{
    private static final String DEFAULT_PROTOCOL = "Carrier Pigeon";
    private static final String[] DEFAULT_PROTOCOLS = {DEFAULT_PROTOCOL};

    private static final String HANDLER2_PROTOCOL = DEFAULT_PROTOCOL + "-2";
    private static final String HANDLER3_PROTOCOL = DEFAULT_PROTOCOL + "-3";

    private static final String TEST_GROUP_NAME = "testGroup";

    private ChatService chatServiceUnderTest;

    @Mocked(allowCaching = false)
    private IChatProtocolHandler defaultProtocolHandler;
    @Mocked(allowCaching = false)
    private IChatProtocolHandler protocolHandlerTwo;
    @Mocked(allowCaching = false)
    private IChatProtocolHandler protocolHandlerThree;

    @Mocked
    private IChatServiceClient chatServiceClient;

    @Mocked
    private IChatOperationCallback operationCallback;

    @Mocked(allowCaching = false)
    private IChatMessage chatMessageOne;
    @Mocked(allowCaching = false)
    private IChatMessage chatMessageTwo;

    @Mocked
    private GeoChatRootGroupContact rootGroupContact;
    @Mocked
    private IContactService contactService;

    static
    {
        MockStrategy mockStrategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes)
                .withMockStrategy(mockStrategy));
    }

    @BeforeEach
    public void setupChatService()
    {
        when(protocolHandlerTwo.getProtocol()).thenReturn(HANDLER2_PROTOCOL);
        when(protocolHandlerThree.getProtocol()).thenReturn(HANDLER3_PROTOCOL);

        ChatOperationResult res = new ChatOperationResult(Operation.SEND_MESSAGE, Status.SUCCESS, "", rootGroupContact);
        for (IChatProtocolHandler handler : Arrays.asList(defaultProtocolHandler, protocolHandlerTwo, protocolHandlerThree))
        {
            when(handler.sendMessage(any(), any()))
                    .thenReturn(res);
            when(handler.createGroup(anyString(), any(), any()))
                    .thenReturn(res);
            when(handler.updateGroup(any(), any()))
                    .thenReturn(res);
            when(handler.deleteGroup(any()))
                    .thenReturn(res);
            when(handler.sendReceipt(any(), anyString(), anyBoolean()))
                    .thenReturn(res);
        }

        if (chatServiceUnderTest != null) chatServiceUnderTest.stop();

        when(defaultProtocolHandler.getProtocol()).thenReturn(DEFAULT_PROTOCOL);
        when(rootGroupContact.getUid()).thenReturn(GeoChatRootGroupContact.GROUPS_ROOT_UID);
        when(rootGroupContact.getName()).thenReturn(GeoChatRootGroupContact.GROUPS_ROOT_DISPLAY_NAME);

        when(chatServiceClient.getUid()).thenReturn("client uid");
        when(chatServiceClient.getName()).thenReturn("client name");

        chatServiceUnderTest = new ChatService();
        chatServiceUnderTest.start(defaultProtocolHandler, chatServiceClient, contactService);

        clearInvocations(defaultProtocolHandler, protocolHandlerTwo, protocolHandlerThree);
    }

    @Test
    public void getChatService_ReturnsSameInstance()
    {
        assertThat(ChatService.getInstance()).isSameAs(ChatService.getInstance());
    }

    @Test
    public void getDefaultProtocol_ReturnsConfiguredProtocol()
    {
        assertThat(chatServiceUnderTest.getDefaultProtocol()).isEqualTo(defaultProtocolHandler.getProtocol());
    }

    @Test
    public void sendMessage_CallsCallback()
    {
        final IIndividualContact c1 = makeDefaultIndividualContact();
        final ChatOperationResult opResult = new ChatOperationResult(Operation.SEND_MESSAGE, Status.SUCCESS, "message", c1);

        when(defaultProtocolHandler.sendMessage(c1, chatMessageOne)).thenReturn(opResult);

        setContactUidExpectations(chatMessageOne, c1);

        chatServiceUnderTest.sendMessage(chatMessageOne, operationCallback);

        verify(defaultProtocolHandler, times(1)).sendMessage(eq(c1), eq(chatMessageOne));
        verify(chatServiceClient, times(1)).messageSent(eq(c1), eq(chatMessageOne));
        verify(operationCallback, times(1)).operationAttempted(eq(opResult));
    }

    @Test
    public void sendMessage_WithNoMatchingProtocol_CallsCallback()
    {
        final IIndividualContact c1 = makeIndividualContact(HANDLER2_PROTOCOL);

        setContactUidExpectations(chatMessageOne, c1);

        chatServiceUnderTest.sendMessage(chatMessageOne, opResult -> {
            assertThat(opResult.getStatus()).isEqualTo(Status.FAILURE);
            assertThat(opResult.getMessage()).contains(HANDLER2_PROTOCOL);
        });
    }

    @Test
    public void sendMessage_RoutesMessagesToCorrectHandlers_WhenMultipleHandlersRegistered()
    {
        final IIndividualContact c1 = makeDefaultIndividualContact();
        final IIndividualContact c2 = makeIndividualContact(HANDLER2_PROTOCOL);

        setContactUidExpectations(chatMessageOne, c1);
        setContactUidExpectations(chatMessageTwo, c2);

        chatServiceUnderTest.registerProtocolHandler(protocolHandlerTwo);
        chatServiceUnderTest.sendMessage(chatMessageOne, null);
        chatServiceUnderTest.sendMessage(chatMessageTwo, null);

        verify(defaultProtocolHandler, times(1)).sendMessage(eq(c1), eq(chatMessageOne));
        verify(protocolHandlerTwo, times(1)).sendMessage(eq(c2), eq(chatMessageTwo));
    }

    @Test
    public void sendMessage_ThrowsNullPointerException_WhenGivenNullChatMessage()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> chatServiceUnderTest.sendMessage(null, null));
    }

    @Test
    public void sendMessage_NoExceptions_WhenGivenNullCallback()
    {
        setContactUidExpectations(chatMessageOne, makeDefaultIndividualContact());

        assertThatNoException().isThrownBy(() -> chatServiceUnderTest.sendMessage(chatMessageOne, null));
    }

    @Test
    public void sendMessage_ThrowsIllegalArgumentException_WhenContactProtocolsAreEmpty()
    {
        // Create a contact that will return an empty list of protocols
        setContactUidExpectations(chatMessageOne, makeIndividualContact());

        assertThatIllegalArgumentException()
                .isThrownBy(() -> chatServiceUnderTest.sendMessage(chatMessageOne, null));
    }

    @Test
    public void createGroup_RequestsProtocolHandlerCreateGroup_WhenOneHandlerRegistered()
    {
        final IIndividualContact c1 = makeDefaultIndividualContact();
        final Set<IContact> testGroupMembers = Set.of(c1);

        chatServiceUnderTest.createGroup(TEST_GROUP_NAME, testGroupMembers, rootGroupContact, DEFAULT_PROTOCOL, null);

        ArgumentCaptor<Set<IContact>> captor = ArgumentCaptor.forClass(Set.class);
        verify(defaultProtocolHandler, atLeastOnce()).createGroup(eq(TEST_GROUP_NAME), captor.capture(), eq(rootGroupContact));
        assertThat(captor.getValue()).contains(c1);
    }

    @Test
    public void createGroup_RequestsProtocolHandlerCreateGroup_WhenMultipleHandlersRegistered()
    {
        final IIndividualContact c1 = makeDefaultIndividualContact();
        final Set<IContact> testGroupMembers = Set.of(c1);

        chatServiceUnderTest.registerProtocolHandler(protocolHandlerTwo);
        chatServiceUnderTest.createGroup(TEST_GROUP_NAME, testGroupMembers, rootGroupContact, DEFAULT_PROTOCOL, null);

        ArgumentCaptor<Set<IContact>> captor = ArgumentCaptor.forClass(Set.class);
        verify(defaultProtocolHandler, atLeastOnce()).createGroup(eq(TEST_GROUP_NAME), captor.capture(), eq(rootGroupContact));
        assertThat(captor.getValue()).contains(c1);

        verify(protocolHandlerTwo, never()).createGroup(anyString(), any(), eq(rootGroupContact));
    }

    @Test
    @SuppressWarnings("DistinctVarargsChecker")
    public void createGroup_ThrowsIllegalArgumentException_WhenGroupMemberDoesNotHaveRequestedCapability()
    {
        assertThatIllegalArgumentException().isThrownBy(() -> chatServiceUnderTest.createGroup(
                TEST_GROUP_NAME,
                Set.of(makeIndividualContact(), makeIndividualContact()),
                rootGroupContact,
                DEFAULT_PROTOCOL,
                null));
    }

    @Test
    public void createGroup_ThrowsNullPointerException_WhenGroupNameNull()
    {
        assertThatNullPointerException().isThrownBy(() -> chatServiceUnderTest.createGroup(
                null,
                Set.of(makeIndividualContact()),
                rootGroupContact,
                DEFAULT_PROTOCOL,
                null));
    }

    @Test
    public void createGroup_Succeeds_WhenGroupMembersIsEmpty()
    {
        chatServiceUnderTest.createGroup(
                TEST_GROUP_NAME,
                Collections.emptySet(),
                rootGroupContact,
                DEFAULT_PROTOCOL,
                null);
    }

    @Test
    public void createGroup_ThrowsNullPointerException_WhenGroupMembersNull()
    {
        assertThatNullPointerException().isThrownBy(() -> chatServiceUnderTest.createGroup(
                TEST_GROUP_NAME,
                null,
                rootGroupContact,
                DEFAULT_PROTOCOL,
                null));
    }

    @Test
    public void createGroup_ThrowsNullPointerException_WhenChatServiceCapabilityNull()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> chatServiceUnderTest.createGroup(
                        TEST_GROUP_NAME,
                        Set.of(makeIndividualContact()),
                        rootGroupContact,
                        null,
                        null));
    }

    @Test
    public void deleteGroupContact_RequestsProtocolHandlerDeleteGroupContact_WhenOneHandlerRegistered()
    {
        final IGroupContact groupContact = makeGroupContact();

        chatServiceUnderTest.deleteGroup(groupContact, null);

        verify(defaultProtocolHandler, times(1)).deleteGroup(eq(groupContact));
    }

    @Test
    public void deleteGroupContact_RequestsProtocolHandlerDeleteGroupContact_WhenMultipleHandlersRegistered()
    {
        final IGroupContact groupContact = makeGroupContact();

        chatServiceUnderTest.registerProtocolHandler(protocolHandlerTwo);
        chatServiceUnderTest.deleteGroup(groupContact, null);

        verify(defaultProtocolHandler, times(1)).deleteGroup(eq(groupContact));
        verify(protocolHandlerTwo, never()).deleteGroup(any());
    }

    @Test
    public void deleteGroupContact_ThrowsNullPointerException_WhenGivenNullGroupContact()
    {
        assertThatNullPointerException().isThrownBy(() -> chatServiceUnderTest.deleteGroup(null, null));
    }

    @Test
    public void updateGroupContact_RequestsProtocolHandlerUpdateGroupContact_WhenOneHandlerRegistered()
    {
        final IGroupContact groupContact = makeGroupContact();

        chatServiceUnderTest.updateGroup(groupContact, Collections.emptySet(), null);

        verify(defaultProtocolHandler, times(1)).updateGroup(eq(groupContact), any());
    }

    @Test
    public void updateGroupContact_RequestsProtocolHandlerUpdateGroupContact_WhenMultipleHandlersRegistered()
    {
        final IGroupContact groupContact = makeGroupContact();

        chatServiceUnderTest.registerProtocolHandler(protocolHandlerTwo);
        chatServiceUnderTest.updateGroup(groupContact, Collections.emptySet(), null);

        verify(defaultProtocolHandler, times(1)).updateGroup(eq(groupContact), any());
        verify(protocolHandlerTwo, never()).updateGroup(any(), eq(Collections.emptySet()));
    }

    @Test
    public void updateGroupContact_ThrowsNullPointerException_WhenGivenNullGroupContact()
    {
        assertThatNullPointerException()
                .isThrownBy(() -> chatServiceUnderTest.updateGroup(null, Collections.emptySet(), null));
    }

    @Test
    public void registerProtocolHandler_HandlersStarted_WhenInvokedWithDifferentHandlers()
    {
        chatServiceUnderTest.registerProtocolHandler(protocolHandlerTwo);
        chatServiceUnderTest.registerProtocolHandler(protocolHandlerThree);

        verify(protocolHandlerTwo, times(1)).start(any());
        verify(protocolHandlerThree, times(1)).start(any());
    }

    @Test
    public void registerProtocolHandler_StartOnlyCalledOnce_WhenHandlerHasAlreadyBeenRegistered()
    {
        chatServiceUnderTest.registerProtocolHandler(protocolHandlerTwo);

        // Second call rejected
        assertThatIllegalArgumentException().isThrownBy(() -> chatServiceUnderTest.registerProtocolHandler(protocolHandlerTwo));

        verify(protocolHandlerTwo, times(1)).start(any());
    }

    @Test
    public void registerProtocolHandler_DoesNotRegisterHandlerInstance_WhenDifferentHandlerWithSameCapabilityHasAlreadyBeenRegistered()
    {
        chatServiceUnderTest.registerProtocolHandler(protocolHandlerTwo);

        // Create a temporary handler for mocking a handler for protocol 2 and attempt to add it.
        // Because we already have a handler for the protocol, it should fail
        IChatProtocolHandler handlerTwoMocked = mock(IChatProtocolHandler.class);
        when(handlerTwoMocked.getProtocol()).thenReturn(HANDLER2_PROTOCOL);
        assertThatIllegalArgumentException().isThrownBy(() -> chatServiceUnderTest.registerProtocolHandler(handlerTwoMocked));
    }

    @Test
    public void registerProtocolHandler_ThrowsIllegalArgumentException_WhenGivenPrimaryHandler()
    {
        assertThatIllegalArgumentException().isThrownBy(() -> chatServiceUnderTest.registerProtocolHandler(defaultProtocolHandler));
    }

    @Test
    public void registerProtocolHandler_ThrowsNullPointerException_WhenGivenNullHandler()
    {
        assertThatNullPointerException().isThrownBy(() -> chatServiceUnderTest.registerProtocolHandler(null));
    }

    @Test
    public void unregisterProtocolHandler_SuccessfullyUnregistersHandler_WhenHandlerHasAlreadyBeenRegistered()
    {
        setContactUidExpectations(chatMessageOne, makeIndividualContact(HANDLER2_PROTOCOL));

        chatServiceUnderTest.registerProtocolHandler(protocolHandlerTwo);
        chatServiceUnderTest.unregisterProtocolHandler(protocolHandlerTwo);
        chatServiceUnderTest.sendMessage(chatMessageOne, operationCallback);

        verify(protocolHandlerTwo, never()).sendMessage(any(), any());
    }

    @Test
    public void unregisterProtocolHandler_CallsStop_WhenHandlerIsRegistered()
    {
        chatServiceUnderTest.registerProtocolHandler(protocolHandlerTwo);
        chatServiceUnderTest.unregisterProtocolHandler(protocolHandlerTwo);

        verify(protocolHandlerTwo, times(1)).stop();
    }

    @Test
    public void unregisterProtocolHandler_DoesNotCallStop_WhenHandlerIsNotRegistered()
    {
        chatServiceUnderTest.unregisterProtocolHandler(protocolHandlerTwo);

        verify(protocolHandlerTwo, never()).stop();
    }

    @Test
    public void unregisterProtocolHandler_ThrowsNullPointerException_WhenGivenNullHandler()
    {
        assertThatNullPointerException().isThrownBy(() -> chatServiceUnderTest.unregisterProtocolHandler(null));
    }

    @Test
    public void unregisterProtocolHandler_ThrowsIllegalArgumentException_WhenCalledWithPrimaryHandler()
    {
        setContactUidExpectations(chatMessageOne, makeDefaultIndividualContact());

        assertThatIllegalArgumentException().isThrownBy(() -> chatServiceUnderTest.unregisterProtocolHandler(defaultProtocolHandler));

        // This call should still succeed without throwing exceptions
        chatServiceUnderTest.sendMessage(chatMessageOne, null);
    }

    @Test
    public void getChatMessageBuilder_ReturnsValidBuilder()
    {
        final IChatMessageBuilder builder = chatServiceUnderTest
                .getChatMessageBuilder("text", 1L, "conversationUid");

        assertThat(builder).isNotNull();
    }

    @Test
    public void getMessageFinder_ReturnsClientFinder()
    {
        final IIndividualContact contact = makeDefaultIndividualContact();
        setContactUidExpectations(chatMessageOne, contact);

        chatServiceUnderTest.sendMessage(chatMessageOne, null);

        final IChatMessageFinder chatMessageFinder = chatServiceUnderTest.getChatMessageFinder();

        // Finder is mocked so just confirm it's the correct object. Actual tests are in ChatMessagePersistTest
        assertThat(chatMessageFinder).isEqualTo(chatServiceClient.getChatMessageFinder());
    }

    @Test
    public void registerAndUnregisterProtocolHandler_InjectedDelegateFunctionsProperly()
    {
        chatServiceUnderTest.registerProtocolHandler(protocolHandlerTwo);
        chatServiceUnderTest.unregisterProtocolHandler(protocolHandlerTwo);

        final ArgumentCaptor<IChatProtocolHandlerDelegate> delegateArgumentCaptor =
                ArgumentCaptor.forClass(IChatProtocolHandlerDelegate.class);

        // Verify start was called and capture the delegate that should have been injected
        verify(protocolHandlerTwo).start(delegateArgumentCaptor.capture());
        verify(protocolHandlerTwo, atLeastOnce()).getProtocol();

        final IChatProtocolHandlerDelegate delegate = delegateArgumentCaptor.getValue();
        assertThat(delegate).isNotNull();

        final IContact contact = makeDefaultIndividualContact();

        delegate.receiveMessage(contact, chatMessageOne);
        // Verify delegate is wired to chat client
        verify(chatServiceClient).messageReceived(contact, chatMessageOne);

        final ChatReceipt chatReceipt = new ChatReceipt(contact, "messageUid", "cotType");
        delegate.receiveReceipt(chatReceipt);
        // Verify delegate is wired to chat client
        verify(chatServiceClient).receiveReceipt(chatReceipt);

        // The builder provider is mocked, so just verify it exists
        final IChatMessageBuilder builder =
                delegate.getChatMessageBuilder("", "", "", 0L, "");
        assertThat(builder).isNotNull();

        verify(protocolHandlerTwo).stop();
    }

    /**
     * @return A new individual contact supporting the default protocol
     * @since 3.4
     */
    private IIndividualContact makeDefaultIndividualContact()
    {
        return makeIndividualContact(DEFAULT_PROTOCOL);
    }

    /**
     * @return A new individual contact supporting the given protocols
     * @since 3.4
     */
    private IIndividualContact makeIndividualContact(String... protocols)
    {
        final IIndividualContact contact = new IndividualContactBuilder().withDisplayName("c1").build();
        contact.setProtocols(protocols);
        return contact;
    }

    /**
     * @return A new group contact supporting the default protocol with two newly created members
     * @since 3.4
     */
    @SuppressWarnings("DistinctVarargsChecker")
    private IGroupContact makeGroupContact()
    {
        final IPersistableGroupContact contact = new GroupContactBuilder()
                .withGroupMembers(Set.of(makeDefaultIndividualContact(), makeDefaultIndividualContact()))
                .build();
        contact.setProtocols(DEFAULT_PROTOCOLS);
        return contact;
    }

    /**
     * Sets a message's conversation UID to the recipient contact UID and makes the contact service return the correct
     * contact when passed the contact's UID.
     *
     * @param message          The message to configure
     * @param recipientContact The recipient contact
     * @since 3.4
     */
    private void setContactUidExpectations(IChatMessage message, IContact recipientContact)
    {
        final String contactUid = recipientContact.getUid();

        when(message.getConversationUid()).thenReturn(contactUid);
        when(contactService.getContact(eq(contactUid))).thenReturn(recipientContact);
    }
}
