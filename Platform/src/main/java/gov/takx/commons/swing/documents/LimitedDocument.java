package gov.takx.commons.swing.documents;

import jakarta.annotation.Nonnull;

import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;

import java.awt.Toolkit;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Creates a document suitable for use as a JTextField/Pane document that can limit the maximum length
 * and allowable characters, validate input characters, and transform input strings.
 *
 * @since 3.0
 */
public class LimitedDocument extends DefaultStyledDocument
{
    /**
     * Default validator, allows ALL characters.
     */
    public static final Predicate<Character> ALLOW_ALL = c -> true;

    private final int lengthLimit;
    private final Predicate<Character> validator;
    private final Function<String, String> transformer;

    /**
     * Construct a document with the given length limit, allowing all characters.
     *
     * @param lengthLimit Maximum length of document (text)
     * @see #ALLOW_ALL
     */
    public LimitedDocument(int lengthLimit)
    {
        this(lengthLimit, ALLOW_ALL);
    }

    /**
     * Construct a document allowing only characters passed by the given validator, with an unlimited length.
     *
     * @param validator Validator to test characters in the document
     */
    public LimitedDocument(@Nonnull Predicate<Character> validator)
    {
        this(Integer.MAX_VALUE, validator);
    }

    /**
     * Construct a document with the given length limit, allowing only characters passed by the given validator.
     *
     * @param lengthLimit Maximum length of document (text)
     * @param validator   Validator to test characters in the document
     */
    public LimitedDocument(int lengthLimit, @Nonnull Predicate<Character> validator)
    {
        this(lengthLimit, validator, Function.identity());
    }

    /**
     * Construct a document with the given length limit, allowing only characters passed by the given validator, and transforming all input strings
     * using the given transformer.
     *
     * @param lengthLimit Maximum length of document (text)
     * @param validator   Validator to test characters in the document
     * @param transformer A transformer for inserted strings
     * @since 5.8
     */
    public LimitedDocument(int lengthLimit, @Nonnull Predicate<Character> validator, @Nonnull Function<String, String> transformer)
    {
        this.lengthLimit = lengthLimit;
        this.validator = validator;
        this.transformer = transformer;
    }

    /**
     * Overridden to test against maximum length and allowed characters.
     */
    @Override
    public void insertString(int offset, String str, AttributeSet attr) throws BadLocationException
    {
        if (str == null) return;

        str = transform(str);
        if ((getLength() + str.length()) <= lengthLimit && isValid(str))
        {
            super.insertString(offset, str, attr);
        } else
        {
            Toolkit.getDefaultToolkit().beep();
        }
    }

    /**
     * Validate each character in the given string using our configured validator.
     *
     * @param s String to test
     * @return {@code true} if all characters in the string pass the validator
     */
    private boolean isValid(String s)
    {
        for (int i = 0, length = s.length(); i < length; i++)
        {
            if (!validator.test(s.charAt(i))) return false;
        }
        return true;
    }

    /**
     * Applies the transformer.
     *
     * @param s the String to transform
     * @return the transformed string, or the input string if there is no transformer
     */
    private String transform(String s)
    {
        return transformer.apply(s);
    }
}
