package gov.tak.core.chat.protocol.geochat;

import gov.tak.api.chat.IChatMessage;
import gov.tak.api.chat.protocol.IChatProtocolHandlerDelegate;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.blacksmith.Blacksmith;
import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.tak.blacksmith.mock.strategy.MockStrategy;
import gov.tak.blacksmith.mock.strategy.MockitoStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Objects;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link GeoChatMessageReceiver} using real WinTAK and ATAK chat data.
 *
 * @since 3.4
 */
@ExtendWith(BlacksmithExtension.class)
public class GeoChatMessageReceiverTest
{
    private static final String FILES_DIR = "chat/xml/";

    @Mocked
    private IChatProtocolHandlerDelegate chatServiceProviderDelegate;

    @Mocked
    private GeoChatContactProvider geoChatContactProvider;

    @Mocked
    private GeoChatMessageParser parser;

    @Mocked
    private GeoChatRootGroupContact rootGroupContact;

    @Mocked
    private IGroupContact groupContact;

    @Mocked
    private IIndividualContact individualContact;

    @Mocked
    private IChatMessage message;

    private GeoChatMessageReceiver receiver;

    static
    {
        MockStrategy mockStrategy = new MockitoStrategy().cached();
        BlacksmithExtension.setBlacksmithFactory(classes -> new Blacksmith(classes)
                .withMockStrategy(mockStrategy));
    }

    @BeforeEach
    public void setup()
    {
        clearInvocations(parser, geoChatContactProvider, chatServiceProviderDelegate);

        when(individualContact.getUid()).thenReturn("individualUid");
        when(individualContact.getName()).thenReturn("individualDisplayName");

        when(rootGroupContact.getParentContact()).thenReturn(null);

        when(groupContact.getParentContact()).thenReturn(rootGroupContact);
        when(groupContact.getUid()).thenReturn("groupUid");
        when(groupContact.getName()).thenReturn("groupDisplayName");

        when(chatServiceProviderDelegate.getClientUid()).thenReturn("clientUid");

        expectParseGroupCotEvent();
        expectParseDirectCotEvent();

        receiver = new GeoChatMessageReceiver(chatServiceProviderDelegate, geoChatContactProvider, parser);
    }

