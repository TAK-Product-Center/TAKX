package gov.takx.commons.utils;

import gov.tak.api.annotation.IncubatingApi;
import jakarta.annotation.Nonnull;

import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.DocumentFilter;
import javax.swing.text.JTextComponent;

import java.util.regex.Pattern;

/**
 * Document filter that blocks any input that does not match a given pattern.
 *
 * @since 5.4
 */
@IncubatingApi(since = "5.4") // May merge with LimitedDocument in the future
public class RegexDocumentFilter extends DocumentFilter
{
    private final Pattern pattern;

    /**
     * Construct a new document filter that limits the document to a specified number of characters.
     *
     * @param maxLength the maximum number of characters to allow
     * @since 5.7
     */
    @SuppressWarnings("unused")
    public RegexDocumentFilter(int maxLength)
    {
        this(maxLength, false, false);
    }

    /**
     * Construct a new document filter that limits the document to a specified number of characters, and optionally
     * restricting to alpha characters, numeric characters, or both.
     *
     * @param maxLength the maximum number of characters to allow
     * @param alpha {@code true} to apply alpha restriction (does not override the numeric argument)
     * @param numeric {@code true} to apply numeric restriction (does not override the alpha argument)
     * @since 5.7
     */
    public RegexDocumentFilter(int maxLength, boolean alpha, boolean numeric)
    {
        this(Pattern.compile("[" +
                ((alpha || numeric)
                        ? (alpha ? "A-Za-z" : "") + (numeric ? "0-9" : "")
                        : ".")
                + "]" +
                (maxLength > 0 ? "{0," + maxLength + "}" : "")));
    }

    /**
     * Create a new filter with the given regex pattern.
     * @param pattern Regex pattern string to compile.
     */
    public RegexDocumentFilter(@Nonnull String pattern)
    {
        this(Pattern.compile(pattern));
    }

    /**
     * Create a new filter with the given regex pattern.
     * @param pattern Regex pattern.
     */
    public RegexDocumentFilter(@Nonnull Pattern pattern)
    {
        this.pattern = pattern;
    }

    /**
     * @param component Component to install this document filter into.
     */
    public void install(@Nonnull JTextComponent component)
    {
        ((AbstractDocument) component.getDocument()).setDocumentFilter(this);
    }

    @Override
    public void insertString(DocumentFilter.FilterBypass fb, int offset, String text, AttributeSet attr) throws BadLocationException
    {
        Document document = fb.getDocument();

        String newText = document.getText(0, offset) + text + document.getText(offset, document.getLength() - offset);

        if (pattern.matcher(newText).matches())
        {
            super.insertString(fb, offset, text, attr);
        }
    }

    @Override
    public void replace(DocumentFilter.FilterBypass fb, int offset, int length, String string, AttributeSet attr) throws BadLocationException
    {
        if (length > 0)
        {
            fb.remove(offset, length);
        }
        insertString(fb, offset, string, attr);
    }
}
