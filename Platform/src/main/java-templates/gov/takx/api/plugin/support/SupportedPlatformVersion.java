package gov.takx.api.plugin.support;

import gov.takx.commons.utils.PlatformVersion;

/**
 * Constants to be used when specifying the minimum required platform version for a plugin.
 *
 * @see gov.takx.api.plugin.annotations.PluginSpecification#minimumSupportedVersion()
 * @since 2.0.0
 */
public enum SupportedPlatformVersion
{
${constants}
    ;

    private final PlatformVersion version;

    SupportedPlatformVersion(PlatformVersion version)
    {
        this.version = version;
    }

    /**
     * @return The platform version associated with this constant
     */
    public PlatformVersion getVersion()
    {
        return version;
    }
}
