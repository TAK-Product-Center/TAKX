package gov.tak.api.location;

import jakarta.annotation.Nonnull;

/**
 * Contract for a configuration for a location provider.  The details of the configuration are intentionally opaque.
 *
 * @since 3.0
 */
public interface ILocationProviderConfiguration
{
    /**
     * @return {@code true} if this configuration is "empty", i.e., is in an "un-configured" state
     */
    boolean isEmpty();

    /**
     * @return String to be placed in program preferences for future restoration
     * @see ILocationProvider#setRestoredState(String)
     */
    @Nonnull
    String getRestorableState();

    /**
     * @return A human readable string showing the configuration
     */
    @Nonnull
    String getDisplayableString();
}
