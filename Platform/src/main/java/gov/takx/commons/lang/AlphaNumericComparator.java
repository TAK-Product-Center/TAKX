package gov.takx.commons.lang;

import java.util.Comparator;
import java.util.function.Function;
import java.util.regex.Pattern;

import static java.lang.String.CASE_INSENSITIVE_ORDER;

/**
 * Comparator comparing a String extracted from a given object type using a case-insensitive, alpha-numeric ordering.
 *
 * @since 3.2
 * @see <a href="https://gist.github.com/akshaymnair/00a060b51d6d4e47fbc9d3dfcf99ef14">heavily modified from this gist</a>
 */
public class AlphaNumericComparator<T> implements Comparator<T>
{
    // Note: this pattern differs from the one in the original gist in order to make it correctly handle
    // upper case and special characters (not letters and not digits).
    private static final Pattern SPLIT_PATTERN = Pattern.compile("((?<=[^\\d])(?=[\\d]))|((?<=[\\d])(?=[^\\d]))");

    private final Function<? super T, String> keyExtractor;

    /**
     * Construct a comparator that uses {@link Object#toString()} as the key extractor.
     */
    public AlphaNumericComparator()
    {
        this(Object::toString);
    }

    /**
     * Construct a comparator using the given key extraction function.
     *
     * @param keyExtractor The function used to extract the String sort key
     */
    public AlphaNumericComparator(Function<? super T, String> keyExtractor)
    {
        this.keyExtractor = keyExtractor;
    }

    @Override
    public int compare(T o1, T o2)
    {
        final String key1 = keyExtractor.apply(o1);
        final String key2 = keyExtractor.apply(o2);

        if (CASE_INSENSITIVE_ORDER.compare(key1, key2) == 0) return 0;

        // Split the strings into alphabetic and numeric elements
        final String[] parts1 = SPLIT_PATTERN.split(key1);
        final String[] parts2 = SPLIT_PATTERN.split(key2);

        for (int i = 0, length = Math.min(parts1.length, parts2.length); i < length; i++)
        {
            final String part1 = parts1[i];
            final String part2 = parts2[i];

            final int asStringComparison = CASE_INSENSITIVE_ORDER.compare(part1, part2);

            if (asStringComparison != 0) return compareAsInteger(part1, part2, asStringComparison);
        }

        return parts1.length > parts2.length ? 1 : -1;
    }

    /**
     * @param defaultComparison Comparison value to return if parsing as integer fails
     * @return comparison of n1 to n2, or {@code defaultComparison} if either input cannot be parsed as an integer
     */
    private int compareAsInteger(String n1, String n2, int defaultComparison)
    {
        try
        {
            return Integer.compare(Integer.parseInt(n1), Integer.parseInt(n2));
        } catch (Exception ignore)
        {
            return defaultComparison;
        }
    }
}
