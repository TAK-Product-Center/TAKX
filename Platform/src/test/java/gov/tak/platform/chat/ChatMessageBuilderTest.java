package gov.tak.platform.chat;

import gov.tak.api.chat.IChatMessage;
import gov.takx.core.chat.ChatMessageBuilder;
import jakarta.annotation.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests {@link ChatMessageBuilder}
 *
 * @since 3.4
 */
public class ChatMessageBuilderTest
{
    private static final String SENDER_UID = "sender UID";
    private static final String SENDER_NAME = "sender name";
    private static final String TEXT = "text";
    private static final long SEND_TIME = 1L;
    private static final String CONVERSATION_UID = "conversation UID";

    private static final String MODIFIED_TEXT = "modified text";
    private static final String MODIFIED_UID = "modified UID";
    private static final long MODIFIED_RECEIVE_TIME = 1L;

    @Test
    public void build_ReturnsCorrectValues()
    {
        final IChatMessage message = new ChatMessageBuilder(SENDER_UID, SENDER_NAME, TEXT, SEND_TIME, CONVERSATION_UID).build();

        checkMessageValues(message, SENDER_UID, SENDER_NAME, TEXT, SEND_TIME,
                ChatMessageBase.UNSET_RECEIVE_TIME, CONVERSATION_UID, null);
    }

    @Test
    public void build_ReturnsCorrectValues_WhenAdditionalValuesSet()
    {
        final IChatMessage message = new ChatMessageBuilder(SENDER_UID, SENDER_NAME, TEXT, SEND_TIME, CONVERSATION_UID)
                .withReceiveTime(MODIFIED_RECEIVE_TIME)
                .withText(MODIFIED_TEXT)
                .withUid(MODIFIED_UID)
                .build();

        checkMessageValues(message, SENDER_UID, SENDER_NAME, MODIFIED_TEXT, SEND_TIME,
                MODIFIED_RECEIVE_TIME, CONVERSATION_UID, MODIFIED_UID);
    }

    @Test
    public void build_ReturnsCorrectValues_WhenCreatedWithCopyConstructor()
    {
        final IChatMessage original =
                new ChatMessageBuilder(SENDER_UID, SENDER_NAME, TEXT, SEND_TIME, CONVERSATION_UID)
                        .withText(MODIFIED_TEXT)
                        .withUid(MODIFIED_UID)
                        .withReceiveTime(MODIFIED_RECEIVE_TIME)
                        .build();

        final IChatMessage copy = new ChatMessageBuilder(original).build();

        checkMessageValues(copy, original);
    }

    @Test
    public void constructor_ThrowsIllegalArgumentException_WhenPassedInvalidSendTime()
    {
        //noinspection ResultOfObjectAllocationIgnored
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ChatMessageBuilder(SENDER_UID, SENDER_NAME, TEXT, -1, CONVERSATION_UID));
    }

    @Test
    public void withReceiveTime_ThrowsIllegalArgumentException_WhenPassedInvalidReceiveTime()
    {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new ChatMessageBuilder(SENDER_UID, SENDER_NAME, TEXT, SEND_TIME, CONVERSATION_UID).withReceiveTime(-1));
    }

    private void checkMessageValues(IChatMessage copy, IChatMessage original)
    {
        checkMessageValues(copy, original.getSenderUid(), original.getSenderName(),
                original.getText(), original.getSendTime(), original.getReceiveTime(),
                original.getConversationUid(), original.getUid());
    }

    private void checkMessageValues(IChatMessage message, String senderUid, String senderName, String text,
                                    long sendTime, long receiveTime, String conversationUid, @Nullable String uid)
    {
        assertThat(message.getSenderUid()).isEqualTo(senderUid);
        assertThat(message.getSenderName()).isEqualTo(senderName);
        assertThat(message.getText()).isEqualTo(text);
        assertThat(message.getSendTime()).isEqualTo(sendTime);
        assertThat(message.getReceiveTime()).isEqualTo(receiveTime);
        assertThat(message.getConversationUid()).isEqualTo(conversationUid);

        final String resultUid = message.getUid();

        // The UID param is expected to be null if no UID was explicitly set; we just need to verify that a value was
        // auto-generated
        if (uid == null)
        {
            assertThat(resultUid).isNotNull();
        } else
        {
            assertThat(resultUid).isEqualTo(uid);
        }
    }
}