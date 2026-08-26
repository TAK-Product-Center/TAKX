package gov.takx.api.ui;

import javax.swing.JMenuItem;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 */
public class IconGroupSelectionButton extends APopupButton implements ActionListener
{
    private AIconSelectionPanel owner;

    public IconGroupSelectionButton(AIconSelectionPanel owner)
    {
        super("Select Icon Group to Display");

        setMenuOrientation(MenuOrientation.Up);
        this.owner = owner;
    }

    /**
     * Set the group names to be used as menu items on this popup button
     *
     * @param groups a list of group names to add as menu items
     */
    public void setGroups(List<String> groups)
    {
        removeAllMenuItems();
        for (String groupName : groups)
        {
            JMenuItem menuItem = new JMenuItem(groupName);
            menuItem.setName(groupName);
            addMenuItem(menuItem);
        }

        addMenuItemsActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem source = (JMenuItem) e.getSource();
        owner.selectIconGroupView(source.getName());
    }
}
