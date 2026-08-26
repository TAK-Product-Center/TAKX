package gov.takx.core.view.gui.cachecleaner;

import javax.swing.AbstractButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

/**
 */
public class AddDrivePopupMenu extends JPopupMenu implements ActionListener
{
    private final CacheCleanerDialog owner;

    /**
     * Constructor.
     *
     * @param owner the cache cleaner dialog.
     */
    public AddDrivePopupMenu(CacheCleanerDialog owner)
    {
        this.owner = owner;

        // Iterate through available file roots and add all which are both readable and writable to the menu.
        for (File root : File.listRoots())
        {
            if (root.exists() && root.canRead() && root.canWrite())
            {
                String name = root.toString();
                JMenuItem menuItem = new JMenuItem(name);
                menuItem.setName(name);
                menuItem.addActionListener(this);
                add(menuItem);
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem source = (JMenuItem) e.getSource();
        owner.addRootDrive(source.getName());
    }

    /**
     * Shows the menu over the provided button.
     *
     * @param button The button above which to show the menu
     */
    public void showMenu(AbstractButton button)
    {
        setVisible(true);
        show(button, 0, button.getY() - getHeight());
        requestFocus();
    }
}
