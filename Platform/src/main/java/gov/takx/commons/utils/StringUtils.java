package gov.takx.commons.utils;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.Collection;

/**
 * A collection of generally useful operations on strings.
 *
 * @since 1.24
 */
@SuppressWarnings("Duplicates")
public class StringUtils
{
    /**
     * When joining a collection or an array of values, we'll use this string if the collection
     * or array is {@code null}.
     */
    private static final String JOIN_RESULT_ON_NULL_STRINGS = "null";

    /**
     * Determine if a string is blank, {@code null} is considered empty/blank.
     *
     * @param s String to test
     * @return {@code true} if string is {@code null} or blank, according to {@link String#isBlank()}
     * @since 5.5
     */
    public static boolean isBlank(String s)
    {
        return s == null || s.isBlank();
    }

    /**
     * Determine if a string is empty, {@code null} is considered empty.
     *
     * @param s String to test
     * @return {@code true} if string is {@code null} or empty, according to {@link String#isEmpty()}
     */
    public static boolean isEmpty(String s)
    {
        return s == null || s.isEmpty();
    }

    /**
     * Determine if a string is empty, {@code null} is considered empty.
     *
     * @param s    String to test
     * @param trim If {@code true} then the {@link String#isBlank()} will be used to test instead of {@link String#isEmpty()}
     * @return {@code true} if string is {@code null} or empty, according to {@link String#isEmpty()}/{@link String#isBlank()}
     */
    public static boolean isEmpty(String s, boolean trim)
    {
        return s == null || (trim ? s.isBlank() : s.isEmpty());
    }

    /**
     * Get the index of the Nth occurrence of the given target character.
     *
     * @param str    String to search
     * @param target Character to search for
     * @param n      occurrence to find (one-based)
     * @return Index of Nth occurrence, -1 if that occurrence doesn't exist
     * @since 2.30
     */
    public static int ordinalIndexOf(String str, char target, int n)
    {
        if (n <= 0) return -1;

        int pos = str.indexOf(target);
        while (--n > 0 && pos != -1)
        {
            pos = str.indexOf(target, pos + 1);
        }
        return pos;
    }

    /**
     * Tests if the given string starts with the specified prefix, ignoring case.
     *
     * @param s      String to test
     * @param prefix Prefix to test against
     * @return {@code true} if the given string starts with the specified prefix, ignoring case
     * @see org.apache.commons.lang3.StringUtils#startsWithIgnoreCase(java.lang.CharSequence, java.lang.CharSequence)
     * @since 3.0
     */
    public static boolean startsWithIgnoreCase(CharSequence s, CharSequence prefix)
    {
        return org.apache.commons.lang3.StringUtils.startsWithIgnoreCase(s, prefix);
    }

    /**
     * Tests if the given string ends with the specified suffix, ignoring case.
     *
     * @param s      String to test
     * @param suffix Suffix to test against
     * @return {@code true} if the given string ends with the specified suffix, ignoring case
     * @see org.apache.commons.lang3.StringUtils#endsWithIgnoreCase(java.lang.CharSequence, java.lang.CharSequence)
     * @since 3.0
     */
    public static boolean endsWithIgnoreCase(CharSequence s, CharSequence suffix)
    {
        return org.apache.commons.lang3.StringUtils.endsWithIgnoreCase(s, suffix);
    }

    /**
     * Join a collection of objects into one string placing the {@code delim} string between elements.  If the
     * collection of objects is {@code null}, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     * <p>
     * <b>Sample Usage</b>
     * <p>
     * <pre>
     *     // Assume strings is a collection of the strings "speed" and "pitch" and "roll"
     *
     *     join(strings, ", ", "rdm.")
     *
     *     returns: rdm.speed, rdm.pitch, rdm.roll
     * </pre>
     *
     * @param strings Strings to join, may be {@code null}
     * @param delim   Delimiter to place between strings
     * @param prefix  Optional prefix to prepend to each string as it is joined
     * @return joined string
     */
    public static String join(Collection<?> strings, String delim, String prefix)
    {
        return join((Iterable<?>) strings, delim, prefix);
    }

