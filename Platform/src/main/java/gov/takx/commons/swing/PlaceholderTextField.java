package gov.takx.commons.swing;

import gov.takx.commons.utils.StringUtils;

import javax.swing.JTextField;
import javax.swing.text.Document;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * An extension for {@link JTextField} that allows for placing default placeholder text in the background of the text
 * field.  As soon as the user starts typing, the placeholder text will be replaced with the user input.
 * <p>
 * Use the {@link #setPlaceholderText(String)} method to populate the background placeholder text.
 *
 * @since 2.27
 */
public class PlaceholderTextField extends JTextField
{
    private String placeholderText;

    public PlaceholderTextField()
    {
    }

    public PlaceholderTextField(Document doc, String text, int columns)
    {
        super(doc, text, columns);
    }

    public PlaceholderTextField(int columns)
    {
        super(columns);
    }

    public PlaceholderTextField(String text)
    {
        super(text);
    }

    public PlaceholderTextField(String text, int columns)
    {
        super(text, columns);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (StringUtils.isEmpty(placeholderText) || !getText().isEmpty()) return;

        final Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setColor(getDisabledTextColor());
        graphics2D.drawString(placeholderText, getInsets().left, g.getFontMetrics().getMaxAscent() + getInsets().top);
    }

    /**
     * @param placeholderText The placeholder text that is displayed as "disabled text" in the background of the
     *                        {@link JTextField}.
     */
    public void setPlaceholderText(String placeholderText)
    {
        this.placeholderText = placeholderText;
    }

    public String getPlaceholderText()
    {
        return placeholderText;
    }
}
