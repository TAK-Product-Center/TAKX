package gov.takx.commons.swing;

import jakarta.annotation.Nullable;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;

/**
 * An extension of {@link JButton}  with added usability features. Specifically:
 * <li>Ability to set disabled ToolTipText which is automatically applied/reverted when button is disabled/enabled.</li>
 *
 * @since 5.2
 */
public class JButtonExt extends JButton
{
    private String disabledToolTipText; // tooltip text to use when disabled
    private String revertToolTipText; // tooltip text to revert to when re-enabled

    /**
     * Creates a button with no set text or icon.
     */
    public JButtonExt()
    {
        super();
    }

    /**
     * Creates a button with an icon.
     *
     * @param icon  the Icon image to display on the button
     */
    public JButtonExt(Icon icon)
    {
        super(icon);
    }

    /**
     * Creates a button with text.
     *
     * @param text  the text of the button
     */
    public JButtonExt(String text)
    {
        super(text);
    }

    /**
     * Creates a button where properties are taken from the {@link Action} supplied.
     *
     * @param a the {@link Action} used to specify the new button
     */
    public JButtonExt(Action a)
    {
        super(a);
    }

    /**
     * Creates a button with initial text and an icon.
     *
     * @param text  the text of the button
     * @param icon  the Icon image to display on the button
     */
    public JButtonExt(String text, Icon icon)
    {
        super(text, icon);
    }

    /**
     * @return the disabledToolTipText string
     */
    @Nullable
    public String getDisabledToolTipText()
    {
        return disabledToolTipText;
    }

    /**
     * Sets the components tooltip text.
     *
     * @param text the string to display, or null for no tooltip
     */
    @Override
    public void setToolTipText(@Nullable String text)
    {
        if (!isEnabled() && hasDisabledToolTipText())
        {
            revertToolTipText = text;
        } else
        {
            super.setToolTipText(text);
        }
    }

    /**
     * Set the string to use as the tooltip when the button is disabled.
     *
     * @param disabledToolTipText the string to use when button is disabled, or null to use the normal tooltip text
     */
    public void setDisabledToolTipText(@Nullable String disabledToolTipText)
    {
        if (!isEnabled())
        {
            if (disabledToolTipText == null)
            {
                revertDisabledToolTipText();
            } else if (!hasDisabledToolTipText())
            {
                applyDisabledToolTipText(disabledToolTipText);
            } else
            {
                super.setToolTipText(disabledToolTipText);
            }
        }
        this.disabledToolTipText = disabledToolTipText;
    }

    /**
     * Enables (or disables) the button, changing the tooltip as necessary.
     *
     * @param enabled true to enable the button, otherwise false
     */
    @Override
    public void setEnabled(boolean enabled)
    {
        super.setEnabled(enabled);
        if (enabled)
        {
            revertDisabledToolTipText();
        } else
        {
            applyDisabledToolTipText(disabledToolTipText);
        }
    }

    /**
     * Checks for the disabled tool tip.
     *
     * @return whether the tooltip is non-null.
     */
    private boolean hasDisabledToolTipText()
    {
        return disabledToolTipText != null;
    }

    /**
     * Reverts the tooltip to the normal state.
     */
    private void revertDisabledToolTipText()
    {
        if (hasDisabledToolTipText())
        {
            super.setToolTipText(revertToolTipText);
            revertToolTipText = null;
        }
    }

    /**
     * Applies a new disabled state tooltip and captures current tooltip.
     *
     * @param disabledToolTipText the new tooltip.
     */
    private void applyDisabledToolTipText(String disabledToolTipText)
    {
        revertToolTipText = getToolTipText();
        super.setToolTipText(disabledToolTipText);
    }
}