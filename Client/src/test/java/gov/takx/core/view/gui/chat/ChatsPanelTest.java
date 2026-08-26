package gov.takx.core.view.gui.chat;

import gov.takx.core.chat.ChatMessageBuilder;
import gov.takx.core.chat.Conversation;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.SwingTestUtils;
import jakarta.annotation.Nonnull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the {@link ChatsPanel}.
 *
 * @since 5.2
 */
public class ChatsPanelTest extends AIntegrationTest
{
    private ChatsPanel chatsPanel;

    @BeforeEach
    void beforeEach()
    {
        chatsPanel = new ChatsPanel(conversation -> {
        }, conversation -> null, (label, integer) -> {
        });
    }

    @Test
    public void onNewMessage_addsNewConversationsToPanelMap()
    {
        final List<Conversation> conversations = List.of(createConversation(), createConversation(), createConversation());

        // Each conversation should be added b/c they are new
        conversations.forEach(conversation -> chatsPanel.onNewMessage(conversation));
        SwingTestUtils.waitForEdtEvents();
        assertThat(chatsPanel.conversationAndPanels.keySet()).containsAll(conversations);

        // Existing conversations should not result in new panels
        conversations.forEach(conversation -> chatsPanel.onNewMessage(conversation));
        SwingTestUtils.waitForEdtEvents();
        assertThat(chatsPanel.conversationAndPanels.size()).isEqualTo(conversations.size());
    }

    @Test
    public void onNewMessage_updatesExistingConversationsInPanelMap() throws Throwable
    {
        long sendTime = 100;
        final Conversation conversation = createEmptyConversation();
        addMessage(conversation, sendTime);

        // Add the conversation and its message
        SwingTestUtils.invokeAndWaitOnEDT(() -> chatsPanel.onNewMessage(conversation));

        // Check the panel's time to confirm it matches the last (only) message's time
        assertThat(chatsPanel.conversationAndPanels.get(conversation).getTime()).isEqualTo(sendTime);

        // Add a new message to the conversation with a new send time
        sendTime = 200;
        addMessage(conversation, sendTime);
        SwingTestUtils.invokeAndWaitOnEDT(() -> chatsPanel.onNewMessage(conversation));

        // Check the panel's time to confirm it matches the last message's time
        assertThat(chatsPanel.conversationAndPanels.get(conversation).getTime()).isEqualTo(sendTime);
    }

    @Test
    public void releaseResources_clearsPanelMap()
    {
        chatsPanel.onNewMessage(createConversation());
        SwingTestUtils.waitForEdtEvents();
        chatsPanel.releaseResources();

        assertThat(chatsPanel.conversationAndPanels.isEmpty()).isTrue();
    }

    /**
     * @return A new {@link Conversation} with a single message
     */
    private Conversation createConversation()
    {
        final Conversation conversation = createEmptyConversation();
        addMessage(conversation, 100);
        return conversation;
    }

    /**
     * @return A new {@link Conversation} with no messages
     */
    private Conversation createEmptyConversation()
    {
        final String uid = UUID.randomUUID().toString();
        return new Conversation(uid, "name", true);
    }

    /**
     * Adds a message to a {@link Conversation}
     *
     * @param conversation The conversation to update
     * @param sendTime The message's send time
     */
    private void addMessage(@Nonnull Conversation conversation, long sendTime)
    {
        final var chatMessage = new ChatMessageBuilder("senderUid", "senderName", "text", sendTime, conversation.getUid()).build();
        conversation.addReadMessage(chatMessage);
    }
}
