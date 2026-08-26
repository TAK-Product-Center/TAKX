package gov.takx.commons.security;

import jakarta.annotation.Nonnull;

/**
 * Parser Encryption Key is an immutable hex key object.  A timestamp is created at instantiation, and the timestamp
 * field is used in the compareTo() method to determine sequencing in relation to other keys.
 *
 * @since 1.2
 */
@SuppressWarnings("ComparableImplementedButEqualsNotOverridden")
public class ParserEncryptionKey implements IParserEncryptionKey
{
    private final String hexKey;
    private final long timestamp = System.currentTimeMillis();

    /**
     * Create an immutable IParserEncryptionKey instance based on a String representation of a hexadecimal encryption
     * key.
     *
     * @param hexKey Hexadecimal encryption key
     */
    public ParserEncryptionKey(String hexKey)
    {
        this.hexKey = hexKey;
    }

    @Override
    public String getHexKey()
    {
        return hexKey;
    }

    @Override
    public long getTimestamp()
    {
        return timestamp;
    }

    /**
     * Comparison is based on the immutable timestamp from the time at which the ParserEncryptionKey instance
     * was created.
     *
     * @param o Instance of IParserEncryptionKey
     * @return See Comparable JavaDocs
     */
    @Override
    public int compareTo(@Nonnull IParserEncryptionKey o)
    {
        return Long.compare(getTimestamp(), o.getTimestamp());
    }
}
