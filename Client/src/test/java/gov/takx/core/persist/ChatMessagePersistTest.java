package gov.takx.core.persist;

import gov.tak.api.chat.ChatMessageStatus;
import gov.tak.api.chat.IChatMessage;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.api.persist.IChatMessagePersist;
import gov.takx.core.chat.ChatMessageBuilder;
import gov.takx.core.chat.IPersistableChatMessage;
import gov.takx.test.AContainerTest;
import gov.takx.test.PersistTestSetup;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link gov.tak.api.chat.IChatMessage} persist services provided by {@link gov.takx.api.persist.IChatMessagePersist}.
 * 
 * @since 3.4
 */
@PseudoBean
public class ChatMessagePersistTest extends AContainerTest
{
    private static final String LOCAL_USER_UID = "Local User UID";
    private static final String LOCAL_USER_NAME = "Local User Name";
    private static final String MESSAGE_TEXT = "text";

    private static final String CONVERSATION_A_UID = "Conversation A UID";
    private static final String REMOTE_USER_A_UID = "Remote User A UID";
    private static final String REMOTE_USER_A_NAME = "Remote User A Name";

    private static final IPersistableChatMessage SENT_MESSAGE_A1 =
            new ChatMessageBuilder(LOCAL_USER_UID, LOCAL_USER_NAME, MESSAGE_TEXT, 1L, CONVERSATION_A_UID).build();
    private static final IPersistableChatMessage SENT_MESSAGE_A2 =
            new ChatMessageBuilder(LOCAL_USER_UID, LOCAL_USER_NAME, MESSAGE_TEXT, 2L, CONVERSATION_A_UID).build();
    private static final IPersistableChatMessage RECEIVED_MESSAGE_A1 =
            new ChatMessageBuilder(REMOTE_USER_A_UID, REMOTE_USER_A_NAME, MESSAGE_TEXT, 1L, CONVERSATION_A_UID).withReceiveTime(2L).build();
    private static final IPersistableChatMessage RECEIVED_MESSAGE_A2 =
            new ChatMessageBuilder(REMOTE_USER_A_UID, REMOTE_USER_A_NAME, MESSAGE_TEXT, 2L, CONVERSATION_A_UID).withReceiveTime(3L).build();

    private static final String CONVERSATION_B_UID = "Conversation B UID";
    private static final String REMOTE_USER_B_UID = "Remote User B UID";
    private static final String REMOTE_USER_B_NAME = "Remote User B Name";

    private static final IPersistableChatMessage SENT_MESSAGE_B1 =
            new ChatMessageBuilder(LOCAL_USER_UID, LOCAL_USER_NAME, MESSAGE_TEXT, 1L, CONVERSATION_B_UID).build();
    private static final IPersistableChatMessage SENT_MESSAGE_B2 =
            new ChatMessageBuilder(LOCAL_USER_UID, LOCAL_USER_NAME, MESSAGE_TEXT, 2L, CONVERSATION_B_UID).build();
    private static final IPersistableChatMessage RECEIVED_MESSAGE_B1 =
            new ChatMessageBuilder(REMOTE_USER_B_UID, REMOTE_USER_B_NAME, MESSAGE_TEXT, 1L, CONVERSATION_B_UID).withReceiveTime(2L).build();
    private static final IPersistableChatMessage RECEIVED_MESSAGE_B2 =
            new ChatMessageBuilder(REMOTE_USER_B_UID, REMOTE_USER_B_NAME, MESSAGE_TEXT, 2L, CONVERSATION_B_UID).withReceiveTime(3L).build();

    private PersistTestSetup test;

    @Inject
    private IChatMessagePersist chatMessagePersist;

    @BeforeAll
    public void setup()
    {
        test = new PersistTestSetup();
        test.init();

        chatMessagePersist.persistMessage(SENT_MESSAGE_A1);
        chatMessagePersist.persistMessage(SENT_MESSAGE_A2);
        chatMessagePersist.persistMessage(RECEIVED_MESSAGE_A1);
        chatMessagePersist.persistMessage(RECEIVED_MESSAGE_A2);

        chatMessagePersist.persistMessage(SENT_MESSAGE_B1);
        chatMessagePersist.persistMessage(SENT_MESSAGE_B2);
        chatMessagePersist.persistMessage(RECEIVED_MESSAGE_B1);
        chatMessagePersist.persistMessage(RECEIVED_MESSAGE_B2);
    }

    @AfterAll
    public void teardown()
    {
        test.shutdown();
    }

