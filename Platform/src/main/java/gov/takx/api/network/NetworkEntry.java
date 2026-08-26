package gov.takx.api.network;

import gov.tak.api.annotation.IncubatingApi;
import gov.takx.commons.utils.StringUtils;
import jakarta.annotation.Nonnull;

/**
 * Defines a network input or output. An {@link IllegalArgumentException} will be thrown the name, scheme or host is empty
 * or if the port is out of range.
 * 
 * @param name The display name and key
 * @param scheme The {@link Scheme}; only {@link Scheme#udp} is supported for outputs
 * @param host The host address
 * @param port The port
 * @param enabled {@code true} if the input or output should be enabled
 * @since 5.8
 */
@IncubatingApi(since = "5.8")
public record NetworkEntry(@Nonnull String name, @Nonnull Scheme scheme, @Nonnull String host, int port, boolean enabled)
{
    public enum Scheme
    {
        tcp,
        udp
    }

    public static final int MIN_PORT = 0;
    public static final int MAX_PORT = 65_535;

    public NetworkEntry {
        if (StringUtils.isEmpty(name)) throw new IllegalArgumentException("name is null or empty");
        if (StringUtils.isEmpty(host)) throw new IllegalArgumentException("host is null or empty");
        if (port < MIN_PORT || port > MAX_PORT) throw new IllegalArgumentException("Port out of range");
    }

    /**
     * Defines a network input or output that is enabled by default.
     *
     * @param name The display name and key
     * @param scheme The {@link Scheme}; only {@link Scheme#udp} is supported for outputs
     * @param host The host address
     * @param port The port
     */
    public NetworkEntry(@Nonnull String name, @Nonnull Scheme scheme, @Nonnull String host, int port)
    {
        this(name, scheme, host, port, true);
    }
}