    /**
     * Join a collection of objects into one string placing the {@code delimiter} string between elements.  If the
     * collection of objects is {@code null}, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     * <p>
     * <b>Sample Usage</b>
     * <p>
     * <pre>
     *     // Assume strings is a collection of the strings "speed" and "pitch" and "roll"
     *
     *     join(strings, ", ", "rdm.")
     *
     *     returns: rdm.speed, rdm.pitch, rdm.roll
     * </pre>
     *
     * @param strings   Strings to join, may be {@code null}
     * @param delimiter Delimiter to place between strings
     * @param prefix    Optional prefix to prepend to each string as it is joined
     * @return joined string
     * @since 3.0
     */
    public static String join(Iterable<?> strings, String delimiter, String prefix)
    {
        if (null == strings) return JOIN_RESULT_ON_NULL_STRINGS;

        StringBuilder sb = new StringBuilder();
        boolean first = true;

        for (Object s : strings)
        {
            if (!first && delimiter != null) sb.append(delimiter); // Add in delimiter
            first = false;

            if (prefix != null) sb.append(prefix);
            sb.append(s);
        }

        return sb.toString();
    }

    /**
     * Join a collection of objects into one string placing a comma (',') between elements.
     *
     * @param strings Strings to join
     * @return joined string
     */
    public static String join(Collection<?> strings)
    {
        return join(strings, ",", null);
    }

    /**
     * Join an array of objects into one string placing the {@code delim} string between elements.  If the
     * array of objects is {@code null}, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     * <p>
     * <b>Sample Usage</b>
     * <p>
     * <pre>
     *     // Assume strings is a collection of the strings "speed" and "pitch" and "roll"
     *
     *     join(strings, ", ", "rdm.")
     *
     *     returns: rdm.speed, rdm.pitch, rdm.roll
     * </pre>
     *
     * @param strings Strings to join, may be {@code null}.
     * @param delim   Delimiter to place between strings
     * @param prefix  Optional prefix to prepend to each string as it is joined
     * @return joined string
     */
    public static String join(Object[] strings, String delim, String prefix)
    {
        return null == strings
                ? JOIN_RESULT_ON_NULL_STRINGS
                : join(Arrays.asList(strings), delim, prefix);
    }

    /**
     * Join an array of objects into one string placing a comma (',') between elements.  If the
     * array of objects is {@code null}, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     *
     * @param strings Strings to join, may be {@code null}.
     * @return joined string
     */
    public static String join(Object[] strings)
    {
        return null == strings
                ? JOIN_RESULT_ON_NULL_STRINGS
                : join(Arrays.asList(strings), ",", null);
    }

