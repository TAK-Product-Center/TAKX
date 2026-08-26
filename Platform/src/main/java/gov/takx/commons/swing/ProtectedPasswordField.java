package gov.takx.commons.swing;

import gov.takx.platform.ui.UIResources;
import jakarta.annotation.Nonnull;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JToggleButton;
import javax.swing.text.Document;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import static gov.takx.platform.ui.ResourceDirectory.PLATFORM_ICONS;

/**
 * An extension for {@link JPasswordField}  that hides the text value when it is set programmatically e.g. during editing a
 * form. The user sees placeholder text to indicate that the password exists, but cannot select it or reveal it. Also handles
 * setting the state of a reveal toggle button. A clear button is also necessary since the user has no easy way to overwrite
 * an existing value with an empty string.
 * TODO should probably use this in place of all password fields where the user may want to reveal the value
 * @since 5.1
 */
public class ProtectedPasswordField extends JPasswordField implements FocusListener
{

    private static final Icon VISIBILITY_ON_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "visibility.png");
    private static final Icon VISIBILITY_OFF_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "visibility_off.png");
    private static final Icon CLEAR_ICON = UIResources.getStandardSizedIcon(PLATFORM_ICONS, "cancel.png");

    private char echoChar = '*';
    private String placeholderText = "";
    private String hiddenPlaceholderText = "<Cannot Reveal Existing Password>";
    // holds the hidden password value
    private String hiddenText;
    //tells the field to revert the reveal toggle if the user made no change
    private boolean revertReveal = false;
    //Button to reveal the text, only if the user has changed it
    private final JToggleButton revealToggleButton;
    //Button to clear the value, necessary since we revert to the hidden text if the user doesnt enter a value
    private final JButton clearButton;

    /**
     * Constructs a new {@link ProtectedPasswordField} with a default document, {@code null} starting text string, and 0
     * column width.
     */
    public ProtectedPasswordField()
    {
        this(null, null, 0);
    }

    /**
     * Constructs a new empty {@link ProtectedPasswordField} with the specified number of columns.  A default model is
     * created, and the initial string is set to {@code null}.
     *
     * @param columns the number of columns &gt;= 0
     */
    public ProtectedPasswordField(int columns)
    {
        this(null, null, columns);
    }

    /**
     * Constructs a new {@link ProtectedPasswordField} initialized with the specified text.  The document model is set to the
     * default, and the number of columns to 0.
     *
     * @param text the text to be displayed, {@code null} if none
     */
    public ProtectedPasswordField(String text)
    {
        this(null, text, 0);
    }

    /**
     * Constructs a new {@link ProtectedPasswordField} initialized with the specified text and columns.  The document model
     * is set to the default.
     *
     * @param text the text to be displayed, {@code null} if none
     * @param columns the number of columns &gt;= 0
     */
    public ProtectedPasswordField(String text, int columns)
    {
        this(null, text, columns);
    }

    /**
     * Constructs a new {@link ProtectedPasswordField} that uses the given text storage model and the given number of
     * columns. This is the constructor through which the other constructors feed. The echo character is set to '*', but may
     * be changed by the current Look and Feel.  If the document model is {@code null}, a default one will be created.
     *
     * @param doc the text storage to use
     * @param txt the text to be displayed, {@code null} if none
     * @param columns the number of columns to use to calculate the preferred width &gt;= 0; if columns is set to zero, the
     * preferred width will be whatever naturally results from the component implementation
     */
    public ProtectedPasswordField(Document doc, String txt, int columns)
    {
        super(doc, txt, columns);

        addFocusListener(this);

        revealToggleButton = new JToggleButton();
        revealToggleButton.setBorder(null);
        revealToggleButton.setToolTipText("Reveal Password");
        revealToggleButton.setIcon(VISIBILITY_ON_ICON);
        revealToggleButton.setSelectedIcon(VISIBILITY_OFF_ICON);
        new ComponentBorder(revealToggleButton).install(this);
        this.revealToggleButton.addActionListener(action -> {
            if (hiddenText != null)
            {
                //This tells the field to revert the toggle button state back to false if the user does not change the value
                revertReveal = true;
                //grab the focus to force the user to change the value
                this.grabFocus();
            }
            revealChange();
        });

        clearButton = new JButton();
        clearButton.setBorder(null);
        clearButton.setIcon(CLEAR_ICON);
        clearButton.setToolTipText("Clear");
        clearButton.setMargin(new Insets(1, 1, 1, 1));
        clearButton.addActionListener(action -> {
            hiddenText = null;
            //TODO TAKX-2592 we need this to fire document listeners to trigger validation
            super.setText("");
            this.grabFocus();
            //need to repaint since we might not actually be changing the text
            super.repaint();

        });
        new ComponentBorder(clearButton).install(this);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        //dont render placeholder if text is nonempty, if field has focus, or if placeholder text is empty
        if (hasFocus() || !super.getText().isEmpty()) return;
        String currentPlaceholder = hiddenText != null ? hiddenPlaceholderText : placeholderText;
        if (currentPlaceholder.isEmpty()) return;

        final Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setColor(getDisabledTextColor());
        graphics2D.setFont(getFont().deriveFont(Font.ITALIC));
        graphics2D.drawString(currentPlaceholder, getInsets().left, g.getFontMetrics().getMaxAscent() + getInsets().top);
    }

    /**
     * Update properties based on changes to the reveal toggle button
     */
    private void revealChange()
    {
        super.setEchoChar(revealToggleButton.isSelected() ? 0 : echoChar);
        revealToggleButton.setToolTipText(revealToggleButton.isSelected() ? "Hide Password" : "Reveal Password");
    }

    /**
     * Set the placeholder text that is used to hide the actual value
     *
     * @param hiddenPlaceholderText the string
     */
    public void setHiddenPlaceholderText(@Nonnull String hiddenPlaceholderText)
    {
        this.hiddenPlaceholderText = hiddenPlaceholderText;
    }

    /**
     * @return the placeholder text
     */
    public String getHiddenPlaceholderText()
    {
        return hiddenPlaceholderText;
    }

    /**
     * @return the echo character to use when the password is hidden
     */
    @Override
    public char getEchoChar()
    {
        return echoChar;
    }

    /**
     * Set the echo character
     *
     * @param echoChar the echo character to display
     */
    @Override
    public void setEchoChar(char echoChar)
    {
        this.echoChar = echoChar;
    }

    /**
     * @return the clear button
     */
    public JButton getClearButton()
    {
        return clearButton;
    }

    /**
     * @return the reveal toggle button
     */
    public JToggleButton getRevealToggleButton()
    {
        return revealToggleButton;
    }

    /**
     * @return the placeholder for empty values
     */
    public String getPlaceholderText()
    {
        return placeholderText;
    }

    /**
     * Set the placeholder for empty values
     *
     * @param placeholderText the string
     */
    public void setPlaceholderText(@Nonnull String placeholderText)
    {
        this.placeholderText = placeholderText;
    }

    /**
     * This sets the {@link #hiddenText} value but does not change the text value of the parent class
     *
     * @param text the new text to be set
     */
    @Override
    public void setText(@Nonnull String text)
    {
        if (!text.isEmpty())
        {
            super.setText("");
            hiddenText = text;
        } else
        {
            super.setText(text);
        }
    }

    /**
     * If a hidden internal value is present, then return it, else return the super value.
     *
     * @return the hidden password or the parent password
     */
    @Override
    public char[] getPassword()
    {
        return hiddenText != null
                ? hiddenText.toCharArray()
                : super.getPassword();
    }

    /**
     * @param event the event to be processed
     */
    @Override
    public void focusGained(FocusEvent event)
    {
    }

    /**
     * When focus is lost, check to see if the user has entered a value. If they have, then disable protection features by
     * setting {@link #hiddenText} to null. Also revert the toggle button state if necessary.
     *
     * @param event the event to be processed
     */
    @Override
    public void focusLost(FocusEvent event)
    {
        if (hiddenText != null)
        {
            if (ProtectedPasswordField.super.getPassword().length != 0)
            {
                //user has entered a value, unset the hidden value
                hiddenText = null;
            } else
            {
                //user left field without typing new password, revert the revealState if necessary and reset the placeholder
                if (revertReveal)
                {
                    revealToggleButton.setSelected(false);
                    revealChange();
                    revertReveal = false;
                }
                super.setText("");
            }
        }
    }
}
