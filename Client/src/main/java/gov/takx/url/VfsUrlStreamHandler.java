package gov.takx.url;

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;

import static gov.takx.url.VfsUrlConnection.GLOBAL;

/**
 * URL stream handler for the {@code vfs} URL protocol.  URLs within the {@code vfs} protocol can either refer
 * to files within Global or Project VFS.  To refer to Global VFS, the URL has the form {@code vfs:global:path/...},
 * without the {@code 'global:'} prefix, the path is assumed relative to the current Project's VFS.
 *
 * @since 5.1
 */
public class VfsUrlStreamHandler extends URLStreamHandler
{
    @Override
    protected URLConnection openConnection(URL url)
    {
        return new VfsUrlConnection(url);
    }

    /**
     * Special handling to support a path prefix indicating a reference to Global VSF vs. Project VFS.  E.g.,
     * {@code vfs:global:some/path/image.png} will be interpreted to be within Global VFS, whereas
     * {@code vfs:some/path/image.png} will be interpreted to be within the current project's VFS.  If the
     * {@code 'global:'} prefix is found, it is removed and the URL authority is set to {@code 'global:'}, and the
     * remainder is used as the relative path within VFS.
     * <p/>
     * {@inheritDoc}
     *
     * @see VfsUrlConnection
     */
    @Override
    protected void parseURL(URL u, String spec, int start, int limit)
    {
        final boolean global = spec.regionMatches(true, start, GLOBAL, 0, GLOBAL.length());

        final String auth = global ? GLOBAL : null;
        final String path = global ? spec.substring(start + GLOBAL.length()) : spec.substring(start);

        setURL(u, u.getProtocol(), null, 0, auth, null, path, null, null);
    }
}
