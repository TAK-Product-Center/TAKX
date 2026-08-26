package gov.takx.core.pluginclient;

import gov.takx.api.plugin.gateway.IGatewayParserDelegate;
import gov.takx.commons.security.IParserEncryptionKey;

import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages a list of encryption keys per (Device Type + Unit ID).
 * <p/>
 * This concrete implementation is hidden behind the IDevicePluginDelegate and IParserDelegate.
 *
 * @see IGatewayParserDelegate
 * @since 1.2
 */
public class EncryptionKeyMap
{
    private static final Map<String, SortedSet<IParserEncryptionKey>> parserKeyMap = new ConcurrentHashMap<>();

    /**
     * Attempts to retrieve a list of keys associated with the given map object key
     *
     * @param mapObjectKey Key of map object
     * @return set of encryption keys (may be empty)
     */
    public static SortedSet<IParserEncryptionKey> getParserKeys(String mapObjectKey)
    {
        SortedSet<IParserEncryptionKey> results;

        if (null != mapObjectKey)
        {
            results = parserKeyMap.get(mapObjectKey);
        } else
        {
            results = new TreeSet<>();
        }

        return results;
    }

    /**
     * Stores a list of developer-defined encryption keys, indexed using a developer defined approach (e.g. Key1, Key2,
     * etc.).
     *
     * @param mapObjectKey Device Type + Unit ID used to locate a particular list of keys
     * @param sortedKeySet Sorted set of keys
     */
    public static void setParserKeys(String mapObjectKey, SortedSet<IParserEncryptionKey> sortedKeySet)
    {
        if (null != mapObjectKey)
        {
            parserKeyMap.remove(mapObjectKey);

            parserKeyMap.put(mapObjectKey, sortedKeySet);
        }
    }

    private EncryptionKeyMap()
    {
    }
}