    @Test
    public void getConversationMessages_returnsCorrectResults()
    {
        final List<IPersistableChatMessage> conversationA = chatMessagePersist.getConversationMessages(CONVERSATION_A_UID);

        assertThat(conversationA).containsOnly(SENT_MESSAGE_A1, SENT_MESSAGE_A2, RECEIVED_MESSAGE_A1, RECEIVED_MESSAGE_A2);

        final IChatMessage sentMessageA1 =
                conversationA.stream().filter(m -> m.getUid().equals(SENT_MESSAGE_A1.getUid())).findFirst().orElse(null);
        verifyMessagesAreTheSame(sentMessageA1, SENT_MESSAGE_A1);

        final IChatMessage receivedMessageA1 =
                conversationA.stream().filter(m -> m.getUid().equals(RECEIVED_MESSAGE_A1.getUid())).findFirst().orElse(null);
        verifyMessagesAreTheSame(receivedMessageA1, RECEIVED_MESSAGE_A1);

        final List<IPersistableChatMessage> conversationB = chatMessagePersist.getConversationMessages(CONVERSATION_B_UID);

        assertThat(conversationB).containsOnly(SENT_MESSAGE_B1, SENT_MESSAGE_B2, RECEIVED_MESSAGE_B1, RECEIVED_MESSAGE_B2);

        final IChatMessage sentMessageB2 =
                conversationB.stream().filter(m -> m.getUid().equals(SENT_MESSAGE_B2.getUid())).findFirst().orElse(null);
        verifyMessagesAreTheSame(sentMessageB2, SENT_MESSAGE_B2);

        final IChatMessage receivedMessageB2 =
                conversationB.stream().filter(m -> m.getUid().equals(RECEIVED_MESSAGE_B2.getUid())).findFirst().orElse(null);
        verifyMessagesAreTheSame(receivedMessageB2, RECEIVED_MESSAGE_B2);
    }

    /**
     * Verify that a message is not {@code null} and matches another message.
     *
     * @param message1 The message to check
     * @param message2 The message with the expected values
     */
    private void verifyMessagesAreTheSame(IChatMessage message1, IChatMessage message2)
    {
        assertThat(message1).isNotNull();

        assertThat(message1.getUid()).isEqualTo(message2.getUid());
        assertThat(message1.getConversationUid()).isEqualTo(message2.getConversationUid());
        assertThat(message1.getSenderUid()).isEqualTo(message2.getSenderUid());
        assertThat(message1.getSendTime()).isEqualTo(message2.getSendTime());
        assertThat(message1.getReceiveTime()).isEqualTo(message2.getReceiveTime());
        assertThat(message1.getStatus()).isEqualTo(message2.getStatus());
        assertThat(message1.getText()).isEqualTo(message2.getText());
        assertThat(message1.getSenderName()).isEqualTo(message2.getSenderName());
    }

    @Test
    public void persistMessage_updatesExistingMessage()
    {
        final String conversationUid = UUID.randomUUID().toString();
        final IPersistableChatMessage originalMessage = makeMessage(conversationUid);

        chatMessagePersist.persistMessage(originalMessage);
        final IPersistableChatMessage queryMessage1 = chatMessagePersist.getConversationMessages(conversationUid).get(0);
        assertThat(queryMessage1.getStatus()).isEqualTo(ChatMessageStatus.none);

        queryMessage1.setStatus(ChatMessageStatus.sent);
        chatMessagePersist.persistMessage(queryMessage1);
        final IChatMessage queryMessage2 = chatMessagePersist.getConversationMessages(conversationUid).get(0);
        assertThat(queryMessage2.getStatus()).isEqualTo(ChatMessageStatus.sent);
    }

    @Test
    public void deleteConversation_removesAllConversationMessages()
    {
        final String conversationUid = UUID.randomUUID().toString();
        final IPersistableChatMessage message1 = makeMessage(conversationUid);
        final IPersistableChatMessage message2 = makeMessage(conversationUid);

        chatMessagePersist.persistMessage(message1);
        chatMessagePersist.persistMessage(message2);
        assertThat(chatMessagePersist.getConversationMessages(conversationUid)).containsOnly(message1, message2);

        chatMessagePersist.deleteConversation(conversationUid);
        assertThat(chatMessagePersist.getConversationMessages(conversationUid)).isEmpty();
    }

    /**
     * @param conversationUid The conversation UID
     * @return A new chat message with the local user's info and the provided conversation id
     */
    private IPersistableChatMessage makeMessage(String conversationUid)
    {
        return new ChatMessageBuilder(LOCAL_USER_UID, LOCAL_USER_NAME, MESSAGE_TEXT, System.currentTimeMillis(),
                conversationUid).build();
    }
}