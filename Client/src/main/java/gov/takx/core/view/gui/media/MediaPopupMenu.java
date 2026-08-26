package gov.takx.core.view.gui.media;

import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

/**
 * Popup menu for Media in the Explorer tree
 *
 * @since 2.8
 */
public class MediaPopupMenu extends JPopupMenu
{
    public MediaPopupMenu(MediaExplorable mediaExplorable)
    {
        JMenuItem play = add(new JMenuItem("Play"));
        play.addActionListener(ae -> {
            mediaExplorable.launchInPlayer();
        });

        JMenuItem props = add(new JMenuItem("Properties..."));
        props.addActionListener(ae -> {
            mediaExplorable.displayPropertiesDialog();
        });

        JMenuItem delete = add(new JMenuItem("Delete"));
        delete.addActionListener(ae -> {
            mediaExplorable.requestDelete();
        });
    }
}
