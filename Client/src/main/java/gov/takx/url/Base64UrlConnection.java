package gov.takx.url;

import android.util.Base64;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * URL Connection for the {@code base64} protocol/scheme.  The scheme specific part is a base64 encoded byte array.
 *
 * @since 5.6
 */
public class Base64UrlConnection extends URLConnection
{
    private static final String BASE64_PREFIX = "base64://";

    private String base64ToDecode;

    /**
     * Constructs a URL connection to the specified URL.
     *
     * @param url the specified URL
     */
    public Base64UrlConnection(URL url)
    {
        super(url);
    }

    @Override
    public void connect() throws IOException
    {
        final String urlString = url.toString();
        if (!urlString.startsWith(BASE64_PREFIX)) throw new IOException("Malformed 'base64' URL: " + url);

        // Accommodate for using android-port's base64 class by ensuring contents conform to the 'URL_SAFE' flag.
        // The URL_SAFE flag expects the following replacements:
        //  '+'  -->  '-'
        //  '/'  -->  '_'
        String content = urlString.substring(BASE64_PREFIX.length());
        if (content.indexOf('+') > 0 || content.indexOf('/') > 0) content = content.replace('/', '_').replace('+', '-');

        base64ToDecode = content;
        connected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        connect();

        try
        {
            return new ByteArrayInputStream(Base64.decode(base64ToDecode, Base64.URL_SAFE));
        } catch (Exception e)
        {
            throw new IOException("Malformed 'base64' URL; can't decode bytes: " + url, e);
        }
    }
}
