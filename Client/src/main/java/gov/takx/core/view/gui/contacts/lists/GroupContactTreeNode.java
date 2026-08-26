package gov.takx.core.view.gui.contacts.lists;

import gov.tak.api.contact.IGroupContact;
import gov.takx.commons.swing.ICheckBoxItem;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Tree node for representing an {@link gov.tak.api.contact.IGroupContact}.
 *
 * @since 3.0 (renamed from ContactTreeNode in 3.5)
 */
public class GroupContactTreeNode extends DefaultMutableTreeNode implements ICheckBoxItem
{
    private boolean selected = false;

    /**
     * Constructor.
     *
     * @param contact The contact represented by the node
     */
    public GroupContactTreeNode(@Nullable IGroupContact contact)
    {
        super(contact);
    }

    @Override
    public boolean isSelected()
    {
        return selected;
    }

    @Override
    public void setSelected(boolean selected)
    {
        this.selected = selected;
    }

    /**
     * @return The contact represented by the node.
     */
    @Nullable
    public IGroupContact getContact()
    {
        return (IGroupContact) getUserObject();
    }

    @Override
    public void setUserObject(@Nonnull Object userObject)
    {
        if (!(userObject instanceof IGroupContact))
        {
            throw new IllegalArgumentException("User object must be an IGroupContact");
        }

        super.setUserObject(userObject);
    }
}
