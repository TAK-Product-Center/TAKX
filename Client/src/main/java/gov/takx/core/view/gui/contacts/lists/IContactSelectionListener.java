package gov.takx.core.view.gui.contacts.lists;

/**
 * Contract for a listener that will be notified when contacts are selected in a JTree, JList, etc.
 *
 * @param <T> The control entry type that will be returned when a contact is selected
 * @since 3.5
 */
@FunctionalInterface
public interface IContactSelectionListener<T>
{
    /**
     * @param entry The control entry that was selected
     */
    void onSelect(T entry);
}