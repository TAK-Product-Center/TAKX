package gov.tak.api.comms.security;

import gov.tak.api.comms.commo.StreamingConnectionConfiguration;
import gov.takx.api.annotation.NoPluginImplementations;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.net.ssl.SSLContext;

/**
 * Contract for a service to provide easy access to {@link SSLContext} instances based on provided
 * {@link StreamingConnectionConfiguration streaming configurations}.
 *
 * @since 3.5
 */
@NoPluginImplementations
public interface ISslContextProvider
{
    /**
     * Get or create the SSL context necessary to connect to host outlined in the streaming config.
     *
     * @param config Connection config to look up an instance based on
     *               the target {@link StreamingConnectionConfiguration#getUri() uri}.
     *
     * @return The {@link SSLContext} associated with the uri, if any. Otherwise {@code null}.
     */
    @Nullable
    SSLContext getSslContext(@Nonnull StreamingConnectionConfiguration config);

    /**
     * Used to check if a given connection configuration is viable to use with {@link #getSslContext(StreamingConnectionConfiguration)}.
     *
     * @param config Connection config to check for certificate information required for use in
     * {@link #getSslContext(StreamingConnectionConfiguration)}.
     *
     * @return {@code true} when the connection config has the appropriate certificate data to construct a {@link SSLContext}.
     * {@code false} when the connection config is missing certificate data.
     *
     * @since 5.2
     */
    boolean hasRequiredCertificates(@Nonnull StreamingConnectionConfiguration config);
}
