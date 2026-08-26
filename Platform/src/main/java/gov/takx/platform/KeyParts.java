package gov.takx.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;

/**
 * Utility class to handle the splitting of a tri-part key, including MapEntity key which contains the family, type,
 * and version, and MapObject key, which includes the family, type, and unit Id.
 *
 * @since 2.0.0
 */
public final class KeyParts
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final String family;
    private final String type;
    private final String versionOrUnitId;

    /**
     * Split the given key into its component parts: family, type, and (version or unit id).
     *
     * @param key Key consisting of family, type, and (version or unit id)
     */
    public KeyParts(String key)
    {
        // Split the map entity key into parts.
        // Note that TOOL map objects do not have versions, so we can get 2 or 3 parts from the split
        // Note further, that shapes are given unit ids that contain '/' characters by the shape service.  So,
        // we need to take elements 3+ and splice them back together to get the correct unit id.

        final String[] parts = key.split(KeyUtils.KEY_SEP);
        final int length = parts.length;

        if (length < 2)
        {
            logger.error("Invalid key: [{}] has too few parts [{}]", key, length);
            throw new RuntimeException("key failure on key: " + key);
        }

        family = parts[0];
        type = parts[1];

        if (length < 3)
        {
            versionOrUnitId = "";
        } else if (length == 3)
        {
            versionOrUnitId = parts[2];
        } else
        {
            versionOrUnitId = KeyUtils.getUniqueKey(Arrays.copyOfRange(parts, 2, parts.length));
        }
    }

    /**
     * Construct an instance with distinct family, type, and unit Id values.
     *
     * @since 2.18
     */
    public KeyParts(String family, String type, String versionOrUnitId)
    {
        this.family = family;
        this.type = type;
        this.versionOrUnitId = versionOrUnitId;
    }

    /**
     * Construct an instance with distinct
     *
     * @param parts Array of family, type, and unit Id values.  The array must be at least 3 elements
     *              long, and only the first 3 elements are used, corresponding to family, type, and unitId in that order.
     * @since 2.18
     */
    public KeyParts(String[] parts)
    {
        this.family = parts[0];
        this.type = parts[1];
        this.versionOrUnitId = parts[2];
    }

    public String getFamily()
    {
        return family;
    }

    public String getType()
    {
        return type;
    }

    public String getVersion()
    {
        return versionOrUnitId;
    }

    public String getUnitId()
    {
        return versionOrUnitId;
    }

    @Override
    public String toString()
    {
        return "KeyParts{" +
                "family='" + family + '\'' +
                ", type='" + type + '\'' +
                ", versionOrUnitId='" + versionOrUnitId + '\'' +
                '}';
    }
}