    /**
     * Join the contents of the specified array, placing the {@code delim} string between elements.  If the
     * array is {@code null}, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     *
     * @param a      array to join, may be {@code null}
     * @param delim  Delimiter to place between elements
     * @param prefix Optional prefix to prepend to each element as it is joined
     * @return joined string
     * @see #join(java.util.Collection, String, String)
     * @since 1.49
     */
    public static String join(byte[] a, String delim, String prefix)
    {
        if (a == null) return JOIN_RESULT_ON_NULL_STRINGS;

        int max = a.length - 1;
        if (max == -1) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0;; i++)
        {
            if (prefix != null) sb.append(prefix);
            sb.append(a[i]);

            if (i == max) return sb.toString();

            if (delim != null) sb.append(delim);
        }
    }

    /**
     * Join the contents of the specified array, placing the {@code delim} string between elements.  If the
     * array is {@code null}, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     *
     * @param a      array to join, may be {@code null}
     * @param delim  Delimiter to place between elements
     * @param prefix Optional prefix to prepend to each element as it is joined
     * @return joined string
     * @see #join(java.util.Collection, String, String)
     * @since 1.49
     */
    public static String join(char[] a, String delim, String prefix)
    {
        if (a == null) return JOIN_RESULT_ON_NULL_STRINGS;

        int max = a.length - 1;
        if (max == -1) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0;; i++)
        {
            if (prefix != null) sb.append(prefix);
            sb.append(a[i]);

            if (i == max) return sb.toString();

            if (delim != null) sb.append(delim);
        }
    }

    /**
     * Join the contents of the specified array, placing the {@code delim} string between elements.  If the
     * array is {@code null}, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     *
     * @param a      array to join, may be {@code null}
     * @param delim  Delimiter to place between elements
     * @param prefix Optional prefix to prepend to each element as it is joined
     * @return joined string
     * @see #join(java.util.Collection, String, String)
     * @since 1.49
     */
    public static String join(boolean[] a, String delim, String prefix)
    {
        if (a == null) return JOIN_RESULT_ON_NULL_STRINGS;

        int max = a.length - 1;
        if (max == -1) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0;; i++)
        {
            if (prefix != null) sb.append(prefix);
            sb.append(a[i]);

            if (i == max) return sb.toString();

            if (delim != null) sb.append(delim);
        }
    }

    /**
     * Join the contents of the specified array, placing the {@code delim} string between elements.  If the
     * array is {@code null}, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     *
     * @param a      array to join, may be {@code null}
     * @param delim  Delimiter to place between elements
     * @param prefix Optional prefix to prepend to each element as it is joined
     * @return joined string
     * @see #join(java.util.Collection, String, String)
     * @since 1.49
     */
    public static String join(short[] a, String delim, String prefix)
    {
        if (a == null) return JOIN_RESULT_ON_NULL_STRINGS;

        int max = a.length - 1;
        if (max == -1) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0;; i++)
        {
            if (prefix != null) sb.append(prefix);
            sb.append(a[i]);

            if (i == max) return sb.toString();

            if (delim != null) sb.append(delim);
        }
    }

    /**
     * Join the contents of the specified array, placing the {@code delim} string between elements.  If the
     * array is {@code null}, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     *
     * @param a      array to join, may be {@code null}
     * @param delim  Delimiter to place between elements
     * @param prefix Optional prefix to prepend to each element as it is joined
     * @return joined string
     * @see #join(java.util.Collection, String, String)
     * @since 1.49
     */
    public static String join(int[] a, String delim, String prefix)
    {
        if (a == null) return JOIN_RESULT_ON_NULL_STRINGS;

        int max = a.length - 1;
        if (max == -1) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0;; i++)
        {
            if (prefix != null) sb.append(prefix);
            sb.append(a[i]);

            if (i == max) return sb.toString();

            if (delim != null) sb.append(delim);
        }
    }

    /**
     * Join the contents of the specified array, placing the {@code delim} string between elements.  If the
     * array is null, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     *
     * @param a      array to join, may be {@code null}
     * @param delim  Delimiter to place between elements
     * @param prefix Optional prefix to prepend to each element as it is joined
     * @return joined string
     * @see #join(java.util.Collection, String, String)
     * @since 1.49
     */
    public static String join(long[] a, String delim, String prefix)
    {
        if (a == null) return JOIN_RESULT_ON_NULL_STRINGS;

        int max = a.length - 1;
        if (max == -1) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0;; i++)
        {
            if (prefix != null) sb.append(prefix);
            sb.append(a[i]);

            if (i == max) return sb.toString();

            if (delim != null) sb.append(delim);
        }
    }

    /**
     * Join the contents of the specified array, placing the {@code delim} string between elements.  If the
     * array is {@code null}, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     *
     * @param a      array to join, may be {@code null}
     * @param delim  Delimiter to place between elements
     * @param prefix Optional prefix to prepend to each element as it is joined
     * @return joined string
     * @see #join(java.util.Collection, String, String)
     * @since 1.49
     */
    public static String join(float[] a, String delim, String prefix)
    {
        if (a == null) return JOIN_RESULT_ON_NULL_STRINGS;

        int max = a.length - 1;
        if (max == -1) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0;; i++)
        {
            if (prefix != null) sb.append(prefix);
            sb.append(a[i]);

            if (i == max) return sb.toString();

            if (delim != null) sb.append(delim);
        }
    }

    /**
     * Join the contents of the specified array, placing the {@code delim} string between elements.  If the
     * array is {@code null}, then JOIN_RESULT_ON_NULL_STRINGS is returned.
     *
     * @param a      array to join, may be {@code null}
     * @param delim  Delimiter to place between elements
     * @param prefix Optional prefix to prepend to each element as it is joined
     * @return joined string
     * @see #join(java.util.Collection, String, String)
     * @since 1.49
     */
    public static String join(double[] a, String delim, String prefix)
    {
        if (a == null) return JOIN_RESULT_ON_NULL_STRINGS;

        int max = a.length - 1;
        if (max == -1) return "";

        StringBuilder sb = new StringBuilder();

        for (int i = 0;; i++)
        {
            if (prefix != null) sb.append(prefix);
            sb.append(a[i]);

            if (i == max) return sb.toString();

            if (delim != null) sb.append(delim);
        }
    }

    /**
     * Determine if a given string, {@code s}, starts with any of the strings given in {@code prefixes}.
     *
     * @param s        String to test
     * @param prefixes Collection of strings prefixes to test against
     * @return {@code true} if for any prefix {@code p} from {@code prefixes},
     * {@code }s.startsWith(p)} returns {@code true}, {@code false} otherwise
     * @since 1.41
     */
    public static boolean startsWithAny(String s, Collection<String> prefixes)
    {
        for (String start : prefixes)
        {
            if (s.startsWith(start)) return true;
        }

        return false;
    }

    /**
     * Remove all whitespace from the given string.
     *
     * @param s String to process
     * @return string with whitespace removed
     */
    public static String stripWhitespace(String s)
    {
        return s.replaceAll("\\W", "");
    }

    /**
     * Given a source string and a marker string, return the portion of the source before the marker, if it exists.
     * For example, given source = {@code "foo:bar"} and marker = {@code ":"}, this method returns {@code "foo"}.
     *
     * @param source Source string
     * @param marker marker to search for
     * @return Source trimmed at the occurrence of the marker
     * @since 2.0.0
     */
    public static String trimAt(String source, String marker)
    {
        final int index = source.indexOf(marker);

        return (index > 0) ? source.substring(0, index) : source;
    }

    /**
     * Parse a String of comma-separated integers to an array of int.  No exceptions are handled here, so any
     * failure to parse a sub-element will be seen by the caller.
     *
     * @param s String to parse, if {@code null} or empty, a zero-length array is returned
     * @return Array of integers parsed from given string
     * @throws NumberFormatException If any substring fails to parse as an integer
     * @since 3.0
     */
    @Nonnull
    public static int[] splitToInt(@Nullable String s) throws NumberFormatException
    {
        if (isEmpty(s)) return new int[0];

        final String[] parts = s.split(",");
        final int[] values = new int[parts.length];

        for (int i = 0; i < parts.length; i++)
        {
            values[i] = Integer.parseInt(parts[i].trim());
        }

        return values;
    }

    /**
     * Parse a String of comma-separated integers to an array of int.  If a sub-element fails to parse as an
     * integer, it will be replaced with the given {@code defaultValue}.
     *
     * @param s            String to parse, if {@code null} or empty, a zero-length array is returned
     * @param defaultValue Value to use in place of any sub-element that fails to parse
     * @return Array of integers parsed from given string
     * @since 3.0
     */
    @Nonnull
    public static int[] splitToInt(@Nullable String s, int defaultValue)
    {
        if (isEmpty(s)) return new int[0];

        final String[] parts = s.split(",");
        final int[] values = new int[parts.length];

        for (int i = 0; i < parts.length; i++)
        {
            try
            {
                values[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException ignore)
            {
                values[i] = defaultValue;
            }
        }

        return values;
    }

    /**
     * Return the input string, potentially abbreviated if its length is longer than the given {@code maxLength}.
     * If the input is longer than {@code maxLength}, it will be truncated at that length - 3 and {@code ...} will be
     * appended.  Thus, the resulting string will be at most {@code maxLength} characters long.
     *
     * @param input String to process
     * @param maxLength Maximum length of {@code input}, over which abbreviation is performed
     * @return input string, possibly abbreviated
     * @throws IllegalArgumentException if {@code maxLength <= 3}
     * @since 3.4
     */
    @Nonnull
    public static String abbreviate(@Nonnull String input, int maxLength)
    {
        return abbreviate(input, maxLength, "...");
    }

    /**
     * Return the input string, potentially abbreviated if its length is longer than the given {@code maxLength}.
     * If the input is longer than {@code maxLength}, it will be truncated at that length minus the ellipsis length
     * and the given {@code ellipsis} string will be appended.  Thus, the resulting string will be at most
     * {@code maxLength} characters long.
     *
     * @param input String to process
     * @param maxLength Maximum length of {@code input}, over which abbreviation is performed
     * @param ellipsis String to append if input is longer than {@code maxLength}
     * @return input string, possibly abbreviated
     * @throws IllegalArgumentException if {@code maxLength <= ellipsis.length()}
     * @since 3.4
     */
    @Nonnull
    public static String abbreviate(@Nonnull String input, int maxLength, @Nonnull String ellipsis)
    {
        final int ellipsisLength = ellipsis.length();
        if (maxLength <= ellipsisLength) throw new IllegalArgumentException(
                String.format("'maxLength [%d] must be > ellipsis.length() [%d]", maxLength, ellipsisLength));

        return input.length() > maxLength ? (input.substring(0, maxLength - ellipsisLength) + ellipsis) : input;
    }

    private StringUtils()
    {
        /* no instances */
    }
}
