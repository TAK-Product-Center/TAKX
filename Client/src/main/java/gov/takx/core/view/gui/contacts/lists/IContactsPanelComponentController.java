package gov.takx.core.view.gui.contacts.lists;

import gov.tak.api.contact.IContact;
import jakarta.annotation.Nonnull;

import javax.swing.JComponent;

/**
 * Represents a controller for a panel used to display a list of contacts to the user.
 *
 * @since 3.0  (renamed from IContactsListPanelController in 3.5)
 */
public interface IContactsPanelComponentController<T extends JComponent>
{
    /**
     * Called when the given contact has been selected by the user.
     *
     * @param selectedContact The selected contact
     */
    void select(@Nonnull IContact selectedContact);

    /**
     * Called when the JComponent (tree, list) will be managed by this controller has been set.
     *
     * @param component The JComponent that will be managed by this controller
     * @since 3.5
     */
    void onComponentSet(@Nonnull T component);
}
