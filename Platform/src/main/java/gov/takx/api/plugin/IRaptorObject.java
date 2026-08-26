package gov.takx.api.plugin;

import gov.takx.platform.KeyUtils;

/**
 * Namespace-protected object that includes descriptive metadata.
 *
 */
public interface IRaptorObject extends IMetadata
{
    /**
     * Get the globally unique namespace that the object is registered under.  This is a true namespace, and
     * developers are strongly encouraged to use reverse-domain notation, e.g. com.acme.plugins.
     * <p/>
     * <b>Note:</b> The namespace prefix gov.takx is reserved and must not be used.
     *
     * @return The namespace that this plugin is registered under
     */
    String getFamily();

    /**
     * Get the type identifier for this object.  The type identifier must be unique within a given namespace.  Type
     * names must restrict themselves to the characters [a-zA-Z0-9] plus the following punctuation: .,@#$%*
     * <p/>
     * <b>Note:</b> The use of slashes in type names is expressly prohibited.
     *
     * @return The unique type/identifier for this plugin
     */
    String getType();

    /**
     * Get a unique key. Rarely should custom key creation logic be utilized. Instead, use one of the
     * KeyUtils.getUniqueKey() variants to create a key.
     *
     * @return a globally unique key representing the object
     * @see KeyUtils
     */
    String getUniqueKey();
}
