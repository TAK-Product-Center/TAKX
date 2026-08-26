package gov.takx.commons.swing;

/**
 * Simple POJO that tracks the checked state of an item. For more advanced uses, extend this class and use it as the
 * class to populate the model of the JList, JTable, or JTree that is being represented as a check list.
 *
 * @see CheckBoxCellRenderer
 * @since 1.4 (renamed in 3.0)
 */
public class CheckBoxItem implements ICheckBoxItem, Comparable<CheckBoxItem>
{
    private final String label;
    private boolean selected = true;

    public CheckBoxItem(String label)
    {
        this.label = label;
    }

    public CheckBoxItem(String label, boolean selected)
    {
        this.label = label;
        this.selected = selected;
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

    @Override
    public String toString()
    {
        return label;
    }

    @Override
    public int compareTo(CheckBoxItem other)
    {
        return label.compareTo(other.label);
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CheckBoxItem that = (CheckBoxItem) o;

        return label.equals(that.label);
    }

    @Override
    public int hashCode()
    {
        return label.hashCode();
    }
}
