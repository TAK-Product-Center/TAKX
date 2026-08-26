package gov.takx.core.view.gui.contacts.lists;

import gov.tak.api.contact.IIndividualContact;
import gov.takx.commons.swing.CheckBoxItem;
import jakarta.annotation.Nonnull;

/**
 * Represents a checkbox version of a contact, allowing contacts to be selected or deselected in a contact panel.
 *
 * @since 3.0 (renamed from ContactCheckListItem in 3.5)
 */
public class IndividualContactCheckListItem extends CheckBoxItem
{
    private final IIndividualContact contact;

    public IndividualContactCheckListItem(@Nonnull IIndividualContact contact)
    {
        super(contact.getName(), false);
        this.contact = contact;
    }

    /**
     * @return The contact associated with the checklist item instance
     */
    @Nonnull
    public IIndividualContact getContact()
    {
        return contact;
    }
}
