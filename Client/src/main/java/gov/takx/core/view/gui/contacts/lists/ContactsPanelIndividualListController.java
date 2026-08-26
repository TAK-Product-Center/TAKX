package gov.takx.core.view.gui.contacts.lists;

import gov.tak.api.contact.IIndividualContact;
import gov.takx.api.annotation.InvokeOnEDT;
import gov.takx.api.event.project.ProjectClosing;
import gov.takx.core.chat.ConversationStore;
import gov.takx.core.view.gui.contacts.ContactsPanelController;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import jakarta.annotation.Nonnull;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.enterprise.event.Reception;
import jakarta.inject.Inject;

import javax.swing.Icon;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import java.awt.Component;
import java.util.function.Function;

/**
 * Adds additional functionality to {@link IndividualContactListController} to support the list that is displayed in
 * the {@link gov.takx.core.view.gui.contacts.ContactsPanel}.
 *
 * @since 3.5
 */
@ApplicationScoped
public class ContactsPanelIndividualListController extends AContactsPanelComponentController<JList<IIndividualContact>>
{
    private final ConversationStore conversationStore;
    private final IndividualContactManager individualContactManager;
    private IndividualContactListController<IIndividualContact> listController;

    @Inject
    ContactsPanelIndividualListController(ContactsPanelController contactsPanelController,
                                          ConversationStore conversationStore,
                                          IndividualContactManager individualContactManager)
    {
        super(contactsPanelController);

        this.conversationStore = conversationStore;
        this.individualContactManager = individualContactManager;
    }

    @Override
    public void onComponentSet(@Nonnull JList<IIndividualContact> component)
    {
        listController = new IndividualContactListController<>(component, Function.identity(), Function.identity(),
                contactSelectionListener::onSelect);
        component.setCellRenderer(new ContactListCellRenderer());
    }

    @SuppressWarnings("unused")
    @InvokeOnEDT
    private void onProjectClosing(@Observes(notifyObserver = Reception.IF_EXISTS) ProjectClosing event)
    {
        listController.releaseResources();
    }

    /**
     * Renderer for {@link IIndividualContact}s displayed in the {@link gov.takx.core.view.gui.contacts.ContactsPanel}.
     */
    private class ContactListCellRenderer implements ListCellRenderer<IIndividualContact>
    {
        private final ContactListCellRendererPanel contactRendererPanel = new ContactListCellRendererPanel();

        @Override
        public Component getListCellRendererComponent(JList<? extends IIndividualContact> list,
                                                      IIndividualContact contact, int index,
                                                      boolean isSelected, boolean cellHasFocus)
        {
            contactRendererPanel.setConversation(conversationStore.getConversationForContact(contact));

            final Icon icon = individualContactManager.getContactIconWithStatus(contact.getUid());
            if (icon != null) contactRendererPanel.setIcon(icon);

            return contactRendererPanel;
        }
    }
}
