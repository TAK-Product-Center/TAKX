package gov.takx.api.media;

/**
 * Simple globally available class for making a media key.
 *
 * @since 2.37
 */
public class MediaKey
{
    private static final String KEY_SEP = ":";

    /**
     * Make a key to be used in an IMedia instance.
     *
     * @param keyOfSource The key for the source of the media. Typically, a plugin's key or "user" for user added media.
     * @param name        The name of the media. Most often the name of the file or the endpoint of the url.
     * @return the key
     */
    public static String makeKey(String keyOfSource, String name)
    {
        return keyOfSource + KEY_SEP + name;
    }

    /**
     * Given a key, return the name portion.
     *
     * @param key Key to process
     * @return Name portion, or the given {@code key} if it doesn't contain the key separator.
     * @since 5.0
     */
    public static String nameFromKey(String key)
    {
        final String[] parts = key.split(KEY_SEP);
        return parts.length > 1 ? parts[1] : key;
    }
}
