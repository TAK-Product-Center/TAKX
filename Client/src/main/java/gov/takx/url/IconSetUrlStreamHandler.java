package gov.takx.url;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URL stream handler for the {@code iconset} URL protocol.  URLs within the {@code iconset} protocol have either a two
 * or three part path: {@code [icon set UID]/[icon name]} or {@code [icon set UID]/[Group]/[icon name]}.  The Group
 * component is ignored by TAKX, but is required by other TAK EUDs in order to properly determine the icon.
 *
 * @see gov.tak.core.icons.IconSetManager
 * @since 5.2
 */
public class IconSetUrlStreamHandler extends URLStreamHandler
{
    @Override
    protected URLConnection openConnection(URL url)
    {
        return new IconSetUrlConnection(url);
    }
}