    @Test
    public void cotMessageReceived_WhenAllChatRoomsMessageReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("all_chat_rooms_message_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseDirectCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenAllChatRoomsMessageReceived_FromAtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("all_chat_rooms_message_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseDirectCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenAllChatRoomsMessageReceived_FromWebtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("all_chat_rooms_message_webtak_4.10.1.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseDirectCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupsMessageReceived_FromWintakSimulator_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("all_chat_rooms_message_wintak_simulator.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseDirectCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenChatReceiptDeliveredReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("chat_receipt_delivered_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseChatReceiptCalled();
        verifyReceiveMessageCalled(0); // no message expected for receipts
    }

    @Test
    public void cotMessageReceived_WhenChatReceiptDeliveredReceived_FromAtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("chat_receipt_delivered_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseChatReceiptCalled();
        verifyReceiveMessageCalled(0); // no message expected for receipts
    }

    @Test
    public void cotMessageReceived_WhenChatReceiptReadReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("chat_receipt_read_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseChatReceiptCalled();
        verifyReceiveMessageCalled(0); // no message expected for receipts
    }

    @Test
    public void cotMessageReceived_WhenChatReceiptReadReceived_FromAtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("chat_receipt_read_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseChatReceiptCalled();
        verifyReceiveMessageCalled(0); // no message expected for receipts
    }

    @Test
    public void cotMessageReceived_WhenDirectMessageReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("direct_message_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseDirectCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenDirectMessageReceived_FromAtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("direct_message_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseDirectCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenDirectMessageReceived_FromWebtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("direct_message_webtak_4.10.1.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseDirectCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupAddMeReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("group_add_me_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupAddMeReceived_FromAtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("group_add_me_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupAddOtherReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("group_add_other_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupAddOtherReceived_FromAtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("group_add_other_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupCotEventCalled();
    }

    @Test
    public void cotMessageReceived_WhenGroupCreatedReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("group_created_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupCreatedReceived_FromAtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("group_created_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupDeletedReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        when(parser.parseGroupDeletionEvent(any()))
                .thenReturn(groupContact);

        final String cotMessage = getFileAsString("group_deleted_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupDeletionEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupDeletedReceived_FromAtak_CorrectParserMethodCalled() throws IOException
    {
        when(parser.parseGroupDeletionEvent(any()))
                .thenReturn(groupContact);

        final String cotMessage = getFileAsString("group_deleted_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupDeletionEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupMessageReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("group_message_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupMessageReceived_FromAtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("group_message_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupRemoveMeReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("group_remove_me_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyRemoveGroupCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupRemoveMeReceived_FromAtak_CorrectParserMethodCalled(@Mocked GeoChatParsedMessage message)
            throws IOException
    {
        when(message.getContact()).thenReturn(groupContact);
        when(parser.parseGroupCotEvent(any(), any(), anyBoolean())).thenReturn(message);

        final String cotMessage = getFileAsString("group_remove_me_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyRemoveGroupCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupRemoveOtherReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("group_remove_other_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupRemoveOtherReceived_FromAtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("group_remove_other_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupsMessageReceived_FromWintak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("groups_message_wintak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseGroupCotEventCalled();
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void cotMessageReceived_WhenGroupsMessageReceived_FromAtak_CorrectParserMethodCalled() throws IOException
    {
        final String cotMessage = getFileAsString("groups_message_atak_4.6.xml");
        receiver.cotMessageReceived(cotMessage, "");

        verifyParseDirectCotEventCalled(); // special case for ATAK that has a different "Groups" message format
        verifyReceiveMessageCalled(1);
    }

    @Test
    public void contactIsMemberOfDescendantGroup_FindsContactInDescendants(@Mocked(allowCaching = false) IGroupContact a1,
                                                                           @Mocked(allowCaching = false) IGroupContact a1b1,
                                                                           @Mocked(allowCaching = false) IGroupContact a1b2,
                                                                           @Mocked(allowCaching = false) IGroupContact a1b1c1,
                                                                           @Mocked(allowCaching = false) IGroupContact a1b2c1,
                                                                           @Mocked(allowCaching = false) IGroupContact a1b2c2)
    {
        setUpAncestors(a1, a1b1, a1b2, a1b1c1, a1b2c1, a1b2c2);

        when(a1b2c1.getGroupMembers()).thenReturn(Set.of(individualContact));

        final String uid = individualContact.getUid();
        final String ignoredGroupUid = a1b2c1.getUid();

        assertThat(receiver.contactIsMemberOfDescendantGroup(a1, "", uid)).isTrue();
        assertThat(receiver.contactIsMemberOfDescendantGroup(a1, ignoredGroupUid, uid)).isFalse();
        assertThat(receiver.contactIsMemberOfDescendantGroup(a1b1, "", uid)).isFalse();
        assertThat(receiver.contactIsMemberOfDescendantGroup(a1b1c1, "", uid)).isFalse();
        assertThat(receiver.contactIsMemberOfDescendantGroup(a1b2, "", uid)).isTrue();
        assertThat(receiver.contactIsMemberOfDescendantGroup(a1b2, ignoredGroupUid, uid)).isFalse();
        assertThat(receiver.contactIsMemberOfDescendantGroup(a1b2c1, "", uid)).isTrue();
        assertThat(receiver.contactIsMemberOfDescendantGroup(a1b2c1, ignoredGroupUid, uid)).isFalse();
    }

    /**
     * Sets up the UIDs and parent-child relationships for an arbitrary set of group contacts
     *
     * @param a1 base group
     * @param a1b1 child 1 of a1
     * @param a1b2 child 2 of a1
     * @param a1b1c1 child 1 of b1
     * @param a1b2c1 child 1 of b2
     * @param a1b2c2 child 2 of b2
     * @since 3.5
     */
    private void setUpAncestors(IGroupContact a1, IGroupContact a1b1, IGroupContact a1b2, IGroupContact a1b1c1,
                                IGroupContact a1b2c1, IGroupContact a1b2c2)
    {
        when(rootGroupContact.getGroupMembers()).thenReturn(Set.of(a1));
        when(a1.getUid()).thenReturn("a1");
        when(a1.getParentContact()).thenReturn(rootGroupContact);
        when(a1.getGroupMembers()).thenReturn(Set.of(a1b1, a1b2));

        when(a1b1.getUid()).thenReturn("a1b1");
        when(a1b1.getParentContact()).thenReturn(a1);
        when(a1b1.getGroupMembers()).thenReturn(Set.of(a1b1c1));

        when(a1b2.getUid()).thenReturn("a1b2");
        when(a1b2.getParentContact()).thenReturn(a1);
        when(a1b2.getGroupMembers()).thenReturn(Set.of(a1b2c1, a1b2c2));

        when(a1b1c1.getUid()).thenReturn("a1b1c1");
        when(a1b1c1.getParentContact()).thenReturn(a1b1);

        when(a1b2c1.getUid()).thenReturn("a1b2c1");
        when(a1b2c1.getParentContact()).thenReturn(a1b2);

        when(a1b2c2.getUid()).thenReturn("a1b2c2");
        when(a1b2c2.getParentContact()).thenReturn(a1b2);
    }

    /**
     * Verify that {@link GeoChatMessageParser#parseDirectCotEvent(CotEvent)} is called exactly once.
     */
    private void verifyParseDirectCotEventCalled()
    {
        verify(parser, times(1)).parseDirectCotEvent(any());
    }

    /**
     * Verify that {@link GeoChatMessageParser#parseReceipt(CotEvent)} is called exactly once.
     */
    private void verifyParseChatReceiptCalled()
    {
        verify(parser, times(1)).parseReceipt(any());
    }

    /**
     * Verify that {@link GeoChatMessageParser#parseGroupCotEvent(CotEvent, String, boolean)}  is called exactly once.
     */
    private void verifyParseGroupCotEventCalled()
    {
        verify(parser, times(1)).parseGroupCotEvent(any(), any(), anyBoolean());
        clearInvocations(parser);
    }

    /**
     * Verify that {@link GeoChatMessageParser#parseGroupDeletionEvent(CotEvent)} is called exactly once.
     */
    private void verifyParseGroupDeletionEventCalled()
    {
        verify(parser, times(1)).parseGroupDeletionEvent(any());
        clearInvocations(parser);
    }

    /**
     * Expect {@link GeoChatMessageParser#parseGroupCotEvent(CotEvent, String, boolean)} to be called and return a
     * {@link GeoChatParsedMessage} that has {@link #groupContact} as the sender.
     */
    private void expectParseGroupCotEvent()
    {
        when(parser.parseGroupCotEvent(any(), anyString(), anyBoolean()))
                .thenReturn(new GeoChatParsedMessage(groupContact, message));
    }

    /**
     * Expect {@link GeoChatMessageParser#parseDirectCotEvent(CotEvent)} to be called and return a
     * {@link GeoChatParsedMessage} that has {@link #groupContact} as the sender.
     */
    private void expectParseDirectCotEvent()
    {
        when(parser.parseDirectCotEvent(any()))
                .thenReturn(new GeoChatParsedMessage(groupContact, message));
    }

    /**
     * Verify that {@link GeoChatContactProvider#removeGroup(IGroupContact)} is called exactly once.
     */
    private void verifyRemoveGroupCalled()
    {
        verify(geoChatContactProvider, times(1)).removeGroup(eq(groupContact));
    }

    /**
     * Verify that {@link IChatProtocolHandlerDelegate#receiveMessage(IContact, IChatMessage)} is, or isn't, called.
     * This will also prevent parsing exceptions from going unnoticed even though
     * {@link GeoChatMessageReceiver#cotMessageReceived(String, String)} logs exceptions without rethrowing them.
     *
     * @param expectedTimes The number of times {@code receiveMessage(IContact, IChatMessage)} is expected to be called.
     */
    private void verifyReceiveMessageCalled(int expectedTimes)
    {
        verify(chatServiceProviderDelegate, times(expectedTimes)).receiveMessage(any(), any());
    }

    /**
     * Returns the specified file as a String.
     *
     * @param fileName The full file name
     * @return The file contents as a String
     * @throws IOException if there are errors retrieving the file
     */
    private String getFileAsString(String fileName) throws IOException
    {
        String filePath = FILES_DIR + fileName;
        URL url = Objects.requireNonNull(getClass().getClassLoader().getResource(filePath), "Could not locate " + filePath);

        try (InputStream is = url.openStream())
        {
            return new String(is.readAllBytes(), Charset.defaultCharset());
        }
    }
}