package gov.takx.url;

import gov.tak.api.icons.IIconSetManager;
import gov.takx.api.annotation.PseudoBean;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * URL Connection for the {@code iconset} protocol/scheme.  URLs within the {@code iconset} protocol have either a two
 * or three part path: {@code [icon set UID]/[icon name]} or {@code [icon set UID]/[Group]/[icon name]}.  The Group
 * component is ignored by TAKX, but is required by other TAK EUDs in order to properly determine the icon.
 *
 * @see gov.tak.core.icons.IconSetManager
 * @since 5.2
 */
@SuppressWarnings("CdiManagedBeanInconsistencyInspection")
@PseudoBean
public class IconSetUrlConnection extends URLConnection
{
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Inject
    private IIconSetManager iconSetManager;

    private String iconSetUid;
    private String iconName;

    /**
     * Constructs a URL connection to the specified URL.
     *
     * @param url the specified URL
     */
    public IconSetUrlConnection(URL url)
    {
        super(url);
    }

    @Override
    public void connect() throws IOException
    {
        // Find the icon set from the first part of the URL path, which is the icon set's UID.
        final Path path = Paths.get(URLDecoder.decode(url.getPath(), StandardCharsets.UTF_8));
        final int nameCount = path.getNameCount();
        if (nameCount != 2 && nameCount != 3) throw new IOException("Invalid URI path; incorrect path count: " + url);

        iconSetUid = path.getName(0).toString();
        iconName = path.getName(nameCount - 1).toString();

        logger.debug("connect to {}/{}", iconSetUid, iconName);
        connected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException
    {
        connect();

        final var userIcon = iconSetManager.getIcon(iconSetUid, iconName);
        if (userIcon == null) throw new IOException("Icon not found: " + url);

        return new ByteArrayInputStream(userIcon.getImageBytes());
    }
}
