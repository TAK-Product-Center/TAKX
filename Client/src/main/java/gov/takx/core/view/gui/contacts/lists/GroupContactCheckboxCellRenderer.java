package gov.takx.core.view.gui.contacts.lists;

import gov.tak.api.contact.IGroupContact;
import gov.takx.api.annotation.PseudoBean;
import gov.takx.commons.swing.CheckBoxCellRenderer;
import gov.takx.core.view.gui.contacts.GroupContactManager;
import jakarta.inject.Inject;

import java.awt.Component;

/**
 * Checkbox cell renderer that displays a {@link GroupContactTreeNode} as a checkbox, group icon and name.
 *
 * @since 3.5
 */
@PseudoBean
public class GroupContactCheckboxCellRenderer extends CheckBoxCellRenderer<GroupContactTreeNode>
{
    @Inject
    private GroupContactManager groupContactManager;

    public GroupContactCheckboxCellRenderer()
    {
        // used to distinguish icon clicks from other panel clicks
        iconLabel.putClientProperty(GroupContactTreeController.ICON_LABEL_PROPERTY, true);
    }

    @Override
    protected void updateRendererComponentForValue(Component parent, GroupContactTreeNode node)
    {
        super.updateRendererComponentForValue(parent, node);

        final IGroupContact contact = node.getContact();

        if (contact != null) // root group nodes don't have contacts
        {
            setText(contact.getName());
            setIcon(groupContactManager.getIcon(contact));
        }
    }
}