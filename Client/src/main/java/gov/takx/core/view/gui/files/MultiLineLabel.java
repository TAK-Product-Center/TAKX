package gov.takx.core.view.gui.files;

import jakarta.annotation.Nullable;

/**
 * Helper class to take an input string and convert it to HTML with line breaks at (near) a given maximum line length.
 *
 * @since 5.1 - previously an extension of JLabel, now just a helper class
 */
public class MultiLineLabel
{
    /**
     * Break up the given String into multiple lines, using the given {@code maxCharsPerLine} and the given
     * background color (or default if {@code null}).
     *
     * @param text The original text
     * @param maxCharsPerLine Target length at which to break lines, may be exceeded to prevent words from being split
     * @param backgroundColorCode  Color to specify in the HTML style for the text background.  Must be a color name
     *                             from the W3C supported list (like red, blue, etc.), or a hex value like {@code #303030}.
     *                             If {@code null} no style element is added, so the default background color will be used.
     * @return HTML with appropriate line breaks added, or {@code null} if the input {@code text} was {@code null}
     * @since 5.1
     */
    @Nullable
    public static String addLineBreaks(@Nullable String text, int maxCharsPerLine, @Nullable String backgroundColorCode)
    {
        if (text == null) return null;

        StringBuilder builder = new StringBuilder();
        char[] chars = text.trim().toCharArray();

        // The index of the start of line being accumulated.
        int lineStart = 0;

        // The index of the last whitespace seen.
        int lastWhitespace = -1;

        // The number of trailing whitespace characters, based on the current index.
        int trailingWhitespaceCount = 0;

        // This enables wrapping in the tooltip.
        builder.append("<html>");
        if (backgroundColorCode != null) builder.append("<div style='background:").append(backgroundColorCode).append(";'>");

        for (int i = 0; i < chars.length; i++)
        {
            if (Character.isWhitespace(chars[i]))
            {
                lastWhitespace = i;
                trailingWhitespaceCount++;

                // If the whitespace is at the beginning of the new line, ignore it.
                if (i == lineStart)
                {
                    lineStart++;
                }
            } else
            {
                trailingWhitespaceCount = 0;
            }

            // If we've exceeded the max length and there's a spot where we can create
            // a line break, split.
            if (i - lineStart > maxCharsPerLine && lastWhitespace != -1)
            {
                // If there was trailing whitespace, don't include it in the line.
                builder.append(chars, lineStart, lastWhitespace - lineStart
                        - (trailingWhitespaceCount > 0 ? (trailingWhitespaceCount - 1) : 0));
                builder.append("<br>");

                // The next line will start right after the last whitespace.
                lineStart = lastWhitespace + 1;

                // We need to look for the next whitespace.
                lastWhitespace = -1;
            }
        }

        // Look for trailing text that didn't exceed the max chars and append it.
        if (lineStart != chars.length - 1)
        {
            builder.append(chars, lineStart, chars.length - lineStart);
        }

        if (backgroundColorCode != null) builder.append("</div>");
        builder.append("</html>");

        return builder.toString();
    }
}
