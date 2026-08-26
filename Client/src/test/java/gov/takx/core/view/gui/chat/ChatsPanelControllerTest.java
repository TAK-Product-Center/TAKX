package gov.takx.core.view.gui.chat;

import gov.tak.blacksmith.BlacksmithExtension;
import gov.tak.blacksmith.mock.Mocked;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.core.chat.ChatMessageBuilder;
import gov.takx.core.chat.Conversation;
import gov.takx.core.chat.ConversationStore;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import gov.takx.core.view.gui.contacts.conversation.ConversationMessageAddedEvent;
import gov.takx.test.AIntegrationTest;
import gov.takx.test.utils.CdiTestUtils;
import gov.takx.test.utils.SwingTestUtils;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests {@link ChatsPanelController}.
 *
 * @since 5.2
 */
@PseudoBean
public class ChatsPanelControllerTest extends AIntegrationTest
{
    @Mocked
    private IndividualContactManager individualContactManager;
    @Mocked
    private GroupContactManager groupContactManager;
    @Mocked
    private ConversationStore conversationStore;

    @Inject
    private Event<ConversationMessageAddedEvent> event;

    private ChatsPanelController controller;

    @BeforeAll
    void beforeAll()
    {
        controller = getBean(ChatsPanelController.class);
        BlacksmithExtension.setBeansToUpdate(CdiTestUtils.getInstance(controller));
    }

    @Test
    public void init_queriesConversationsAndAddsListeners() throws Throwable
    {
        final List<Conversation> persistedConversations = List.of(createConversation(), createConversation());
        when(conversationStore.getConversations()).thenReturn(persistedConversations);

        SwingTestUtils.invokeAndWaitOnEDT(() -> controller.init());

        verify(conversationStore).getConversations();
        verify(individualContactManager).addListener(any());
        verify(groupContactManager).addListener(any());

        // The ChatsPanel should contain all Conversations returned by the ConversationStore
        assertThat(controller.getPanel().conversationAndPanels.keySet())
                .containsExactlyInAnyOrderElementsOf(persistedConversations);
    }

    @Test
    public void conversationMessageAddedEvent_updatesChatsPanel() throws Throwable
    {
        final Conversation conversation = createConversation();
        final ConversationMessageAddedEvent messageAddedEvent = new ConversationMessageAddedEvent(conversation);

        // Run on EDT to ensure calls complete before the assertions are checked
        SwingTestUtils.invokeAndWaitOnEDT(() -> {
            controller.init();

            // The controller should update the ChatsPanel whenever a message added event is fired
            event.fire(messageAddedEvent);
        });

        // The panel map should now contain a mapping for the new conversation
        assertThat(controller.getPanel().conversationAndPanels).containsKey(conversation);
    }

    /**
     * @return A new {@link Conversation} containing a single message
     */
    private Conversation createConversation()
    {
        final String uid = UUID.randomUUID().toString();
        final Conversation conversation = new Conversation(uid, "name", true);
        conversation.addReadMessage(new ChatMessageBuilder("sender", "senderName", "text", 1, uid).build());

        return conversation;
    }
}
