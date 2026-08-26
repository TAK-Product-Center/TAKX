package gov.takx.commons.security;

import jakarta.annotation.Nonnull;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

/**
 * Utility methods to encode/decode hex strings.
 *
 * @since 1.2 (recreated in 3.0)
 */
public final class EncodingUtils
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Converts the byte array into a string that contains the hexadecimal equivalent.
     * The string uses all upper case characters for backward compatibility.
     *
     * @param bytes byte array to convert to hexadecimal
     * @return Hexadecimal values of the byte array as a string
     */
    public static String bytesToHex(byte[] bytes)
    {
        return new String(Hex.encodeHex(bytes, false));
    }

    /**
     * Converts hexadecimal digits in a string to a byte array.
     *
     * @param hexString Hexadecimal values in a string
     * @return byte array equivalent of the hexadecimal values, {@code null} if conversion fails
     */
    public static byte[] hexToBytes(@Nonnull String hexString)
    {
        try
        {
            return Hex.decodeHex(hexString.toCharArray());
        } catch (DecoderException e)
        {
            logger.error("Failed to convert hex: [{}]: {}", hexString, e.getMessage());
            return null;
        }
    }

    private EncodingUtils()
    {
    }
}
