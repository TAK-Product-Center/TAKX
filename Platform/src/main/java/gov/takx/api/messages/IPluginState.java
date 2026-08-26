package gov.takx.api.messages;

import gov.takx.api.persist.IPersistable;

/**
 * Describes the persistable state of any TAKX plugin.
 *
 * @since 1.3
 */

public interface IPluginState extends IPersistable
{
    /**
     * Get the unique key for this record.
     *
     * @return Unique key
     */
    public String getUniqueKey();

    /**
     * Set the unique key used to index the state record.
     *
     * @param key Unique key used to index the state record
     */
    public void setUniqueKey(String key);

    /**
     * Get the state string. The format of this string is arbitrary and determined by the plugin.
     *
     * @return The state string.
     */
    public String getState();

    /**
     * Set the state string.
     *
     * @param state Arbitrary state string
     */
    public void setState(String state);
}
