package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nullable;

import javax.swing.JToggleButton;

import java.awt.Color;

/**
 * Extension of {@link JToggleButton} that allows customizing the background and foreground colors based on the selection state.
 *
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class JToggleButtonExt extends JToggleButton
{
    private Color unselectedBackground;
    private Color unselectedForeground;
    private Color selectedBackground;
    private Color selectedForeground;

    /**
     * Construct a new unselected toggle button.
     */
    public JToggleButtonExt()
    {
        setModel(new ToggleButtonModelExt());
        model.setSelected(false);
    }

    /**
     * @return the Color to use as the background when the button is in the selected state
     */
    @Nullable
    public Color getSelectedBackground()
    {
        return selectedBackground;
    }

    /**
     * @param selectedBackground the Color to use as the background when the button is in the selected state
     */
    public void setSelectedBackground(@Nullable Color selectedBackground)
    {
        this.selectedBackground = selectedBackground;
    }

    /**
     * @return the Color to use as the foreground when the button is in the selected state
     */
    @Nullable
    public Color getSelectedForeground()
    {
        return selectedForeground;
    }

    /**
     * @param selectedForeground the Color to use as the foreground when the button is in the selected state
     */
    public void setSelectedForeground(@Nullable Color selectedForeground)
    {
        this.selectedForeground = selectedForeground;
    }

    /**
     * @since 5.7
     */
    @IncubatingApi(since = "5.7")
    public class ToggleButtonModelExt extends ToggleButtonModel
    {
        @Override
        public void setSelected(boolean selected)
        {
            if (selected != isSelected())
            {
                super.setSelected(selected);
                if (selectedBackground != null)
                {
                    if (selected)
                    {
                        unselectedBackground = getBackground();
                        setBackground(selectedBackground);
                    } else
                    {
                        setBackground(unselectedBackground);
                    }
                }
                if (selectedForeground != null)
                {
                    if (selected)
                    {
                        unselectedForeground = getForeground();
                        setForeground(selectedForeground);
                    } else
                    {
                        setForeground(unselectedForeground);
                    }
                }
            }
        }
    }
}
