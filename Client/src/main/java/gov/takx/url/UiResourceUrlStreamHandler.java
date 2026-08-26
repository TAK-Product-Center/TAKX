package gov.takx.url;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

/**
 * URL stream handler for the {@code uiresource} URL protocol.
 *
 * @see UiResourceUrlConnection
 * @see gov.takx.platform.ui.UIResources
 * @see gov.takx.platform.ui.ResourceDirectory
 * @since 5.7.1
 */
public class UiResourceUrlStreamHandler extends URLStreamHandler
{
    @Override
    protected URLConnection openConnection(URL url)
    {
        return new UiResourceUrlConnection(url);
    }
}
