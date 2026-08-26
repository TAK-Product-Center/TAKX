package gov.takx.commons.swing;

import javax.swing.JPopupMenu;

/**
 * Interface for providing a {@link JPopupMenu} to a {@link SplitButton} to use as the drop-down menu.
 *
 * @since 3.3
 */
public interface IPopupMenuProvider
{
    /**
     * @return The popup menu to display for the button.
     */
    JPopupMenu getPopupMenu();
}
