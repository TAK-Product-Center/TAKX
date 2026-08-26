package gov.takx.commons.security;

/**
 * Gateway Parser Encryption Key implementation must implement this interface which extends Comparable.  The sequence
 * that keys are tried when decrypting are at the developer's discretion.  Consider using ParserEncryptionKey for
 * any implementation that should rely on the creation timestamp (the newest key is always tried first for decryption).
 *
 * @see ParserEncryptionKey
 * @since 1.2
 */
public interface IParserEncryptionKey extends Comparable<IParserEncryptionKey>
{
    /**
     * Identifies the encryption key as a hexadecimal string
     *
     * @return Hex encryption key
     */
    String getHexKey();

    /**
     * @return Time at which this key was created
     * @since 3.0
     */
    long getTimestamp();
}
