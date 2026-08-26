package gov.takx.url;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URL stream handler for the {@code base64} URL protocol.  The scheme specific part is a base64 encoded byte array.
 *
 * @since 5.6
 */
public class Base64UrlStreamHandler extends URLStreamHandler
{
    @Override
    protected URLConnection openConnection(URL url)
    {
        return new Base64UrlConnection(url);
    }
}
