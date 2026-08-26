package gov.takx.core.view.gui.contacts.lists;

import gov.tak.api.contact.IIndividualContact;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.swing.CheckBoxCellRenderer;
import gov.takx.core.view.gui.contacts.IndividualContactManager;
import jakarta.inject.Inject;

import java.awt.Component;

/**
 * Checkbox cell renderer that displays an {@link IndividualContactCheckListItem}'s {@link IIndividualContact}'s name
 * and icon.
 *
 * @since 3.5
 */
@PseudoBean
public class IndividualContactCheckBoxCellRenderer extends CheckBoxCellRenderer<IndividualContactCheckListItem>
{
    @Inject
    private IndividualContactManager individualContactManager;

    public IndividualContactCheckBoxCellRenderer()
    {
        // used to distinguish icon clicks from other panel clicks
        iconLabel.putClientProperty(GroupContactTreeController.ICON_LABEL_PROPERTY, true);
    }

    @Override
    protected void updateRendererComponentForValue(Component parent, IndividualContactCheckListItem node)
    {
        super.updateRendererComponentForValue(parent, node);

        final IIndividualContact contact = node.getContact();
        setText(contact.getName());
        setIcon(individualContactManager.getContactIconWithStatus(contact.getUid()));
    }
}