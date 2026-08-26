package gov.takx.core.view.gui.contacts.lists;

import gov.tak.api.contact.IContact;
import jakarta.annotation.Nonnull;

import javax.swing.JComponent;

/**
 * Base controller for the controllers responsible for managing the {@link gov.takx.core.view.gui.contacts.ContactsPanel}
 * group contact tree and individual contact list.
 *
 * @since 3.5
 * @param <T> The type of component managed by the controller (e.g. JTree, JList)
 */
public abstract class AContactsPanelComponentController<T extends JComponent>
        implements IContactsPanelComponentController<T>
{
    @Nonnull
    protected final IContactSelectionListener<IContact> contactSelectionListener;

    T component;

    /**
     * @param contactSelectionListener The listener that will respond when a contact is selected.
     */
    protected AContactsPanelComponentController(@Nonnull IContactSelectionListener<IContact> contactSelectionListener)
    {
        this.contactSelectionListener = contactSelectionListener;
    }

    /**
     * Sets the JComponent that will be managed by this controller and notifies the implementor.
     *
     * @param component The JComponent that will be managed by this controller.
     */
    public void setComponent(@Nonnull T component)
    {
        this.component = component;
        onComponentSet(component);
    }

    @Override
    public void select(@Nonnull IContact selectedContact)
    {
        contactSelectionListener.onSelect(selectedContact);
    }
}
