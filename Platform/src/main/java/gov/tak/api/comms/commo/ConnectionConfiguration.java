package gov.tak.api.comms.commo;

import com.atakmap.comms.NetConnectString;
import gov.takx.platform.preferences.PreferencesNode;
import jakarta.annotation.Nonnull;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Class to describe a connection configuration to be used with the Common Commo library to listen to CoT messages on
 * specified address/port.
 *
 * @since 3.0
 */
public class ConnectionConfiguration extends PreferencesNode
{
    private final static String NAME_KEY = "name";
    private final static String URI_KEY = "uri";
    private final static String ENABLED_KEY = "enabled";

    private String displayName;
    private URI uri;
    private boolean enabled;

    /**
     * Create a Connection Configuration from a Preference Node.
     *
     * @param node Preference Node
     */
    public ConnectionConfiguration(@Nonnull PreferencesNode node)
    {
        super(node);

        displayName = node.get(NAME_KEY, name());

        try
        {
            uri = new URI(get(URI_KEY, ""));
            enabled = getBoolean(ENABLED_KEY, false);
        } catch (URISyntaxException e)
        {
            uri = null;
            enabled = false;
        }
    }

    public final String getDisplayName()
    {
        return displayName;
    }

    public void setDisplayName(@Nonnull String displayName)
    {
        this.displayName = displayName;
        put(NAME_KEY, displayName);
    }

    /**
     * Clear the display name (restoring it to {@link #name()} and remove the node map entry.
     * @since 5.4
     */
    public void clearDisplayName()
    {
        displayName = name();
        remove(NAME_KEY);
    }

    public final URI getUri()
    {
        return uri;
    }

    public void setUri(@Nonnull URI uri)
    {
        this.uri = uri;
        put(URI_KEY, uri.toString());
    }

    /**
     * Set the URI from component parts.
     *
     * @param protocol Protocol to use
     * @param host     Host address/name
     * @param port     Port number
     */
    public void setUri(@Nonnull String protocol, @Nonnull String host, int port)
    {
        setUri(URI.create(protocol.toLowerCase() + "://" + host + ":" + port));
    }

    public final boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(final boolean enabled)
    {
        put(ENABLED_KEY, this.enabled = enabled);
    }

    /**
     * @return The NetConnectString derived from the URI of this configuration
     * @since 3.1
     */
    public NetConnectString getNetConnectString()
    {
        return NetConnectString.fromString(uri.toString());
    }

    @Override
    public String toString()
    {
        return "ConnectionConfiguration{" +
                "displayName='" + displayName + '\'' +
                ", uri=" + uri +
                ", enabled=" + enabled +
                '}';
    }
}
