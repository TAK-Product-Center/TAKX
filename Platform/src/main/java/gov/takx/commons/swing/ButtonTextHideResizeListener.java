package gov.takx.commons.swing;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import javax.swing.AbstractButton;

import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * A {@link java.awt.event.ComponentListener} that responds to {@link #componentResized(ComponentEvent)} calls. When the
 * width of the referenced component falls below it's preferred width, all buttons registered with this listener will have
 * their text set to {@code null} and the size at which this occurred will be saved. When the component size increases beyond
 * the saved size, all buttons will have their text reverted to the original value.
 *
 * @apiNote This class expects button text to remain constant. Do not register buttons that have their text manipulated after
 * registration.
 * @since 5.7
 */
@IncubatingApi(since = "5.7")
public class ButtonTextHideResizeListener extends ComponentAdapter
{
    private static final String HIDDEN_TEXT_FLAG_PROPERTY = "IS_TEXT_HIDDEN";
    private static final String HIDDEN_TEXT_PROPERTY = "HIDDEN_TEXT";

    private final Set<AbstractButton> buttons = new HashSet<>();
    private boolean textHidden;
    private int unhideAtWidth;

    /**
     * Register a button with class, to have its text hidden when a component size is too small.
     *
     * @param button a button within the container that this listener is registered with
     */
    public void registerButton(@Nonnull AbstractButton button)
    {
        buttons.add(button);
    }

    @Override
    public void componentResized(ComponentEvent e)
    {
        var container = (Container) e.getSource();
        var size = container.getSize();
        if (!textHidden)
        {
            var prefSize = container.getPreferredSize();
            if (prefSize.width == size.width)
            {
                textHidden = true;
                unhideAtWidth = prefSize.width;
                buttons.forEach(this::hideText);
            }
        } else if (size.width > unhideAtWidth)
        {
            textHidden = false;
            unhideAtWidth = 0;
            buttons.forEach(this::unhideText);
        }
    }

    private void hideText(AbstractButton button)
    {
        if (button.getClientProperty(HIDDEN_TEXT_FLAG_PROPERTY) == null)
        {
            button.putClientProperty(HIDDEN_TEXT_PROPERTY, button.getText());
            button.putClientProperty(HIDDEN_TEXT_FLAG_PROPERTY, true);
            button.setText(null);
        }
    }

    private void unhideText(AbstractButton button)
    {
        if (button.getClientProperty(HIDDEN_TEXT_FLAG_PROPERTY) != null)
        {
            button.setText((String) button.getClientProperty(HIDDEN_TEXT_PROPERTY));
            button.putClientProperty(HIDDEN_TEXT_PROPERTY, null);
            button.putClientProperty(HIDDEN_TEXT_FLAG_PROPERTY, null);
        }
    }
}
