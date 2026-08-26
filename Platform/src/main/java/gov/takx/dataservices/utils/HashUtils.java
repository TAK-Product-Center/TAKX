package gov.takx.dataservices.utils;

import gov.takx.api.annotation.Internal;
import gov.takx.commons.security.EncodingUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Static methods to support generating MD5 hashes.
 *
 */
@Internal
public class HashUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    // Previously, we had a static, shared MessageDigest object and we synchronized use of it.  That decreased the
    // concurrency we can achieve when starting up, so we moved to thread local objects.

    private static final ThreadLocal<MessageDigest> DIGESTERS = ThreadLocal.withInitial(HashUtils::getMD5Digester);

    private static MessageDigest getMD5Digester()
    {
        try
        {
            return MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e)
        {
            logger.error("Failed to get MD5 digester", e);
        }
        return null;
    }

    private static MessageDigest getAndResetDigester()
    {
        final MessageDigest md = DIGESTERS.get();
        md.reset();
        return md;
    }

    /**
     * Get the MD5 hash of the given bytes returned as a string of encoded hex characters.
     *
     * @param input Array of data to digest
     * @return hex string encoding of digest value
     */
    public static String getHash(byte[] input)
    {
        return EncodingUtils.bytesToHex(getAndResetDigester().digest(input));
    }

    /**
     * Get a "short" (8 character) MD5 hash of the given bytes returned as a string of encoded hex characters.
     * <p>
     * The full hash is collapsed down to 4 bytes using XOR to reduce the chance of a collision compared to just
     * taking the first 4 bytes of the original digest.
     *
     * @param input Array of data to digest
     * @return 8 character hex string encoding of digest value
     * @since 3.2
     */
    public static String getShortHash(byte[] input)
    {
        final byte[] digest = getAndResetDigester().digest(input);

        // Collapse the full hash down to 4 bytes
        final byte[] xorBytes = new byte[4];

        for (int i = 0; i < digest.length; i++) xorBytes[i % 4] ^= digest[i];

        return EncodingUtils.bytesToHex(xorBytes);
    }

    /**
     * Ensure no instances.
     */
    private HashUtils()
    {
    }
}
