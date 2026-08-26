package gov.takx.core.globe.layers.worldwind;

import java.util.Base64;
import java.util.Objects;

/**
 * A container class for host, port, username and (optionally) a base-64 authentication HTTP authorization String.
 *
 * @since 1.43
 */
public class HTTPLoginInfo
{
    public final String url;
    public final String username;
    public final String authentication;

    public HTTPLoginInfo(String url, String username)
    {
        this(url, username, null);
    }

    public HTTPLoginInfo(String url, String username, String password)
    {
        this.url = url;
        this.username = username;

        // Encode login information for HTTP requests.
        // The username and password are base-64 authentication, but are not encrypted or secure.
        authentication = "Basic " + Base64.getEncoder().encodeToString((username + ":" + password).getBytes());
    }

    public boolean hasAuthentication()
    {
        return authentication != null;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        HTTPLoginInfo that = (HTTPLoginInfo) o;

        if (!url.equals(that.url)) return false;
        if (!username.equals(that.username)) return false;
        return Objects.equals(authentication, that.authentication);
    }

    @Override
    public int hashCode()
    {
        int result = url.hashCode();
        result = 31 * result + username.hashCode();
        result = 31 * result + (authentication != null ? authentication.hashCode() : 0);
        return result;
    }
}
