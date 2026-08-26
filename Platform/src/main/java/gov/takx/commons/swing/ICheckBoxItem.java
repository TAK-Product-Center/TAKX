package gov.takx.commons.swing;

/**
 * Interface for use with {@link CheckBoxCellRenderer} when extending {@link CheckBoxItem} isn't possible (e.g. for a
 * tree node that is already extending a tree node class).
 *
 * @since 3.0
 */
public interface ICheckBoxItem
{
    /**
     * @return Whether the item is selected.
     */
    boolean isSelected();

    /**
     * @param selected Whether the item is selected
     */
    void setSelected(boolean selected);
}
