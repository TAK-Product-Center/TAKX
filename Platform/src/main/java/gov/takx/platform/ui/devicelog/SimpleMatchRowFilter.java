package gov.takx.platform.ui.devicelog;

import javax.swing.RowFilter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Table row filter that performs a simple string contains search against a configured list of columns.
 * No Regex support is offered.
 *
 * @since 2.16
 */
public class SimpleMatchRowFilter extends RowFilter<StructureTableModel, Integer>
{
    private final boolean regex;
    private final int[] columns;
    private final String textMatch;
    private final Matcher matcher;

    /**
     * Construct a filter on the given columns and matches the provided String as a regular string.
     *
     * @param textMatch The value string to match against the value in the specified {@code column}.  If the value
     *                  contains {@code match}, then it is included.
     * @param columns   The column model indices against which to test the {@code match} string
     */
    public SimpleMatchRowFilter(String textMatch, int... columns)
    {
        this(textMatch, false, columns);
    }

    /**
     * Construct a filter on the given columns and match the provided string either as a regular string or as a regex.
     *
     * @param match   The value string to match against the value in the specified {@code column}.  If the value
     *                contains {@code match}, then it is included.
     * @param regex   True if the match string should be treated as a regex, false otherwise.
     * @param columns The column model indices against which to test the {@code match} string
     * @since 2.36
     */
    public SimpleMatchRowFilter(String match, boolean regex, int... columns)
    {
        this.regex = regex;
        this.columns = columns;
        this.textMatch = match;

        matcher = regex ? Pattern.compile(match, Pattern.CASE_INSENSITIVE).matcher("") : null;
    }

    /**
     * @return {@code true} if the given row should be included
     */
    @Override
    public boolean include(Entry<? extends StructureTableModel, ? extends Integer> entry)
    {
        final int count = entry.getValueCount();

        for (int column : columns)
        {
            if (column < count)
            {
                final String columnStringValue = entry.getStringValue(column);
                if (regex)
                {
                    matcher.reset(columnStringValue);
                    if (matcher.find()) return true;
                } else
                {
                    // Regular text based search
                    if (columnStringValue.contains(textMatch)) return true;
                }
            }
        }

        return false;
    }
}
