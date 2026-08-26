package gov.takx.core.view.gui.contacts.lists;

import javax.swing.JList;

import java.awt.Point;

/**
 * A JList that only selects an item from user click if the click was within the bounds of an item cell. This differs
 * from the default JList behavior of selecting the last index in the list if the user clicks in the blank area of the
 * component.
 *
 * @since 3.0
 */
public class ImprovedSelectionJList<E> extends JList<E>
{
    @Override
    public int locationToIndex(Point location)
    {
        int index = super.locationToIndex(location);
        if (index != -1 && !getCellBounds(index, index).contains(location))
        {
            return -1;
        } else
        {
            return index;
        }
    }
}
