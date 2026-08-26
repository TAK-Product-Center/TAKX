package gov.takx.commons.swing;

import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;

/**
 * Extends the default ButtonGroup implementation to allow all buttons to be deselected.
 * Copied from https://stackoverflow.com/a/16731838.
 *
 * @since 3.3
 */
public class DeselectableButtonGroup extends ButtonGroup
{
    @SuppressWarnings("ConstantConditions")
    @Override
    public void setSelected(ButtonModel model, boolean selected)
    {
        if (selected)
        {
            super.setSelected(model, selected);
        } else
        {
            clearSelection();
        }
    }
}
